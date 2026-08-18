package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TargetResolverTest {
    @Test
    public void feedKeysAcceptBaseAndIndexedForms() {
        assertTrue(TargetResolver.isFeedKey("feed"));
        assertTrue(TargetResolver.isFeedKey("feed#2"));
        assertTrue(TargetResolver.isFeedKey("feed#10"));
        assertFalse(TargetResolver.isFeedKey("feedback"));
        assertFalse(TargetResolver.isFeedKey("getter.title"));
        assertFalse(TargetResolver.isFeedKey(null));
    }

    @Test
    public void splashKeysAcceptBaseAndIndexedForms() {
        assertTrue(TargetResolver.isSplashKey("splash_base"));
        assertTrue(TargetResolver.isSplashKey("splash_base#2"));
        assertFalse(TargetResolver.isSplashKey("splash_screen"));
        assertFalse(TargetResolver.isSplashKey("feed"));
    }

    @Test
    public void indexedKeysKeepTheFirstEntryUnsuffixed() {
        assertEquals("feed", TargetResolver.indexedKey("feed", 0));
        assertEquals("feed#2", TargetResolver.indexedKey("feed", 1));
        assertEquals("splash_base#3", TargetResolver.indexedKey("splash_base", 2));
    }

    @Test
    public void legacySplashNamesCoverAllHistoricalVersions() {
        assertTrue(TargetResolver.LEGACY_SPLASH_CLASS_NAMES.contains(
                "com.coolapk.market.view.splash.SplashActivity"));
        assertTrue(TargetResolver.LEGACY_SPLASH_CLASS_NAMES.contains(
                "com.coolapk.market.view.splash.SplashAdActivity"));
        assertTrue(TargetResolver.LEGACY_SPLASH_CLASS_NAMES.contains(
                "com.coolapk.market.view.splash.FullScreenAdActivity"));
    }

    @Test
    public void withKeyPreservesTheRecordAndChangesOnlyTheKey() {
        ResolvedTarget base = new ResolvedTarget("feed", "dexkit", "Lcls;", "Lcls;->m()V");
        ResolvedTarget indexed = base.withKey("feed#2");

        assertEquals("feed#2", indexed.key);
        assertEquals(base.source, indexed.source);
        assertEquals(base.classDescriptor, indexed.classDescriptor);
        assertEquals(base.methodDescriptor, indexed.methodDescriptor);
        assertEquals("feed", base.key);
    }

    @Test
    public void indexedTargetsKeepDistinctKeysInMaps() {
        // Regression: both multi-feed entries used to serialize with key
        // "feed", so the cache reload dropped all but the last entry.
        java.util.Map<String, ResolvedTarget> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            String key = TargetResolver.indexedKey("feed", i);
            ResolvedTarget base = new ResolvedTarget("feed", "dexkit",
                    "Lc" + i + ";", "Lc" + i + ";->m()V");
            map.put(key, base.withKey(key));
        }

        assertEquals(3, map.size());
        assertEquals(3, map.values().stream().map(t -> t.key).distinct().count());
    }
}
