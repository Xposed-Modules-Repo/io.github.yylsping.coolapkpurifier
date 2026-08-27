package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
 *           → FULL_RESOLVE → READY
 * failure: DEGRADED
 * </pre>
 *
 * Normal triggers are Application.attach, runtime class load and Activity
 * pre-create. Timer watchdogs are only a last-resort fallback: the 8s watchdog
 * retries a session from ANY non-terminal state (including FULL_RESOLVE with
 * unsettled feed coverage), and the 20s deadline takes precedence over every
 * intermediate state and always terminates (READY when core filtering works,
 * DEGRADED otherwise). READY itself is two-layer: core hooks installed AND
 * feed coverage settled (every discovered feed method of both anchor classes
 * live-hooked, or deadline). READY/DEGRADED are frozen once entered; a
 * trigger arriving while a session runs is coalesced into exactly one
 * follow-up session instead of being dropped.
 */
final class HookCoordinator implements SplashHooks.ActivityObserver,
        RuntimeDexObserver.Listener {
    private static final long WATCHDOG_DELAY_MILLIS = 8_000L;
    private static final long DEADLINE_MILLIS = 20_000L;
    private static final String WATCHDOG_RETRY_REASON = "watchdog 8s";
    private static final String WATCHDOG_DEADLINE_REASON = "watchdog 20s deadline";

    private static final String ANCHOR_AD_HELPER_DESCRIPTOR =
            DescriptorUtils.classDescriptorOf(NormalResolver.AD_HELPER_CLASS);
    private static final String ANCHOR_ENTITY_LIST_FRAGMENT_DESCRIPTOR =
            DescriptorUtils.classDescriptorOf(NormalResolver.ENTITY_LIST_FRAGMENT_CLASS);

    private final XposedModule module;
    private final ModuleLog log;
    private final ClassLoader primaryLoader;
    private final HookLedger hookLedger = new HookLedger();
    private final SplashHooks splashHooks;
    private final EntityListHooks entityListHooks;
    private final FeatureInstallState featureInstallState = new FeatureInstallState();
    private final SplashGate splashGate = new SplashGate();
    private final RuntimeDexObserver runtimeDexObserver;
    private final RecoveryController recoveryController;
    private final FirstAdaptationToast firstAdaptationToast;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable ->
            new Thread(runnable, "pool-resolver-worker"));
    private final Object stateLock = new Object();
    private final Object configLock = new Object();
    /** Freezes READY/DEGRADED; late sessions can never flip a terminal state. */
    private final TerminalStateGate stateGate = new TerminalStateGate(BootstrapState.BOOTSTRAP);
    /** Coalesces triggers that arrive while a session runs into one follow-up. */
    private final SessionScheduler sessionScheduler = new SessionScheduler();
    private final AtomicBoolean bootstrapRetired = new AtomicBoolean();
    private final AtomicBoolean attachHookRetired = new AtomicBoolean();
    private final AtomicBoolean attachUnhookExecuted = new AtomicBoolean();
    /** Dedicated retry lane: mainHandler is drained by terminal cleanup. */
    private volatile HandlerThread replyRetryThread;
    private volatile Handler replyRetryHandler;
    private volatile Application.ActivityLifecycleCallbacks replyRetryLifecycle;
    private volatile Application replyRetryApplication;
    private volatile ReplyDiscoveryBudget replyBudget;
    private final FeatureRuntimeHealth runtimeHealth = new FeatureRuntimeHealth();
    private final SplashLifecycleGuard splashLifecycleGuard;
    private volatile List<String> coreMissingRequired = java.util.Collections.emptyList();
    private final OnceFlag firstActivityPreRecorded = new OnceFlag();
    private final OnceFlag firstActivityPostRecorded = new OnceFlag();
    private final OnceFlag terminalCleaned = new OnceFlag();
    /** Diagnostic only; bridge ownership lives in ResolutionSessionContext. */
    private final AtomicBoolean resolutionInFlight = new AtomicBoolean();
    private final List<HookHandle> bootstrapHandles = new java.util.ArrayList<>();

    private volatile BootstrapState state = BootstrapState.BOOTSTRAP;
    private volatile Context appContext;
    private volatile BootstrapTrace trace;
    private volatile ResolutionCache cache;
    private volatile TargetIdentity identity;
    private volatile PurifierConfig config;
    private volatile FeatureHooks featureHooks;
    private volatile SettingsHooks settingsHooks;
    private volatile int coolapkMajor;
    /** Descriptor map verified/resolved for the current runtime generation only. */
    private final CurrentGenerationTargets generationTargets =
            new CurrentGenerationTargets(0L);
    private final ResolutionEpoch runtimeEpoch;
    private final TerminalTransaction terminalTransaction;
    private final RuntimeConfigurationTransaction configurationTransaction;
    private volatile boolean splashCandidateSeenBeforeReady;
    private volatile boolean splashFinishedByHook;
    private int sessionAttempt;
    private long nextSessionId;

    private static final class StaleResolutionSessionException extends RuntimeException {
        StaleResolutionSessionException(String commitPoint) {
            super(commitPoint, null, false, false);
        }
    }

    HookCoordinator(XposedModule module, ModuleLog log, ClassLoader primaryLoader) {
        this.module = module;
        this.log = log;
        this.primaryLoader = primaryLoader;
        this.runtimeEpoch = new ResolutionEpoch(primaryLoader);
        this.terminalTransaction = new TerminalTransaction(runtimeEpoch);
        this.configurationTransaction = new RuntimeConfigurationTransaction(runtimeEpoch);
        this.splashHooks = new SplashHooks(module, log, this, hookLedger);
        this.entityListHooks = new EntityListHooks(module, log, hookLedger);
        this.runtimeDexObserver = new RuntimeDexObserver(module, log, this);
        this.recoveryController = new RecoveryController(log, null, null);
        this.firstAdaptationToast = new FirstAdaptationToast(log);
        this.splashLifecycleGuard = new SplashLifecycleGuard(splashGate,
                () -> config == null || config.isEffectiveEnabled(
                        PurifierConfig.Feature.SPLASH, coolapkMajor),
                activity -> splashHooks.finishSplash(activity, "lifecycle"), log);
        runtimeHealth.addListener(this::logRuntimeHealth);
    }

    void install() throws ReflectiveOperationException {
        markState(BootstrapState.BOOTSTRAP);
        traceAfterContext("packageReady", "loader=" + System.identityHashCode(primaryLoader));

        splashHooks.installInstrumentationFallback();
        runtimeDexObserver.install();
        hookLedger.record(HookLedger.Layer.FRAMEWORK, "coordinator",
                "runtime-observer-loadClass-1", "ClassLoader.loadClass(String)");
        hookLedger.record(HookLedger.Layer.FRAMEWORK, "coordinator",
                "runtime-observer-loadClass-2", "ClassLoader.loadClass(String,boolean)");
        installApplicationAttachHook();

        mainHandler.postDelayed(() -> watchdog(WATCHDOG_RETRY_REASON), WATCHDOG_DELAY_MILLIS);
        mainHandler.postDelayed(() -> watchdog(WATCHDOG_DEADLINE_REASON), DEADLINE_MILLIS);
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
                        Object hostApplication = chain.getThisObject();
                        Object baseContext = chain.getArg(0);
                        if (hostApplication instanceof Application
                                && baseContext instanceof Context) {
                            onApplicationAttached(
                                    (Application) hostApplication,
                                    (Context) baseContext);
                        }
                        return result;
                    });
            bootstrapHandles.add(handle);
            hookLedger.record(HookLedger.Layer.FRAMEWORK, "coordinator",
                    "application-attach", "Application.attach(Context)");
            traceAfterContext("attachHookInstalled", "before attach");
        } catch (Throwable throwable) {
            log.error("Application.attach bootstrap hook install failed", throwable);
        }
    }

    private void onApplicationAttached(Application application, Context baseContext) {
        long start = SystemClock.elapsedRealtime();
        try {
            // Application.getApplicationContext() is briefly null on some protected
            // Coolapk builds. The hooked receiver itself is already attached after
            // chain.proceed() and is the canonical lifecycle owner.
            appContext = application != null ? application : baseContext;
            trace = new BootstrapTrace(appContext);
            trace.mark("attachAfter", "context=" + appContext.getPackageName());
            initializeRuntimeConfiguration(appContext);
            cache = new ResolutionCache(appContext);
            recoveryController.attachContext(appContext);
            recoveryController.attachTrace(trace);
            markState(BootstrapState.WAIT_RUNTIME_DEX);
            log.info("coordinator attachAfter state=" + state
                    + " attachElapsedMs=" + (SystemClock.elapsedRealtime() - start));
            ensureIdentityAsync();
            maybeRetireApplicationAttach();
        } catch (Throwable throwable) {
            // Mode A-ZF Phase 1: a failed handoff must never silently retire
            // the bootstrap attach hook. The watchdog/session machinery is the
            // explicit fallback; the hook stays installed for a later retry.
            log.error("attach handoff failed; Application.attach hook retained",
                    throwable);
            traceAfterContext("attachHandoffFailed",
                    "error=" + throwable.getClass().getName());
        }
    }

    /**
     * Mode A-ZF Phase 1: once the coordinator holds everything the attach
     * hook provided (context, config, settings lifecycle, self-sufficient
     * session triggers), the framework hook is unhooked. The unhook runs on
     * the next main-thread message so it never races the interceptor that is
     * currently executing this handoff.
     */
    private void maybeRetireApplicationAttach() {
        AttachHandoffPolicy.HandoffState handoff = attachHandoffState();
        if (!AttachHandoffPolicy.canRetireAttach(handoff)) {
            String missing = AttachHandoffPolicy.missingCondition(handoff);
            log.info("attach hook retained reason=" + missing);
            traceAfterContext("attachRetireDeferred", "reason=" + missing);
            return;
        }
        if (!attachHookRetired.compareAndSet(false, true)) {
            return;
        }
        mainHandler.post(() -> retireApplicationAttachNow("handoffComplete"));
    }

    private AttachHandoffPolicy.HandoffState attachHandoffState() {
        SettingsHooks settings = settingsHooks;
        return new AttachHandoffPolicy.HandoffState(appContext != null,
                config != null,
                settings != null && settings.isLifecycleCallbacksInstalled());
    }

    /**
     * Idempotent unhook of the bootstrap attach handle. The handoff posts it
     * to the main looper, but terminal cleanup drains mainHandler — on a
     * cache-hit boot READY can beat the posted message — so cleanupTerminal
     * also invokes this directly. The second invocation is a no-op.
     */
    private synchronized void retireApplicationAttachNow(String reason) {
        // Terminal cleanup is a second execution path, not permission to
        // bypass a failed handoff. Do not consume the one-shot on deferral.
        AttachHandoffPolicy.HandoffState handoff = attachHandoffState();
        if (!AttachHandoffPolicy.canRetireAttach(handoff)) {
            String missing = AttachHandoffPolicy.missingCondition(handoff);
            log.info("attach hook retained reason=" + missing + " trigger=" + reason);
            traceAfterContext("attachRetireDeferred", "reason=" + missing
                    + " trigger=" + reason);
            return;
        }
        // Serialize with a posted retirement already in progress so the
        // terminal ledger cannot overtake its unhook/ledger publication.
        if (!attachUnhookExecuted.compareAndSet(false, true)) {
            return;
        }
        int unhooked = 0;
        int failed = 0;
        List<HookHandle> retained = new ArrayList<>();
        synchronized (bootstrapHandles) {
            for (HookHandle handle : bootstrapHandles) {
                try {
                    handle.unhook();
                    unhooked++;
                } catch (Throwable throwable) {
                    failed++;
                    retained.add(handle);
                    log.error("Application.attach unhook failed", throwable);
                }
            }
            bootstrapHandles.clear();
            bootstrapHandles.addAll(retained);
        }
        if (retained.isEmpty()) {
            hookLedger.retire("application-attach", reason
                    + " unhooked=" + unhooked);
        }
        boolean retired = retained.isEmpty();
        log.info("coordinator attachHookRetired=" + retired + " reason=" + reason
                + " unhooked=" + unhooked + " failed=" + failed
                + " remaining=" + retained.size());
        traceAfterContext(retired ? "attachHookRetired" : "attachRetireFailed", "reason=" + reason
                + " failed=" + failed);
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
        // Terminal states deliberately do not retain a loader monitor. A
        // core-loader replacement after READY/DEGRADED requires a normal
        // Coolapk process restart and must not reset frozen readiness.
        if (!state.isTerminal() && runtimeEpoch.isActivated()
                && activityLoader != runtimeEpoch.loader()) {
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
        PurifierConfig currentConfig = config;
        if (currentConfig != null
                && !currentConfig.isEffectiveEnabled(
                PurifierConfig.Feature.SPLASH, coolapkMajor)) {
            return false;
        }
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
        if (state.isTerminal()) {
            log.info("coordinator post-terminal runtimeDex ignored trigger=" + trigger
                    + " boundary=processRestartRequired");
            return;
        }
        ClassLoader loader = runtimeClassLoader != null ? runtimeClassLoader : primaryLoader;
        long previous = runtimeEpoch.isActivated()
                ? System.identityHashCode(runtimeEpoch.loader()) : -1L;
        traceAfterContext("runtimeDexReady", "trigger=" + trigger
                + " runtimeLoaderIdentity=" + System.identityHashCode(loader)
                + " previousLoaderIdentity=" + previous);

        if (appContext == null) {
            appContext = currentApplication();
        }
        if (appContext != null && trace == null) {
            trace = new BootstrapTrace(appContext);
        }

        transitionRuntimeLoader(loader, "runtimeDex:" + trigger);
        markState(BootstrapState.CACHE_VERIFY);
        log.info("coordinator runtimeDexReady trigger=" + trigger
                + " runtimeLoaderIdentity=" + System.identityHashCode(loader)
                + " previousLoaderIdentity=" + previous
                + " generation=" + runtimeEpoch.generation());
        triggerSession("runtimeDex:" + trigger);
    }

    /**
     * Rearms the runtime-dex observer for the next retry, but never after a
     * terminal state: a resolution session that is still running when the 20s
     * deadline terminates the coordinator must not resurrect the closed
     * loadClass hooks (they would then stay installed for the whole process).
     */
    private void rearmObserverForRetry() {
        if (state == BootstrapState.READY || state == BootstrapState.DEGRADED) {
            return;
        }
        runtimeDexObserver.rearm();
    }

    private void onRuntimeLoaderChanged(ClassLoader loader, String reason) {
        if (transitionRuntimeLoader(loader, reason)) {
            rearmObserverForRetry();
        }
    }

    private boolean transitionRuntimeLoader(ClassLoader loader, String reason) {
        if (loader == null) {
            return false;
        }
        final ResolutionEpoch.Transition[] result = {null};
        runtimeEpoch.exclusive(() -> {
            // Recheck terminal while holding the same monitor used by every
            // session commit; a READY decision cannot race this transition.
            if (state.isTerminal()) {
                return;
            }
            ResolutionEpoch.Transition transition = runtimeEpoch.transition(loader);
            result[0] = transition;
            if (!transition.changed) {
                return;
            }
            long nextGeneration = transition.generation;
            // Build-cache descriptors can cross generations; live targets cannot.
            generationTargets.beginGeneration(nextGeneration);
            featureInstallState.beginGeneration(nextGeneration);
            entityListHooks.beginGeneration(nextGeneration, loader);
            FeatureHooks hooks = featureHooks;
            if (hooks != null) {
                hooks.beginGeneration(nextGeneration, loader);
                // Mode A-ZF Phase 3: the temporary loadClass hooks are NOT
                // installed here. A resolution session decides after applying
                // its targets whether lazy discovery is still needed.
            }
        });
        ResolutionEpoch.Transition transition = result[0];
        if (transition == null) {
            return false;
        }
        if (!transition.changed) {
            return false;
        }
        long nextGeneration = transition.generation;
        traceAfterContext("runtimeLoaderChanged", "reason=" + reason
                + " generation=" + nextGeneration
                + " runtimeLoaderIdentity=" + System.identityHashCode(loader)
                + " previousLoaderIdentity=" + transition.previousLoaderIdentity
                + " readinessReset=true t4Gate=false splashReady=false");
        log.info("coordinator runtimeLoaderChanged reason=" + reason
                + " generation=" + nextGeneration
                + " runtimeLoaderIdentity=" + System.identityHashCode(loader)
                + " previousLoaderIdentity=" + transition.previousLoaderIdentity
                + " readinessReset=true t4Gate=false splashReady=false");
        if (resolutionInFlight.get()) {
            // The old context is superseded but its bridge remains owned by
            // its worker. Coalesce exactly one current-generation follow-up.
            triggerSession("loaderGeneration:" + reason);
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Resolution session
    // ------------------------------------------------------------------

    private void triggerSession(String trigger) {
        SessionScheduler.SubmitResult result =
                sessionScheduler.submit(trigger, state.isTerminal(), this::launchSession);
        if (result == SessionScheduler.SubmitResult.COALESCED) {
            // The trigger is folded into the follow-up session that runs when
            // the current one finishes — never dropped, never parallel.
            log.info("coordinator session coalesced into pending follow-up trigger="
                    + trigger);
        }
    }

    /**
     * Starts one session on the resolver worker. Called by the scheduler for
     * both direct submissions and coalesced follow-ups; at most one session
     * is in flight at any time.
     */
    private void launchSession(String trigger, boolean followUp) {
        int attempt = ++sessionAttempt;
        Context sessionAppContext = appContext;
        if (sessionAppContext == null) {
            // Bootstrap-only fallback. Once Application.attach provides a
            // reliable Context, every resolver session captures and reuses it.
            sessionAppContext = currentApplication();
            if (sessionAppContext != null) {
                appContext = sessionAppContext;
            }
        }
        ResolutionSessionContext sessionContext =
                runtimeEpoch.capture(++nextSessionId, sessionAppContext);
        if (followUp) {
            traceAfterContext("sessionPending", "trigger=" + trigger + " attempt=" + attempt);
            log.info("coordinator sessionPending dispatched trigger=" + trigger
                    + " attempt=" + attempt);
        }
        traceAfterContext("sessionStart", "trigger=" + trigger + " attempt=" + attempt
                + " " + sessionContext.describe());
        resolutionInFlight.set(true);
        try {
            worker.execute(() -> {
                try {
                    runSession(sessionContext, trigger, attempt);
                } catch (StaleResolutionSessionException stale) {
                    discardStaleSession(sessionContext, trigger, attempt, stale.getMessage());
                } catch (Throwable throwable) {
                    if (!isSessionCurrent(sessionContext)) {
                        discardStaleSession(sessionContext, trigger, attempt,
                                "exceptionAfterSupersede:" + throwable.getClass().getName());
                    } else {
                        String failureReason = sessionFailureReason(throwable);
                        log.error("coordinator resolution session failed", throwable);
                        traceAfterContext("sessionError", "trigger=" + trigger
                                + " error=" + throwable
                                + " stack=" + android.util.Log.getStackTraceString(throwable));
                        TerminalTransaction.Result terminal = commitSessionTerminal(
                                sessionContext,
                                TerminalTransaction.Intent.FORCE_DEGRADED,
                                failureReason);
                        if (!terminal.sessionCurrent) {
                            discardStaleSession(sessionContext, trigger, attempt,
                                    "errorTerminalCommitAfterSupersede");
                        } else if (terminal.snapshot != null) {
                            completeDegraded(terminal.snapshot, failureReason);
                        }
                    }
                } finally {
                    // The worker that created/used the bridge is its only
                    // closer. A loader-transition thread merely supersedes.
                    sessionContext.close();
                    runtimeEpoch.finish(sessionContext);
                    traceAfterContext("sessionResourceClosed", sessionContext.describe());
                    resolutionInFlight.set(false);
                    sessionScheduler.onFinished(state.isTerminal(),
                            HookCoordinator.this::launchSession);
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            // The worker was shut down by a terminal decision between the
            // scheduler check and this dispatch; release the slot without a
            // follow-up.
            log.info("coordinator session dispatch rejected trigger=" + trigger
                    + " state=" + state);
            sessionContext.close();
            runtimeEpoch.finish(sessionContext);
            resolutionInFlight.set(false);
            sessionScheduler.onFinished(true, (nextTrigger, nextFollowUp) -> {
            });
        }
    }

    static String sessionFailureReason(Throwable failure) {
        return failure instanceof DexKitNativeLoader.LoadFailure
                ? DexKitNativeLoader.FAILURE_REASON
                : "sessionError:" + failure.getClass().getName();
    }

    private void runSession(ResolutionSessionContext sessionContext,
                            String trigger, int attempt) {
        if (!isSessionCurrent(sessionContext)) {
            // The deadline terminated the coordinator while this session was
            // queued/running; its (still useful, additive) results are logged
            // by the resolvers, but it must not touch lifecycle state.
            traceAfterContext("sessionDiscarded", "trigger=" + trigger
                    + " state=" + state + " attempt=" + attempt);
            log.info("coordinator late session discarded trigger=" + trigger
                    + " state=" + state + " " + sessionContext.describe());
            throw new StaleResolutionSessionException("sessionEntry");
        }
        if (appContext == null && sessionContext.appContext != null) {
            appContext = sessionContext.appContext;
        }
        if (appContext == null) {
            log.info("coordinator session skipped context=null trigger=" + trigger);
            return;
        }
        if (trace == null) {
            trace = new BootstrapTrace(appContext);
        }
        initializeRuntimeConfiguration(appContext);
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

        requireCurrent(sessionContext, "cacheLookup");
        ResolutionCache.CachedResolution cachedRes = cache.loadResolution(identity);
        trace.mark("cacheLookupStart", "attempt=" + attempt + " trigger=" + trigger
                + " persistedSettled=" + cachedRes.coverageSettled);
        Map<String, ResolvedTarget> verified =
                verifyCacheTargets(sessionContext, cachedRes.targets);
        if (!verified.isEmpty()) {
            requireApplied(applyTargets(sessionContext, verified, "cache"),
                    "cacheApply");
        }
        // Cache-hit READY needs BOTH the persisted anchors-settled flag
        // (deadline-settled or partial saves must re-resolve) AND every
        // listed feed method live-hooked in THIS process — staged DEX can
        // still make some of them unloadable this early. A selected reply
        // holder that is not installed also blocks the fast path (Mode A-ZF
        // Phase 3): the session must fall through so the temporary
        // loadClass hooks can discover it, matching the old window.
        FeatureHooks hooksForFastPath = featureHooks;
        boolean replyBlocksFastPath = hooksForFastPath != null
                && LazyDiscoveryPolicy.blocksCacheFastPath(
                        hooksForFastPath.isReplyHolderSelected(),
                        hooksForFastPath.isReplyHolderInstalled(),
                        cachedRes.targets.containsKey(TargetResolver.KEY_REPLY_HOLDER)
                                || cachedRes.targets.containsKey(TargetResolver.KEY_REPLY_SELF_DRAW),
                        coolapkMajor >= 16);
        if (config.pendingKind() == PurifierConfig.PendingKind.NONE
                && !replyBlocksFastPath
                && isCoreReady() && areSelectedFeaturesReady(sessionContext)
                && cachedRes.coverageSettled
                && cachedFeedMethodsAllLive(sessionContext, cachedRes.targets)) {
            trace.mark("cacheHit", "entries=" + verified.size() + " dexkitScan=false");
            log.info("resolver path=cache hit=true identity=" + identity.shortToken()
                    + " verified=" + verified.size() + " dexkitScan=false state=" + state);
            finishReady(sessionContext, "cache");
            return;
        }
        trace.mark("cacheMiss", "verified=" + verified.size()
                + " total=" + cachedRes.targets.size()
                + " trigger=" + trigger);
        // Mode A-ZF Phase 3: a cache miss means this session needs the
        // discovery channel for late semantic classes. Arming it here keeps
        // the observer window as wide as the pre-refactor eager install.
        ensureLazyDiscoveryAfterSession("cacheMiss:" + trigger);

        requireCurrent(sessionContext, "bridgeCreate");
        org.luckypray.dexkit.DexKitBridge bridge = sessionContext.ensureBridge(
                log, trace, trigger);
        if (bridge == null || !bridge.isValid()) {
            requireCurrent(sessionContext, "bridgeUnavailable");
            commitState(sessionContext, BootstrapState.WAIT_RUNTIME_DEX);
            log.info("resolver bridge unavailable state=WAIT_RUNTIME_DEX trigger=" + trigger);
            rearmObserverForRetry();
            ensureLazyDiscoveryAfterSession("bridgeUnavailable:" + trigger);
            return;
        }

        ClassLoader loader = sessionContext.loader;
        // Only reachable after a cache miss / invalid cache. Cache-hit runs
        // return earlier, so the Toast is never shown on cache hits.
        PurifierConfig.PendingKind pending = config == null
                ? PurifierConfig.PendingKind.DEFAULT : config.pendingKind();
        if (pending == PurifierConfig.PendingKind.NONE
                && config != null && config.hasNonDefaultSelections()) {
            // A different Coolapk identity has no cache yet, but the user has
            // already selected Issue2 options. Adapt all selected targets and
            // use the selection wording instead of pretending this is the
            // three-default first run.
            pending = PurifierConfig.PendingKind.SELECTION;
        }
        PurifierConfig.PendingKind toastKind = pending == PurifierConfig.PendingKind.SELECTION
                ? PurifierConfig.PendingKind.SELECTION : PurifierConfig.PendingKind.DEFAULT;
        requireApplied(runtimeEpoch.commit(sessionContext,
                () -> firstAdaptationToast.showStartOnce(appContext, toastKind)),
                "adaptationToast");
        commitState(sessionContext, BootstrapState.SPLASH_CRITICAL);
        trace.mark("splashResolveStart", "trigger=" + trigger);
        List<ResolvedTarget> splashes = new SplashCriticalResolver(bridge, loader, log).resolve();
        requireCurrent(sessionContext, "splashResolveEnd");
        trace.mark("splashResolveEnd", "candidates=" + splashes.size());
        if (!splashes.isEmpty()) {
            Map<String, ResolvedTarget> splashTargets = new LinkedHashMap<>();
            for (int i = 0; i < splashes.size(); i++) {
                String key = TargetResolver.indexedKey(TargetResolver.KEY_SPLASH_BASE, i);
                splashTargets.put(key, splashes.get(i).withKey(key));
            }
            requireApplied(applyTargets(sessionContext, splashTargets, "dexkit"),
                    "splashApply");
        } else {
            commitState(sessionContext, BootstrapState.WAIT_RUNTIME_DEX);
            rearmObserverForRetry();
            log.info("resolver splash retryable state=WAIT_RUNTIME_DEX"
                    + " reason=zeroOrUnverifiableCandidates trigger=" + trigger);
            ensureLazyDiscoveryAfterSession("splashRetryable:" + trigger);
        }

        Map<String, ResolvedTarget> featureTargets = new Issue2Resolver(
                bridge, loader, log).resolve(config, coolapkMajor);
        if (config.isEnabled(PurifierConfig.Feature.REPLY_SPONSOR) && coolapkMajor >= 16) {
            ResolvedTarget reply = new ReplySelfDrawResolver(bridge, loader, log).resolve();
            if (reply != null) featureTargets.put(reply.key, reply);
        }
        requireCurrent(sessionContext, "featureResolveEnd");
        trace.mark("featureResolveEnd", "targets=" + featureTargets.keySet());
        requireApplied(applyTargets(sessionContext, featureTargets, "dexkit-feature"),
                "featureApply");

        commitState(sessionContext, BootstrapState.FULL_RESOLVE);
        trace.mark("normalResolveStart", "trigger=" + trigger);
        Map<String, ResolvedTarget> normal = new NormalResolver(bridge, loader, log).resolve();
        requireCurrent(sessionContext, "normalResolveEnd");
        trace.mark("normalResolveEnd", "targets=" + normal.keySet());
        requireApplied(applyTargets(sessionContext, normal, "dexkit"), "normalApply");

        // Real-scan anchor snapshot: an anchor is COMPLETE only when every
        // feed-shaped method THIS scan discovered for it is live-hooked.
        List<FeedCoverage.Anchor> anchors = evaluateAnchorCoverage(normal, loader);
        boolean coverageSettled = FeedCoverage.settledByAnchors(anchors);

        Map<String, ResolvedTarget> all = currentTargets(sessionContext);
        if (!all.isEmpty()) {
            Map<String, ResolvedTarget> saved = saveCurrentTargets(
                    sessionContext, all, coverageSettled);
            trace.mark("cacheSaved", "entries=" + saved.size()
                    + " identity=" + identity.shortToken()
                    + " coverageSettled=" + coverageSettled);
        }

        boolean legacyCoreReady = isCoreReady();
        requireCurrent(sessionContext, "readinessEvaluate");
        boolean selectedFeaturesReady = areSelectedFeaturesReady(sessionContext);
        List<String> missingRequired = selectedFeaturesReady
                ? java.util.Collections.emptyList()
                : missingSelectedFeatureTargets(sessionContext);
        boolean coreReady = legacyCoreReady && selectedFeaturesReady;
        ReadinessPolicy.SessionOutcome outcome =
                ReadinessPolicy.sessionOutcome(coreReady, coverageSettled);
        switch (outcome) {
            case READY:
                // Final direct-install attempt for any still-missing semantic
                // class before the terminal cleanup retires the observers.
                ensureLazyDiscoveryAfterSession("readyOutcome:" + trigger);
                maybeRecoverAfterSplashResolved(sessionContext);
                finishReady(sessionContext, "anchors");
                return;
            case RETRY_COVERAGE:
                // Core filtering already works, but an anchor is not fully
                // harvested: the shell may still append its DEX, or a hook
                // install failed. Deterministic retry: the re-armed observer
                // fires on the next business class load, and
                // The next session owns a fresh DexKit bridge, so appended DEX
                // of the same loader becomes visible without sharing a native
                // handle across worker transactions.
                rearmObserverForRetry();
                commitState(sessionContext, BootstrapState.FULL_RESOLVE);
                log.info("resolver coreReady coveragePending state=FULL_RESOLVE"
                        + " coverage=[" + FeedCoverage.describe(anchors) + "]"
                        + " trigger=" + trigger);
                break;
            default:
                // Core capability still missing (splash, feed hooks or
                // accessors). Also deterministically retried: observer
                // re-armed here, 8s watchdog retries FULL_RESOLVE too.
                // Feature-only misses must not re-arm the class-load observer:
                // DexKit's own class loading would otherwise trigger an
                // unbounded self-feedback loop. The 8s watchdog still gives
                // the protected app one deterministic rescan opportunity.
                if (!legacyCoreReady) {
                    rearmObserverForRetry();
                }
                if (isSplashReady()) {
                    commitState(sessionContext, BootstrapState.FULL_RESOLVE);
                    log.info("resolver splashReady coreIncomplete state=FULL_RESOLVE"
                            + " selectedFeaturesReady=" + selectedFeaturesReady
                            + " missingRequired=" + missingRequired
                            + " observerRearmed=" + (!legacyCoreReady)
                            + " trigger=" + trigger);
                } else {
                    // A zero-candidate splash is retryable and must not be terminal.
                    log.info("resolver incomplete splash=false core=false"
                            + " state=RETRYABLE trigger=" + trigger);
                }
                break;
        }

        requireCurrent(sessionContext, "sessionFinish");
        maybeRecoverAfterSplashResolved(sessionContext);
        ensureLazyDiscoveryAfterSession(trigger);
    }

    /**
     * Mode A-ZF Phase 3: the ONLY installer of the temporary feature lazy
     * loadClass hooks. Called when a resolution session finishes (or takes a
     * retryable exit) with selected semantic targets still missing; the hooks
     * self-retire as soon as the last target installs.
     */
    private void ensureLazyDiscoveryAfterSession(String trigger) {
        FeatureHooks hooks = featureHooks;
        if (hooks == null) {
            return;
        }
        hooks.ensureLazyDiscovery(
                generationTargets.snapshot(hooks.generation()), trigger);
    }

    private boolean isSessionCurrent(ResolutionSessionContext sessionContext) {
        return !state.isTerminal() && runtimeEpoch.isCurrent(sessionContext);
    }

    private void requireCurrent(ResolutionSessionContext sessionContext,
                                String commitPoint) {
        if (!isSessionCurrent(sessionContext)) {
            throw new StaleResolutionSessionException(commitPoint);
        }
    }

    private static void requireApplied(boolean applied, String commitPoint) {
        if (!applied) {
            throw new StaleResolutionSessionException(commitPoint);
        }
    }

    private void commitState(ResolutionSessionContext sessionContext,
                             BootstrapState next) {
        final boolean[] applied = {false};
        boolean current = runtimeEpoch.commit(sessionContext, () -> {
            if (!state.isTerminal()) {
                markState(next);
                applied[0] = true;
            }
        });
        requireApplied(current && applied[0], "stateCommit:" + next);
    }

    private void discardStaleSession(ResolutionSessionContext sessionContext,
                                     String trigger, int attempt, String commitPoint) {
        sessionContext.supersede();
        traceAfterContext("sessionDiscarded", "trigger=" + trigger
                + " attempt=" + attempt + " commitPoint=" + commitPoint
                + " " + sessionContext.describe());
        log.info("coordinator stale session discarded trigger=" + trigger
                + " attempt=" + attempt + " commitPoint=" + commitPoint
                + " apply=false cacheSave=false degraded=false "
                + sessionContext.describe());
        if (!state.isTerminal()) {
            triggerSession("superseded:" + commitPoint);
        }
    }

    private Map<String, ResolvedTarget> saveCurrentTargets(
            ResolutionSessionContext sessionContext,
            Map<String, ResolvedTarget> candidates,
            boolean coverageSettled) {
        final Map<String, ResolvedTarget> saved = new LinkedHashMap<>();
        final boolean[] savedCurrentGeneration = {false};
        boolean current = runtimeEpoch.commit(sessionContext, () -> {
            for (ResolvedTarget target : candidates.values()) {
                if (TargetVerifier.verify(target, sessionContext.loader) == null) {
                    saved.put(target.key, target);
                }
            }
            if (!isSessionCurrent(sessionContext)
                    || !generationTargets.replace(sessionContext.generation, saved)) {
                return;
            }
            if (!saved.isEmpty()) {
                cache.saveTargets(identity, saved, coverageSettled);
            }
            savedCurrentGeneration[0] = isSessionCurrent(sessionContext);
        });
        requireApplied(current && savedCurrentGeneration[0], "cacheSave");
        return saved;
    }

    private Map<String, ResolvedTarget> verifyCacheTargets(
            ResolutionSessionContext sessionContext,
            Map<String, ResolvedTarget> cached) {
        Map<String, ResolvedTarget> verified = new LinkedHashMap<>();
        ClassLoader loader = sessionContext.loader;
        for (ResolvedTarget target : cached.values()) {
            requireCurrent(sessionContext, "cacheVerify:" + target.key);
            String problem = TargetVerifier.verify(target, loader);
            if (problem == null) {
                verified.put(target.key, target);
            } else if (isClassNotReady(problem)) {
                log.info("cache target pending key=" + target.key
                        + " reason=" + problem + " state=WAIT_RUNTIME_DEX");
                commitState(sessionContext, BootstrapState.WAIT_RUNTIME_DEX);
            } else {
                log.info("cache target invalid key=" + target.key
                        + " reason=" + problem + " currentEntryOnly=true");
                requireApplied(runtimeEpoch.commit(sessionContext,
                        () -> cache.removeTargets(identity)), "cacheRemoveInvalid");
                return new LinkedHashMap<>();
            }
        }
        return verified;
    }

    private boolean isClassNotReady(String problem) {
        return problem != null && (problem.contains("ClassNotFound")
                || problem.contains("class not loadable"));
    }

    private boolean applyTargets(ResolutionSessionContext sessionContext,
                                 Map<String, ResolvedTarget> targets, String source) {
        if (targets == null || targets.isEmpty()) {
            return isSessionCurrent(sessionContext);
        }
        ClassLoader loader = sessionContext.loader;
        long generation = sessionContext.generation;
        final boolean[] committed = {false};
        boolean current = runtimeEpoch.commit(sessionContext, () -> {
            generationTargets.merge(generation, targets);
            Map<String, ResolvedTarget> merged = generationTargets.snapshot(generation);
            // Hold the epoch transaction through framework installation: a
            // transition cannot occur after validation but before the Hook.
            entityListHooks.updateAccessors(merged, loader, generation);
            FeatureHooks currentFeatureHooks = featureHooks;
            if (currentFeatureHooks != null) {
                currentFeatureHooks.installTargets(merged, loader, generation);
            }

            for (Map.Entry<String, ResolvedTarget> entry : targets.entrySet()) {
            if (!TargetResolver.isFeedKey(entry.getKey())) {
                continue;
            }
            ResolvedTarget feed = entry.getValue();
            Method method = DescriptorUtils.methodForDescriptor(feed.methodDescriptor, loader);
            if (method != null) {
                entityListHooks.install(method);
            } else {
                log.info("feed descriptor not loadable source=" + source
                        + " key=" + entry.getKey() + " target=" + feed.describe());
            }
            }
            if (entityListHooks.hookedMethodCount() > 0) {
            log.info("installed feed hooks source=" + source
                    + " total=" + entityListHooks.hookedMethodCount());
            }

            for (Map.Entry<String, ResolvedTarget> entry : targets.entrySet()) {
            if (!TargetResolver.isSplashKey(entry.getKey())) {
                continue;
            }
            ResolvedTarget splash = entry.getValue();
            try {
                Class<?> type = DescriptorUtils.classForName(splash.classDescriptor, loader);
                if (type == null) {
                    log.info("splash descriptor not loadable source=" + source
                            + " target=" + splash.describe());
                    continue;
                }
                splashGate.addResolvedSplashClass(type);
                boolean installed = splashHooks.installSpecific(type);
                if (installed && generation == runtimeEpoch.generation()
                        && loader == runtimeEpoch.loader()
                        && type.getClassLoader() == loader) {
                    featureInstallState.markSplashHook(generation, type.getName());
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
            committed[0] = isSessionCurrent(sessionContext);
        });
        return current && committed[0];
    }

    private Map<String, ResolvedTarget> currentTargets(
            ResolutionSessionContext sessionContext) {
        if (!isSessionCurrent(sessionContext)) {
            return new LinkedHashMap<>();
        }
        return generationTargets.snapshot(sessionContext.generation);
    }

    private boolean isSplashReady() {
        return featureInstallState.hasSplashHook();
    }

    /** Core ad-filtering capability: splash covered, live feed hooks, accessors complete. */
    private boolean isCoreReady() {
        return ReadinessPolicy.isCoreReady(isSplashReady(),
                entityListHooks.hookedMethodCount(),
                entityListHooks.isAccessorsComplete());
    }

    /** Called only by TerminalTransaction while the runtime epoch is held. */
    private TerminalSnapshot.Readiness readTerminalReadiness(
            long generation, ClassLoader loader) {
        List<String> missing = new ArrayList<>();
        boolean splashReady = isSplashReady();
        int feedHooks = entityListHooks.hookedMethodCount();
        boolean accessorsReady = entityListHooks.isAccessorsComplete();
        if (!splashReady) {
            missing.add("core:splashHook");
        }
        if (feedHooks <= 0) {
            missing.add("core:feedHook");
        }
        if (!accessorsReady) {
            missing.add("core:entityAccessors");
        }

        Map<String, ResolvedTarget> targets = generationTargets.snapshot(generation);
        FeatureHooks hooks = featureHooks;
        boolean featureTargetsReady = hooks != null && hooks.requiredTargetsReady(targets);
        if (hooks == null) {
            missing.add("featureHooks");
        } else {
            missing.addAll(hooks.missingRequiredTargets(targets));
        }
        boolean legacyCoreReady = ReadinessPolicy.isCoreReady(
                splashReady, feedHooks, accessorsReady);
        boolean allRequiredReady = legacyCoreReady && featureTargetsReady;
        // generation/loader are parameters supplied from the epoch-held
        // transaction. Touch loader here so future changes cannot silently
        // replace this probe with a coordinator-global loader lookup.
        if (loader == null) {
            missing.add("runtime:loader");
            allRequiredReady = false;
        }
        return new TerminalSnapshot.Readiness(allRequiredReady, missing);
    }

    private boolean areSelectedFeaturesReady(ResolutionSessionContext sessionContext) {
        FeatureHooks hooks = featureHooks;
        return isSessionCurrent(sessionContext)
                && hooks != null && hooks.requiredTargetsReady(currentTargets(sessionContext));
    }

    private List<String> missingSelectedFeatureTargets(
            ResolutionSessionContext sessionContext) {
        FeatureHooks hooks = featureHooks;
        return hooks == null
                ? java.util.Collections.singletonList("featureHooks")
                : hooks.missingRequiredTargets(currentTargets(sessionContext));
    }

    /**
     * Builds the per-anchor coverage snapshot from a REAL resolver scan: the
     * fallback tier of {@link NormalResolver} scans both anchor classes via
     * reflection whenever they are loadable, so a loadable anchor always has
     * all of its declared feed-shaped methods inside {@code scanOutput}.
     */
    private List<FeedCoverage.Anchor> evaluateAnchorCoverage(
            Map<String, ResolvedTarget> scanOutput, ClassLoader loader) {
        List<FeedCoverage.Anchor> anchors = new ArrayList<>();
        anchors.add(anchorCoverageFor(ANCHOR_AD_HELPER_DESCRIPTOR, scanOutput, loader));
        anchors.add(anchorCoverageFor(
                ANCHOR_ENTITY_LIST_FRAGMENT_DESCRIPTOR, scanOutput, loader));
        return anchors;
    }

    private FeedCoverage.Anchor anchorCoverageFor(String classDescriptor,
                                                  Map<String, ResolvedTarget> scanOutput,
                                                  ClassLoader loader) {
        boolean loadable = false;
        try {
            loadable = DescriptorUtils.classForName(classDescriptor, loader) != null;
        } catch (Throwable ignored) {
        }
        List<String> discovered = new ArrayList<>();
        for (ResolvedTarget target : scanOutput.values()) {
            if (TargetResolver.isFeedKey(target.key)
                    && classDescriptor.equals(target.classDescriptor)
                    && target.methodDescriptor != null
                    && !target.methodDescriptor.isEmpty()) {
                discovered.add(target.methodDescriptor);
            }
        }
        return FeedCoverage.anchor(classDescriptor, loadable, discovered, descriptor -> {
            Method method = DescriptorUtils.methodForDescriptor(descriptor, loader);
            return method != null && entityListHooks.isHooked(method);
        });
    }

    /**
     * Cache-hit guard: every persisted feed method must resolve to a live
     * installed hook in THIS process. Staged DEX that keeps one of them
     * unloadable fails here and forces a fresh resolver run.
     */
    private boolean cachedFeedMethodsAllLive(ResolutionSessionContext sessionContext,
                                             Map<String, ResolvedTarget> cachedTargets) {
        if (cachedTargets.isEmpty()) {
            return false;
        }
        requireCurrent(sessionContext, "cacheFeedLiveCheck");
        ClassLoader loader = sessionContext.loader;
        int feedEntries = 0;
        for (ResolvedTarget target : cachedTargets.values()) {
            if (!TargetResolver.isFeedKey(target.key)) {
                continue;
            }
            feedEntries++;
            Method method = DescriptorUtils.methodForDescriptor(
                    target.methodDescriptor, loader);
            if (method == null || !entityListHooks.isHooked(method)) {
                return false;
            }
        }
        return feedEntries > 0;
    }

    private void finishReady(ResolutionSessionContext sessionContext,
                             String coverageSource) {
        TerminalTransaction.Result result = commitSessionTerminal(
                sessionContext, TerminalTransaction.Intent.REQUIRE_READY,
                coverageSource);
        if (!result.sessionCurrent) {
            throw new StaleResolutionSessionException("readyCommit");
        }
        if (result.snapshot == null) {
            // A late session (or an already-READY deadline) cannot flip or
            // duplicate the terminal decision. If same-generation readiness
            // no longer holds, leave the lifecycle retryable for the deadline.
            log.info("resolver late READY ignored terminal=" + state
                    + " coverageSettledBy=" + coverageSource);
            return;
        }
        completeReady(result.snapshot, coverageSource);
    }

    private TerminalTransaction.Result commitSessionTerminal(
            ResolutionSessionContext sessionContext,
            TerminalTransaction.Intent intent, String source) {
        return terminalTransaction.commitSession(sessionContext, intent, source,
                this::readTerminalReadiness, new TerminalTransaction.StateAccess() {
                    @Override
                    public boolean isTerminal() {
                        return state.isTerminal();
                    }

                    @Override
                    public TerminalStateGate.Transition mark(BootstrapState next) {
                        return markTerminalState(next);
                    }
                });
    }

    private TerminalTransaction.Result commitDeadlineTerminal(String source) {
        return terminalTransaction.commitDeadline(source,
                this::readTerminalReadiness, new TerminalTransaction.StateAccess() {
                    @Override
                    public boolean isTerminal() {
                        return state.isTerminal();
                    }

                    @Override
                    public TerminalStateGate.Transition mark(BootstrapState next) {
                        return markTerminalState(next);
                    }
                });
    }

    private void recordTerminalSnapshot(TerminalSnapshot snapshot) {
        // State/trace/log side effects deliberately happen after the epoch
        // transaction. Only the gate + volatile state write are lock-held.
        traceAfterContext("state", snapshot.terminalState.name());
        log.info("coordinator state=" + snapshot.terminalState);
        traceAfterContext("terminalSnapshot", snapshot.describe());
        log.info("coordinator terminalSnapshot " + snapshot.describe());
    }

    private void completeReady(TerminalSnapshot snapshot, String coverageSource) {
        recordTerminalSnapshot(snapshot);
        cleanupTerminal(snapshot);
        scheduleReplyDiscoveryRetry();
        if (firstAdaptationToast.hasStarted() && config != null) {
            boolean persisted = config.markAdapted();
            firstAdaptationToast.showCompletionOnce(appContext);
            log.info("adaptation completed kind=" + firstAdaptationToast.activeKind()
                    + " configMarkerPersisted=" + persisted);
        } else if (config != null && config.pendingKind() != PurifierConfig.PendingKind.NONE) {
            // A fully valid cache means no first adaptation was needed for the
            // pending UI revision. Clear the stale marker without a Toast.
            config.markAdapted();
        }
        maybeScheduleBootstrapRetire();
        log.info("resolver fullReady state=READY feedInstalled="
                + entityListHooks.hookedMethodCount()
                + " splashSpecificInstalled=" + featureInstallState.splashClasses()
                + " splashCoveredByLegacy=" + splashLegacyCoverageSummary()
                + " coverageSettledBy=" + coverageSource
                + " terminalGeneration=" + snapshot.generation
                + " terminalLoaderIdentity="
                + System.identityHashCode(snapshot.loader));
    }

    private void completeDegraded(TerminalSnapshot snapshot, String reason) {
        recordTerminalSnapshot(snapshot);
        cleanupTerminal(snapshot);
        maybeScheduleBootstrapRetire();
        log.info("resolver terminal state=DEGRADED reason=" + reason
                + " terminalGeneration=" + snapshot.generation
                + " terminalLoaderIdentity="
                + System.identityHashCode(snapshot.loader)
                + " missingRequired=" + snapshot.missingRequired);
    }

    /**
     * Per legacy splash name: whether a specific hook covers it, or only the
     * retained passive Instrumentation gate does. Never claim passive
     * coverage after the framework safety gate has actually been unhooked.
     */
    private String splashLegacyCoverageSummary() {
        StringBuilder sb = new StringBuilder();
        for (String name : new java.util.TreeSet<>(TargetResolver.LEGACY_SPLASH_CLASS_NAMES)) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            int lastDot = name.lastIndexOf('.');
            sb.append(lastDot < 0 ? name : name.substring(lastDot + 1)).append('=')
                    .append(featureInstallState.hasSplashClass(name)
                            ? "specific" : splashLifecycleGuard.isInstalled()
                                    ? "lifecycle" : splashHooks.isInstrumentationSafetyActive()
                                            ? "passive" : "uncovered");
        }
        return sb.toString();
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
                state, splashGate.isMainActivitySeen(), isSplashReady())) {
            mainHandler.post(this::retireBootstrap);
        }
    }

    private void retireBootstrap() {
        if (!BootstrapRetirePolicy.canRetire(
                state, splashGate.isMainActivitySeen(), isSplashReady())) {
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
        // Coordinator callbacks stop. Instrumentation was already unhooked
        // on READY; only a retained DEGRADED safety gate stays passive.
        splashHooks.retireBootstrapCallbacks();
        log.info("coordinator bootstrapRetired=true state=" + state
                + " traceFrozen=" + (current != null && current.isFrozen()));
    }

    private void cleanupTerminal(TerminalSnapshot terminalSnapshot) {
        // One-shot regardless of who reached terminal first; idempotent for
        // the same terminal state and never re-run after a flip attempt was
        // rejected by the gate.
        if (!terminalCleaned.tryOnce()) {
            return;
        }
        // Terminal cleanup makes every still-running resolver transaction
        // stale, but intentionally does not close its native bridge here.
        runtimeEpoch.terminalizeActive();
        sessionScheduler.cancelPending();
        mainHandler.removeCallbacksAndMessages(null);
        // A cache-hit READY can reach terminal before the posted handoff
        // retire runs; the drain above just cancelled it, so retire here.
        retireApplicationAttachNow("terminalCleanup:" + state);
        RuntimeDexObserver.CloseResult runtimeWatcherResult = runtimeDexObserver.close();
        FeatureHooks hooks = featureHooks;
        LazyHookRegistry.RetireResult featureLazyResult = hooks == null
                ? new LazyHookRegistry.RetireResult(0, 0, 0)
                : hooks.retireLazyResolversPermanently("terminal:" + state);
        boolean featureLazyActive = featureLazyResult.isActive();

        // Mode A-ZF Phase 2: on a clean READY with the specific splash hook
        // installed, the generic Instrumentation safety gate is really
        // unhooked; every other terminal outcome retains it as the documented
        // DEGRADED fallback.
        ensureSplashLifecycleGuard();
        boolean instrumentationRetired =
                FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                        terminalSnapshot.terminalState, isSplashReady(), splashLifecycleGuard.isInstalled());
        boolean instrumentationSafetyActive;
        if (instrumentationRetired) {
            SplashHooks.SafetyRetireResult safetyResult =
                    splashHooks.retireInstrumentationSafety("terminal:" + state);
            instrumentationSafetyActive = safetyResult.isFrameworkActive();
        } else {
            instrumentationSafetyActive = splashHooks.isInstrumentationSafetyActive();
            log.info("instrumentation safety retained reason="
                    + FrameworkRetirePolicy.retainReason(
                            terminalSnapshot.terminalState, isSplashReady(), splashLifecycleGuard.isInstalled()));
        }
        String observerRetireReason = "terminal:" + state
                + " unhooked=" + runtimeWatcherResult.totalUnhooked
                + " failed=" + runtimeWatcherResult.totalFailures
                + " remaining=" + runtimeWatcherResult.remaining;
        if (!runtimeWatcherResult.isFrameworkActive()) {
            hookLedger.retire("runtime-observer-loadClass-1", observerRetireReason);
            hookLedger.retire("runtime-observer-loadClass-2", observerRetireReason);
        }
        List<String> missingRequired = terminalSnapshot.missingRequired;
        traceAfterContext("terminalCleanup", "state=" + terminalSnapshot.terminalState
                + " terminalGeneration=" + terminalSnapshot.generation
                + " terminalLoaderIdentity="
                + System.identityHashCode(terminalSnapshot.loader)
                + " featureLazyUnhookedThisClose="
                + featureLazyResult.unhookedThisClose
                + " featureLazyUnhookFailedThisClose="
                + featureLazyResult.failedThisClose
                + " featureLazyTotalUnhooked=" + featureLazyResult.totalUnhooked
                + " featureLazyTotalFailures=" + featureLazyResult.totalFailures
                + " featureLazyFrameworkActive=" + featureLazyActive
                + " featureLazyLogicalEnabled=" + featureLazyResult.logicalEnabled
                + " runtimeWatcherUnhookedThisClose="
                + runtimeWatcherResult.unhookedThisClose
                + " runtimeWatcherUnhookFailedThisClose="
                + runtimeWatcherResult.failedThisClose
                + " runtimeWatcherTotalUnhooked=" + runtimeWatcherResult.totalUnhooked
                + " runtimeWatcherTotalFailures=" + runtimeWatcherResult.totalFailures
                + " runtimeWatcherFrameworkActive="
                + runtimeWatcherResult.isFrameworkActive()
                + " runtimeWatcherLogicalEnabled=" + runtimeWatcherResult.logicalEnabled
                + " runtimeWatcherSummaryComplete=" + runtimeWatcherResult.summaryComplete
                + " missingRequired=" + missingRequired);
        log.info("coordinator terminal bridge ownership=workerSession"
                + " resolutionInFlight=" + resolutionInFlight.get()
                + " terminalGeneration=" + terminalSnapshot.generation
                + " terminalLoaderIdentity="
                + System.identityHashCode(terminalSnapshot.loader));
        String ledgerSummary = hookLedger.summaryLine(state.name());
        log.info(ledgerSummary);
        log.info("instrumentationSafetyActive=" + instrumentationSafetyActive
                + " instrumentationSafetyRetired=" + instrumentationRetired);
        traceAfterContext("hookLedger", ledgerSummary);
        traceAfterContext("instrumentationSafety",
                "retired=" + instrumentationRetired + " active=" + instrumentationSafetyActive);
        coreMissingRequired = terminalSnapshot.missingRequired;
        runtimeHealth.updateCore(isSplashReady(), entityListHooks.hookedMethodCount() > 0);
        if (hooks != null && hooks.isReplyHolderInstalled()) runtimeHealth.replyInstalled();
        if (terminalSnapshot.terminalState == BootstrapState.DEGRADED) {
            runtimeHealth.replyUnavailable("bootstrapDegraded");
        }
        logRuntimeHealth();
        worker.shutdown();
        log.info("coordinator bootstrap lifecycle retired executorShutdown=true"
                + " runtimeWatcherUnhookedThisClose="
                + runtimeWatcherResult.unhookedThisClose
                + " runtimeWatcherUnhookFailedThisClose="
                + runtimeWatcherResult.failedThisClose
                + " runtimeWatcherTotalUnhooked=" + runtimeWatcherResult.totalUnhooked
                + " runtimeWatcherTotalFailures=" + runtimeWatcherResult.totalFailures
                + " runtimeWatcherFrameworkActive="
                + runtimeWatcherResult.isFrameworkActive()
                + " runtimeWatcherLogicalEnabled=" + runtimeWatcherResult.logicalEnabled
                + " runtimeWatcherSummaryComplete=" + runtimeWatcherResult.summaryComplete
                + " featureLazyUnhookedThisClose="
                + featureLazyResult.unhookedThisClose
                + " featureLazyUnhookFailedThisClose="
                + featureLazyResult.failedThisClose
                + " featureLazyTotalUnhooked=" + featureLazyResult.totalUnhooked
                + " featureLazyTotalFailures=" + featureLazyResult.totalFailures
                + " featureLazyFrameworkActive=" + featureLazyActive
                + " featureLazyLogicalEnabled=" + featureLazyResult.logicalEnabled
                + " missingRequired=" + missingRequired);
    }

    private void maybeRecoverAfterSplashResolved(
            ResolutionSessionContext sessionContext) {
        if (!splashCandidateSeenBeforeReady || splashFinishedByHook || !isSplashReady()) {
            return;
        }
        if (cache == null || identity == null) {
            return;
        }
        Map<String, ResolvedTarget> persisted = cache.loadTargets(identity);
        ResolvedTarget splash = persisted.get(TargetResolver.KEY_SPLASH_BASE);
        if (splash == null || TargetVerifier.verify(splash, sessionContext.loader) != null) {
            log.info("recovery skipped reason=cacheVerificationFailed");
            return;
        }
        requireApplied(runtimeEpoch.commit(sessionContext, () -> {
            recoveryController.attachIdentity(identity);
            recoveryController.markSplashEscaped();
            recoveryController.onSplashResolved(cache);
        }), "recoveryCommit");
    }

    /**
     * Mode A-ZF Phase 3 persistence: a lazily discovered semantic class (the
     * reply holder) becomes a cached class-only target so the NEXT process
     * installs it directly and never needs the loadClass hooks. Runs off the
     * interceptor thread; when the resolver worker is already shut down
     * (post-READY retry lane) the same thread persists inline.
     */
    private void onSemanticClassDiscovered(String cacheKey, String classDescriptor,
                                           ClassLoader loader, long generation) {
        final ResolutionCache persistCache = cache;
        final TargetIdentity persistIdentity = identity;
        if (persistCache == null || persistIdentity == null || loader == null) {
            log.info("semantic persist skipped reason=stateMissing key=" + cacheKey);
            return;
        }
        Runnable persist = () -> {
            try {
                ResolvedTarget target = new ResolvedTarget(
                        cacheKey, "lazy_semantic_class", classDescriptor, "");
                String problem = TargetVerifier.verify(target, loader);
                if (problem != null) {
                    log.info("semantic persist rejected key=" + cacheKey
                            + " reason=" + problem);
                    return;
                }
                ResolutionCache.CachedResolution current =
                        persistCache.loadResolution(persistIdentity);
                Map<String, ResolvedTarget> merged =
                        new LinkedHashMap<>(current.targets);
                merged.put(cacheKey, target);
                generationTargets.merge(
                        runtimeEpoch.generation(),
                        java.util.Collections.singletonMap(cacheKey, target));
                persistCache.saveTargets(persistIdentity, merged,
                        current.coverageSettled);
                log.info("semantic target persisted key=" + cacheKey
                        + " totalEntries=" + merged.size());
            } catch (Throwable throwable) {
                log.error("semantic persist failed key=" + cacheKey, throwable);
            }
        };
        try {
            worker.execute(persist);
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            persist.run();
        }
    }

    private Application lifecycleApplication() {
        Context current = currentApplication();
        if (current instanceof Application) return (Application) current;
        return appContext instanceof Application ? (Application) appContext : null;
    }

    private void ensureSplashLifecycleGuard() {
        splashLifecycleGuard.install(lifecycleApplication());
    }

    private void logRuntimeHealth() {
        String summary = "runtime health state=" + state
                + " coreMissingRequired=" + coreMissingRequired
                + " frameworkActive=" + hookLedger.hasActiveFrameworkHooks()
                + " frameworkActiveHooks=" + hookLedger.activeIds(HookLedger.Layer.FRAMEWORK)
                + " splashLifecycleGuard=" + splashLifecycleGuard.isInstalled()
                + " " + runtimeHealth.summary();
        log.info(summary);
        traceAfterContext("runtimeHealth", summary);
    }

    /** All actual retry work is serialized on one bounded worker lane. */
    private void scheduleReplyDiscoveryRetry() {
        FeatureHooks hooks = featureHooks;
        if (hooks == null || !hooks.isReplyHolderSelected()) return;
        if (hooks.isReplyHolderInstalled()) {
            runtimeHealth.replyInstalled();
            return;
        }
        if (replyBudget != null) return;
        replyBudget = new ReplyDiscoveryBudget(SystemClock.elapsedRealtime());
        HandlerThread thread = new HandlerThread("pool-reply-retry");
        thread.start();
        replyRetryThread = thread;
        Handler handler = new Handler(thread.getLooper());
        replyRetryHandler = handler;
        Application application = lifecycleApplication();
        if (application != null) {
            Application.ActivityLifecycleCallbacks callback = new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityResumed(Activity activity) {
                    if (!replyBudget.isStopped()) handler.post(() -> runReplyAttempt(true));
                }
                @Override public void onActivityCreated(Activity activity, Bundle state) { }
                @Override public void onActivityStarted(Activity activity) { }
                @Override public void onActivityPaused(Activity activity) { }
                @Override public void onActivityStopped(Activity activity) { }
                @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
                @Override public void onActivityDestroyed(Activity activity) { }
            };
            try {
                application.registerActivityLifecycleCallbacks(callback);
                replyRetryApplication = application;
                replyRetryLifecycle = callback;
                log.info("reply retry lifecycle observer registered maxResumeAttempts="
                        + ReplyDiscoveryBudget.MAX_RESUME_ATTEMPTS
                        + " maxElapsedMs=" + ReplyDiscoveryBudget.MAX_ELAPSED_MILLIS);
            } catch (Throwable failure) {
                log.error("reply retry lifecycle registration failed; timed lane retained", failure);
            }
        }
        handler.postDelayed(() -> finishReplyBudget(), ReplyDiscoveryBudget.MAX_ELAPSED_MILLIS);
        handler.postDelayed(() -> runReplyAttempt(false), ReplyDiscoveryRetryPolicy.delayFor(0));
    }

    private void runReplyAttempt(boolean fromResume) {
        ReplyDiscoveryBudget budget = replyBudget;
        if (budget == null || budget.isStopped() || finishReplyBudget()) return;
        long now = SystemClock.elapsedRealtime();
        boolean allowed = fromResume ? budget.tryResume(now) : budget.tryTimed(now);
        if (!allowed) {
            finishReplyBudget();
            return;
        }
        try {
            featureHooks.tryDirectReplyInstall();
        } catch (Throwable failure) {
            log.error("reply retry attempt failed", failure);
        }
        if (!finishReplyBudget() && !fromResume) {
            Handler handler = replyRetryHandler;
            if (handler != null) handler.postDelayed(() -> runReplyAttempt(false),
                    ReplyDiscoveryRetryPolicy.delayFor(budget.timedAttempts()));
        }
    }

    private boolean finishReplyBudget() {
        ReplyDiscoveryBudget budget = replyBudget;
        FeatureHooks hooks = featureHooks;
        if (budget == null || hooks == null) return true;
        return budget.finishIfNeeded(hooks.isReplyHolderInstalled(),
                SystemClock.elapsedRealtime(), this::unregisterReplyRetryLifecycle,
                this::quitReplyRetryThread, runtimeHealth);
    }

    private void unregisterReplyRetryLifecycle() {
        Application.ActivityLifecycleCallbacks observer = replyRetryLifecycle;
        Application application = replyRetryApplication;
        if (observer == null || application == null) return;
        try {
            application.unregisterActivityLifecycleCallbacks(observer);
            replyRetryLifecycle = null;
            replyRetryApplication = null;
            log.info("reply retry lifecycle observer stopped unregister=true");
        } catch (Throwable failure) {
            // The stopped budget makes any retained callback inert.
            log.error("reply retry lifecycle unregister failed logicalStopped=true", failure);
        }
    }

    private void quitReplyRetryThread() {
        Handler handler = replyRetryHandler;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        HandlerThread thread = replyRetryThread;
        replyRetryThread = null;
        replyRetryHandler = null;
        if (thread != null) thread.quitSafely();
        log.info("reply retry stopped timedAttempts=" + replyBudget.timedAttempts()
                + " observerActive=" + (replyRetryLifecycle != null));
    }

    private void watchdog(String reason) {
        traceAfterContext("watchdog", reason);
        log.info("coordinator watchdog fired reason=" + reason + " state=" + state);
        if (state.isTerminal()) {
            return;
        }
        if (WATCHDOG_DEADLINE_REASON.equals(reason)) {
            // Deadline semantics take precedence over ANY intermediate state:
            // a process stuck in WAIT_RUNTIME_DEX/CACHE_VERIFY/... at 20s must
            // still terminate here instead of returning early and suspending
            // forever. Coverage settles by definition at the deadline, so a
            // core-ready process finishes READY; a core-incapable one is
            // DEGRADED. The passive Instrumentation splash safety net is
            // retained in both cases (SplashHooks are never unhooked).
            BootstrapState intermediateState = state;
            TerminalTransaction.Result result = commitDeadlineTerminal("deadline");
            TerminalSnapshot snapshot = result.snapshot;
            if (snapshot == null) {
                return;
            }
            log.info("resolver watchdog deadline intermediateState=" + intermediateState
                    + " " + snapshot.describe());
            if (snapshot.terminalState == BootstrapState.READY) {
                completeReady(snapshot, "deadline");
            } else {
                completeDegraded(snapshot, "deadline");
                log.info("resolver watchdog deadline state=DEGRADED"
                        + " splashSpecificInstalled=" + featureInstallState.splashClasses()
                        + " splashCoveredByLegacy=" + splashLegacyCoverageSummary()
                        + " passiveSplashNet=retained");
            }
            return;
        }
        if (state == BootstrapState.BOOTSTRAP) {
            markState(BootstrapState.WAIT_RUNTIME_DEX);
        }
        if (ReadinessPolicy.shouldWatchdogRetrySession(state)) {
            // Retry from every non-terminal state, FULL_RESOLVE included:
            // core-ready-but-coverage-pending must not depend on further
            // class loading to get its next resolution session.
            triggerSession(reason);
        }
    }

    /**
     * Delegates to the terminal gate: once READY/DEGRADED is entered it can
     * never be flipped (not even READY→DEGRADED by a late background
     * exception, nor DEGRADED→READY by a late session success). Re-marking
     * the same terminal state is IDEMPOTENT and changes nothing.
     */
    private TerminalStateGate.Transition markState(BootstrapState next) {
        synchronized (stateLock) {
            TerminalStateGate.Transition transition = stateGate.mark(next);
            switch (transition) {
                case APPLIED:
                    state = next;
                    traceAfterContext("state", next.name());
                    log.info("coordinator state=" + next);
                    break;
                case REJECTED:
                    log.info("coordinator state change rejected terminal=" + state
                            + " attempted=" + next);
                    break;
                case IDEMPOTENT:
                default:
                    break;
            }
            return transition;
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

    /** Pure terminal gate update used while runtimeEpoch is held. */
    private TerminalStateGate.Transition markTerminalState(BootstrapState next) {
        if (next == null || !next.isTerminal()) {
            throw new IllegalArgumentException("terminal state required: " + next);
        }
        synchronized (stateLock) {
            TerminalStateGate.Transition transition = stateGate.mark(next);
            if (transition == TerminalStateGate.Transition.APPLIED) {
                state = next;
            }
            return transition;
        }
    }

    private void initializeRuntimeConfiguration(Context context) {
        if (config != null || context == null) {
            return;
        }
        synchronized (configLock) {
            if (config != null) {
                return;
            }
            Context candidate = context.getApplicationContext();
            Context application = candidate != null ? candidate : context;
            int loadedMajor = readCoolapkMajor(application);
            PurifierConfig loaded = PurifierConfig.load(application, log);
            HookInstallPlan plan = HookInstallPlan.from(loaded, loadedMajor);
            FeatureHooks loadedFeatureHooks = new FeatureHooks(
                    module, log, loaded, loadedMajor,
                    entityListHooks, plan, featureInstallState, hookLedger);
            loadedFeatureHooks.setSemanticDiscoveryListener(
                    this::onSemanticClassDiscovered);
            runtimeHealth.configure(
                    loaded.isEffectiveEnabled(PurifierConfig.Feature.SPLASH, loadedMajor),
                    loaded.isEffectiveEnabled(PurifierConfig.Feature.FEED_SPONSOR, loadedMajor),
                    loaded.isEffectiveEnabled(PurifierConfig.Feature.REPLY_SPONSOR, loadedMajor));
            SettingsHooks loadedSettingsHooks = new SettingsHooks(
                    module, hookLedger, log, loaded, loadedMajor);
            final long[] boundGeneration = {0L};
            configurationTransaction.publish(() -> state.isTerminal(),
                    (generation, loader, activated, terminal) -> {
                        boundGeneration[0] = generation;
                        if (terminal) {
                            loadedFeatureHooks.disableLazyDiscoveryBeforePublication();
                        } else if (activated && generation > 0) {
                            loadedFeatureHooks.beginGeneration(generation, loader);
                            entityListHooks.beginGeneration(generation, loader);
                        }
                        entityListHooks.setConfig(loaded, loadedMajor);
                        coolapkMajor = loadedMajor;
                        featureHooks = loadedFeatureHooks;
                        settingsHooks = loadedSettingsHooks;
                        // Publish the initialized marker last.
                        config = loaded;
                    });
            Context lifecycleContext = currentApplication();
            loadedSettingsHooks.install(lifecycleContext != null
                    ? lifecycleContext : application);
            ensureSplashLifecycleGuard();
            // Mode A-ZF Phase 3: no eager lazy-resolver install here. The
            // temporary loadClass hooks are only installed by a resolution
            // session that ends with selected semantic targets still missing.
            log.info("configuration initialized coolapkMajor=" + loadedMajor
                    + " pending=" + loaded.pendingKind()
                    + " revision=" + loaded.revision()
                    + " hookPlan=classLoader:" + plan.installClassLoader
                    + " generation=" + boundGeneration[0]
                    + " terminal=" + state.isTerminal()
                    + " lazyDiscovery=onDemand"
                    + " lazyLogicalEnabled="
                    + loadedFeatureHooks.areLazyResolversLogicallyEnabled()
                    + " lazyFrameworkActive="
                    + loadedFeatureHooks.hasActiveLazyResolvers());
        }
    }

    private static int readCoolapkMajor(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    CoolapkModule.TARGET_PACKAGE, 0);
            String version = info.versionName;
            if (version == null || version.isEmpty()) {
                return 0;
            }
            int dot = version.indexOf('.');
            String major = dot < 0 ? version : version.substring(0, dot);
            return Integer.parseInt(major.replaceAll("[^0-9]", ""));
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
