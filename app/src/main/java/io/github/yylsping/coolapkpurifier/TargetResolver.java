package io.github.yylsping.coolapkpurifier;

/** Shared target keys. The actual resolvers are split by startup priority. */
final class TargetResolver {
    static final String KEY_FEED = "feed";
    static final String KEY_SPLASH_BASE = "splash_base";
    static final String KEY_GETTER_TEMPLATE = "getter.entityTemplate";
    static final String KEY_GETTER_ENTITY_ID = "getter.entityId";
    static final String KEY_GETTER_TITLE = "getter.title";
    static final String KEY_GETTER_ENTITY_TYPE = "getter.entityType";

    private TargetResolver() {
    }
}
