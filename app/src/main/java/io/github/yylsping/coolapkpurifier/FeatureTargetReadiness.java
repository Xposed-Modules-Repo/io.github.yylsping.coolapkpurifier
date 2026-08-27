package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure required-target policy shared by runtime readiness and JVM tests. */
final class FeatureTargetReadiness {
    private FeatureTargetReadiness() {
    }

    static List<String> missing(PurifierConfig config, int coolapkMajor,
                                Map<String, ResolvedTarget> targets,
                                FeatureInstallState installState) {
        List<String> missing = new ArrayList<>();
        requirePrimaryHook(config, coolapkMajor, targets, installState,
                PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND,
                TargetResolver.KEY_TOPIC_RECOMMEND, missing);
        requirePrimaryHook(config, coolapkMajor, targets, installState,
                PurifierConfig.Feature.DETAIL_SPONSOR,
                TargetResolver.KEY_DETAIL_SPONSOR, missing);
        requireSemanticEvidence(config, coolapkMajor, targets, installState,
                PurifierConfig.Feature.SAME_TOPIC_FEED,
                TargetResolver.KEY_SAME_TOPIC_FEED, missing);
        requireInstalledHook(config, coolapkMajor, installState,
                PurifierConfig.Feature.AUTO_COMMENT,
                TargetResolver.KEY_AUTO_COMMENT, false, missing);
        requireInstalledHook(config, coolapkMajor, installState,
                PurifierConfig.Feature.RELATED_DATA,
                TargetResolver.KEY_RELATED_DATA, true, missing);
        return missing;
    }

    private static void requirePrimaryHook(
            PurifierConfig config, int coolapkMajor,
            Map<String, ResolvedTarget> targets, FeatureInstallState installState,
            PurifierConfig.Feature feature, String key, List<String> missing) {
        if (!config.isEffectiveEnabled(feature, coolapkMajor)) {
            return;
        }
        ResolvedTarget target = targets.get(key);
        if (target == null || target.methodDescriptor == null
                || target.methodDescriptor.isEmpty()) {
            missing.add(key + ":descriptor");
        } else if (!installState.hasPrimaryHook(key)) {
            missing.add(key + ":primaryHook");
        }
    }

    private static void requireSemanticEvidence(
            PurifierConfig config, int coolapkMajor,
            Map<String, ResolvedTarget> targets, FeatureInstallState installState,
            PurifierConfig.Feature feature, String key, List<String> missing) {
        if (!config.isEffectiveEnabled(feature, coolapkMajor)) {
            return;
        }
        ResolvedTarget target = targets.get(key);
        if (target == null || target.methodDescriptor == null
                || target.methodDescriptor.isEmpty()) {
            missing.add(key + ":descriptor");
        } else if (!installState.hasSemanticEvidence(key)) {
            missing.add(key + ":semanticEvidence");
        }
    }

    private static void requireInstalledHook(
            PurifierConfig config, int coolapkMajor, FeatureInstallState installState,
            PurifierConfig.Feature feature, String key, boolean dedicatedHolder,
            List<String> missing) {
        if (!config.isEffectiveEnabled(feature, coolapkMajor)) {
            return;
        }
        boolean installed = dedicatedHolder
                ? installState.hasFallbackHook(key)
                : installState.hasPrimaryHook(key);
        if (!installed) {
            missing.add(key + (dedicatedHolder ? ":holderHook" : ":primaryHook"));
        }
    }
}
