package io.github.yylsping.coolapkpurifier;

import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedModule;

/**
 * TEMPORARY diagnostic for the staged search-UI hook goal (16.6.2 phase).
 * Observe-only: intercepts the three candidate UI terminals, proceeds
 * untouched, then logs the bound data and the host fragment. Must be removed
 * before acceptance; never part of a release build.
 */
final class SearchUiDiagnostic {
    private static final String SEARCH_HOT_HOLDER = "ene";
    private static final String SEARCH_HOT_LIST_HOLDER = "dne";
    private static final String LINK_CARD_HOLDER = "com.coolapk.market.viewholder.ދ";
    private static final String ENTITY_CARD_CLASS = "com.coolapk.market.model.EntityCard";
    /** ob: bind(Object) — real name U+0788. */
    private static final String BINDER_OBJECT = "ވ";
    /** link card holder: bind(EntityCard) — real name U+0794. */
    private static final String BINDER_ENTITY_CARD = "ޔ";
    /** ob: binding component getter — real name U+078A. */
    private static final String COMPONENT_GETTER = "ފ";
    /** od1: container getter (Fragment) — real name U+037F. */
    private static final String FRAGMENT_GETTER = "Ϳ";

    private static final String LIST_ADAPTER = "q1";
    private static final int Q1_LOG_CAP = 400;
    private static final java.util.concurrent.atomic.AtomicInteger q1LogCount =
            new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.Set<String> q1SeenCombo =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private SearchUiDiagnostic() {
    }

    static void install(XposedModule module, ModuleLog log, ClassLoader loader) {
        hookBinder(module, log, loader, SEARCH_HOT_HOLDER, BINDER_OBJECT,
                Object.class, "searchHot");
        hookBinder(module, log, loader, SEARCH_HOT_LIST_HOLDER, BINDER_OBJECT,
                Object.class, "searchHotListCard");
        try {
            Class<?> entityCard = Class.forName(ENTITY_CARD_CLASS, false, loader);
            hookBinder(module, log, loader, LINK_CARD_HOLDER, BINDER_ENTITY_CARD,
                    entityCard, "linkCard");
        } catch (Throwable failure) {
            log.error("searchui-diag linkCard class lookup failed", failure);
        }
        hookListAdapter(module, log, loader);
    }

    private static void hookListAdapter(XposedModule module, ModuleLog log,
                                        ClassLoader loader) {
        try {
            Class<?> adapter = Class.forName(LIST_ADAPTER, false, loader);
            Class<?> viewHolder = Class.forName(
                    "androidx.recyclerview.widget.RecyclerView$ViewHolder", false, loader);
            Method onBind = adapter.getDeclaredMethod("onBindViewHolder", viewHolder,
                    int.class);
            Method getItem = adapter.getDeclaredMethod("getItem", int.class);
            module.hook(onBind)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("searchui-diag-q1bind")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            Object holder = chain.getArg(0);
                            int position = (Integer) chain.getArg(1);
                            Object data = getItem.invoke(chain.getThisObject(), position);
                            String template = String.valueOf(
                                    invokeGetter(data, "getEntityTemplate"));
                            String combo = holder.getClass().getName() + "|" + template;
                            boolean novel = q1SeenCombo.add(combo);
                            if (novel
                                    && q1LogCount.incrementAndGet() <= Q1_LOG_CAP) {
                                log.info("searchui-diag label=q1bind"
                                        + " holder=" + holder.getClass().getName()
                                        + " data={" + describeEntity(data) + "}"
                                        + " host={" + describeHost(holder) + "}");
                            }
                        } catch (Throwable loggingFailure) {
                            log.error("searchui-diag q1bind logging failed", loggingFailure);
                        }
                        return result;
                    });
            log.info("searchui-diag install=OK label=q1bind target=" + LIST_ADAPTER
                    + "#onBindViewHolder");
        } catch (Throwable failure) {
            log.error("searchui-diag install failed label=q1bind", failure);
        }
    }

    private static void hookBinder(XposedModule module, ModuleLog log, ClassLoader loader,
                                   String className, String methodName, Class<?> paramType,
                                   String label) {
        try {
            Class<?> holder = Class.forName(className, false, loader);
            Method binder = findMethod(holder, methodName, paramType);
            if (binder == null) {
                log.info("searchui-diag install=MISSING label=" + label
                        + " class=" + className);
                return;
            }
            module.hook(binder)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("searchui-diag-" + label)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        try {
                            log.info("searchui-diag label=" + label
                                    + " holder=" + chain.getThisObject().getClass().getName()
                                    + " data={" + describeEntity(chain.getArg(0)) + "}"
                                    + " host={" + describeHost(chain.getThisObject()) + "}");
                        } catch (Throwable loggingFailure) {
                            log.error("searchui-diag logging failed label=" + label,
                                    loggingFailure);
                        }
                        return result;
                    });
            log.info("searchui-diag install=OK label=" + label
                    + " target=" + binder.getDeclaringClass().getName()
                    + "#" + binder.getName());
        } catch (Throwable failure) {
            log.error("searchui-diag install failed label=" + label, failure);
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?> paramType) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, paramType);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String describeEntity(Object entity) {
        if (entity == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("class=").append(entity.getClass().getName());
        appendProperty(builder, entity, "getEntityTemplate", "template");
        appendProperty(builder, entity, "getEntityType", "type");
        appendProperty(builder, entity, "getTitle", "title");
        appendProperty(builder, entity, "getUrl", "url");
        Object entities = invokeGetter(entity, "getEntities");
        if (entities instanceof List) {
            List<?> list = (List<?>) entities;
            builder.append(" entities=").append(list.size()).append('[');
            int limit = Math.min(list.size(), 4);
            for (int i = 0; i < limit; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                Object child = list.get(i);
                Object title = child == null ? null : invokeGetter(child, "getTitle");
                builder.append(title);
            }
            if (list.size() > limit) {
                builder.append(", ...");
            }
            builder.append(']');
        }
        return builder.toString();
    }

    private static void appendProperty(StringBuilder builder, Object target,
                                       String getter, String label) {
        Object value = invokeGetter(target, getter);
        if (value != null) {
            builder.append(' ').append(label).append('=').append(value);
        }
    }

    private static Object invokeGetter(Object target, String name) {
        Class<?> current = target.getClass();
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getName().equals(name)) {
                    try {
                        method.setAccessible(true);
                        return method.invoke(target);
                    } catch (Throwable ignored) {
                        return null;
                    }
                }
            }
            current = current.getSuperclass();
        }
        // Interface getters (model interfaces) may only be visible publicly.
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String describeHost(Object holder) {
        try {
            Object component = invokeNamedNoArg(holder, COMPONENT_GETTER);
            if (component == null) {
                return "component=null";
            }
            Object fragment = invokeNamedNoArg(component, FRAGMENT_GETTER);
            if (fragment == null) {
                return "fragment=null component=" + component.getClass().getName();
            }
            StringBuilder builder = new StringBuilder();
            builder.append("fragment=").append(fragment.getClass().getName());
            Bundle arguments = invokeArguments(fragment);
            if (arguments != null) {
                Set<String> keys = arguments.keySet();
                builder.append(" args=").append(keys);
                for (String key : keys) {
                    Object value = arguments.get(key);
                    if (value instanceof String && ((String) value).contains("V9")) {
                        builder.append(' ').append(key).append('=').append(value);
                    }
                }
            }
            Object urlField = readField(fragment, "url");
            if (urlField != null) {
                builder.append(" urlField=").append(urlField);
            }
            return builder.toString();
        } catch (Throwable failure) {
            return "hostError=" + failure;
        }
    }

    private static Bundle invokeArguments(Object fragment) {
        try {
            Method method = fragment.getClass().getMethod("getArguments");
            Object result = method.invoke(fragment);
            return result instanceof Bundle ? (Bundle) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNamedNoArg(Object target, String name) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Throwable failure) {
                return null;
            }
        }
        return null;
    }

    private static Object readField(Object target, String name) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable failure) {
                return null;
            }
        }
        return null;
    }
}
