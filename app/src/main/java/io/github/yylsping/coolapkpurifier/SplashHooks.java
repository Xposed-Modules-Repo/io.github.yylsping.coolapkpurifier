package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

final class SplashHooks {
    static final String MAIN_ACTIVITY = "com.coolapk.market.view.main.MainActivity";

    // Legacy compatibility names retained only as the last fallback predicate.
    private static final String LEGACY_SPLASH_ACTIVITY = "com.coolapk.market.view.splash.SplashActivity";
    private static final String LEGACY_FULL_SCREEN_AD_ACTIVITY =
            "com.coolapk.market.view.splash.FullScreenAdActivity";
    private static final String SPLASH_PACKAGE = "com.coolapk.market.view.splash";
    private static final int MAIN_INTENT_FLAGS =
            Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP;

    interface ActivityObserver {
        void onActivityCreated(Activity activity);
    }

    private final XposedModule module;
    private final ModuleLog log;
    private final ActivityObserver observer;
    private final List<HookHandle> handles = new ArrayList<>();
    private volatile Class<?> resolvedSplashBase;

    SplashHooks(XposedModule module, ModuleLog log, ActivityObserver observer) {
        this.module = module;
        this.log = log;
        this.observer = observer;
    }

    void setResolvedSplashBase(Class<?> resolvedSplashBase) {
        this.resolvedSplashBase = resolvedSplashBase;
    }

    void installInstrumentationFallback() throws ReflectiveOperationException {
        Method regular = Instrumentation.class.getDeclaredMethod(
                "callActivityOnCreate", Activity.class, Bundle.class);
        Method persistent = Instrumentation.class.getDeclaredMethod(
                "callActivityOnCreate", Activity.class, Bundle.class, PersistableBundle.class);
        installInstrumentationMethod(regular, "coolapk-activity-create-2");
        installInstrumentationMethod(persistent, "coolapk-activity-create-3");
        log.info("Instrumentation splash fallback installed");
    }

    private void installInstrumentationMethod(Method method, String id) {
        HookHandle handle = module.hook(method)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .setId(id)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    Object candidate = chain.getArg(0);
                    if (candidate instanceof Activity) {
                        Activity activity = (Activity) candidate;
                        observer.onActivityCreated(activity);
                        finishSplash(activity, "instrumentation");
                    }
                    return result;
                });
        handles.add(handle);
    }

    boolean installSpecific(Class<?> splashBase) {
        int count = 0;
        for (Method method : splashBase.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (!"onCreate".equals(method.getName())
                    || parameters.length != 1
                    || parameters[0] != Bundle.class) {
                continue;
            }
            HookHandle handle = module.hook(method)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-specific-splash")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Activity) {
                            finishSplash((Activity) thisObject, "specific");
                        }
                        return result;
                    });
            handles.add(handle);
            count++;
        }
        if (count > 0) {
            log.info("specific splash hook installed class=" + splashBase.getName());
        }
        return count > 0;
    }

    private void finishSplash(Activity activity, String source) {
        try {
            String className = activity.getClass().getName();
            if (!isSplashActivity(activity) || activity.isFinishing()) {
                return;
            }
            if (activity.isTaskRoot()) {
                Intent intent = new Intent();
                intent.setClassName(CoolapkModule.TARGET_PACKAGE, MAIN_ACTIVITY);
                intent.addFlags(MAIN_INTENT_FLAGS);
                activity.startActivity(intent);
            }
            activity.finish();
            log.info("finished splash via " + source + ": " + className
                    + " (resolvedBase=" + (resolvedSplashBase == null
                    ? "null" : resolvedSplashBase.getName()) + ")");
        } catch (Throwable throwable) {
            log.error("unable to finish splash", throwable);
        }
    }

    private boolean isSplashActivity(Activity activity) {
        String name = activity.getClass().getName();
        Class<?> base = resolvedSplashBase;
        if (base != null && base.isAssignableFrom(activity.getClass())) {
            return true;
        }
        if (SPLASH_PACKAGE.equals(packageNameOf(activity.getClass()))) {
            String simpleName = simpleNameOf(activity.getClass());
            if (simpleName != null
                    && (simpleName.contains("Splash")
                    || simpleName.contains("FullScreenAd"))) {
                return true;
            }
        }
        return LEGACY_SPLASH_ACTIVITY.equals(name) || LEGACY_FULL_SCREEN_AD_ACTIVITY.equals(name);
    }

    private static String packageNameOf(Class<?> type) {
        Package pkg = type.getPackage();
        if (pkg != null) {
            return pkg.getName();
        }
        String name = type.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? name : name.substring(0, lastDot);
    }

    private static String simpleNameOf(Class<?> type) {
        String name = type.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? name : name.substring(lastDot + 1);
    }
}
