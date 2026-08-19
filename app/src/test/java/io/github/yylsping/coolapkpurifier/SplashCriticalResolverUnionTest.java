package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.coolapk.market.view.splash.SplashActivity;
import com.coolapk.market.view.splash.SplashAdActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * The splash tiers must form a genuine union: a non-empty strong result must
 * not short-circuit the weak scan or the legacy-name reflection, otherwise a
 * loadable SplashActivity is missed when the DexKit snapshot only contains
 * SplashAdActivity (Issue #3 splash leak).
 */
public final class SplashCriticalResolverUnionTest {
    private static final String SPLASH_ACTIVITY_D =
            DescriptorUtils.classDescriptorOf(SplashActivity.class);
    private static final String SPLASH_AD_ACTIVITY_D =
            DescriptorUtils.classDescriptorOf(SplashAdActivity.class);

    @Test
    public void strongHitStillUnionsTheLegacyReflectionTier() {
        List<ResolvedTarget> strong = new ArrayList<>();
        strong.add(splashTarget(SPLASH_AD_ACTIVITY_D,
                SplashCriticalResolver.SOURCE_FINGERPRINT_STRONG));

        List<ResolvedTarget> resolved = resolver(strong, new ArrayList<>()).resolve();

        assertEquals(2, resolved.size());
        Map<String, String> sourceByDescriptor = sourcesByKey(resolved);
        assertEquals(SplashCriticalResolver.SOURCE_FINGERPRINT_STRONG,
                sourceByDescriptor.get(SPLASH_AD_ACTIVITY_D));
        assertEquals(SplashCriticalResolver.SOURCE_LEGACY_NAME,
                sourceByDescriptor.get(SPLASH_ACTIVITY_D));
    }

    @Test
    public void weakTierAlsoUnionsInsteadOfBeingSkipped() {
        List<ResolvedTarget> weak = new ArrayList<>();
        weak.add(splashTarget(SPLASH_AD_ACTIVITY_D,
                SplashCriticalResolver.SOURCE_FINGERPRINT_WEAK));

        List<ResolvedTarget> resolved = resolver(new ArrayList<>(), weak).resolve();

        assertEquals(2, resolved.size());
        Map<String, String> sourceByDescriptor = sourcesByKey(resolved);
        assertEquals(SplashCriticalResolver.SOURCE_FINGERPRINT_WEAK,
                sourceByDescriptor.get(SPLASH_AD_ACTIVITY_D));
        assertEquals(SplashCriticalResolver.SOURCE_LEGACY_NAME,
                sourceByDescriptor.get(SPLASH_ACTIVITY_D));
    }

    @Test
    public void sameClassAcrossTiersIsDeduplicatedByDescriptor() {
        List<ResolvedTarget> strong = new ArrayList<>();
        strong.add(splashTarget(SPLASH_AD_ACTIVITY_D,
                SplashCriticalResolver.SOURCE_FINGERPRINT_STRONG));
        List<ResolvedTarget> weak = new ArrayList<>();
        weak.add(splashTarget(SPLASH_AD_ACTIVITY_D,
                SplashCriticalResolver.SOURCE_FINGERPRINT_WEAK));

        List<ResolvedTarget> resolved = resolver(strong, weak).resolve();

        assertEquals(2, resolved.size());
        // Strong wins the source label for the duplicated descriptor.
        assertEquals(SplashCriticalResolver.SOURCE_FINGERPRINT_STRONG,
                sourcesByKey(resolved).get(SPLASH_AD_ACTIVITY_D));
    }

    @Test
    public void emptyDexTiersStillResolveThroughLegacyNames() {
        List<ResolvedTarget> resolved = resolver(new ArrayList<>(), new ArrayList<>()).resolve();

        assertEquals(2, resolved.size());
        Map<String, String> sourceByDescriptor = sourcesByKey(resolved);
        assertEquals(SplashCriticalResolver.SOURCE_LEGACY_NAME,
                sourceByDescriptor.get(SPLASH_ACTIVITY_D));
        assertEquals(SplashCriticalResolver.SOURCE_LEGACY_NAME,
                sourceByDescriptor.get(SPLASH_AD_ACTIVITY_D));
    }

    private static SplashCriticalResolver resolver(List<ResolvedTarget> strong,
                                                   List<ResolvedTarget> weak) {
        return new SplashCriticalResolver(
                new FixedQueries(strong, weak),
                SplashCriticalResolverUnionTest.class.getClassLoader(),
                new ModuleLog(null));
    }

    private static ResolvedTarget splashTarget(String classDescriptor, String source) {
        return new ResolvedTarget(TargetResolver.KEY_SPLASH_BASE, source,
                classDescriptor, "");
    }

    private static Map<String, String> sourcesByKey(List<ResolvedTarget> resolved) {
        Map<String, String> byDescriptor = new LinkedHashMap<>();
        for (ResolvedTarget target : resolved) {
            byDescriptor.put(target.classDescriptor, target.source);
        }
        return byDescriptor;
    }

    private static final class FixedQueries implements SplashCriticalResolver.SplashQueries {
        private final List<ResolvedTarget> strong;
        private final List<ResolvedTarget> weak;

        FixedQueries(List<ResolvedTarget> strong, List<ResolvedTarget> weak) {
            this.strong = strong;
            this.weak = weak;
        }

        @Override
        public List<ResolvedTarget> queryStrong() {
            return new ArrayList<>(strong);
        }

        @Override
        public List<ResolvedTarget> queryWeak() {
            return new ArrayList<>(weak);
        }
    }

    /** Sanity guard for the fixtures themselves. */
    @Test
    public void fixturesAreDistinctSplashActivities() {
        assertTrue(android.app.Activity.class.isAssignableFrom(SplashActivity.class));
        assertTrue(android.app.Activity.class.isAssignableFrom(SplashAdActivity.class));
        assertTrue(TargetVerifier.findOnCreate(SplashActivity.class) != null);
        assertTrue(TargetVerifier.findOnCreate(SplashAdActivity.class) != null);
        assertTrue(!SplashActivity.class.equals(SplashAdActivity.class));
    }
}
