package io.github.yylsping.coolapkpurifier;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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
    enum HookSite {
        LOAD_CLASS_ONE_ARG,
        LOAD_CLASS_TWO_ARG
    }

    static final class CloseResult {
        final int unhookedThisClose;
        final int failedThisClose;
        final int totalUnhooked;
        final int totalFailures;
        final int remaining;
        final boolean logicalEnabled;
        final boolean summaryComplete;

        CloseResult(int unhookedThisClose, int failedThisClose,
                    int totalUnhooked, int totalFailures, int remaining,
                    boolean logicalEnabled, boolean summaryComplete) {
            this.unhookedThisClose = unhookedThisClose;
            this.failedThisClose = failedThisClose;
            this.totalUnhooked = totalUnhooked;
            this.totalFailures = totalFailures;
            this.remaining = remaining;
            this.logicalEnabled = logicalEnabled;
            this.summaryComplete = summaryComplete;
        }

        boolean isFrameworkActive() {
            return remaining > 0;
        }
    }

    interface Listener {
        void onRuntimeDexReady(String trigger, ClassLoader runtimeClassLoader);
    }

    /** Creates the loadClass hooks; they are published only if no close() ran meanwhile. */
    interface HookInstaller {
        Map<HookSite, HookHandle> installLoadClassHooks(Set<HookSite> missingSites)
                throws Throwable;
    }

    private final ModuleLog log;
    private final Listener listener;
    private final HookInstaller hookInstaller;
    private final XposedModule module;
    private final Map<HookSite, HookHandle> handles = new EnumMap<>(HookSite.class);
    private final Set<HookSite> installingSites = EnumSet.noneOf(HookSite.class);
    private boolean armed;
    /**
     * Guards against concurrent hook installation: two threads rearming
     * while an installer is still running used to both observe an empty
     * handle list and install the loadClass hooks twice.
     */
    private boolean closing;
    private volatile boolean logicalEnabled;
    /**
     * Bumped by every {@link #close()}. An installation that started under an
     * older epoch is discarded at publish time — close() could not have
     * unhooked handles that did not exist yet.
     */
    private int closeEpoch;
    private int totalUnhooked;
    private int totalFailures;

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
        Set<HookSite> missing;
        int epoch;
        synchronized (this) {
            if (closing) {
                return;
            }
            armed = true;
            logicalEnabled = true;
            missing = EnumSet.allOf(HookSite.class);
            missing.removeAll(handles.keySet());
            missing.removeAll(installingSites);
            if (missing.isEmpty()) {
                return;
            }
            installingSites.addAll(missing);
            epoch = closeEpoch;
        }
        Map<HookSite, HookHandle> created = new EnumMap<>(HookSite.class);
        try {
            Map<HookSite, HookHandle> installed =
                    hookInstaller.installLoadClassHooks(EnumSet.copyOf(missing));
            if (installed != null) {
                for (Map.Entry<HookSite, HookHandle> entry : installed.entrySet()) {
                    if (missing.contains(entry.getKey()) && entry.getValue() != null) {
                        created.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Throwable throwable) {
            log.error("runtime dex observer install failed reason=" + reason, throwable);
        }
        boolean publish;
        synchronized (this) {
            publish = epoch == closeEpoch && armed && logicalEnabled && !closing;
            if (publish) {
                for (Map.Entry<HookSite, HookHandle> entry : created.entrySet()) {
                    if (!handles.containsKey(entry.getKey())) {
                        handles.put(entry.getKey(), entry.getValue());
                    }
                }
                installingSites.removeAll(missing);
                notifyAll();
            }
        }
        if (publish) {
            log.info("runtime dex observer installed reason=" + reason
                    + " coverage=" + publishedHandleCount() + "/2");
        } else {
            recordDiscardAttempt(unhookAll(created));
            synchronized (this) {
                installingSites.removeAll(missing);
                notifyAll();
            }
            log.info("runtime dex observer install discarded reason=" + reason
                    + " closedDuringInstall=true handles=" + created.size());
        }
    }

    private Map<HookSite, HookHandle> installDefaultHooks(Set<HookSite> missingSites)
            throws ReflectiveOperationException {
        Map<HookSite, HookHandle> created = new EnumMap<>(HookSite.class);
        if (missingSites.contains(HookSite.LOAD_CLASS_ONE_ARG)) {
            Method oneArg = ClassLoader.class.getDeclaredMethod("loadClass", String.class);
            created.put(HookSite.LOAD_CLASS_ONE_ARG, module.hook(oneArg)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-runtime-dex-1")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (logicalEnabled && result instanceof Class<?>) {
                        onClassLoaded((Class<?>) result);
                    }
                    return result;
                }));
        }
        if (missingSites.contains(HookSite.LOAD_CLASS_TWO_ARG)) {
            Method twoArgs = ClassLoader.class.getDeclaredMethod(
                    "loadClass", String.class, boolean.class);
            created.put(HookSite.LOAD_CLASS_TWO_ARG, module.hook(twoArgs)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-runtime-dex-2")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (logicalEnabled && result instanceof Class<?>) {
                        onClassLoaded((Class<?>) result);
                    }
                    return result;
                }));
        }
        return created;
    }

    /** Sink for the loadClass interceptors; single-shot per arming. */
    void onClassLoaded(Class<?> loadedClass) {
        if (!isBusinessClass(loadedClass.getName())) {
            return;
        }
        ClassLoader loader;
        synchronized (this) {
            if (!armed || !logicalEnabled) {
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
            if (!armed || !logicalEnabled) {
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
    CloseResult close() {
        final int unhookedBefore;
        final int failuresBefore;
        final Map<HookSite, HookHandle> toUnhook;
        synchronized (this) {
            closeEpoch++;
            closing = true;
            unhookedBefore = totalUnhooked;
            failuresBefore = totalFailures;
            // Inert-first: residual framework callbacks can no longer emit a
            // trigger even if unhook throws.
            logicalEnabled = false;
            armed = false;
            toUnhook = new EnumMap<>(handles);
            handles.clear();
        }
        recordDiscardAttempt(unhookAll(toUnhook));

        boolean summaryComplete;
        synchronized (this) {
            // An installer that started before close owns unpublished handles.
            // Wait briefly for its discard/unhook result so terminal trace sees
            // residual failures instead of racing an incomplete snapshot.
            long deadline = System.currentTimeMillis() + 2_000L;
            while (!installingSites.isEmpty()) {
                long remainingMillis = deadline - System.currentTimeMillis();
                if (remainingMillis <= 0L) {
                    break;
                }
                try {
                    wait(remainingMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            summaryComplete = installingSites.isEmpty();
            closing = false;
            notifyAll();
            return new CloseResult(totalUnhooked - unhookedBefore,
                    totalFailures - failuresBefore,
                    totalUnhooked, totalFailures, handles.size(),
                    logicalEnabled, summaryComplete);
        }
    }

    private static UnhookAttempt unhookAll(Map<HookSite, HookHandle> toUnhook) {
        int unhooked = 0;
        Map<HookSite, HookHandle> failed = new EnumMap<>(HookSite.class);
        for (Map.Entry<HookSite, HookHandle> entry : toUnhook.entrySet()) {
            try {
                entry.getValue().unhook();
                unhooked++;
            } catch (Throwable ignored) {
                failed.put(entry.getKey(), entry.getValue());
            }
        }
        return new UnhookAttempt(unhooked, failed);
    }

    private synchronized void recordDiscardAttempt(UnhookAttempt attempt) {
        totalUnhooked += attempt.unhooked;
        totalFailures += attempt.failedHandles.size();
        for (Map.Entry<HookSite, HookHandle> entry : attempt.failedHandles.entrySet()) {
            if (!handles.containsKey(entry.getKey())) {
                handles.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static final class UnhookAttempt {
        final int unhooked;
        final Map<HookSite, HookHandle> failedHandles;

        UnhookAttempt(int unhooked, Map<HookSite, HookHandle> failedHandles) {
            this.unhooked = unhooked;
            this.failedHandles = failedHandles;
        }
    }

    boolean isArmed() {
        synchronized (this) {
            return armed;
        }
    }

    boolean isLogicallyEnabled() {
        return logicalEnabled;
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
