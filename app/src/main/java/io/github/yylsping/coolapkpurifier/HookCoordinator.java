package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

final class HookCoordinator {
    private static final long RESOLVE_DEADLINE_MILLIS = 20_000L;

    private final XposedModule module;
    private final ModuleLog log;
    private final ClassLoader primaryLoader;
    private final SplashHooks splashHooks;
    private final EntityListHooks entityListHooks;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean attemptRunning = new AtomicBoolean();

    private volatile TargetResolver resolver;
    private volatile Context appContext;
    private volatile Method installedFeedMethod;
    private volatile String installedSplashClass;
    private volatile boolean resolverStopped;

    HookCoordinator(XposedModule module, ModuleLog log, ClassLoader primaryLoader) {
        this.module = module;
        this.log = log;
        this.primaryLoader = primaryLoader;
        this.splashHooks = new SplashHooks(module, log, this::onActivityCreated);
        this.entityListHooks = new EntityListHooks(module, log);
    }

    void install() throws ReflectiveOperationException {
        // Framework fallback is installed synchronously so an early splash is
        // handled even before the first DexKit scan completes.
        splashHooks.installInstrumentationFallback();
        scheduleAttempt("0.5 second probe", 500L);
        scheduleAttempt("1.5 second probe", 1_500L);
        scheduleAttempt("5 second probe", 5_000L);
        scheduleAttempt("10 second probe", 10_000L);
        mainHandler.postDelayed(this::stopAtDeadline, RESOLVE_DEADLINE_MILLIS);
    }

    private void scheduleAttempt(String label, long delayMillis) {
        mainHandler.postDelayed(() -> {
            if (resolverStopped) {
                return;
            }
            log.info("coordinator resolution attempt: " + label);
            runAttempt(label);
        }, delayMillis);
    }

    private void runAttempt(String label) {
        if (resolverStopped || !attemptRunning.compareAndSet(false, true)) {
            log.info("coordinator resolution attempt skipped: " + label
                    + " running=" + attemptRunning.get() + " stopped=" + resolverStopped);
            return;
        }
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            try {
                ensureResolver();
                log.info("coordinator ensureResolver done resolver="
                        + (resolver == null ? "null" : resolver.getClass().getName()));
                TargetResolver targetResolver = resolver;
                if (targetResolver == null) {
                    log.info("coordinator resolution skipped: target app context is not ready");
                    return;
                }
                log.info("coordinator calling resolver.attempt");
                targetResolver.attempt();
                log.info("coordinator resolver.attempt returned");
                applyResolved(targetResolver.getResolved());
                if (targetResolver.areRequiredTargetsResolved()) {
                    stopResolver("required targets resolved via " + label);
                }
            } catch (Throwable throwable) {
                log.error("coordinator resolution attempt failed", throwable);
            } finally {
                attemptRunning.set(false);
            }
        });
    }

    private void ensureResolver() {
        if (resolver != null) {
            return;
        }
        if (appContext == null) {
            appContext = currentApplication();
        }
        if (appContext == null) {
            return;
        }
        resolver = new TargetResolver(log, primaryLoader, appContext);
    }

    private void applyResolved(Map<String, ResolvedTarget> targets) {
        entityListHooks.updateAccessors(targets, primaryLoader);

        ResolvedTarget feed = targets.get(TargetResolver.KEY_FEED);
        if (feed != null && installedFeedMethod == null) {
            Method method = DescriptorUtils.methodForDescriptor(feed.methodDescriptor, primaryLoader);
            if (method != null) {
                entityListHooks.install(method);
                installedFeedMethod = method;
            } else {
                log.info("coordinator feed descriptor not loadable yet: " + feed.describe());
            }
        }

        ResolvedTarget splash = targets.get(TargetResolver.KEY_SPLASH_BASE);
        if (splash != null && installedSplashClass == null) {
            try {
                Class<?> type = DescriptorUtils.classForName(splash.classDescriptor, primaryLoader);
                if (type != null) {
                    splashHooks.setResolvedSplashBase(type);
                    splashHooks.installSpecific(type);
                    installedSplashClass = type.getName();
                }
            } catch (Throwable throwable) {
                log.info("coordinator splash descriptor not loadable yet: " + splash.describe());
            }
        }
    }

    private void onActivityCreated(Activity activity) {
        if (activity == null || resolverStopped) {
            return;
        }
        try {
            if (resolver != null) {
                applyResolved(resolver.getResolved());
            }
        } catch (Throwable throwable) {
            log.error("coordinator activity resolution failed", throwable);
        }
        // Activities are created on the main thread and the protected DEX is
        // always loaded by then, so this is a reliable extra trigger.
        runAttempt("activity-created");
    }

    private void stopResolver(String reason) {
        if (resolverStopped) {
            return;
        }
        resolverStopped = true;
        TargetResolver targetResolver = resolver;
        log.info("coordinator resolver stopped: " + reason
                + " feedInstalled=" + (installedFeedMethod != null)
                + " splashInstalled=" + (installedSplashClass != null)
                + " resolved=" + (targetResolver == null ? "{}" : targetResolver.getResolved().keySet()));
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void stopAtDeadline() {
        stopResolver(RESOLVE_DEADLINE_MILLIS + " ms deadline");
    }

    private static Context currentApplication() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object instance = activityThread.getMethod("currentActivityThread").invoke(null);
            if (instance == null) {
                return null;
            }
            Object application = activityThread.getMethod("currentApplication").invoke(instance);
            return application instanceof Context ? (Context) application : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
