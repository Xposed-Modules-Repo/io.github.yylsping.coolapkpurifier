package io.github.yylsping.coolapkpurifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Shared target keys. The actual resolvers are split by startup priority. */
final class TargetResolver {
    static final String KEY_FEED = "feed";
    static final String KEY_SPLASH_BASE = "splash_base";
    static final String KEY_GETTER_TEMPLATE = "getter.entityTemplate";
    static final String KEY_GETTER_ENTITY_ID = "getter.entityId";
    static final String KEY_GETTER_TITLE = "getter.title";
    static final String KEY_GETTER_ENTITY_TYPE = "getter.entityType";

    /**
     * Historically observed splash-family activity names. Used as a reflection
     * fallback so known versions keep splash coverage even when the DexKit
     * fingerprint tiers find nothing (e.g. obfuscated source metadata).
     */
    static final Set<String> LEGACY_SPLASH_CLASS_NAMES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "com.coolapk.market.view.splash.SplashActivity",
                    "com.coolapk.market.view.splash.SplashAdActivity",
                    "com.coolapk.market.view.splash.FullScreenAdActivity")));

    private TargetResolver() {
    }

    /** Feed entries may be suffixed (feed#2, feed#3...) for multi-method coverage. */
    static boolean isFeedKey(String key) {
        return key != null && (key.equals(KEY_FEED) || key.startsWith(KEY_FEED + "#"));
    }

    /** Splash entries may be suffixed (splash_base#2...) for multi-activity coverage. */
    static boolean isSplashKey(String key) {
        return key != null
                && (key.equals(KEY_SPLASH_BASE) || key.startsWith(KEY_SPLASH_BASE + "#"));
    }

    static String indexedKey(String base, int index) {
        return index == 0 ? base : base + "#" + (index + 1);
    }
}
