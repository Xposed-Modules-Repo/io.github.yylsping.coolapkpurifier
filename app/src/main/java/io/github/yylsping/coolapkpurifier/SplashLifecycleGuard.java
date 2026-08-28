package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Strict post-onCreate fallback, using Android observers, never an Xposed hook. */
final class SplashLifecycleGuard implements Application.ActivityLifecycleCallbacks {
    private final SplashGate gate;
    private final BooleanSupplier enabled;
    private final Consumer<Activity> finish;
    private final ModuleLog log;
    private Application owner;
    private BiConsumer<String, Activity> diagnostics;

    void observeWith(BiConsumer<String, Activity> diagnostics) { this.diagnostics = diagnostics; }
    private void observe(String event, Activity activity) {
        try {
            if (diagnostics != null) diagnostics.accept(event, activity);
        } catch (Throwable ignored) { /* Never let optional observation affect existing removal. */ }
    }

    SplashLifecycleGuard(SplashGate gate, BooleanSupplier enabled,
                         Consumer<Activity> finish, ModuleLog log) {
        this.gate = gate;
        this.enabled = enabled;
        this.finish = finish;
        this.log = log;
    }

    synchronized boolean install(Application application) {
        if (application == null) {
            return false;
        }
        if (owner == application) {
            return true;
        }
        try {
            application.registerActivityLifecycleCallbacks(this);
            if (owner != null) {
                owner.unregisterActivityLifecycleCallbacks(this);
            }
            owner = application;
            log.info("splash lifecycle guard registered frameworkHooks=0 applicationIdentity="
                    + System.identityHashCode(application));
            return true;
        } catch (Throwable failure) {
            // A callback on an old Application is not proof of current coverage.
            try {
                application.unregisterActivityLifecycleCallbacks(this);
            } catch (Throwable ignored) { }
            owner = null;
            log.error("splash lifecycle guard registration failed", failure);
            return false;
        }
    }

    synchronized boolean isInstalled() {
        return owner != null;
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {
        observe("activityCreated", activity);
        if (activity == null) {
            return;
        }
        if (SplashHooks.MAIN_ACTIVITY.equals(activity.getClass().getName())) {
            gate.markMainActivity();
        }
        // No namespace/simple-name matching here, including during bootstrap.
        if (enabled.getAsBoolean() && gate.shouldFinishLifecycleSplash(activity)) {
            finish.accept(activity);
        }
    }

    @Override public void onActivityStarted(Activity activity) { observe("activityStarted", activity); }
    @Override public void onActivityResumed(Activity activity) { observe("activityResumed", activity); }
    @Override public void onActivityPaused(Activity activity) { observe("activityPaused", activity); }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) { observe("activityDestroyed", activity); }
}
