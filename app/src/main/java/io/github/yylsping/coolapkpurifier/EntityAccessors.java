package io.github.yylsping.coolapkpurifier;

import java.lang.reflect.Method;

final class EntityAccessors {
    private final Method template;
    private final Method entityId;
    private final Method title;
    private final Method entityType;

    EntityAccessors(Method template, Method entityId, Method title, Method entityType) {
        this.template = template;
        this.entityId = entityId;
        this.title = title;
        this.entityType = entityType;
    }

    String readTemplate(Object instance) {
        return invoke(template, instance);
    }

    String readEntityId(Object instance) {
        return invoke(entityId, instance);
    }

    String readTitle(Object instance) {
        return invoke(title, instance);
    }

    String readEntityType(Object instance) {
        return invoke(entityType, instance);
    }

    private static String invoke(Method method, Object instance) {
        if (method == null) {
            return "";
        }
        try {
            Object value = method.invoke(instance);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }
}
