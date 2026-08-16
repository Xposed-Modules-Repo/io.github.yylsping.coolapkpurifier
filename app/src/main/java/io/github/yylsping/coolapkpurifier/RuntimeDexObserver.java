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
        void onRuntimeDexReady(String trigger);
    }

    private final XposedModule module;
    private final ModuleLog log;
    private final Listener listener;
    private final List<HookHandle> handles = new ArrayList<>();
    private volatile boolean ready;

    RuntimeDexObserver(XposedModule module, ModuleLog log, Listener listener) {
        this.module = module;
        this.log = log;
        this.listener = listener;
    }

    void install() throws ReflectiveOperationException {
        if (ready) {
            return;
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

    private void onResult(Object result) {
        if (ready || !(result instanceof Class<?>)) {
            return;
        }
        String name = ((Class<?>) result).getName();
        if (!isBusinessClass(name)) {
            return;
        }
        synchronized (this) {
            if (ready) {
                return;
            }
            ready = true;
        }
        close();
        log.info("runtime dex ready trigger=loadClass class=" + name);
        listener.onRuntimeDexReady("loadClass:" + name);
    }

    void notifyFirstActivityPre() {
        if (ready) {
            return;
        }
        synchronized (this) {
            if (ready) {
                return;
            }
            ready = true;
        }
        close();
        log.info("runtime dex ready trigger=firstActivityPre");
        listener.onRuntimeDexReady("firstActivityPre");
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
