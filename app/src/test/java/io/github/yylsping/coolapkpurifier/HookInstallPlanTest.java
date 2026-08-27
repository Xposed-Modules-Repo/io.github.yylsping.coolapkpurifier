package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.EnumMap;

public final class HookInstallPlanTest {
    @Test
    public void defaultConfigurationUsesOnlyTemporaryReplyDiscovery() {
        EnumMap<PurifierConfig.Feature, Boolean> enabled = defaults();
        HookInstallPlan plan = HookInstallPlan.from(enabled, 16);

        assertTrue(plan.installClassLoader);
        assertTrue(plan.resolveReplyHolder);
        assertFalse(plan.resolveAutoComment);
        assertFalse(plan.resolveDetailSponsor);
    }

    @Test
    public void onlyT1InstallsItsLazyResolver() {
        HookInstallPlan plan = only(PurifierConfig.Feature.AUTO_COMMENT);
        assertTrue(plan.installClassLoader);
        assertTrue(plan.resolveAutoComment);
        assertFalse(plan.resolveTopicRecommend);
    }

    @Test
    public void onlyT2InstallsSemanticResolver() {
        HookInstallPlan plan = only(PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND);
        assertTrue(plan.installClassLoader);
        assertTrue(plan.resolveTopicRecommend);
    }

    @Test
    public void onlyT3InstallsDedicatedHolderDiscovery() {
        HookInstallPlan plan = only(PurifierConfig.Feature.RELATED_DATA);
        assertTrue(plan.installClassLoader);
        assertTrue(plan.resolveRelatedData);
        assertFalse(plan.resolveDetailSponsor);
    }

    @Test
    public void onlyT4NeedsNoGlobalClassLoaderHook() {
        HookInstallPlan plan = only(PurifierConfig.Feature.SAME_TOPIC_FEED);
        assertFalse(plan.installClassLoader);
        assertTrue(plan.resolveSameTopicFeed);
    }

    @Test
    public void onlyT5InstallsItsBusinessHooks() {
        HookInstallPlan plan = only(PurifierConfig.Feature.DETAIL_SPONSOR);
        assertTrue(plan.installClassLoader);
        assertTrue(plan.resolveDetailSponsor);
        assertFalse(plan.resolveRelatedData);
    }

    @Test
    public void allIssue2FeaturesInstallTheCompletePlan() {
        EnumMap<PurifierConfig.Feature, Boolean> enabled = allOff();
        for (PurifierConfig.Feature feature : PurifierConfig.Feature.values()) {
            if (feature.requiresCoolapk15) {
                enabled.put(feature, true);
            }
        }
        HookInstallPlan plan = HookInstallPlan.from(enabled, 16);
        assertTrue(plan.installClassLoader);
        assertTrue(plan.resolveAutoComment);
        assertTrue(plan.resolveTopicRecommend);
        assertTrue(plan.resolveRelatedData);
        assertTrue(plan.resolveSameTopicFeed);
        assertTrue(plan.resolveDetailSponsor);
    }

    @Test
    public void issue2SelectionsRemainInactiveBelowCoolapk15() {
        EnumMap<PurifierConfig.Feature, Boolean> enabled = allOff();
        enabled.put(PurifierConfig.Feature.DETAIL_SPONSOR, true);
        HookInstallPlan plan = HookInstallPlan.from(enabled, 14);
        assertFalse(plan.installClassLoader);
    }

    private static HookInstallPlan only(PurifierConfig.Feature feature) {
        EnumMap<PurifierConfig.Feature, Boolean> enabled = allOff();
        enabled.put(feature, true);
        return HookInstallPlan.from(enabled, 16);
    }

    private static EnumMap<PurifierConfig.Feature, Boolean> defaults() {
        EnumMap<PurifierConfig.Feature, Boolean> enabled = allOff();
        for (PurifierConfig.Feature feature : PurifierConfig.Feature.values()) {
            enabled.put(feature, feature.defaultEnabled);
        }
        return enabled;
    }

    private static EnumMap<PurifierConfig.Feature, Boolean> allOff() {
        EnumMap<PurifierConfig.Feature, Boolean> enabled =
                new EnumMap<>(PurifierConfig.Feature.class);
        for (PurifierConfig.Feature feature : PurifierConfig.Feature.values()) {
            enabled.put(feature, false);
        }
        return enabled;
    }
}
