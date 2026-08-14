package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

final class HookCoordinator {
    private static final String SPLASH_BASE = "com.coolapk.market.view.splash.Ϳ";
    private static final String ENTITY_AD_HELPER = "com.coolapk.market.view.ad.EntityAdHelper";
    private static final String ENTITY_LIST_FRAGMENT = "com.coolapk.market.view.cardlist.EntityListFragment";
    private static final long WATCHER_TIMEOUT_MILLIS = 15_000L;

    private final Object installLock = new Object();
    private final XposedModule module;
    private final ModuleLog log;
    private final ClassLoader primaryLoader;
    private final SplashHooks splashHooks;
    private final EntityListHooks entityListHooks;
    private final List<HookHandle> classLoadHandles = new ArrayList<>();
    private final ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CoolapkAdBlock-Resolver");
        thread.setDaemon(true);
        return thread;
    });

    private InstallState splashState = InstallState.UNINSTALLED;
    private InstallState adHelperState = InstallState.UNINSTALLED;
    private InstallState listFragmentState = InstallState.UNINSTALLED;
    private boolean watcherInstalled;
    private boolean watcherStopped;
    private boolean mainActivitySeen;

    HookCoordinator(XposedModule module, ModuleLog log, ClassLoader primaryLoader) {
        this.module = module;
        this.log = log;
        this.primaryLoader = primaryLoader;
        this.splashHooks = new SplashHooks(module, log, this::onActivityCreated);
        this.entityListHooks = new EntityListHooks(module, log);
    }

    void install() throws ReflectiveOperationException {
        splashHooks.installInstrumentationFallback();
        synchronized (installLock) {
            tryInstallTargets(primaryLoader);
            if (!allRuntimeTargetsInstalled()) {
                installTemporaryClassLoadWatcher();
                scheduleBoundedResolution();
            }
        }
    }

    private void scheduleBoundedResolution() {
        // The protected Coolapk APK appends its real DEX to the same PathClassLoader after
        // onPackageReady. Two background probes cover that transition without main-thread
        // polling; the temporary watcher is still the fast path when loadClass is observable.
        retryExecutor.schedule(() -> retryResolve("1.5 second probe"), 1_500L, TimeUnit.MILLISECONDS);
        retryExecutor.schedule(() -> retryResolve("5 second probe"), 5_000L, TimeUnit.MILLISECONDS);
        retryExecutor.schedule(this::stopWatcherAtDeadline, WATCHER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void retryResolve(String source) {
        synchronized (installLock) {
            if (watcherStopped) {
                return;
            }
            tryInstallTargets(primaryLoader);
            maybeStopWatcher();
            if (allRuntimeTargetsInstalled()) {
                log.info("runtime targets resolved by " + source);
            }
        }
    }

    private void onActivityCreated(Activity activity) {
        if (activity == null) {
            return;
        }
        String name = activity.getClass().getName();
        synchronized (installLock) {
            if (SplashHooks.MAIN_ACTIVITY.equals(name)) {
                mainActivitySeen = true;
            }
            tryInstallTargets(activity.getClass().getClassLoader());
            maybeStopWatcher();
        }
    }

    private void onClassLoaded(Class<?> loadedClass) {
        String name = loadedClass.getName();
        synchronized (installLock) {
            if (SPLASH_BASE.equals(name) && splashState.needsAttempt()) {
                installSplashClass(loadedClass);
            } else if (ENTITY_AD_HELPER.equals(name) && adHelperState.needsAttempt()) {
                adHelperState = installEntityClass(loadedClass, ENTITY_AD_HELPER);
            } else if (ENTITY_LIST_FRAGMENT.equals(name) && listFragmentState.needsAttempt()) {
                listFragmentState = installEntityClass(loadedClass, ENTITY_LIST_FRAGMENT);
            }
            maybeStopWatcher();
        }
    }

    private void tryInstallTargets(ClassLoader loader) {
        if (loader == null) {
            return;
        }
        if (splashState.needsAttempt()) {
            Class<?> type = findLoadedClass(SPLASH_BASE, loader);
            if (type != null) {
                installSplashClass(type);
            }
        }
        if (adHelperState.needsAttempt()) {
            Class<?> type = findLoadedClass(ENTITY_AD_HELPER, loader);
            if (type != null) {
                adHelperState = installEntityClass(type, ENTITY_AD_HELPER);
            }
        }
        if (listFragmentState.needsAttempt()) {
            Class<?> type = findLoadedClass(ENTITY_LIST_FRAGMENT, loader);
            if (type != null) {
                listFragmentState = installEntityClass(type, ENTITY_LIST_FRAGMENT);
            }
        }
    }

    private void installSplashClass(Class<?> type) {
        splashState = InstallState.INSTALLING;
        try {
            splashState = splashHooks.installSpecific(type)
                    ? InstallState.INSTALLED
                    : InstallState.UNAVAILABLE;
        } catch (Throwable throwable) {
            splashState = InstallState.FAILED_RETRYABLE;
            log.error("specific splash hook failed", throwable);
        }
    }

    private InstallState installEntityClass(Class<?> type, String label) {
        try {
            int count = entityListHooks.install(type);
            if (count == 0) {
                log.info("no compatible list transformer in " + label);
                return InstallState.UNAVAILABLE;
            }
            log.info("installed " + count + " list hook(s) in " + label);
            return InstallState.INSTALLED;
        } catch (Throwable throwable) {
            log.error("list hook installation failed for " + label, throwable);
            return InstallState.FAILED_RETRYABLE;
        }
    }

    private void installTemporaryClassLoadWatcher() throws ReflectiveOperationException {
        if (watcherInstalled || watcherStopped) {
            return;
        }
        Method oneArg = ClassLoader.class.getDeclaredMethod("loadClass", String.class);
        Method twoArgs = ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
        classLoadHandles.add(module.hook(oneArg)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-classload-1")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof Class<?>) {
                        onClassLoaded((Class<?>) result);
                    }
                    return result;
                }));
        classLoadHandles.add(module.hook(twoArgs)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId("coolapk-classload-2")
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (result instanceof Class<?>) {
                        onClassLoaded((Class<?>) result);
                    }
                    return result;
                }));
        watcherInstalled = true;
        log.info("temporary ClassLoader watcher installed");
    }

    private void maybeStopWatcher() {
        if (watcherInstalled && adHelperState.isTerminal() && listFragmentState.isTerminal()
                && (splashState.isTerminal() || mainActivitySeen)) {
            stopWatcher("targets resolved");
        }
    }

    private void stopWatcherAtDeadline() {
        synchronized (installLock) {
            stopWatcher("15 second deadline");
        }
    }

    private void stopWatcher(String reason) {
        if (!watcherInstalled || watcherStopped) {
            return;
        }
        watcherStopped = true;
        watcherInstalled = false;
        for (HookHandle handle : classLoadHandles) {
            try {
                handle.unhook();
            } catch (Throwable throwable) {
                log.error("unable to remove class-load watcher", throwable);
            }
        }
        classLoadHandles.clear();
        retryExecutor.shutdownNow();
        log.info("temporary ClassLoader watcher removed: " + reason);
    }

    private boolean allRuntimeTargetsInstalled() {
        return splashState == InstallState.INSTALLED
                && adHelperState == InstallState.INSTALLED
                && listFragmentState == InstallState.INSTALLED;
    }

    private static Class<?> findLoadedClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private enum InstallState {
        UNINSTALLED,
        INSTALLING,
        INSTALLED,
        FAILED_RETRYABLE,
        UNAVAILABLE;

        boolean needsAttempt() {
            return this == UNINSTALLED || this == FAILED_RETRYABLE;
        }

        boolean isTerminal() {
            return this == INSTALLED || this == UNAVAILABLE;
        }
    }
}
