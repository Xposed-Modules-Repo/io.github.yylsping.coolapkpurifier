package io.github.yylsping.coolapkpurifier;

import java.util.EnumMap;
import java.util.Map;

/** Immutable startup plan for optional framework fallbacks and semantic resolvers. */
final class HookInstallPlan {
    final boolean installLayoutInflater;
    final boolean installViewTag;
    final boolean installClassLoader;
    final boolean resolveReplyHolder;
    final boolean resolveAutoComment;
    final boolean resolveTopicRecommend;
    final boolean resolveRelatedData;
    final boolean resolveSameTopicFeed;
    final boolean resolveDetailSponsor;

    static HookInstallPlan from(PurifierConfig config, int coolapkMajor) {
        EnumMap<PurifierConfig.Feature, Boolean> effective =
                new EnumMap<>(PurifierConfig.Feature.class);
        for (PurifierConfig.Feature feature : PurifierConfig.Feature.values()) {
            effective.put(feature, config.isEffectiveEnabled(feature, coolapkMajor));
        }
        return from(effective, coolapkMajor);
    }

    static HookInstallPlan from(Map<PurifierConfig.Feature, Boolean> enabled,
                                int coolapkMajor) {
        boolean reply = enabled(enabled, PurifierConfig.Feature.REPLY_SPONSOR);
        boolean auto = issueEnabled(enabled, PurifierConfig.Feature.AUTO_COMMENT,
                coolapkMajor);
        boolean topic = issueEnabled(enabled,
                PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND, coolapkMajor);
        boolean related = issueEnabled(enabled, PurifierConfig.Feature.RELATED_DATA,
                coolapkMajor);
        boolean sameTopic = issueEnabled(enabled,
                PurifierConfig.Feature.SAME_TOPIC_FEED, coolapkMajor);
        boolean detail = issueEnabled(enabled, PurifierConfig.Feature.DETAIL_SPONSOR,
                coolapkMajor);
        return new HookInstallPlan(
                auto || topic || related || sameTopic || detail,
                related || sameTopic || detail,
                reply || auto || topic || related || detail,
                reply, auto, topic, related, sameTopic, detail);
    }

    private HookInstallPlan(boolean installLayoutInflater, boolean installViewTag,
                            boolean installClassLoader, boolean resolveReplyHolder,
                            boolean resolveAutoComment, boolean resolveTopicRecommend,
                            boolean resolveRelatedData, boolean resolveSameTopicFeed,
                            boolean resolveDetailSponsor) {
        this.installLayoutInflater = installLayoutInflater;
        this.installViewTag = installViewTag;
        this.installClassLoader = installClassLoader;
        this.resolveReplyHolder = resolveReplyHolder;
        this.resolveAutoComment = resolveAutoComment;
        this.resolveTopicRecommend = resolveTopicRecommend;
        this.resolveRelatedData = resolveRelatedData;
        this.resolveSameTopicFeed = resolveSameTopicFeed;
        this.resolveDetailSponsor = resolveDetailSponsor;
    }

    boolean needsSemanticFeature(PurifierConfig.Feature feature) {
        if (feature == PurifierConfig.Feature.REPLY_SPONSOR) {
            return resolveReplyHolder;
        }
        if (feature == PurifierConfig.Feature.AUTO_COMMENT) {
            return resolveAutoComment;
        }
        if (feature == PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND) {
            return resolveTopicRecommend;
        }
        if (feature == PurifierConfig.Feature.RELATED_DATA) {
            return resolveRelatedData;
        }
        if (feature == PurifierConfig.Feature.SAME_TOPIC_FEED) {
            return resolveSameTopicFeed;
        }
        if (feature == PurifierConfig.Feature.DETAIL_SPONSOR) {
            return resolveDetailSponsor;
        }
        return false;
    }

    private static boolean issueEnabled(Map<PurifierConfig.Feature, Boolean> enabled,
                                        PurifierConfig.Feature feature,
                                        int coolapkMajor) {
        return coolapkMajor >= 15 && enabled(enabled, feature);
    }

    private static boolean enabled(Map<PurifierConfig.Feature, Boolean> enabled,
                                   PurifierConfig.Feature feature) {
        return Boolean.TRUE.equals(enabled.get(feature));
    }
}
