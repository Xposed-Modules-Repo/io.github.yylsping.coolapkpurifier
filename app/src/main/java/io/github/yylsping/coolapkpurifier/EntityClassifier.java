package io.github.yylsping.coolapkpurifier;

import java.util.Locale;

final class EntityClassifier {
    private volatile EntityAccessors accessors;

    EntityClassifier() {
    }

    void setAccessors(EntityAccessors accessors) {
        this.accessors = accessors;
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
        return fingerprint.contains("sponsor")
                || fingerprint.contains("feed_detail_reply_sponsor_card")
                || template.contains("nativead")
                || template.contains("advert")
                || template.endsWith("adcard")
                || "advertisement".equals(entityType)
                || "native_ad".equals(entityType);
    }
}
