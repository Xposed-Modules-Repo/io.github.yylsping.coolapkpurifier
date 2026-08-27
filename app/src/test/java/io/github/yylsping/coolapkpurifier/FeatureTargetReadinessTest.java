package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class FeatureTargetReadinessTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void enabledDetailSponsorWithoutTargetReportsDescriptor() throws Exception {
        List<String> missing = FeatureTargetReadiness.missing(configWithDetail(true), 16,
                Collections.emptyMap(), new FeatureInstallState());

        assertTrue(missing.contains(TargetResolver.KEY_DETAIL_SPONSOR + ":descriptor"));
    }

    @Test
    public void detailFallbackCannotMasqueradeAsPrimaryHook() throws Exception {
        FeatureInstallState state = new FeatureInstallState();
        state.markFallbackHook(TargetResolver.KEY_DETAIL_SPONSOR);

        List<String> missing = FeatureTargetReadiness.missing(configWithDetail(true), 16,
                detailTargets(), state);

        assertTrue(missing.contains(TargetResolver.KEY_DETAIL_SPONSOR + ":primaryHook"));
    }

    @Test
    public void primaryDetailHookAllowsReadyWithoutFallback() throws Exception {
        FeatureInstallState state = new FeatureInstallState();
        state.markPrimaryHook(TargetResolver.KEY_DETAIL_SPONSOR);

        assertFalse(FeatureTargetReadiness.missing(configWithDetail(true), 16,
                detailTargets(), state).stream()
                .anyMatch(value -> value.startsWith(TargetResolver.KEY_DETAIL_SPONSOR)));
    }

    @Test
    public void oldGenerationDetailPrimaryCannotSatisfyNewGeneration() throws Exception {
        FeatureInstallState state = new FeatureInstallState();
        state.beginGeneration(1);
        state.markPrimaryHook(1, TargetResolver.KEY_DETAIL_SPONSOR);
        state.beginGeneration(2);

        assertTrue(FeatureTargetReadiness.missing(configWithDetail(true), 16,
                detailTargets(), state).contains(
                TargetResolver.KEY_DETAIL_SPONSOR + ":primaryHook"));
    }

    @Test
    public void disabledDetailSponsorNeedsNeitherPrimaryNorFallback() throws Exception {
        assertFalse(FeatureTargetReadiness.missing(configWithDetail(false), 16,
                Collections.emptyMap(), new FeatureInstallState()).stream()
                .anyMatch(value -> value.startsWith(TargetResolver.KEY_DETAIL_SPONSOR)));
    }

    @Test
    public void invalidDetailDescriptorStillFailsWhenFallbackExists() throws Exception {
        FeatureInstallState state = new FeatureInstallState();
        state.markFallbackHook(TargetResolver.KEY_DETAIL_SPONSOR);
        Map<String, ResolvedTarget> targets = new LinkedHashMap<>();
        targets.put(TargetResolver.KEY_DETAIL_SPONSOR, new ResolvedTarget(
                TargetResolver.KEY_DETAIL_SPONSOR, "cache", "LFeed;", ""));

        assertTrue(FeatureTargetReadiness.missing(configWithDetail(true), 16,
                targets, state).contains(
                TargetResolver.KEY_DETAIL_SPONSOR + ":descriptor"));
    }

    @Test
    public void fallbackNeverSatisfiesAnotherSemanticPrimary() throws Exception {
        PurifierConfig config = configWithDetail(false);
        config.setEnabled(PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND, true);
        FeatureInstallState state = new FeatureInstallState();
        state.markFallbackHook(TargetResolver.KEY_TOPIC_RECOMMEND);
        Map<String, ResolvedTarget> targets = new LinkedHashMap<>();
        targets.put(TargetResolver.KEY_TOPIC_RECOMMEND, new ResolvedTarget(
                TargetResolver.KEY_TOPIC_RECOMMEND, "test", "LTopic;",
                "LTopic;->entry()V"));

        assertTrue(FeatureTargetReadiness.missing(config, 16, targets, state)
                .contains(TargetResolver.KEY_TOPIC_RECOMMEND + ":primaryHook"));
    }

    @Test
    public void sameTopicRequiresEvidenceRatherThanPrimaryHook() throws Exception {
        PurifierConfig config = configWithDetail(false);
        config.setEnabled(PurifierConfig.Feature.SAME_TOPIC_FEED, true);
        Map<String, ResolvedTarget> targets = new LinkedHashMap<>();
        targets.put(TargetResolver.KEY_SAME_TOPIC_FEED, new ResolvedTarget(
                TargetResolver.KEY_SAME_TOPIC_FEED, "test", "LFeedList;",
                "LFeedList;->predicate(Ljava/lang/Object;)Z"));
        FeatureInstallState wrongState = new FeatureInstallState();
        wrongState.markPrimaryHook(TargetResolver.KEY_SAME_TOPIC_FEED);

        assertTrue(FeatureTargetReadiness.missing(config, 16, targets, wrongState)
                .contains(TargetResolver.KEY_SAME_TOPIC_FEED + ":semanticEvidence"));

        FeatureInstallState verified = new FeatureInstallState();
        verified.markSemanticEvidence(TargetResolver.KEY_SAME_TOPIC_FEED);
        assertFalse(FeatureTargetReadiness.missing(config, 16, targets, verified).stream()
                .anyMatch(value -> value.startsWith(TargetResolver.KEY_SAME_TOPIC_FEED)));
    }

    @Test
    public void pre15UsesLegacySplashFallbackWithoutDecisionTarget() throws Exception {
        PurifierConfig config = configWithDetail(false);
        config.setEnabled(PurifierConfig.Feature.SPLASH, true);

        assertTrue(FeatureTargetReadiness.missing(config, 14,
                Collections.emptyMap(), new FeatureInstallState()).isEmpty());
    }

    @Test
    public void currentCoolapkUsesActivitySplashBoundaryWithoutDecisionTarget()
            throws Exception {
        PurifierConfig config = configWithDetail(false);
        config.setEnabled(PurifierConfig.Feature.SPLASH, true);

        assertTrue(FeatureTargetReadiness.missing(config, 16,
                Collections.emptyMap(), new FeatureInstallState()).isEmpty());
    }

    @Test
    public void autoCommentRequiresItsBusinessHook() throws Exception {
        PurifierConfig config = configWithDetail(false);
        config.setEnabled(PurifierConfig.Feature.AUTO_COMMENT, true);
        FeatureInstallState missingState = new FeatureInstallState();
        assertTrue(FeatureTargetReadiness.missing(config, 16,
                Collections.emptyMap(), missingState).contains(
                TargetResolver.KEY_AUTO_COMMENT + ":primaryHook"));

        FeatureInstallState verified = new FeatureInstallState();
        verified.markPrimaryHook(TargetResolver.KEY_AUTO_COMMENT);
        assertFalse(FeatureTargetReadiness.missing(config, 16,
                Collections.emptyMap(), verified).stream().anyMatch(
                value -> value.startsWith(TargetResolver.KEY_AUTO_COMMENT)));
    }

    @Test
    public void relatedDataRequiresItsDedicatedHolderHook() throws Exception {
        PurifierConfig config = configWithDetail(false);
        config.setEnabled(PurifierConfig.Feature.RELATED_DATA, true);
        FeatureInstallState missingState = new FeatureInstallState();
        assertTrue(FeatureTargetReadiness.missing(config, 16,
                Collections.emptyMap(), missingState).contains(
                TargetResolver.KEY_RELATED_DATA + ":holderHook"));

        FeatureInstallState holder = new FeatureInstallState();
        holder.markFallbackHook(TargetResolver.KEY_RELATED_DATA);
        assertFalse(FeatureTargetReadiness.missing(config, 16,
                Collections.emptyMap(), holder).stream().anyMatch(
                value -> value.startsWith(TargetResolver.KEY_RELATED_DATA)));

        FeatureInstallState primaryOnly = new FeatureInstallState();
        primaryOnly.markPrimaryHook(TargetResolver.KEY_RELATED_DATA);
        assertTrue(FeatureTargetReadiness.missing(config, 16,
                Collections.emptyMap(), primaryOnly).contains(
                TargetResolver.KEY_RELATED_DATA + ":holderHook"));
    }

    private Map<String, ResolvedTarget> detailTargets() {
        Map<String, ResolvedTarget> targets = new LinkedHashMap<>();
        targets.put(TargetResolver.KEY_DETAIL_SPONSOR, new ResolvedTarget(
                TargetResolver.KEY_DETAIL_SPONSOR, "model", "LFeed;",
                "LFeed;->getDetailSponsorCard()LEntity;"));
        return targets;
    }

    @Test
    public void relatedGetterNeedsBothDescriptorAndCurrentPrimaryHook() throws Exception {
        PurifierConfig config = configWithDetail(false);
        config.setEnabled(PurifierConfig.Feature.RELATED_DATA, true);
        FeatureInstallState state = new FeatureInstallState();
        state.beginGeneration(1);
        Map<String, ResolvedTarget> targets = new LinkedHashMap<>();
        String descriptor = DescriptorUtils.classDescriptorOf(
                com.coolapk.market.model.AutoValueFeed.class);
        targets.put(TargetResolver.KEY_RELATED_DATA, new ResolvedTarget(
                TargetResolver.KEY_RELATED_DATA, "model", descriptor,
                descriptor + "->getRelatedData()Ljava/util/List;"));
        assertTrue(FeatureTargetReadiness.missing(config, 16, targets, state).stream()
                .anyMatch(value -> value.startsWith(TargetResolver.KEY_RELATED_DATA)));
        state.markPrimaryHook(1, TargetResolver.KEY_RELATED_DATA);
        assertTrue(FeatureTargetReadiness.missing(config, 16, targets, state).isEmpty());
        state.beginGeneration(2);
        assertTrue(FeatureTargetReadiness.missing(config, 16, targets, state).stream()
                .anyMatch(value -> value.startsWith(TargetResolver.KEY_RELATED_DATA)));
    }

    private PurifierConfig configWithDetail(boolean enabled) throws Exception {
        PurifierConfig config = new PurifierConfig(folder.newFolder(),
                (temp, destination) -> {
                    try {
                        Files.copy(temp.toPath(), destination.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                        return true;
                    } catch (Throwable ignored) {
                        return false;
                    }
                }, null);
        config.setEnabled(PurifierConfig.Feature.SPLASH, false);
        config.setEnabled(PurifierConfig.Feature.DETAIL_SPONSOR, enabled);
        return config;
    }
}
