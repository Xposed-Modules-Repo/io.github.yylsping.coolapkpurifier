package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/** Read-only startup observation. Not instantiated in any normal build variant. */
final class SplashDiagnostics {
    private final XposedModule module;
    private final ModuleLog log;
    private final HookLedger ledger;
    private final Handler handler;
    private final long start = SystemClock.elapsedRealtime();
    private final SplashObservationBudget budget = new SplashObservationBudget(start);
    private final Map<Method, HookHandle> handles = new LinkedHashMap<>();
    private final Map<Method, String> ids = new LinkedHashMap<>();
    private final Runnable stopTask = this::stop;
    private ResolutionCache cache;
    private int scans;
    private long attemptedGeneration = -1;
    private boolean stopped;

    SplashDiagnostics(XposedModule module, ModuleLog log, HookLedger ledger, Handler handler) {
        this.module = module;
        this.log = log;
        this.ledger = ledger;
        this.handler = handler;
        event("processObservationStart wallMs=" + System.currentTimeMillis()
                + " processStartElapsedMs=" + android.os.Process.getStartElapsedRealtime()
                + " mode=OBSERVE_ONLY productionDecision=POST_RESULT existingActivityRemoval=unchanged windowMs="
                + SplashObservationBudget.WINDOW_MS + " maxEvents=" + SplashObservationBudget.MAX_EVENTS);
        handler.postDelayed(stopTask, SplashObservationBudget.WINDOW_MS);
    }

    void lifecycle(String event, Activity activity) {
        if (activity != null && budget.active(SystemClock.elapsedRealtime())) {
            event(event + "=" + activity.getClass().getName()
                    + " instance=" + System.identityHashCode(activity));
        }
    }

    synchronized void event(String detail) {
        long now = SystemClock.elapsedRealtime();
        if (!budget.take(now)) return;
        try {
            log.info("splashDiagnostic relMs=" + (now - start) + " " + detail);
        } catch (Throwable ignored) {
            budget.close();
        }
        if (!budget.active(now)) handler.post(stopTask);
    }

    /** Called only by the existing resolver worker; the caller commits installs in its epoch. */
    Map<String, ResolvedTarget> prepare(ResolutionSessionContext session, TargetIdentity identity,
                                        BootstrapTrace trace) {
        if (!budget.active(SystemClock.elapsedRealtime()) || attemptedGeneration == session.generation)
            return Collections.emptyMap();
        attemptedGeneration = session.generation;
        try {
            // Android sandbox requires this descriptor cache in app-private storage.
            // Separate directory: never pollutes or invalidates the production resolver cache.
            File directory = new File(session.appContext.getFilesDir(), "splash_diagnostic");
            if (!directory.isDirectory() && !directory.mkdirs()) return Collections.emptyMap();
            cache = new ResolutionCache(directory);
            Map<String, ResolvedTarget> cached = cache.loadTargets(identity);
            if (cached.size() == 3 && cached.containsKey(SplashDiagnosticResolver.DECISION)
                    && cached.containsKey(SplashDiagnosticResolver.HOST_START)
                    && cached.containsKey(SplashDiagnosticResolver.FRAGMENT_SHOW)
                    && cached.values().stream().allMatch(t -> SplashDiagnosticResolver.verify(t, session.loader))) {
                event("targets source=diagnosticCache generation=" + session.generation);
                return cached;
            }
            if (scans++ >= 2) return Collections.emptyMap();
            org.luckypray.dexkit.DexKitBridge bridge = session.ensureBridge(log, trace, "splashDiagnostic");
            if (bridge == null || !bridge.isValid()) return Collections.emptyMap();
            Map<String, ResolvedTarget> resolved = SplashDiagnosticResolver.resolve(bridge, session.loader, log);
            event("targets source=dexkit generation=" + session.generation + " keys=" + resolved.keySet());
            return resolved;
        } catch (Throwable failure) {
            log.error("splash diagnostic resolution unavailable; core policy unchanged", failure);
            return Collections.emptyMap();
        }
    }

    synchronized void install(Map<String, ResolvedTarget> targets, ClassLoader loader, TargetIdentity identity) {
        if (!budget.active(SystemClock.elapsedRealtime())) return;
        for (ResolvedTarget target : targets.values()) {
            // The production interceptor logs the real original before overriding. A second
            // interceptor here would depend on hook ordering and could mislabel its result.
            if (SplashDiagnosticResolver.DECISION.equals(target.key)) continue;
            if (!SplashDiagnosticResolver.verify(target, loader)) continue;
            Method method = DescriptorUtils.methodForDescriptor(target.methodDescriptor, loader);
            if (handles.containsKey(method)) continue;
            String id = target.key + "-" + Integer.toHexString(System.identityHashCode(method));
            try {
                HookHandle handle = module.hook(method).setId(id)
                        .setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                            long begin = SystemClock.elapsedRealtime();
                            // Do not catch/retry proceed: return value and exception identity belong to host.
                            Object result = chain.proceed();
                            try {
                                if (budget.active(SystemClock.elapsedRealtime())) {
                                    String detail = target.key + " target=" + target.methodDescriptor;
                                    if (SplashDiagnosticResolver.FRAGMENT_SHOW.equals(target.key)) {
                                        detail += " host=" + fragmentHost(chain.getThisObject());
                                    }
                                    event(detail + " originalCompleted=true durationMs="
                                            + (SystemClock.elapsedRealtime() - begin));
                                }
                            } catch (Throwable ignored) { /* Observation cannot change host behavior. */ }
                            return result;
                        });
                handles.put(method, handle);
                ids.put(method, id);
                ledger.record(HookLedger.Layer.BUSINESS, "splashDiagnostic", id, target.methodDescriptor);
                event("observerInstalled target=" + target.methodDescriptor + " mode=OBSERVE_ONLY");
            } catch (Throwable failure) {
                log.error("splash diagnostic hook unavailable target=" + target.describe(), failure);
            }
        }
        if (cache != null && !targets.isEmpty()) cache.saveTargets(identity, targets, false);
    }

    private static String fragmentHost(Object fragment) {
        try {
            Object activity = fragment.getClass().getMethod("getActivity").invoke(fragment);
            return activity == null ? "null" : activity.getClass().getName();
        } catch (Throwable failure) { return "unavailable"; }
    }

    synchronized void stop() {
        if (stopped) return;
        stopped = true;
        budget.close();
        handler.removeCallbacks(stopTask);
        int failures = 0;
        for (Map.Entry<Method, HookHandle> entry : handles.entrySet()) {
            try {
                entry.getValue().unhook();
                ledger.retire(ids.get(entry.getKey()), "diagnosticBudgetEnd");
            } catch (Throwable failure) {
                failures++;
                log.error("splash diagnostic unhook failed; observer is inert", failure);
            }
        }
        handles.clear();
        ids.clear();
        log.info("splashDiagnostic stopped relMs=" + (SystemClock.elapsedRealtime() - start)
                + " unhookFailures=" + failures + " polling=false " + ledger.summaryLine("DIAGNOSTIC_STOP"));
    }
}
