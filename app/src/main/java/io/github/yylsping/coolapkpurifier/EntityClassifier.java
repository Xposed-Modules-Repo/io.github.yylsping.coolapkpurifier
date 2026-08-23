package io.github.yylsping.coolapkpurifier;

import java.util.Locale;

final class EntityClassifier {
    private volatile EntityAccessors accessors;
    private volatile PurifierConfig config;

    EntityClassifier() {
    }

    void setAccessors(EntityAccessors accessors) {
        this.accessors = accessors;
    }

    void setConfig(PurifierConfig config) {
        this.config = config;
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
        String entityId = resolved.readEntityId(entity).toLowerCase(Locale.ROOT);
        String title = resolved.readTitle(entity).toLowerCase(Locale.ROOT);
        String entityType = resolved.readEntityType(entity).toLowerCase(Locale.ROOT);

        String fingerprint = template + '\n' + entityId + '\n' + title;
        boolean sponsored = fingerprint.contains("sponsor")
                || fingerprint.contains("feed_detail_reply_sponsor_card")
                || template.contains("nativead")
                || template.contains("advert")
                || template.endsWith("adcard")
                || "advertisement".equals(entityType)
                || "native_ad".equals(entityType);
        if (!sponsored) {
            return false;
        }
        PurifierConfig current = config;
        if (current == null) {
            return true;
        }
        if (fingerprint.contains("feed_detail_reply_sponsor_card")
                || template.contains("detailsponsor")) {
            return current.isEnabled(PurifierConfig.Feature.DETAIL_SPONSOR);
        }
        if (template.contains("reply") || template.contains("comment")) {
            return current.isEnabled(PurifierConfig.Feature.REPLY_SPONSOR);
        }
        return current.isEnabled(PurifierConfig.Feature.FEED_SPONSOR);
    }
}
