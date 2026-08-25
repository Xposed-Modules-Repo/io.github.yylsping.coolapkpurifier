package io.github.yylsping.coolapkpurifier;

import java.util.Locale;

final class EntityClassifier {
    private static final String SAME_TOPIC_TEMPLATE = "feedrecommendlistcard";
    enum Context {
        FEED,
        REPLY,
        DETAIL
    }

    private volatile EntityAccessors accessors;
    private volatile PurifierConfig config;
    private volatile int coolapkMajor = Integer.MAX_VALUE;
    private volatile boolean sameTopicSemanticVerified;

    EntityClassifier() {
    }

    void setAccessors(EntityAccessors accessors) {
        this.accessors = accessors;
    }

    void setConfig(PurifierConfig config) {
        this.config = config;
    }

    void setCoolapkMajor(int coolapkMajor) {
        this.coolapkMajor = coolapkMajor;
    }

    void setSameTopicSemanticVerified(boolean verified) {
        sameTopicSemanticVerified = verified;
    }

    boolean isSameTopicSemanticVerified() {
        return sameTopicSemanticVerified;
    }

    boolean isSponsored(Object entity) {
        if (entity == null) {
            return false;
        }
        EntityAccessors resolved = accessors;
        if (resolved == null) {
            // No verified getters: fail closed and keep the original list.
            return false;
        }
        String template = resolved.readTemplate(entity).toLowerCase(Locale.ROOT);
        String entityType = resolved.readEntityType(entity).toLowerCase(Locale.ROOT);

        // Only server-controlled structural metadata is authoritative. A
        // user-controlled title or an isolated entity id containing the word
        // "sponsor" must never be sufficient to delete ordinary content.
        boolean sponsored = template.contains("sponsor")
                || template.contains("feed_detail_reply_sponsor_card")
                || template.contains("nativead")
                || template.contains("advert")
                || template.endsWith("adcard")
                || "advertisement".equals(entityType)
                || "native_ad".equals(entityType);
        if (!sponsored) {
            return false;
        }
        return true;
    }

    boolean isSameTopicRecommendation(Object entity) {
        if (entity == null) {
            return false;
        }
        EntityAccessors resolved = accessors;
        return resolved != null && SAME_TOPIC_TEMPLATE.equals(
                resolved.readTemplate(entity).toLowerCase(Locale.ROOT));
    }

    /**
     * Applies the switch owned by the hook's known rendering context. Entity
     * strings identify sponsor content, but do not get to re-route a reply
     * card to the independent detail-promotion switch.
     */
    boolean shouldRemove(Object entity, Context context) {
        PurifierConfig current = config;
        if (context == Context.FEED && current != null
                && sameTopicSemanticVerified
                && current.isEffectiveEnabled(
                PurifierConfig.Feature.SAME_TOPIC_FEED, coolapkMajor)
                && isSameTopicRecommendation(entity)) {
            return true;
        }
        if (!isSponsored(entity)) {
            return false;
        }
        if (current == null) {
            return true;
        }
        switch (context) {
            case REPLY:
                return current.isEnabled(PurifierConfig.Feature.REPLY_SPONSOR);
            case DETAIL:
                return current.isEffectiveEnabled(
                        PurifierConfig.Feature.DETAIL_SPONSOR, coolapkMajor);
            case FEED:
            default:
                return current.isEnabled(PurifierConfig.Feature.FEED_SPONSOR);
        }
    }
}
