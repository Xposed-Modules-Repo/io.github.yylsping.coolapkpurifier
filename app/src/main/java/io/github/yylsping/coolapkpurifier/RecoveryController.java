package io.github.yylsping.coolapkpurifier;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.widget.Toast;

/**
 * One-shot recovery, only when splash escaped before resolution AND splash
 * was later resolved, cached and verified. Never restarts on DexKit failure.
 */
final class RecoveryController {
    private static final long TOAST_WINDOW_MILLIS = 1200L;

    private final ModuleLog log;
    private volatile BootstrapTrace trace;
    private volatile Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();

    private TargetIdentity identity;
    private boolean splashEscaped;
    private boolean splashResolved;
    private boolean recovered;

    RecoveryController(ModuleLog log, BootstrapTrace trace, Context appContext) {
        this.log = log;
        this.trace = trace;
        this.appContext = appContext;
    }

    void attachContext(Context appContext) {
        this.appContext = appContext;
    }

    void attachTrace(BootstrapTrace trace) {
        this.trace = trace;
    }

    void attachIdentity(TargetIdentity identity) {
        synchronized (lock) {
            this.identity = identity;
        }
    }

    void markSplashEscaped() {
        synchronized (lock) {
            splashEscaped = true;
            trace.mark("splashEscaped", identity == null ? "" : identity.shortToken());
        }
    }

    void onSplashResolved(ResolutionCache cache) {
        synchronized (lock) {
            splashResolved = true;
            if (!canRecover(cache)) {
                return;
            }
        }
        boolean markerPersisted = cache.markRecoveryAttempted(identity);
        if (!markerPersisted) {
            trace.mark("recoveryAborted", "reason=markerPersisted=false");
            log.error("recoveryAborted=true reason=markerPersisted=false identity="
                    + identity.shortToken(), null);
            return;
        }
        synchronized (lock) {
            recovered = true;
        }
        log.info("recoveryRequired=true recoveryReason=firstLaunchSplashEscaped"
                + " cacheVerified=true markerPersisted=true recoveryAttempt=1"
                + " identity=" + identity.shortToken());
        boolean toastRequested = showToast();
        trace.mark("recoveryScheduled", "toastRequested=" + toastRequested
                + " markerPersisted=true");
        mainHandler.postDelayed(this::killTargetProcess, TOAST_WINDOW_MILLIS);
    }

    boolean isRecovered() {
        synchronized (lock) {
            return recovered;
        }
    }

    private boolean canRecover(ResolutionCache cache) {
        if (recovered || !splashEscaped || !splashResolved || identity == null) {
            return false;
        }
        if (cache.isRecoveryAttempted(identity)) {
            log.info("recovery skipped reason=alreadyAttempted identity=" + identity.shortToken());
            return false;
        }
        return true;
    }

    private boolean showToast() {
        try {
            mainHandler.post(() -> {
                try {
                    Toast.makeText(appContext,
                            "酷安净化已完成新版本适配，正在重新启动",
                            Toast.LENGTH_SHORT).show();
                    trace.mark("toastShown", "true");
                } catch (Throwable throwable) {
                    trace.mark("toastShown", "false error=" + throwable);
                }
            });
            return true;
        } catch (Throwable throwable) {
            trace.mark("toastShown", "false error=postFailed");
            return false;
        }
    }

    private void killTargetProcess() {
        trace.mark("restartRequested", "true");
        log.info("recovery restart target pid=" + Process.myPid());
        Process.killProcess(Process.myPid());
    }

}
