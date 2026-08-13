package io.github.coolapk.adblock;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

final class EntityListHooks {
    private final XposedModule module;
    private final ModuleLog log;
    private final EntityListFilter filter = new EntityListFilter(new EntityClassifier(new EntityAccessorCache()));
    private final Set<Method> hookedMethods = new HashSet<>();
    private final Map<Method, HookHandle> handles = new HashMap<>();

    EntityListHooks(XposedModule module, ModuleLog log) {
        this.module = module;
        this.log = log;
    }

    synchronized int install(Class<?> target) {
        int installed = 0;
        for (Method method : target.getDeclaredMethods()) {
            if (!isListTransformer(method) || hookedMethods.contains(method)) {
                continue;
            }
            HookHandle handle = module.hook(method)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-list-" + target.getName() + "-" + method.getName())
                    .intercept(chain -> {
                        Object original = chain.proceed();
                        if (!(original instanceof List<?>)) {
                            return original;
                        }
                        try {
                            List<?> filtered = filter.filter((List<?>) original);
                            if (filtered != original) {
                                log.info("removed " + (((List<?>) original).size() - filtered.size())
                                        + " sponsored item(s) via " + method);
                            }
                            return filtered;
                        } catch (Throwable throwable) {
                            log.error("ad filtering failed; preserving original list", throwable);
                            return original;
                        }
                    });
            handles.put(method, handle);
            hookedMethods.add(method);
            installed++;
        }
        return installed;
    }

    static boolean isListTransformer(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        return parameters.length == 2
                && List.class.isAssignableFrom(parameters[0])
                && parameters[1] == boolean.class
                && List.class.isAssignableFrom(method.getReturnType())
                && !Modifier.isAbstract(method.getModifiers());
    }
}
