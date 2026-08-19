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
 *
 * <p>Lifecycle: {@link #install()} arms the observer and hooks
 * {@code ClassLoader.loadClass}. The first business-class event fires once and
 * closes the hooks. {@link #rearm()} re-arms and, when the hooks were closed,
 * reinstalls them, so a later runtime DEX generation is still observed.
 *
 * <p>Installation is publish-once and close-aware: an installer builds its
 * handles into a local list first; if {@link #close()} ran in the meantime
 * (terminal cleanup racing an in-flight rearm), the freshly created handles
 * are unhooked immediately instead of leaking a live ClassLoader hook past
 * terminal state.
 */
final class RuntimeDexObserver {
    interface Listener {
        void onRuntimeDexReady(String trigger, ClassLoader runtimeClassLoader);
    }

    /** Creates the loadClass hooks; they are published only if no close() ran meanwhile. */
    interface HookInstaller {
        List<HookHandle> installLoadClassHooks() throws Throwable;
    }

    private final ModuleLog log;
    private final Listener listener;
    private final HookInstaller hookInstaller;
    private final XposedModule module;
    private final List<HookHandle> handles = new ArrayList<>();
    private boolean armed;
    /**
     * Guards against concurrent hook installation: two threads rearming
     * while an installer is still running used to both observe an empty
     * handle list and install the loadClass hooks twice.
     */
    private boolean installing;
    /**
     * Bumped by every {@link #close()}. An installation that started under an
     * older epoch is discarded at publish time — close() could not have
     * unhooked handles that did not exist yet.
     */
    private int closeEpoch;

    RuntimeDexObserver(XposedModule module, ModuleLog log, Listener listener) {
        this(module, log, listener, null);
    }

    RuntimeDexObserver(ModuleLog log, Listener listener, HookInstaller hookInstaller) {
        this(null, log, listener, hookInstaller);
    }

    private RuntimeDexObserver(XposedModule module, ModuleLog log, Listener listener,
                               HookInstaller hookInstaller) {
        this.module = module;
        this.log = log;
        this.listener = listener;
        this.hookInstaller = hookInstaller != null ? hookInstaller : this::installDefaultHooks;
    }

    void install() {
        ensureArmedAndHooked("install");
    }

    /**
     * Re-arm after a retryable miss so a later loader generation can retry.
     * Fixes the lost-observer defect: the previous implementation set
     * {@code armed = true} before delegating to {@code install()}, whose own
     * {@code armed} guard then returned without ever reinstalling the closed
     * loadClass hooks, permanently blinding the observer.
     */
    void rearm() {
        ensureArmedAndHooked("rearm");
    }

    private void ensureArmedAndHooked(String reason) {
        boolean needInstall;
        synchronized (this) {
            armed = true;
            needInstall = handles.isEmpty() && !installing;
            if (needInstall) {
                installing = true;
            }
        }
        if (!needInstall) {
            return;
        }
        int epoch;
        synchronized (this) {
            epoch = closeEpoch;
        }
        List<HookHandle> created = new ArrayList<>();
        try {
            List<HookHandle> installed = hookInstaller.installLoadClassHooks();
            if (installed != null) {
                created.addAll(installed);
            }
        } catch (Throwable throwable) {
            log.error("runtime dex observer install failed reason=" + reason, throwable);
        }
        boolean publish;
        synchronized (this) {
            installing = false;
            publish = epoch == closeEpoch && armed;
            if (publish) {
                handles.addAll(created);
            }
        }
        if (publish) {
            log.info("runtime dex observer installed reason=" + reason
                    + " handles=" + created.size());
        } else {
            unhookAll(created);
            log.info("runtime dex observer install discarded reason=" + reason
                    + " closedDuringInstall=true handles=" + created.size());
        }
    }

    private List<HookHandle> installDefaultHooks() throws ReflectiveOperationException {
        Method oneArg = ClassLoader.class.getDeclaredMethod("loadClass", String.class);
        Method twoArgs = ClassLoader.class.getDeclaredMethod(
                "loadClass", String.class, boolean.class);
        List<HookHandle> created = new ArrayList<>();
        created.add(module.hook(oneArg)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-runtime-dex-1")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof Class<?>) {
                        onClassLoaded((Class<?>) result);
                    }
                    return result;
                }));
        created.add(module.hook(twoArgs)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-runtime-dex-2")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof Class<?>) {
                        onClassLoaded((Class<?>) result);
                    }
                    return result;
                }));
        return created;
    }

    /** Sink for the loadClass interceptors; single-shot per arming. */
    void onClassLoaded(Class<?> loadedClass) {
        if (!isBusinessClass(loadedClass.getName())) {
            return;
        }
        ClassLoader loader;
        synchronized (this) {
            if (!armed) {
                return;
            }
            armed = false;
            loader = loadedClass.getClassLoader();
        }
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

    /**
     * Closes this arming: bumps the close epoch (invalidating any in-flight
     * installation), disarms and unhooks every published handle. A later
     * {@link #rearm()} works under the new epoch.
     */
    void close() {
        List<HookHandle> toUnhook;
        synchronized (this) {
            closeEpoch++;
            armed = false;
            toUnhook = new ArrayList<>(handles);
            handles.clear();
        }
        unhookAll(toUnhook);
    }

    private void unhookAll(List<HookHandle> toUnhook) {
        for (HookHandle handle : toUnhook) {
            try {
                handle.unhook();
            } catch (Throwable ignored) {
            }
        }
    }

    boolean isArmed() {
        synchronized (this) {
            return armed;
        }
    }

    /** Visible for tests: number of currently published handles. */
    synchronized int publishedHandleCount() {
        return handles.size();
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
