package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

final class SplashHooks {
    static final String MAIN_ACTIVITY = "com.coolapk.market.view.main.MainActivity";

    private static final int MAIN_INTENT_FLAGS =
            Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP;

    /** Result of unhooking the Instrumentation safety gate (Mode A-ZF Phase 2). */
    static final class SafetyRetireResult {
        final int unhooked;
        final int failed;
        final int remaining;

        SafetyRetireResult(int unhooked, int failed, int remaining) {
            this.unhooked = unhooked;
            this.failed = failed;
            this.remaining = remaining;
        }

        boolean isFrameworkActive() {
            return remaining > 0;
        }
    }

    interface ActivityObserver {
        void onPreActivityCreate(Activity activity);

        void onPostActivityCreate(Activity activity);

        boolean shouldFinishSplash(Activity activity);
    }

    private final XposedModule module;
    private final ModuleLog log;
    private final ActivityObserver observer;
    private final HookLedger ledger;
    private final List<HookHandle> bootstrapHandles = new ArrayList<>();
    private final List<HookHandle> specificHandles = new ArrayList<>();
    private final Set<Method> hookedSpecific = new HashSet<>();
    private volatile boolean bootstrapCallbacksActive = true;

    SplashHooks(XposedModule module, ModuleLog log, ActivityObserver observer,
                HookLedger ledger) {
        this.module = module;
        this.log = log;
        this.observer = observer;
        this.ledger = ledger;
    }

    void installInstrumentationFallback() throws ReflectiveOperationException {
        Method regular = Instrumentation.class.getDeclaredMethod(
                "callActivityOnCreate", Activity.class, Bundle.class);
        Method persistent = Instrumentation.class.getDeclaredMethod(
                "callActivityOnCreate", Activity.class, Bundle.class, PersistableBundle.class);
        installInstrumentationMethod(regular, "coolapk-activity-create-2");
        installInstrumentationMethod(persistent, "coolapk-activity-create-3");
        log.info("Instrumentation bootstrap hooks installed (pre/post split)");
    }

    private void installInstrumentationMethod(Method method, String id) {
        HookHandle handle = module.hook(method)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId(id)
                .intercept(chain -> {
                    Object candidate = chain.getArg(0);
                    Activity activity = candidate instanceof Activity ? (Activity) candidate : null;
                    boolean callbacksActive = bootstrapCallbacksActive;
                    if (activity != null && callbacksActive) {
                        observer.onPreActivityCreate(activity);
                    }
                    Object result = chain.proceed();
                    if (activity != null) {
                        if (callbacksActive) {
                            observer.onPostActivityCreate(activity);
                        }
                        // DEGRADED-only safety net: on a clean READY these
                        // hooks are unhooked by retireInstrumentationSafety;
                        // when retained (fallback mode) they keep finishing
                        // splash-family activities outside the resolved
                        // hierarchy for the rest of the process.
                        if (observer.shouldFinishSplash(activity)) {
                            finishSplash(activity, "instrumentation");
                        }
                    }
                    return result;
                });
        bootstrapHandles.add(handle);
        ledger.record(HookLedger.Layer.FRAMEWORK, "splash",
                "instrumentation-" + id, method.toGenericString());
    }

    /**
     * Mode A-ZF Phase 2: on a clean READY the generic Instrumentation safety
     * gate is unhooked for real. Entries whose unhook threw stay active in the
     * ledger so frameworkActiveHooks keeps reporting them honestly.
     */
    synchronized SafetyRetireResult retireInstrumentationSafety(String reason) {
        bootstrapCallbacksActive = false;
        int unhooked = 0;
        int failed = 0;
        List<HookHandle> retained = new ArrayList<>();
        for (HookHandle handle : bootstrapHandles) {
            try {
                handle.unhook();
                unhooked++;
            } catch (Throwable throwable) {
                failed++;
                retained.add(handle);
                log.error("instrumentation safety unhook failed", throwable);
            }
        }
        bootstrapHandles.clear();
        bootstrapHandles.addAll(retained);
        if (retained.isEmpty()) {
            String detail = reason + " unhooked=" + unhooked;
            ledger.retire("instrumentation-coolapk-activity-create-2", detail);
            ledger.retire("instrumentation-coolapk-activity-create-3", detail);
        }
        SafetyRetireResult result = new SafetyRetireResult(unhooked, failed, retained.size());
        log.info("instrumentation safety retired reason=" + reason
                + " unhooked=" + result.unhooked
                + " failed=" + result.failed
                + " remaining=" + result.remaining);
        return result;
    }

    /** Whether any Instrumentation fallback handle is still installed. */
    synchronized boolean isInstrumentationSafetyActive() {
        return !bootstrapHandles.isEmpty();
    }

    /**
     * Retires the coordinator-facing bootstrap callbacks but keeps the
     * Instrumentation hooks installed in a passive mode (DEGRADED fallback).
     */
    synchronized void retireBootstrapCallbacks() {
        bootstrapCallbacksActive = false;
        log.info("instrumentation bootstrap callbacks retired hooksRetained=true");
    }

    synchronized boolean installSpecific(Class<?> splashBase) {
        Method onCreate = TargetVerifier.findOnCreate(splashBase);
        if (onCreate == null) {
            log.info("specific splash hook skipped class=" + splashBase.getName()
                    + " reason=noCoolapkOnCreate frameworkFallback=true");
            return false;
        }
        if (hookedSpecific.contains(onCreate)) {
            log.info("specific splash hook already installed method=" + onCreate);
            return true;
        }
        try {
            HookHandle handle = module.hook(onCreate)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-specific-splash")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Activity
                                && observer.shouldFinishSplash((Activity) thisObject)) {
                            finishSplash((Activity) thisObject, "specific");
                        }
                        return result;
                    });
            specificHandles.add(handle);
            hookedSpecific.add(onCreate);
            ledger.record(HookLedger.Layer.BUSINESS, "splash",
                    "splash-specific-" + splashBase.getName(), onCreate.toString());
            log.info("specific splash hook installed class=" + splashBase.getName()
                    + " method=" + onCreate);
            return true;
        } catch (Throwable throwable) {
            log.error("specific splash hook install failed class=" + splashBase.getName(),
                    throwable);
            return false;
        }
    }

    private void finishSplash(Activity activity, String source) {
        try {
            if (activity.isFinishing()) {
                return;
            }
            String className = activity.getClass().getName();
            if (activity.isTaskRoot()) {
                Intent intent = new Intent();
                intent.setClassName(CoolapkModule.TARGET_PACKAGE, MAIN_ACTIVITY);
                intent.addFlags(MAIN_INTENT_FLAGS);
                activity.startActivity(intent);
            }
            activity.finish();
            log.info("finished splash via " + source + ": " + className);
        } catch (Throwable throwable) {
            log.error("unable to finish splash", throwable);
        }
    }
}
