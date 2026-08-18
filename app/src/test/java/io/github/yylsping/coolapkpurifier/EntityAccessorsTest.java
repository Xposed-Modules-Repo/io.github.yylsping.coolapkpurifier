package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Accessors must be rebuilt from the complete merged target map. A splash-only
 * increment used to replace the getter set with nulls and fail the classifier
 * closed until the normal resolver re-applied its targets.
 */
public final class EntityAccessorsTest {
    private static final String ENTITY_DESCRIPTOR =
            "L" + Entity.class.getName().replace('.', '/') + ";";

    @Test
    public void completeTargetsProduceCompleteAccessors() {
        Map<String, ResolvedTarget> targets = new HashMap<>();
        putGetter(targets, TargetResolver.KEY_GETTER_TEMPLATE, "getEntityTemplate");
        putGetter(targets, TargetResolver.KEY_GETTER_ENTITY_ID, "getEntityId");
        putGetter(targets, TargetResolver.KEY_GETTER_TITLE, "getTitle");
        putGetter(targets, TargetResolver.KEY_GETTER_ENTITY_TYPE, "getEntityType");

        EntityAccessors accessors =
                EntityAccessors.fromTargets(targets, getClass().getClassLoader());

        assertTrue(accessors.isComplete());
        Entity entity = new Entity("sponsorCard", "42", "hello", "feed");
        assertEquals("sponsorCard", accessors.readTemplate(entity));
        assertEquals("42", accessors.readEntityId(entity));
        assertEquals("hello", accessors.readTitle(entity));
        assertEquals("feed", accessors.readEntityType(entity));
    }

    @Test
    public void missingGetterLeavesAccessorsIncomplete() {
        Map<String, ResolvedTarget> targets = new HashMap<>();
        putGetter(targets, TargetResolver.KEY_GETTER_TEMPLATE, "getEntityTemplate");

        EntityAccessors accessors =
                EntityAccessors.fromTargets(targets, getClass().getClassLoader());

        assertFalse(accessors.isComplete());
    }

    @Test
    public void splashOnlyIncrementDoesNotCarryGetters() {
        // Documents the defect input: applying only the splash increment must
        // be combined with previously resolved targets by the caller, which
        // now passes the merged map instead of this increment.
        Map<String, ResolvedTarget> splashOnly = new HashMap<>();
        splashOnly.put(TargetResolver.KEY_SPLASH_BASE,
                new ResolvedTarget(TargetResolver.KEY_SPLASH_BASE, "dexkit",
                        ENTITY_DESCRIPTOR, ""));

        EntityAccessors accessors =
                EntityAccessors.fromTargets(splashOnly, getClass().getClassLoader());

        assertFalse(accessors.isComplete());
    }

    @Test
    public void descriptorParsingResolvesRealMethods() throws Exception {
        Method expected = Entity.class.getMethod("getEntityTemplate");
        Method resolved = DescriptorUtils.methodForDescriptor(
                ENTITY_DESCRIPTOR + "->getEntityTemplate()Ljava/lang/String;",
                getClass().getClassLoader());

        assertNotNull(resolved);
        assertEquals(expected, resolved);
    }

    private static void putGetter(Map<String, ResolvedTarget> targets, String key,
                                  String methodName) {
        targets.put(key, new ResolvedTarget(key, "fingerprint_strong",
                ENTITY_DESCRIPTOR,
                ENTITY_DESCRIPTOR + "->" + methodName + "()Ljava/lang/String;"));
    }

    public static final class Entity {
        private final String template;
        private final String id;
        private final String title;
        private final String type;

        Entity(String template, String id, String title, String type) {
            this.template = template;
            this.id = id;
            this.title = title;
            this.type = type;
        }

        public String getEntityTemplate() {
            return template;
        }

        public String getEntityId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getEntityType() {
            return type;
        }
    }
}
