package io.github.yylsping.coolapkpurifier;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

final class EntityListHooks {
    private final XposedModule module;
    private final ModuleLog log;
    private final EntityClassifier classifier = new EntityClassifier();
    private final EntityListFilter filter = new EntityListFilter(classifier);

    private volatile Method hookedMethod;
    private HookHandle handle;

    EntityListHooks(XposedModule module, ModuleLog log) {
        this.module = module;
        this.log = log;
    }

    void updateAccessors(Map<String, ResolvedTarget> targets, ClassLoader loader) {
        classifier.setAccessors(EntityAccessors.fromTargets(targets, loader));
    }

    synchronized void install(Method method) {
        if (method == null || method.equals(hookedMethod)) {
            return;
        }
        if (handle != null) {
            try {
                handle.unhook();
            } catch (Throwable ignored) {
            }
        }
        hookedMethod = method;
        handle = module.hook(method)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-feed-filter")
                .intercept(chain -> {
                    Object original = chain.proceed();
                    if (!(original instanceof List<?>)) {
                        return original;
                    }
                    try {
                        List<?> source = (List<?>) original;
                        List<?> filtered = filter.filter(source);
                        if (filtered != source) {
                            log.info("removed " + (source.size() - filtered.size())
                                    + " sponsored item(s) via " + method);
                        }
                        return filtered;
                    } catch (Throwable throwable) {
                        log.error("ad filtering failed; preserving original list", throwable);
                        return original;
                    }
                });
        log.info("installed feed filter hook method=" + method);
    }
}
