package io.github.yylsping.coolapkpurifier;

import android.app.Application;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public final class CoolapkModule extends XposedModule {
    static final String TARGET_PACKAGE = "com.coolapk.market";

    private static final AtomicBoolean initialized = new AtomicBoolean();
    // HookHandle lifetime is tied to its owning object. Keep the complete graph reachable
    // from the framework-owned module entry for the target process lifetime.
    private static volatile HookCoordinator coordinator;

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }

        String processName = currentProcessName();
        if (!TARGET_PACKAGE.equals(processName) || !initialized.compareAndSet(false, true)) {
            return;
        }

        ModuleLog log = new ModuleLog(this);
        try {
            HookCoordinator created = new HookCoordinator(this, log, param.getClassLoader());
            created.install();
            coordinator = created;
            log.info("initialized API 102 hooks in " + processName);
        } catch (Throwable throwable) {
            initialized.set(false);
            log.error("initialization failed", throwable);
        }
    }

    private static String currentProcessName() {
        return Application.getProcessName();
    }
}
