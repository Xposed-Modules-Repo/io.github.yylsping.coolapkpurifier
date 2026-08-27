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
            // Splash targets may legitimately carry an empty method descriptor;
            // the real onCreate is located at install time via findOnCreate.
            // Dedicated renderer targets are class-only records: the actual
            // suppression uses their stable layout resource names.
            if (target.methodDescriptor == null || target.methodDescriptor.isEmpty()) {
                if (TargetResolver.isSplashKey(target.key)
                        && Activity.class.isAssignableFrom(type)) {
                    return null;
                }
                return isVerifiedFeatureClass(target.key, type)
                        ? null : "empty method descriptor";
            }
            Method method = DescriptorUtils.methodForDescriptor(target.methodDescriptor, loader);
            if (method == null) {
                return "method not loadable";
            }
            switch (keyKind(target.key)) {
                case TargetResolver.KEY_FEED:
                    return isFeedShape(method) && !Modifier.isAbstract(method.getModifiers())
                            ? null : "feed shape mismatch";
                case TargetResolver.KEY_SPLASH_BASE:
                    return Activity.class.isAssignableFrom(type)
                            && "onCreate".equals(method.getName())
                            && method.getParameterTypes().length == 1
                            && method.getParameterTypes()[0] == Bundle.class
                            ? null : "splash shape mismatch";
                case TargetResolver.KEY_AUTO_COMMENT:
                    return isAutoCommentEntry(method)
                            ? null : "auto comment shape mismatch";
                case TargetResolver.KEY_TOPIC_RECOMMEND:
                    return isTopicRecommendMethod(method)
                            ? null : "topic recommend shape mismatch";
                case TargetResolver.KEY_DETAIL_SPONSOR:
                    return isDetailSponsorGetter(method)
                            ? null : "detail sponsor getter shape mismatch";
                case TargetResolver.KEY_RELATED_DATA:
                    return isRelatedDataGetter(method)
                            ? null : "related data getter shape mismatch";
                case TargetResolver.KEY_SAME_TOPIC_FEED:
                    return isSameTopicTemplatePredicate(method)
                            ? null : "same topic template predicate shape mismatch";
                default:
                    return method.getParameterTypes().length == 0
                            && method.getReturnType() == String.class
                            ? null : "getter shape mismatch";
            }
        } catch (Throwable throwable) {
            return String.valueOf(throwable);
        }
    }

    private static boolean isBoolean(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    static boolean isTopicRecommendMethod(Method method) {
        if (Modifier.isAbstract(method.getModifiers())) {
            return false;
        }
        if (isBoolean(method.getReturnType())
                && method.getDeclaringClass().getName().contains("TopicRecommend")) {
            return true;
        }
        Class<?>[] parameters = method.getParameterTypes();
        return Modifier.isStatic(method.getModifiers())
                && "kotlin.Unit".equals(method.getReturnType().getName())
                && parameters.length == 4
                && "com.coolapk.market.model.Feed".equals(parameters[0].getName())
                && parameters[1] == method.getDeclaringClass()
                && "androidx.compose.runtime.Composer".equals(parameters[2].getName())
                && parameters[3] == int.class;
    }

    static boolean isAutoCommentEntry(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        return Modifier.isStatic(method.getModifiers())
                && !Modifier.isAbstract(method.getModifiers())
                && method.getReturnType() == void.class
                && parameters.length == 1
                && "com.coolapk.market.view.cardlist.EntityListFragment".equals(
                parameters[0].getName())
                && "com.coolapk.market.view.cardlist.component."
                .concat("RecyclerViewItemFullVisibleControllerKt")
                .equals(method.getDeclaringClass().getName());
    }

    static boolean isDetailSponsorGetter(Method method) {
        return "getDetailSponsorCard".equals(method.getName())
                && method.getParameterTypes().length == 0
                && "com.coolapk.market.model.Entity".equals(
                method.getReturnType().getName())
                && !Modifier.isAbstract(method.getModifiers())
                && inheritsFrom(method.getDeclaringClass(),
                "com.coolapk.market.model.Feed");
    }

    static boolean isSameTopicTemplatePredicate(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        return Modifier.isStatic(method.getModifiers())
                && !Modifier.isAbstract(method.getModifiers())
                && method.getReturnType() == boolean.class
                && parameters.length == 1
                && parameters[0] == Object.class
                && inheritsFrom(method.getDeclaringClass(),
                "com.coolapk.market.view.cardlist.EntityListFragment");
    }

    private static boolean inheritsFrom(Class<?> type, String parentName) {
        Class<?> cursor = type;
        while (cursor != null) {
            if (parentName.equals(cursor.getName())) {
                return true;
            }
            cursor = cursor.getSuperclass();
        }
        return false;
    }

    private static boolean isVerifiedFeatureClass(String key, Class<?> type) {
        String name = type.getName();
        if (TargetResolver.KEY_RELATED_DATA.equals(key)) {
            return name.endsWith(".RelatedDataViewHolder");
        }
        if (TargetResolver.KEY_REPLY_HOLDER.equals(key)) {
            return isReplyHolderClass(type);
        }
        return false;
    }

    static boolean isRelatedDataGetter(Method method) {
        return "getRelatedData".equals(method.getName())
                && method.getParameterTypes().length == 0
                && method.getReturnType() == List.class
                && !Modifier.isStatic(method.getModifiers())
                && !Modifier.isAbstract(method.getModifiers())
                && inheritsFrom(method.getDeclaringClass(),
                "com.coolapk.market.model.Feed");
    }

    /** Class-only Reply cache entries must satisfy the same contract as installation. */
    static boolean isReplyHolderClass(Class<?> type) {
        if (type == null || !"com.coolapk.market.viewholder.MultiFeedReplyViewHolder"
                .equals(type.getName())) {
            return false;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (isReplyBindMethod(method)) {
                return true;
            }
        }
        return false;
    }

    static boolean isReplyBindMethod(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        return !Modifier.isStatic(method.getModifiers())
                && !Modifier.isAbstract(method.getModifiers())
                && method.getReturnType() == void.class
                && parameters.length == 1
                && ("com.coolapk.market.model.Entity".equals(parameters[0].getName())
                || "com.coolapk.market.model.FeedReply".equals(parameters[0].getName()));
    }

    /** Normalizes feed#2 / splash_base#2 style keys to their base key. */
    private static String keyKind(String key) {
        if (TargetResolver.isFeedKey(key)) {
            return TargetResolver.KEY_FEED;
        }
        if (TargetResolver.isSplashKey(key)) {
            return TargetResolver.KEY_SPLASH_BASE;
        }
        return key;
    }

    static boolean isFeedShape(Method method) {
        Class<?>[] params = method.getParameterTypes();
        return params.length == 2
                && List.class.isAssignableFrom(params[0])
                && params[1] == boolean.class
                && List.class.isAssignableFrom(method.getReturnType());
    }

    /**
     * Returns the most-derived onCreate(Bundle) declared by the Coolapk
     * splash hierarchy. Framework/AndroidX Activity.onCreate is deliberately
     * excluded so we never hook every Activity in the process.
     */
    static Method findOnCreate(Class<?> type) {
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            if (!cursor.getName().startsWith("com.coolapk.market.")) {
                break;
            }
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
