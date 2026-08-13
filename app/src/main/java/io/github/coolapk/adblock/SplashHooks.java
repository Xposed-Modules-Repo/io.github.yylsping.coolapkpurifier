package io.github.coolapk.adblock;

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

    private static final String SPLASH_ACTIVITY = "com.coolapk.market.view.splash.SplashActivity";
    private static final String FULL_SCREEN_AD_ACTIVITY = "com.coolapk.market.view.splash.FullScreenAdActivity";
    private static final int MAIN_INTENT_FLAGS = Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP;

    interface ActivityObserver {
        void onActivityCreated(Activity activity);
    }

    private final XposedModule module;
    private final ModuleLog log;
    private final ActivityObserver observer;
    private final List<HookHandle> handles = new ArrayList<>();

    SplashHooks(XposedModule module, ModuleLog log, ActivityObserver observer) {
        this.module = module;
        this.log = log;
        this.observer = observer;
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
            log.info("specific splash hook installed");
        }
        return count > 0;
    }

    private void finishSplash(Activity activity, String source) {
        try {
            String className = activity.getClass().getName();
            if (!isSplashActivity(className) || activity.isFinishing()) {
                return;
            }
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

    private static boolean isSplashActivity(String name) {
        return SPLASH_ACTIVITY.equals(name) || FULL_SCREEN_AD_ACTIVITY.equals(name);
    }
}
