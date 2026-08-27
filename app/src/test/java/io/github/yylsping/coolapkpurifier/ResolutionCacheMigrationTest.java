package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Schema 2 migration gate: a 2.1.0-era schema-1 cache (single positional
 * feed entry) must NEVER satisfy the new multi-target coverage READY
 * condition as a cache hit — the new resolver has to run instead. Entries
 * additionally carry a coverageSettled flag; only anchors-settled saves may
 * later finish READY straight from cache.
 */
public final class ResolutionCacheMigrationTest {
    private static final String IDENTITY_JSON = "{"
            + "\"package\":\"com.coolapk.market\","
            + "\"apkPath\":\"/data/app/base.apk\","
            + "\"apkSize\":12345,"
            + "\"signingHash\":\"abc\","
            + "\"token\":\"stable-token-1\","
            + "\"versionCode\":16551,"
            + "\"versionName\":\"16.5.1\"}";

    /**
     * JVM replacement for the Android atomic rename: Windows refuses to
     * rename onto an existing file, so tests substitute a replacing move —
     * exactly the injectable platform seam CacheAtomicWriter documents.
     */
    private static final CacheAtomicWriter.ReplaceOperation TEST_REPLACE =
            (temp, destination) -> {
                try {
                    java.nio.file.Files.move(temp.toPath(), destination.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            };

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void schemaOneRootAtCurrentPathIsNeverACacheHit() throws Exception {
        File filesDir = folder.newFolder("files1");
        writeRawCache(filesDir, "coolapk_purifier_cache_v4.json",
                schemaOneRootWithSingleFeed());
        TargetIdentity identity = TargetIdentity.fromJson(new JSONObject(IDENTITY_JSON));

        ResolutionCache.CachedResolution loaded =
                new ResolutionCache(filesDir, TEST_REPLACE).loadResolution(identity);

        assertTrue(loaded.targets.isEmpty());
        assertFalse(loaded.coverageSettled);
    }

    @Test
    public void saveAfterSchemaOneRootRewritesWithSchemaTwo() throws Exception {
        File filesDir = folder.newFolder("files2");
        writeRawCache(filesDir, "coolapk_purifier_cache_v4.json",
                schemaOneRootWithSingleFeed());
        TargetIdentity identity = TargetIdentity.fromJson(new JSONObject(IDENTITY_JSON));
        ResolutionCache cache = new ResolutionCache(filesDir, TEST_REPLACE);

        cache.saveTargets(identity, singleFeedTargets(), false);

        String raw = new String(Files.readAllBytes(
                new File(filesDir, "coolapk_purifier_cache_v4.json").toPath()),
                StandardCharsets.UTF_8);
        JSONObject root = new JSONObject(raw);
        assertEquals(2, root.getInt("schema"));
        assertFalse(root.getJSONArray("entries").getJSONObject(0)
                .getBoolean("coverageSettled"));
        assertEquals(singleFeedTargets().keySet(),
                new ResolutionCache(filesDir, TEST_REPLACE)
                        .loadResolution(identity).targets.keySet());
    }

    @Test
    public void settledFlagRoundTripsAndGatesCacheHitReadiness() throws Exception {
        File filesDir = folder.newFolder("files2b");
        TargetIdentity identity = TargetIdentity.fromJson(new JSONObject(IDENTITY_JSON));
        ResolutionCache cache = new ResolutionCache(filesDir, TEST_REPLACE);

        cache.saveTargets(identity, singleFeedTargets(), true);
        assertTrue(cache.loadResolution(identity).coverageSettled);

        // A second identity saved unsettled must not inherit the flag.
        TargetIdentity other = TargetIdentity.fromJson(new JSONObject(IDENTITY_JSON)
                .put("token", "stable-token-2").put("apkSize", 999));
        cache.saveTargets(other, singleFeedTargets(), false);
        assertFalse(cache.loadResolution(other).coverageSettled);
        assertTrue(cache.loadResolution(identity).coverageSettled);
    }

    @Test
    public void verifiedReplyClassSurvivesCacheRoundTripAlongsideCoreTargets() throws Exception {
        File filesDir = folder.newFolder("reply");
        TargetIdentity identity = TargetIdentity.fromJson(new JSONObject(IDENTITY_JSON));
        Map<String, ResolvedTarget> targets = singleFeedTargets();
        ResolvedTarget reply = new ResolvedTarget(TargetResolver.KEY_REPLY_HOLDER,
                "lazy_semantic_class", DescriptorUtils.classDescriptorOf(
                        com.coolapk.market.viewholder.MultiFeedReplyViewHolder.class), "");
        assertNull(TargetVerifier.verify(reply, getClass().getClassLoader()));
        targets.put(reply.key, reply);
        new ResolutionCache(filesDir, TEST_REPLACE).saveTargets(identity, targets, true);

        ResolutionCache.CachedResolution loaded =
                new ResolutionCache(filesDir, TEST_REPLACE).loadResolution(identity);
        assertTrue(loaded.coverageSettled);
        assertEquals(targets.keySet(), loaded.targets.keySet());
        ResolvedTarget cachedReply = loaded.targets.get(reply.key);
        assertEquals(reply.classDescriptor, cachedReply.classDescriptor);
        assertEquals("", cachedReply.methodDescriptor);
        assertNull(TargetVerifier.verify(cachedReply, getClass().getClassLoader()));
    }

    @Test
    public void entryWithoutSettledFlagFailsClosed() throws Exception {
        File filesDir = folder.newFolder("files2c");
        TargetIdentity identity = TargetIdentity.fromJson(new JSONObject(IDENTITY_JSON));
        // Hand-written v4 entry that predates the flag.
        writeRawCache(filesDir, "coolapk_purifier_cache_v4.json", "{"
                + "\"schema\":2,\"entries\":[{"
                + "\"identity\":" + IDENTITY_JSON + ","
                + "\"lastUsedAt\":1,"
                + "\"targets\":[" + feedTargetJson("feed", "LA;") + "]}]}");

        ResolutionCache.CachedResolution loaded =
                new ResolutionCache(filesDir, TEST_REPLACE).loadResolution(identity);

        assertFalse(loaded.targets.isEmpty());
        assertFalse(loaded.coverageSettled);
    }

    @Test
    public void legacyV3FileIsIgnoredEntirely() throws Exception {
        File filesDir = folder.newFolder("files3");
        String v3Content = schemaOneRootWithSingleFeed();
        writeRawCache(filesDir, "coolapk_purifier_cache_v3.json", v3Content);
        TargetIdentity identity = TargetIdentity.fromJson(new JSONObject(IDENTITY_JSON));
        ResolutionCache cache = new ResolutionCache(filesDir, TEST_REPLACE);

        assertTrue(cache.loadResolution(identity).targets.isEmpty());
        assertFalse(new File(filesDir, "coolapk_purifier_cache_v4.json").isFile());

        cache.saveTargets(identity, singleFeedTargets(), true);

        // The new file carries schema 2; the abandoned v3 file stays untouched.
        String v4 = new String(Files.readAllBytes(
                new File(filesDir, "coolapk_purifier_cache_v4.json").toPath()),
                StandardCharsets.UTF_8);
        assertEquals(2, new JSONObject(v4).getInt("schema"));
        assertEquals(v3Content, new String(Files.readAllBytes(
                new File(filesDir, "coolapk_purifier_cache_v3.json").toPath()),
                StandardCharsets.UTF_8));
    }

    private static Map<String, ResolvedTarget> singleFeedTargets() {
        Map<String, ResolvedTarget> fresh = new LinkedHashMap<>();
        fresh.put(TargetResolver.KEY_FEED, new ResolvedTarget(
                TargetResolver.KEY_FEED, "fingerprint_strong",
                "Lcom/coolapk/market/view/ad/EntityAdHelper;",
                "Lcom/coolapk/market/view/ad/EntityAdHelper;->a(Ljava/util/List;Z)Ljava/util/List;"));
        return fresh;
    }

    private static String feedTargetJson(String key, String classDescriptor) {
        return "{\"key\":\"" + key + "\",\"source\":\"fingerprint_strong\","
                + "\"class\":\"" + classDescriptor + "\","
                + "\"method\":\"" + classDescriptor
                + "->m(Ljava/util/List;Z)Ljava/util/List;\",\"at\":1}";
    }

    private static String schemaOneRootWithSingleFeed() {
        return "{\"schema\":1,\"entries\":[{"
                + "\"identity\":" + IDENTITY_JSON + ","
                + "\"lastUsedAt\":1,"
                + "\"targets\":[" + feedTargetJson("feed",
                        "Lcom/coolapk/market/view/ad/EntityAdHelper;") + "]}]}";
    }

    private static void writeRawCache(File filesDir, String fileName, String content)
            throws Exception {
        Files.write(new File(filesDir, fileName).toPath(),
                content.getBytes(StandardCharsets.UTF_8));
    }
}
