package io.github.yylsping.coolapkpurifier;

import java.util.Locale;

final class EntityClassifier {
    private final EntityAccessorCache accessorCache;

    EntityClassifier(EntityAccessorCache accessorCache) {
        this.accessorCache = accessorCache;
    }

    boolean isSponsored(Object entity) {
        if (entity == null) {
            return false;
        }
        EntityAccessors accessors = accessorCache.get(entity.getClass());
        String template = accessors.readTemplate(entity).toLowerCase(Locale.ROOT);
        String entityId = accessors.readEntityId(entity).toLowerCase(Locale.ROOT);
        String title = accessors.readTitle(entity).toLowerCase(Locale.ROOT);
        String entityType = accessors.readEntityType(entity).toLowerCase(Locale.ROOT);

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
