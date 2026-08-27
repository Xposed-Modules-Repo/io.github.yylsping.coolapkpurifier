package io.github.yylsping.coolapkpurifier;

import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Live contract for the dedicated reply sponsor renderer; no obfuscated names. */
final class ReplySelfDrawTarget {
    static final String TEMPLATE = "feedDetailReplySponsorCard";
    static final String FRAGMENT =
            "com.coolapk.market.view.feed.reply.FeedReplyListFragmentV8";
    static final String AD_HELPER = "com.coolapk.market.view.ad.EntityAdHelper";
    static final String BINDING_COMPONENT = "androidx.databinding.DataBindingComponent";
    static final String ENTITY = "com.coolapk.market.model.Entity";
    static final String VIEW_HOLDER = "androidx.recyclerview.widget.RecyclerView$ViewHolder";

    private ReplySelfDrawTarget() { }

    static boolean isBindMethod(Method method, ClassLoader loader) {
        if (method == null) return false;
        try {
            Class<?> holder = method.getDeclaringClass();
            if (!Modifier.isFinal(holder.getModifiers())
                    || !inheritsFrom(holder, VIEW_HOLDER)) return false;
            Class<?> helper = Class.forName(AD_HELPER, false, loader);
            Class<?> component = Class.forName(BINDING_COMPONENT, false, loader);
            if (!Modifier.isPublic(holder.getDeclaredConstructor(
                    View.class, helper, component).getModifiers())) return false;
            Field itemView = holder.getField("itemView");
            if (itemView.getType() != View.class
                    || !VIEW_HOLDER.equals(itemView.getDeclaringClass().getName())) return false;
            boolean hasEntity = false;
            boolean hasHelper = false;
            for (Field field : holder.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                hasEntity |= ENTITY.equals(field.getType().getName());
                hasHelper |= field.getType() == helper;
            }
            if (!hasEntity || !hasHelper || layoutField(holder, loader) == null) return false;
            // Only the unique implementation of the direct parent's abstract
            // Object binder. Object helpers, payload callbacks and ancestors
            // themselves are never hook targets.
            Method unique = null;
            Class<?> parent = holder.getSuperclass();
            for (Method candidate : holder.getDeclaredMethods()) {
                int flags = candidate.getModifiers();
                if (!Modifier.isPublic(flags) || Modifier.isStatic(flags)
                        || Modifier.isAbstract(flags) || candidate.isBridge()
                        || candidate.isSynthetic() || candidate.getReturnType() != void.class
                        || candidate.getParameterCount() != 1
                        || candidate.getParameterTypes()[0] != Object.class) continue;
                Method contract;
                try {
                    contract = parent.getDeclaredMethod(candidate.getName(), Object.class);
                } catch (NoSuchMethodException ignored) {
                    continue;
                }
                if (contract.getReturnType() != void.class
                        || !Modifier.isPublic(contract.getModifiers())
                        || !Modifier.isAbstract(contract.getModifiers())) continue;
                if (unique != null) return false;
                unique = candidate;
            }
            return method.equals(unique);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static Field layoutField(Class<?> holder, ClassLoader loader) throws Exception {
        Class<?> layouts = Class.forName("com.coolapk.market.R$layout", false, loader);
        Field layout = layouts.getDeclaredField("item_reply_self_draw");
        if (layout.getType() != int.class || !Modifier.isStatic(layout.getModifiers())) return null;
        layout.setAccessible(true);
        int expected = layout.getInt(null);
        if (expected == 0) return null;
        Field unique = null;
        for (Field field : holder.getDeclaredFields()) {
            if (field.getType() != int.class || !Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            if (field.getInt(null) != expected) continue;
            if (unique != null) return null;
            unique = field;
        }
        return unique;
    }

    private static boolean inheritsFrom(Class<?> type, String name) {
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            if (name.equals(cursor.getName())) return true;
        }
        return false;
    }
}
