package io.github.yylsping.coolapkpurifier;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Pure UI notification for the first on-device DexKit adaptation of the
 * current stableTargetIdentity. One shot per process, posted to the main
 * thread without delay and without waiting for the Toast.
 */
final class FirstAdaptationToast {
    static final String DEFAULT_START_MESSAGE =
            "首次适配默认选项中，广告可能显示一回，后续自动净化";
    static final String DEFAULT_COMPLETE_MESSAGE =
            "默认选项适配完毕，建议重启软件，后续自动读取配置";
    static final String SELECTION_START_MESSAGE =
            "首次适配中，广告和垃圾布局可能显示一回，后续自动净化";
    static final String SELECTION_COMPLETE_MESSAGE =
            "已全部适配完毕，建议重启软件，后续自动读取配置";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OnceFlag startShown = new OnceFlag();
    private final OnceFlag completionShown = new OnceFlag();
    private final ModuleLog log;
    private volatile PurifierConfig.PendingKind activeKind = PurifierConfig.PendingKind.NONE;

    FirstAdaptationToast(ModuleLog log) {
        this.log = log;
    }

    void showStartOnce(Context context, PurifierConfig.PendingKind kind) {
        if (!startShown.tryOnce()) {
            return;
        }
        activeKind = kind == PurifierConfig.PendingKind.SELECTION
                ? PurifierConfig.PendingKind.SELECTION : PurifierConfig.PendingKind.DEFAULT;
        String message = activeKind == PurifierConfig.PendingKind.SELECTION
                ? SELECTION_START_MESSAGE : DEFAULT_START_MESSAGE;
        post(context, message, "start");
    }

    boolean hasStarted() {
        return activeKind != PurifierConfig.PendingKind.NONE;
    }

    PurifierConfig.PendingKind activeKind() {
        return activeKind;
    }

    void showCompletionOnce(Context context) {
        if (!hasStarted() || !completionShown.tryOnce()) {
            return;
        }
        String message = activeKind == PurifierConfig.PendingKind.SELECTION
                ? SELECTION_COMPLETE_MESSAGE : DEFAULT_COMPLETE_MESSAGE;
        post(context, message, "completion");
    }

    private void post(Context context, String message, String stage) {
        Context appContext = context.getApplicationContext();
        boolean posted = mainHandler.post(() -> {
            try {
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
                log.info("adaptationToast stage=" + stage + " shown=true once=true"
                        + " kind=" + activeKind);
            } catch (Throwable throwable) {
                log.info("adaptationToast stage=" + stage
                        + " shown=false once=true error=" + throwable);
            }
        });
        if (!posted) {
            log.info("adaptationToast stage=" + stage
                    + " shown=false once=true error=postRejected");
        }
    }
}
