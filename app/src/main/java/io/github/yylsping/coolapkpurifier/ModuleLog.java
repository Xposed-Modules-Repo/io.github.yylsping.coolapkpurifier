package io.github.yylsping.coolapkpurifier;

import android.util.Log;

import io.github.libxposed.api.XposedModule;

final class ModuleLog {
    private static final String TAG = "CoolapkAdBlock";

    private final XposedModule module;

    ModuleLog(XposedModule module) {
        this.module = module;
    }

    void info(String message) {
        Log.i(TAG, message);
        module.log(Log.INFO, TAG, message);
    }

    void error(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        module.log(Log.ERROR, TAG, message, throwable);
    }
}
