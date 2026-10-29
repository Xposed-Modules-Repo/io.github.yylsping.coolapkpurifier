package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Migration is optional; host-private snapshots remain authoritative offline. */
public final class ModuleStorageBridgeTest {
    @org.junit.Rule public org.junit.rules.TemporaryFolder folder = new org.junit.rules.TemporaryFolder();

    @Test public void hostConfigurationWorksWhenFrameworkIsUnavailable() {
        FakeReadOnlyBridge remote = new FakeReadOnlyBridge(false);
        ModuleStorage storage = ModuleStorage.hostBackedForTest(remote);
        storage.attachFiles(folder.getRoot());
        PurifierConfig config = new PurifierConfig(storage.configStore(), storage.legacyConfiguration(), null);
        assertTrue(config.setEnabled(PurifierConfig.Feature.AUTO_COMMENT, true));
        assertTrue(new java.io.File(folder.getRoot(), PurifierConfig.FILE_NAME).isFile());
        assertEquals(0, storage.mutationGuard().snapshot().remoteWrites);
        assertTrue(storage.mutationGuard().snapshot().filesDirWrites >= 2);
    }

    @Test public void oldFrameworkConfigAndCacheImportWithoutRemoteWrites() {
        FakeReadOnlyBridge remote = new FakeReadOnlyBridge(true);
        ModuleStorage.MemoryBlobStore old = new ModuleStorage.MemoryBlobStore("old", new HostDataMutationGuard());
        PurifierConfig prior = new PurifierConfig(old, null, null);
        assertTrue(prior.setEnabled(PurifierConfig.Feature.SEARCH_HOT_WORDS, true));
        remote.seed(ModuleStorageContract.CONFIG_KEY, old.read());
        byte[] cache = "{\"schema\":4,\"entries\":[]}".getBytes(StandardCharsets.UTF_8);
        remote.seed(ModuleStorageContract.CACHE_KEY, cache);
        ModuleStorage storage = ModuleStorage.hostBackedForTest(remote);
        storage.attachFiles(folder.getRoot());
        PurifierConfig config = new PurifierConfig(storage.configStore(), storage.legacyConfiguration(), null);
        assertTrue(config.isEnabled(PurifierConfig.Feature.SEARCH_HOT_WORDS));
        assertArrayEquals(cache, storage.resolverCacheStore().read());
        assertEquals(0, storage.mutationGuard().snapshot().remoteWrites);
        FakeReadOnlyBridge offline = new FakeReadOnlyBridge(false);
        ModuleStorage next = ModuleStorage.hostBackedForTest(offline);
        next.attachFiles(folder.getRoot());
        PurifierConfig loaded = new PurifierConfig(next.configStore(), next.legacyConfiguration(), null);
        assertTrue(loaded.isEnabled(PurifierConfig.Feature.SEARCH_HOT_WORDS));
        assertArrayEquals(cache, next.resolverCacheStore().read());
        assertFalse(offline.attempted);
    }

    @Test
    public void snapshotGrammarExposesOnlyFixedConfigAndCacheKeys() {
        assertTrue(ModuleStorageContract.isValidKey(ModuleStorageContract.CONFIG_KEY));
        assertTrue(ModuleStorageContract.isValidKey(ModuleStorageContract.CACHE_KEY));
        assertFalse(ModuleStorageContract.isValidKey("arbitrary"));
        assertEquals(64 * 1024, ModuleStorageContract.maxBytesForKey(
                ModuleStorageContract.CONFIG_KEY));
        assertEquals(CachePolicy.MAX_TOTAL_BYTES, ModuleStorageContract.maxBytesForKey(
                ModuleStorageContract.CACHE_KEY));

        assertTrue(ModuleStorageContract.isValidWriteReason(
                ModuleStorageContract.CONFIG_KEY,
                "configToggle:" + PurifierConfig.Feature.SPLASH.key));
        assertFalse(ModuleStorageContract.isValidWriteReason(
                ModuleStorageContract.CONFIG_KEY, "configToggle:remove_unknown"));
        assertTrue(ModuleStorageContract.isValidWriteReason(
                ModuleStorageContract.CACHE_KEY, "cacheSaveTargets"));
        assertFalse(ModuleStorageContract.isValidWriteReason(
                ModuleStorageContract.CACHE_KEY, "configCreateDefaults"));

    }

    @Test
    public void cacheMigrationRejectsMalformedOrOversizedSnapshots() {
        assertTrue(ResolutionCache.isValidSnapshot(
                "{\"schema\":4,\"entries\":[]}".getBytes(StandardCharsets.UTF_8)));
        assertFalse(ResolutionCache.isValidSnapshot(
                "{\"schema\":3,\"entries\":[]}".getBytes(StandardCharsets.UTF_8)));
        assertFalse(ResolutionCache.isValidSnapshot(
                "{\"schema\":2,\"entries\":[]}".getBytes(StandardCharsets.UTF_8)));
        assertFalse(ResolutionCache.isValidSnapshot(
                "{not-json".getBytes(StandardCharsets.UTF_8)));
        assertFalse(ResolutionCache.isValidSnapshot(
                new byte[ModuleStorageContract.CACHE_MAX_BYTES + 1]));
        assertFalse(ResolutionCache.isValidSnapshot(
                "{\"schema\":4,\"entries\":[{},{},{},{},{},{}]}"
                        .getBytes(StandardCharsets.UTF_8)));
    }

    private static final class FakeReadOnlyBridge
            implements ModuleStorage.ReadOnlyRemoteBridge {
        private final boolean connectSuccess;
        private final Map<String, byte[]> values = new HashMap<>();
        private boolean available;
        private boolean attempted;

        FakeReadOnlyBridge(boolean connectSuccess) {
            this.connectSuccess = connectSuccess;
        }

        void seed(String key, byte[] value) {
            values.put(key, value.clone());
        }

        @Override
        public ModuleStorage.ProbeResult connect() {
            attempted = true;
            available = connectSuccess;
            return new ModuleStorage.ProbeResult(available,
                    available ? "fakeReadable" : "fakeUnavailable");
        }

        @Override
        public byte[] read(String key) {
            if (!available) return null;
            byte[] value = values.get(key);
            return value == null ? null : value.clone();
        }

        @Override public boolean isAvailable() { return available; }
        @Override public String backendName() {
            return available ? "fakeFrameworkReadOnly" : "fakeUnavailable";
        }
    }
}
