package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Inserts a native model group into the settings fragment's own LazyColumn data. */
final class SettingsEntryInjector {
    static final String FRAGMENT =
            "com.coolapk.market.view.settings.SettingEntranceComposeFragment";
    static final String PARENT = "com.coolapk.market.view.settings.components.ComposeFragment";
    static final String CALLBACK = "kotlin.jvm.functions.Function1";

    private SettingsEntryInjector() { }

    static Method findInitData(Class<?> type) throws ReflectiveOperationException {
        if (!FRAGMENT.equals(type.getName()) || type.getSuperclass() == null
                || !PARENT.equals(type.getSuperclass().getName())) return null;
        Method method = type.getDeclaredMethod("initData");
        int flags = method.getModifiers();
        if (!Modifier.isPublic(flags) || !Modifier.isFinal(flags)
                || Modifier.isStatic(flags) || Modifier.isAbstract(flags)
                || method.getReturnType() != void.class || listField(type) == null) return null;
        return method;
    }

    static Field listField(Class<?> type) {
        Field result = null;
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == List.class) {
                if (result != null) return null;
                result = field;
            }
        }
        return result;
    }

    static boolean inject(Object fragment, int icon, Consumer<Activity> open) throws ReflectiveOperationException {
        // A zero icon is the host's destructive, centered button style.
        if (icon <= 0) return false;
        Class<?> type = fragment.getClass();
        if (findInitData(type) == null) return false;
        Field field = listField(type);
        field.setAccessible(true);
        Object value = field.get(fragment);
        // initData owns an ordinary mutable ArrayList of groups in all three
        // inspected versions. Do not mutate an unknown/snapshot-backed source.
        if (value == null || value.getClass() != ArrayList.class) return false;
        @SuppressWarnings("unchecked") List<Object> groups = (List<Object>) value;
        Class<?> model = findModel(groups);
        if (model == null || !hasRenderer(type, model)) return false;
        ClassLoader loader = model.getClassLoader();
        Class<?> function = Class.forName(CALLBACK, false, loader);
        Constructor<?> constructor = modelConstructor(model, function);
        if (constructor == null) return false;
        Object unit = Class.forName("kotlin.Unit", false, loader).getField("INSTANCE").get(null);
        Object click = Proxy.newProxyInstance(loader, new Class<?>[] {function}, new EntryClick(open, unit));
        Object entry = constructor.newInstance("酷安净化", icon, "", click);
        prepend(groups, entry);
        return true;
    }

    static Class<?> findModel(List<?> groups) {
        Class<?> model = null;
        for (Object group : groups) {
            if (group instanceof EntryGroup) continue;
            if (!(group instanceof List<?>)) return null;
            for (Object row : (List<?>) group) {
                if (row == null) return null;
                if (model == null) model = row.getClass();
                if (row.getClass() != model) return null;
            }
        }
        return model;
    }

    static Constructor<?> modelConstructor(Class<?> type, Class<?> callback) {
        if (!Modifier.isFinal(type.getModifiers()) || !callback.isInterface()
                || !CALLBACK.equals(callback.getName())) return null;
        int strings = 0, integers = 0, callbacks = 0;
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (!Modifier.isFinal(field.getModifiers())) return null;
            if (field.getType() == String.class) strings++;
            else if (field.getType() == int.class) integers++;
            else if (field.getType() == callback) callbacks++;
            else return null;
        }
        if (strings != 2 || integers != 1 || callbacks != 1) return null;
        try {
            return type.getConstructor(String.class, int.class, String.class, callback);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static boolean hasRenderer(Class<?> fragment, Class<?> model) {
        for (Method method : fragment.getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if (!Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                    && Modifier.isFinal(method.getModifiers()) && method.getReturnType() == void.class
                    && p.length == 3 && p[0] == model
                    && "androidx.compose.runtime.Composer".equals(p[1].getName()) && p[2] == int.class) return true;
        }
        return false;
    }

    static void prepend(List<Object> groups, Object entry) {
        // Preserve every native group and row, including their identities.
        // Repeat initData / injection never accumulates duplicate module rows.
        groups.removeIf(group -> group instanceof EntryGroup);
        EntryGroup group = new EntryGroup();
        group.add(entry);
        groups.add(0, group);
    }

    private static final class EntryGroup extends ArrayList<Object> { }

    private static final class EntryClick implements InvocationHandler {
        private final Consumer<Activity> open;
        private final Object unit;
        EntryClick(Consumer<Activity> open, Object unit) { this.open = open; this.unit = unit; }
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "equals": return proxy == args[0];
                    case "hashCode": return System.identityHashCode(proxy);
                    default: return "CoolapkPurifierSettingsEntry";
                }
            }
            if ("invoke".equals(method.getName()) && args != null && args.length == 1
                    && args[0] instanceof Activity) open.accept((Activity) args[0]);
            return unit;
        }
    }
}
