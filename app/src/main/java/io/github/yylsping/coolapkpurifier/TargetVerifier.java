package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

final class TargetVerifier {
    private TargetVerifier() {
    }

    /** Returns a failure reason or null when the cached target is usable. */
    static String verify(ResolvedTarget target, ClassLoader loader) {
        if (target == null) {
            return "null target";
        }
        if (target.key == null || target.key.isEmpty()) {
            return "empty key";
        }
        try {
            Class<?> type = DescriptorUtils.classForName(target.classDescriptor, loader);
            if (type == null) {
                return "class not loadable";
            }
            if (target.methodDescriptor != null && !target.methodDescriptor.isEmpty()) {
                Method method = DescriptorUtils.methodForDescriptor(target.methodDescriptor, loader);
                if (method == null) {
                    return "method not loadable";
                }
                switch (target.key) {
                    case TargetResolver.KEY_FEED:
                        return isFeedShape(method) && !Modifier.isAbstract(method.getModifiers())
                                ? null : "feed shape mismatch";
                    case TargetResolver.KEY_SPLASH_BASE:
                        return Activity.class.isAssignableFrom(type)
                                && "onCreate".equals(method.getName())
                                && method.getParameterTypes().length == 1
                                && method.getParameterTypes()[0] == Bundle.class
                                ? null : "splash shape mismatch";
                    default:
                        return method.getParameterTypes().length == 0
                                && method.getReturnType() == String.class
                                ? null : "getter shape mismatch";
                }
            }
            return null;
        } catch (Throwable throwable) {
            return String.valueOf(throwable);
        }
    }

    static boolean isFeedShape(Method method) {
        Class<?>[] params = method.getParameterTypes();
        return params.length == 2
                && List.class.isAssignableFrom(params[0])
                && params[1] == boolean.class
                && List.class.isAssignableFrom(method.getReturnType());
    }

    static Method findOnCreate(Class<?> type) {
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            try {
                Method method = cursor.getDeclaredMethod("onCreate", Bundle.class);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }
}
