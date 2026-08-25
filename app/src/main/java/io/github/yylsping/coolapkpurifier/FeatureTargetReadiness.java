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
        if (coolapkMajor >= 15) {
            requirePrimaryHook(config, coolapkMajor, targets, installState,
                    PurifierConfig.Feature.SPLASH,
                    TargetResolver.KEY_SPLASH_DECISION, missing);
        }
        requirePrimaryHook(config, coolapkMajor, targets, installState,
                PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND,
                TargetResolver.KEY_TOPIC_RECOMMEND, missing);
        requirePrimaryHook(config, coolapkMajor, targets, installState,
                PurifierConfig.Feature.DETAIL_SPONSOR,
                TargetResolver.KEY_DETAIL_SPONSOR, missing);
        requireSemanticEvidence(config, coolapkMajor, targets, installState,
                PurifierConfig.Feature.SAME_TOPIC_FEED,
                TargetResolver.KEY_SAME_TOPIC_FEED, missing);
        requirePrimaryOrFallbackEvidence(config, coolapkMajor, installState,
                PurifierConfig.Feature.AUTO_COMMENT,
                TargetResolver.KEY_AUTO_COMMENT, missing);
        requireRelatedDataFallback(config, coolapkMajor, installState, missing);
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

    private static void requirePrimaryOrFallbackEvidence(
            PurifierConfig config, int coolapkMajor, FeatureInstallState installState,
            PurifierConfig.Feature feature, String key, List<String> missing) {
        if (!config.isEffectiveEnabled(feature, coolapkMajor)) {
            return;
        }
        if (!installState.hasPrimaryHook(key)
                && !installState.hasFallbackEvidence(key)) {
            missing.add(key + ":featureFallback");
        }
    }

    private static void requireRelatedDataFallback(
            PurifierConfig config, int coolapkMajor, FeatureInstallState installState,
            List<String> missing) {
        if (!config.isEffectiveEnabled(PurifierConfig.Feature.RELATED_DATA, coolapkMajor)) {
            return;
        }
        if (!installState.hasFallbackHook(TargetResolver.KEY_RELATED_DATA)
                && !installState.hasFallbackEvidence(TargetResolver.KEY_RELATED_DATA)) {
            missing.add(TargetResolver.KEY_RELATED_DATA + ":featureFallback");
        }
    }
}
