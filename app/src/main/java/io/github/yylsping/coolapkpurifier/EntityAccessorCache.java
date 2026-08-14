package io.github.yylsping.coolapkpurifier;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

final class EntityAccessorCache {
    private final ConcurrentHashMap<Class<?>, EntityAccessors> cache = new ConcurrentHashMap<>();

    EntityAccessors get(Class<?> type) {
        return cache.computeIfAbsent(type, EntityAccessorCache::resolve);
    }

    int size() {
        return cache.size();
    }

    private static EntityAccessors resolve(Class<?> type) {
        return new EntityAccessors(
                findZeroArgMethod(type, "getEntityTemplate"),
                findZeroArgMethod(type, "getEntityId"),
                findZeroArgMethod(type, "getTitle"),
                findZeroArgMethod(type, "getEntityType"));
    }

    private static Method findZeroArgMethod(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            if (method.getParameterTypes().length == 0) {
                return method;
            }
        } catch (Throwable ignored) {
            // Fall through to non-public and inherited declared methods.
        }

        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Method method = cursor.getDeclaredMethod(name);
                if (method.getParameterTypes().length == 0) {
                    try {
                        method.setAccessible(true);
                    } catch (Throwable ignored) {
                        // Invocation may still work for a package/public method.
                    }
                    return method;
                }
            } catch (Throwable ignored) {
                // Continue with the superclass and cache the eventual miss.
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }
}
