package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/**
 * Event-driven bootstrap coordinator.
 *
 * <pre>
 * BOOTSTRAP → WAIT_RUNTIME_DEX → CACHE_VERIFY → SPLASH_CRITICAL
 *           → SPLASH_READY → FULL_RESOLVE → READY
 * failure: DEGRADED
 * </pre>
 *
 * Normal triggers are Application.attach, runtime class load and Activity
 * pre-create. Timer watchdogs are only a last-resort fallback.
 */
final class HookCoordinator implements SplashHooks.ActivityObserver,
        RuntimeDexObserver.Listener {
    private static final long WATCHDOG_DELAY_MILLIS = 8_000L;
    private static final long DEADLINE_MILLIS = 20_000L;

    private final XposedModule module;
    private final ModuleLog log;
    private final ClassLoader primaryLoader;
    private final SplashHooks splashHooks;
    private final EntityListHooks entityListHooks;
    private final SplashGate splashGate = new SplashGate();
    private final RuntimeDexObserver runtimeDexObserver;
    private final RecoveryController recoveryController;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable ->
            new Thread(runnable, "pool-resolver-worker"));
    private final Object stateLock = new Object();
    private final AtomicBoolean sessionRunning = new AtomicBoolean();
    private final AtomicBoolean bootstrapRetired = new AtomicBoolean();
    private final OnceFlag firstActivityPreRecorded = new OnceFlag();
    private final OnceFlag firstActivityPostRecorded = new OnceFlag();
    private final List<HookHandle> bootstrapHandles = new java.util.ArrayList<>();

    private volatile BootstrapState state = BootstrapState.BOOTSTRAP;
    private volatile Context appContext;
    private volatile BootstrapTrace trace;
    private volatile ResolutionCache cache;
    private volatile TargetIdentity identity;
    private volatile DexKitSession dexKitSession;
    private final Map<String, ResolvedTarget> resolvedTargets = new LinkedHashMap<>();
    private volatile ClassLoader activeRuntimeLoader;
    private volatile Method installedFeedMethod;
    private volatile String installedSplashClass;
    private volatile boolean splashCandidateSeenBeforeReady;
    private volatile boolean splashFinishedByHook;
    private volatile boolean terminalCleaned;
    private int sessionAttempt;

    HookCoordinator(XposedModule module, ModuleLog log, ClassLoader primaryLoader) {
        this.module = module;
        this.log = log;
        this.primaryLoader = primaryLoader;
        this.splashHooks = new SplashHooks(module, log, this);
        this.entityListHooks = new EntityListHooks(module, log);
        this.runtimeDexObserver = new RuntimeDexObserver(module, log, this);
        this.recoveryController = new RecoveryController(log, null, null);
    }

    void install() throws ReflectiveOperationException {
        markState(BootstrapState.BOOTSTRAP);
        traceAfterContext("packageReady", "loader=" + System.identityHashCode(primaryLoader));

        splashHooks.installInstrumentationFallback();
        runtimeDexObserver.install();
        installApplicationAttachHook();

        mainHandler.postDelayed(() -> watchdog("watchdog 8s"), WATCHDOG_DELAY_MILLIS);
        mainHandler.postDelayed(() -> watchdog("watchdog 20s deadline"), DEADLINE_MILLIS);
    }

    // ------------------------------------------------------------------
    // Bootstrap hooks
    // ------------------------------------------------------------------

    private void installApplicationAttachHook() {
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            HookHandle handle = module.hook(attach)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-application-attach")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object context = chain.getArg(0);
                        if (context instanceof Context) {
                            onApplicationAttached((Context) context);
                        }
                        return result;
                    });
            bootstrapHandles.add(handle);
            traceAfterContext("attachHookInstalled", "before attach");
        } catch (Throwable throwable) {
            log.error("Application.attach bootstrap hook install failed", throwable);
        }
    }

    private void onApplicationAttached(Context context) {
        long start = SystemClock.elapsedRealtime();
        appContext = context.getApplicationContext();
        trace = new BootstrapTrace(appContext);
        trace.mark("attachAfter", "context=" + appContext.getPackageName());
        cache = new ResolutionCache(appContext);
        recoveryController.attachContext(appContext);
        recoveryController.attachTrace(trace);
        markState(BootstrapState.WAIT_RUNTIME_DEX);
        log.info("coordinator attachAfter state=" + state
                + " attachElapsedMs=" + (SystemClock.elapsedRealtime() - start));
        ensureIdentityAsync();
    }

    // ------------------------------------------------------------------
    // Activity pre/post
    // ------------------------------------------------------------------

    @Override
    public void onPreActivityCreate(Activity activity) {
        if (activity == null) {
            return;
        }
        String name = activity.getClass().getName();
        if (firstActivityPreRecorded.tryOnce()) {
            splashGate.markFirstActivity();
            traceAfterContext("firstActivityPre", "class=" + name);
            runtimeDexObserver.notifyFirstActivityPre(activity.getClass().getClassLoader());
        }
        if (SplashHooks.MAIN_ACTIVITY.equals(name)) {
            splashGate.markMainActivity();
            traceAfterContext("mainActivitySeen", "class=" + name);
        }
        if (splashGate.isFallbackSplashCandidate(activity)
                || splashGate.isLegacySplash(activity)) {
            splashCandidateSeenBeforeReady = true;
        }
        ClassLoader activityLoader = activity.getClass().getClassLoader();
        if (activeRuntimeLoader != null && activityLoader != activeRuntimeLoader) {
            onRuntimeLoaderChanged(activityLoader, "activityLoaderChanged:" + name);
        }
        if (state != BootstrapState.READY && state != BootstrapState.DEGRADED) {
            triggerSession("activityPre:" + name);
        }
        maybeScheduleBootstrapRetire();
    }

    @Override
    public void onPostActivityCreate(Activity activity) {
        if (activity == null) {
            return;
        }
        if (SplashHooks.MAIN_ACTIVITY.equals(activity.getClass().getName())) {
            splashGate.markMainActivity();
        }
        if (firstActivityPostRecorded.tryOnce()) {
            traceAfterContext("firstActivityPost", "class=" + activity.getClass().getName());
        }
        maybeScheduleBootstrapRetire();
    }

    @Override
    public boolean shouldFinishSplash(Activity activity) {
        boolean finish = splashGate.shouldFinishSplash(activity);
        if (finish) {
            splashFinishedByHook = true;
        }
        return finish;
    }

    // ------------------------------------------------------------------
    // Runtime dex / loader generation events
    // ------------------------------------------------------------------

    @Override
    public void onRuntimeDexReady(String trigger, ClassLoader runtimeClassLoader) {
        ClassLoader loader = runtimeClassLoader != null ? runtimeClassLoader : primaryLoader;
        long previous = activeRuntimeLoader == null
                ? -1L : System.identityHashCode(activeRuntimeLoader);
        traceAfterContext("runtimeDexReady", "trigger=" + trigger
                + " runtimeLoaderIdentity=" + System.identityHashCode(loader)
                + " previousLoaderIdentity=" + previous);

        if (appContext == null) {
            appContext = currentApplication();
        }
        if (appContext != null && trace == null) {
            trace = new BootstrapTrace(appContext);
        }

        boolean loaderChanged = activeRuntimeLoader != null && activeRuntimeLoader != loader;
        if (activeRuntimeLoader == null || loaderChanged) {
            if (loaderChanged) {
                closeSession("runtimeLoaderChanged");
            }
            activeRuntimeLoader = loader;
            if (dexKitSession == null && trace != null) {
                dexKitSession = new DexKitSession(log, trace, activeRuntimeLoader);
                dexKitSession.notifyLoaderGenerationChanged(
                        loaderChanged ? "runtimeLoaderChanged" : "initial");
            }
        }
        markState(BootstrapState.CACHE_VERIFY);
        log.info("coordinator runtimeDexReady trigger=" + trigger
                + " runtimeLoaderIdentity=" + System.identityHashCode(loader)
                + " previousLoaderIdentity=" + previous
                + " generation=" + (dexKitSession == null ? -1 : dexKitSession.getGeneration()));
        triggerSession("runtimeDex:" + trigger);
    }

    private void onRuntimeLoaderChanged(ClassLoader loader, String reason) {
        long previous = activeRuntimeLoader == null
                ? -1L : System.identityHashCode(activeRuntimeLoader);
        traceAfterContext("runtimeLoaderChanged", "reason=" + reason
                + " runtimeLoaderIdentity=" + System.identityHashCode(loader)
                + " previousLoaderIdentity=" + previous);
        log.info("coordinator runtimeLoaderChanged reason=" + reason
                + " runtimeLoaderIdentity=" + System.identityHashCode(loader)
                + " previousLoaderIdentity=" + previous);
        closeSession(reason);
        activeRuntimeLoader = loader;
        runtimeDexObserver.rearm();
    }

    // ------------------------------------------------------------------
    // Resolution session
    // ------------------------------------------------------------------

    private void triggerSession(String trigger) {
        if (state == BootstrapState.READY || state == BootstrapState.DEGRADED) {
            return;
        }
        if (!sessionRunning.compareAndSet(false, true)) {
            log.info("coordinator session already running trigger=" + trigger);
            return;
        }
        int attempt = ++sessionAttempt;
        traceAfterContext("sessionStart", "trigger=" + trigger + " attempt=" + attempt);
        worker.execute(() -> {
            try {
                runSession(trigger, attempt);
            } catch (Throwable throwable) {
                log.error("coordinator resolution session failed", throwable);
                traceAfterContext("sessionError", "trigger=" + trigger
                        + " error=" + throwable
                        + " stack=" + android.util.Log.getStackTraceString(throwable));
                markState(BootstrapState.DEGRADED);
                cleanupTerminal();
            } finally {
                sessionRunning.set(false);
            }
        });
    }

    private void runSession(String trigger, int attempt) {
        if (appContext == null) {
            appContext = currentApplication();
            if (appContext == null) {
                log.info("coordinator session skipped context=null trigger=" + trigger);
                return;
            }
        }
        if (trace == null) {
            trace = new BootstrapTrace(appContext);
        }
        if (cache == null) {
            cache = new ResolutionCache(appContext);
            recoveryController.attachContext(appContext);
            recoveryController.attachTrace(trace);
        }
        if (identity == null) {
            long start = SystemClock.elapsedRealtime();
            identity = TargetIdentity.compute(appContext);
            trace.mark("stableIdentityComputed",
                    identity.describe() + " elapsedMs=" + (SystemClock.elapsedRealtime() - start));
            log.info("coordinator stable identity: " + identity.describe());
        }

        Map<String, ResolvedTarget> cached = cache.loadTargets(identity);
        trace.mark("cacheLookupStart", "attempt=" + attempt + " trigger=" + trigger);
        Map<String, ResolvedTarget> verified = verifyCacheTargets(cached);
        if (!verified.isEmpty()) {
            applyTargets(verified, "cache");
        }
        if (isSplashReady() && areNormalTargetsReady()) {
            trace.mark("cacheHit", "entries=" + verified.size() + " dexkitScan=false");
            log.info("resolver path=cache hit=true identity=" + identity.shortToken()
                    + " verified=" + verified.size() + " dexkitScan=false state=" + state);
            finishReady();
            return;
        }
        trace.mark("cacheMiss", "verified=" + verified.size() + " total=" + cached.size()
                + " trigger=" + trigger);

        DexKitSession session = ensureSession(trigger);
        if (session == null) {
            markState(BootstrapState.WAIT_RUNTIME_DEX);
            return;
        }
        org.luckypray.dexkit.DexKitBridge bridge = session.ensureBridge(trigger);
        if (bridge == null || !bridge.isValid()) {
            markState(BootstrapState.WAIT_RUNTIME_DEX);
            log.info("resolver bridge unavailable state=WAIT_RUNTIME_DEX trigger=" + trigger);
            return;
        }

        ClassLoader loader = resolveLoader();
        markState(BootstrapState.SPLASH_CRITICAL);
        trace.mark("splashResolveStart", "trigger=" + trigger);
        ResolvedTarget splash = new SplashCriticalResolver(bridge, loader, log).resolve();
        trace.mark("splashResolveEnd",
                "candidates=" + (splash == null ? "0" : "1")
                        + " source=" + (splash == null ? "none" : splash.source));
        if (splash != null) {
            applyTargets(singletonMap(TargetResolver.KEY_SPLASH_BASE, splash), "dexkit");
        } else {
            markState(BootstrapState.WAIT_RUNTIME_DEX);
            runtimeDexObserver.rearm();
            log.info("resolver splash retryable state=WAIT_RUNTIME_DEX"
                    + " reason=zeroOrUnverifiableCandidates trigger=" + trigger);
        }

        markState(BootstrapState.FULL_RESOLVE);
        trace.mark("normalResolveStart", "trigger=" + trigger);
        Map<String, ResolvedTarget> normal = new NormalResolver(bridge, loader, log).resolve();
        trace.mark("normalResolveEnd", "targets=" + normal.keySet());
        applyTargets(normal, "dexkit");

        Map<String, ResolvedTarget> all = currentTargets(verified);
        if (!all.isEmpty()) {
            cache.saveTargets(identity, all);
            trace.mark("cacheSaved", "entries=" + all.size() + " identity=" + identity.shortToken());
        }

        if (isSplashReady() && areNormalTargetsReady()) {
            finishReady();
        } else if (isSplashReady()) {
            markState(BootstrapState.FULL_RESOLVE);
            log.info("resolver splashReady normalIncomplete state=FULL_RESOLVE");
        } else {
            // A zero-candidate splash is retryable and must not be terminal.
            log.info("resolver incomplete splash=" + isSplashReady()
                    + " normal=" + areNormalTargetsReady()
                    + " state=RETRYABLE trigger=" + trigger);
        }

        maybeRecoverAfterSplashResolved();
    }

    private Map<String, ResolvedTarget> verifyCacheTargets(Map<String, ResolvedTarget> cached) {
        Map<String, ResolvedTarget> verified = new LinkedHashMap<>();
        ClassLoader loader = resolveLoader();
        for (ResolvedTarget target : cached.values()) {
            String problem = TargetVerifier.verify(target, loader);
            if (problem == null) {
                verified.put(target.key, target);
            } else if (isClassNotReady(problem)) {
                log.info("cache target pending key=" + target.key
                        + " reason=" + problem + " state=WAIT_RUNTIME_DEX");
                markState(BootstrapState.WAIT_RUNTIME_DEX);
            } else {
                log.info("cache target invalid key=" + target.key
                        + " reason=" + problem + " currentEntryOnly=true");
                cache.removeTargets(identity);
                return new LinkedHashMap<>();
            }
        }
        return verified;
    }

    private boolean isClassNotReady(String problem) {
        return problem != null && (problem.contains("ClassNotFound")
                || problem.contains("class not loadable"));
    }

    private DexKitSession ensureSession(String trigger) {
        if (trace == null) {
            trace = new BootstrapTrace(appContext);
        }
        if (dexKitSession == null) {
            dexKitSession = new DexKitSession(log, trace, resolveLoader());
            dexKitSession.notifyLoaderGenerationChanged("sessionCreated:" + trigger);
        }
        return dexKitSession;
    }

    private ClassLoader resolveLoader() {
        ClassLoader loader = activeRuntimeLoader;
        return loader == null ? primaryLoader : loader;
    }

    private void closeSession(String reason) {
        DexKitSession session = dexKitSession;
        if (session != null) {
            session.close();
            traceAfterContext("bridgeClosed", "reason=" + reason
                    + " generation=" + session.getGeneration());
        }
        dexKitSession = null;
    }

    private void applyTargets(Map<String, ResolvedTarget> targets, String source) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        ClassLoader loader = resolveLoader();
        synchronized (resolvedTargets) {
            resolvedTargets.putAll(targets);
        }
        entityListHooks.updateAccessors(targets, loader);

        ResolvedTarget feed = targets.get(TargetResolver.KEY_FEED);
        if (feed != null && installedFeedMethod == null) {
            Method method = DescriptorUtils.methodForDescriptor(feed.methodDescriptor, loader);
            if (method != null) {
                entityListHooks.install(method);
                installedFeedMethod = method;
                log.info("installed feed hook source=" + source + " method=" + method);
            }
        }

        ResolvedTarget splash = targets.get(TargetResolver.KEY_SPLASH_BASE);
        if (splash != null && installedSplashClass == null) {
            try {
                Class<?> type = DescriptorUtils.classForName(splash.classDescriptor, loader);
                if (type == null) {
                    log.info("splash descriptor not loadable source=" + source
                            + " target=" + splash.describe());
                    return;
                }
                // Resolved class still powers the framework fallback gate...
                splashGate.setResolvedSplashClass(type);
                // ...but SPLASH_READY requires a real specific hook.
                boolean installed = splashHooks.installSpecific(type);
                if (installed) {
                    installedSplashClass = type.getName();
                    traceAfterContext("splashHookInstalled", splash.describe()
                            + " installed=true source=" + source);
                    log.info("installed splash hook source=" + source
                            + " class=" + type.getName());
                } else {
                    traceAfterContext("splashHookInstallFailed", splash.describe()
                            + " source=" + source);
                    log.info("splash specific hook not installed source=" + source
                            + " class=" + type.getName() + " frameworkFallback=true");
                }
            } catch (Throwable throwable) {
                log.info("splash descriptor not loadable yet source=" + source
                        + " target=" + splash.describe());
            }
        }
    }

    private Map<String, ResolvedTarget> currentTargets(Map<String, ResolvedTarget> cached) {
        synchronized (resolvedTargets) {
            return new LinkedHashMap<>(resolvedTargets);
        }
    }

    private boolean isSplashReady() {
        return installedSplashClass != null;
    }

    private boolean areNormalTargetsReady() {
        synchronized (resolvedTargets) {
            return resolvedTargets.containsKey(TargetResolver.KEY_FEED)
                    && resolvedTargets.containsKey(TargetResolver.KEY_GETTER_TEMPLATE)
                    && resolvedTargets.containsKey(TargetResolver.KEY_GETTER_ENTITY_ID)
                    && resolvedTargets.containsKey(TargetResolver.KEY_GETTER_TITLE)
                    && resolvedTargets.containsKey(TargetResolver.KEY_GETTER_ENTITY_TYPE);
        }
    }

    private void finishReady() {
        markState(BootstrapState.READY);
        cleanupTerminal();
        maybeScheduleBootstrapRetire();
        log.info("resolver fullReady state=READY feedInstalled=" + (installedFeedMethod != null)
                + " splashInstalled=" + installedSplashClass);
    }

    /**
     * Event-driven retire. No polling. If the current call is inside the
     * Instrumentation interceptor, retirement is posted to the next main
     * thread message to avoid self-unhook races.
     */
    private void maybeScheduleBootstrapRetire() {
        if (bootstrapRetired.get()) {
            return;
        }
        if (BootstrapRetirePolicy.canRetire(
                state, splashGate.isMainActivitySeen(), installedSplashClass != null)) {
            mainHandler.post(this::retireBootstrap);
        }
    }

    private void retireBootstrap() {
        if (!BootstrapRetirePolicy.canRetire(
                state, splashGate.isMainActivitySeen(), installedSplashClass != null)) {
            return;
        }
        if (!bootstrapRetired.compareAndSet(false, true)) {
            return;
        }
        BootstrapTrace current = trace;
        if (current != null) {
            current.freeze("terminalState",
                    "state=" + state + " bootstrapRetired=true traceFrozen=true"
                            + " elapsedMs=" + current.elapsedSinceStart());
        }
        splashHooks.retireBootstrapHooks();
        log.info("coordinator bootstrapRetired=true state=" + state
                + " traceFrozen=" + (current != null && current.isFrozen()));
    }

    private void cleanupTerminal() {
        synchronized (stateLock) {
            if (terminalCleaned) {
                return;
            }
            terminalCleaned = true;
        }
        mainHandler.removeCallbacksAndMessages(null);
        runtimeDexObserver.close();
        closeSession("terminal");
        worker.shutdown();
        log.info("coordinator bootstrap lifecycle retired executorShutdown=true"
                + " watcherUnhooked=true");
    }

    private void maybeRecoverAfterSplashResolved() {
        if (!splashCandidateSeenBeforeReady || splashFinishedByHook || !isSplashReady()) {
            return;
        }
        if (cache == null || identity == null) {
            return;
        }
        Map<String, ResolvedTarget> persisted = cache.loadTargets(identity);
        ResolvedTarget splash = persisted.get(TargetResolver.KEY_SPLASH_BASE);
        if (splash == null || TargetVerifier.verify(splash, resolveLoader()) != null) {
            log.info("recovery skipped reason=cacheVerificationFailed");
            return;
        }
        recoveryController.attachIdentity(identity);
        recoveryController.markSplashEscaped();
        recoveryController.onSplashResolved(cache);
    }

    private void watchdog(String reason) {
        traceAfterContext("watchdog", reason);
        log.info("coordinator watchdog fired reason=" + reason + " state=" + state);
        if (state == BootstrapState.READY) {
            return;
        }
        if (state == BootstrapState.WAIT_RUNTIME_DEX || state == BootstrapState.CACHE_VERIFY) {
            triggerSession(reason);
            return;
        }
        if (state == BootstrapState.BOOTSTRAP) {
            markState(BootstrapState.WAIT_RUNTIME_DEX);
            triggerSession(reason);
            return;
        }
        if ("watchdog 20s deadline".equals(reason)) {
            markState(BootstrapState.DEGRADED);
            cleanupTerminal();
            maybeScheduleBootstrapRetire();
            log.info("resolver watchdog deadline state=DEGRADED");
        }
    }

    private void markState(BootstrapState next) {
        synchronized (stateLock) {
            if (state.isTerminal() && next != BootstrapState.READY
                    && next != BootstrapState.DEGRADED) {
                return;
            }
            if (state != next) {
                state = next;
                traceAfterContext("state", next.name());
                log.info("coordinator state=" + next);
            }
        }
    }

    private void traceAfterContext(String event, String detail) {
        BootstrapTrace current = trace;
        if (current != null) {
            current.mark(event, detail);
        }
    }

    private void ensureIdentityAsync() {
        worker.execute(() -> {
            if (identity != null || appContext == null) {
                return;
            }
            identity = TargetIdentity.compute(appContext);
            if (trace != null) {
                trace.mark("stableIdentityComputed", identity.describe());
            }
            log.info("coordinator stable identity: " + identity.describe());
        });
    }

    static Context currentApplication() {
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

    private static Map<String, ResolvedTarget> singletonMap(String key, ResolvedTarget target) {
        Map<String, ResolvedTarget> map = new LinkedHashMap<>();
        map.put(key, target);
        return map;
    }
}
