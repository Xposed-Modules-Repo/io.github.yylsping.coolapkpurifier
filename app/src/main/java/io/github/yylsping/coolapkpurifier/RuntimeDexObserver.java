package io.github.yylsping.coolapkpurifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/**
 * Temporary ClassLoader watcher that emits runtimeDexReady as soon as a real
 * Coolapk business class is loaded. Fixed sleeps are explicitly not used.
 */
final class RuntimeDexObserver {
    interface Listener {
        void onRuntimeDexReady(String trigger, ClassLoader runtimeClassLoader);
    }

    private final XposedModule module;
    private final ModuleLog log;
    private final Listener listener;
    private final List<HookHandle> handles = new ArrayList<>();
    private boolean armed;

    RuntimeDexObserver(XposedModule module, ModuleLog log, Listener listener) {
        this.module = module;
        this.log = log;
        this.listener = listener;
    }

    void install() throws ReflectiveOperationException {
        synchronized (this) {
            if (armed || !handles.isEmpty()) {
                return;
            }
            armed = true;
        }
        Method oneArg = ClassLoader.class.getDeclaredMethod("loadClass", String.class);
        Method twoArgs = ClassLoader.class.getDeclaredMethod(
                "loadClass", String.class, boolean.class);
        handles.add(module.hook(oneArg)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-runtime-dex-1")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    onResult(result);
                    return result;
                }));
        handles.add(module.hook(twoArgs)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-runtime-dex-2")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    onResult(result);
                    return result;
                }));
        log.info("runtime dex observer installed");
    }

    /** Re-arm after a retryable miss so a later loader generation can retry. */
    void rearm() {
        synchronized (this) {
            armed = true;
        }
        if (handles.isEmpty()) {
            try {
                install();
            } catch (Throwable throwable) {
                log.error("runtime dex observer rearm failed", throwable);
            }
        }
    }

    private void onResult(Object result) {
        if (!(result instanceof Class<?>)) {
            return;
        }
        Class<?> loadedClass = (Class<?>) result;
        if (!isBusinessClass(loadedClass.getName())) {
            return;
        }
        synchronized (this) {
            if (!armed) {
                return;
            }
            armed = false;
        }
        ClassLoader loader = loadedClass.getClassLoader();
        close();
        log.info("runtime dex ready trigger=loadClass class=" + loadedClass.getName()
                + " loaderIdentity=" + System.identityHashCode(loader));
        listener.onRuntimeDexReady("loadClass:" + loadedClass.getName(), loader);
    }

    void notifyFirstActivityPre(ClassLoader activityLoader) {
        synchronized (this) {
            if (!armed) {
                return;
            }
            armed = false;
        }
        close();
        log.info("runtime dex ready trigger=firstActivityPre loaderIdentity="
                + System.identityHashCode(activityLoader));
        listener.onRuntimeDexReady("firstActivityPre", activityLoader);
    }

    void close() {
        for (HookHandle handle : handles) {
            try {
                handle.unhook();
            } catch (Throwable ignored) {
            }
        }
        handles.clear();
    }

    private static boolean isBusinessClass(String name) {
        if (name == null || !name.startsWith("com.coolapk.market.")) {
            return false;
        }
        // Shell DEX keeps a few loader classes in the root package. Business
        // code appears under feature packages such as .view and .model.
        return name.startsWith("com.coolapk.market.view.")
                || name.startsWith("com.coolapk.market.model.")
                || name.startsWith("com.coolapk.market.manager.")
                || name.startsWith("com.coolapk.market.util.");
    }
}
