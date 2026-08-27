package io.github.yylsping.coolapkpurifier;

import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/** Installs the cached method-level feature hooks and evaluates selected coverage. */
final class FeatureHooks {
    private static final String KEY_REPLY_HOLDER = TargetResolver.KEY_REPLY_HOLDER;
    private static final String REPLY_HOLDER_CLASS =
            "com.coolapk.market.viewholder.MultiFeedReplyViewHolder";
    private static final String AUTO_COMMENT_CLASS =
            "com.coolapk.market.view.cardlist.component."
                    + "RecyclerViewItemFullVisibleControllerKt";
    private static final String TOPIC_CONFIG_CLASS =
            "com.coolapk.market.view.feedv8.component.TopicRecommendConfig";
    private static final String RELATED_HOLDER_CLASS =
            "com.coolapk.market.viewholder.RelatedDataViewHolder";
    private static final String DETAIL_SPONSOR_HOLDER_CLASS =
            "com.coolapk.market.view.ad.SponsorSelfDrawDetailViewHolder";

    /** Notified when a lazy-discovered semantic class becomes persistable. */
    interface SemanticClassDiscoveredListener {
        void onSemanticClassDiscovered(String cacheKey, String classDescriptor,
                                       ClassLoader loader, long generation);
    }

    private final XposedModule module;
    private final ModuleLog log;
    private final PurifierConfig config;
    private final int coolapkMajor;
    private final EntityListHooks entityListHooks;
    private final HookInstallPlan plan;
    private final HookLedger ledger;
    private final HookSiteRegistry<HookHandle> handles = new HookSiteRegistry<>();
    private final FeatureInstallState installState;
    private final Set<View> guardedHolderViews = java.util.Collections.newSetFromMap(
            new WeakHashMap<>());
    private final LazyHookRegistry lazyHooks = new LazyHookRegistry();
    private volatile SemanticClassDiscoveredListener discoveryListener;
    private boolean targetPlanLogged;
    private boolean lazyDiscoveryPermanentlyDisabled;
    private volatile long generation;
    private volatile ClassLoader activeLoader;

    FeatureHooks(XposedModule module, ModuleLog log, PurifierConfig config,
                 int coolapkMajor, EntityListHooks entityListHooks,
                 HookInstallPlan plan, FeatureInstallState installState,
                 HookLedger ledger) {
        this.module = module;
        this.log = log;
        this.config = config;
        this.coolapkMajor = coolapkMajor;
        this.entityListHooks = entityListHooks;
        this.plan = plan;
        this.installState = installState;
        this.ledger = ledger;
    }

    void setSemanticDiscoveryListener(SemanticClassDiscoveredListener listener) {
        discoveryListener = listener;
    }

    synchronized void beginGeneration(long nextGeneration, ClassLoader loader) {
        if (nextGeneration < generation) {
            log.info("feature generation rollback rejected current=" + generation
                    + " attempted=" + nextGeneration);
            return;
        }
        generation = nextGeneration;
        activeLoader = loader;
        if (!lazyDiscoveryPermanentlyDisabled) {
            lazyHooks.activate();
        }
    }

    synchronized void installTargets(Map<String, ResolvedTarget> targets, ClassLoader loader,
                                     long expectedGeneration) {
        if (!isCurrent(expectedGeneration, loader)) {
            return;
        }
        if (!targetPlanLogged) {
            targetPlanLogged = true;
            log.info("feature target plan reply=" + plan.resolveReplyHolder
                    + " auto=" + plan.resolveAutoComment
                    + " topic=" + plan.resolveTopicRecommend
                    + " related=" + plan.resolveRelatedData
                    + " sameTopic=" + plan.resolveSameTopicFeed
                    + " detail=" + plan.resolveDetailSponsor
                    + " keys=" + targets.keySet());
        }
        if (plan.resolveAutoComment) {
            installSemanticIfLoadable(loader,
                    "com.coolapk.market.view.cardlist.component."
                            + "RecyclerViewItemFullVisibleControllerKt");
        }
        if (plan.resolveTopicRecommend) {
            installSemanticIfLoadable(loader,
                    "com.coolapk.market.view.feedv8.component.TopicRecommendConfig");
            installSemanticIfLoadable(loader,
                    "com.coolapk.market.view.feedv8.component.TopicRecommendKt");
        }
        if (plan.resolveReplyHolder) {
            // Cache-first (Mode A-ZF Phase 3): a persisted reply holder target
            // installs directly whenever the class is loadable, so the two
            // temporary loadClass hooks are never installed on cache hits.
            if (targets.get(KEY_REPLY_HOLDER) != null) {
                installSemanticIfLoadable(loader, REPLY_HOLDER_CLASS);
            }
        }
        if (plan.resolveRelatedData) {
            installSemanticIfLoadable(loader,
                    "com.coolapk.market.viewholder.RelatedDataViewHolder");
        }
        if (plan.resolveDetailSponsor) {
            installSemanticIfLoadable(loader,
                    "com.coolapk.market.view.ad.SponsorSelfDrawDetailViewHolder");
        }
        if (plan.resolveAutoComment) {
            installMethod(targets.get(TargetResolver.KEY_AUTO_COMMENT), loader,
                    expectedGeneration);
        }
        if (plan.resolveTopicRecommend) {
            installMethod(targets.get(TargetResolver.KEY_TOPIC_RECOMMEND), loader,
                    expectedGeneration);
        }
        if (plan.resolveSameTopicFeed) {
            installSemanticEvidence(targets.get(TargetResolver.KEY_SAME_TOPIC_FEED), loader,
                    expectedGeneration);
        }
        if (plan.resolveDetailSponsor) {
            installMethod(targets.get(TargetResolver.KEY_DETAIL_SPONSOR), loader,
                    expectedGeneration);
        }
        retireLazyResolversIfComplete();
    }

    private void installSemanticIfLoadable(ClassLoader loader, String className) {
        try {
            maybeInstallSemanticClass(Class.forName(className, false, loader));
        } catch (Throwable ignored) {
            // Not loadable in this generation yet: the session-end
            // ensureLazyDiscovery fallback installs the temporary loadClass
            // hooks so a later dex append can still be observed.
        }
    }

    /**
     * Mode A-ZF Phase 3: the temporary loadClass hooks are installed only when
     * selected semantic targets are still missing AND a direct install of the
     * already-loadable classes cannot close the gap. Idempotent and safe to
     * call from any session outcome.
     */
    synchronized void ensureLazyDiscovery(Map<String, ResolvedTarget> targets,
                                          String reason) {
        if (lazyDiscoveryPermanentlyDisabled
                || coolapkMajor < 15 || !plan.installClassLoader) {
            return;
        }
        if (!hasMissingSemanticTargets(targets)) {
            log.info("feature lazy discovery skipped reason=allSemanticTargetsInstalled"
                    + " trigger=" + reason);
            return;
        }
        // Classes already appended to the runtime dex install without any
        // framework hook; only genuinely future loads need the observers.
        attemptDirectSemanticInstall();
        if (!hasMissingSemanticTargets(targets)) {
            log.info("feature lazy discovery skipped reason=directInstallComplete"
                    + " trigger=" + reason);
            return;
        }
        installLazyResolvers();
        log.info("feature lazy discovery ensured reason=" + reason
                + " coverage=" + lazyHooks.size() + "/2");
    }

    private void attemptDirectSemanticInstall() {
        ClassLoader loader = activeLoader;
        if (loader == null) {
            return;
        }
        if (plan.resolveReplyHolder) {
            installSemanticIfLoadable(loader, REPLY_HOLDER_CLASS);
        }
        if (plan.resolveAutoComment) {
            installSemanticIfLoadable(loader, AUTO_COMMENT_CLASS);
        }
        if (plan.resolveTopicRecommend) {
            installSemanticIfLoadable(loader, TOPIC_CONFIG_CLASS);
        }
        if (plan.resolveRelatedData) {
            installSemanticIfLoadable(loader, RELATED_HOLDER_CLASS);
        }
        if (plan.resolveDetailSponsor) {
            installSemanticIfLoadable(loader, DETAIL_SPONSOR_HOLDER_CLASS);
        }
    }

    /** Whether any selected semantic target is still uninstalled. */
    synchronized boolean hasMissingSemanticTargets(Map<String, ResolvedTarget> targets) {
        boolean replyMissing = plan.resolveReplyHolder
                && !installState.hasFallbackHook(KEY_REPLY_HOLDER);
        return replyMissing
                || !FeatureTargetReadiness.missing(
                        config, coolapkMajor, targets, installState).isEmpty();
    }

    synchronized boolean isReplyHolderSelected() {
        return plan.resolveReplyHolder;
    }

    synchronized boolean isReplyHolderInstalled() {
        return installState.hasFallbackHook(KEY_REPLY_HOLDER);
    }

    /** Hooks semantic business classes as protected Coolapk appends their dex. */
    synchronized void installLazyResolvers() {
        if (lazyDiscoveryPermanentlyDisabled
                || coolapkMajor < 15 || !plan.installClassLoader) {
            return;
        }
        lazyHooks.activate();
        if (!lazyHooks.contains(LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG)) {
            try {
                hookLoadClass(LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG,
                        ClassLoader.class.getDeclaredMethod("loadClass", String.class));
                ledger.record(HookLedger.Layer.FRAMEWORK, "feature",
                        "feature-lazy-loadClass-1",
                        "ClassLoader.loadClass(String)");
            } catch (Throwable throwable) {
                log.error("feature lazy one-arg resolver install failed", throwable);
            }
        }
        if (!lazyHooks.contains(LazyHookRegistry.HookSite.LOAD_CLASS_TWO_ARG)) {
            try {
                hookLoadClass(LazyHookRegistry.HookSite.LOAD_CLASS_TWO_ARG,
                        ClassLoader.class.getDeclaredMethod(
                                "loadClass", String.class, boolean.class));
                ledger.record(HookLedger.Layer.FRAMEWORK, "feature",
                        "feature-lazy-loadClass-2",
                        "ClassLoader.loadClass(String,boolean)");
            } catch (Throwable throwable) {
                log.error("feature lazy two-arg resolver install failed", throwable);
            }
        }
        log.info("feature lazy class resolver installed coverage="
                + lazyHooks.size() + "/2 missing=" + lazyHooks.missingSites());
    }

    private void hookLoadClass(LazyHookRegistry.HookSite site, Method loadClass) {
        try {
            HookHandle handle = module.hook(loadClass)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-issue2-lazy-" + loadClass.getParameterCount())
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (lazyHooks.isLogicalEnabled() && result instanceof Class<?>) {
                            maybeInstallSemanticClass((Class<?>) result);
                        }
                        return result;
                    });
            lazyHooks.put(site, handle);
        } catch (Throwable throwable) {
            log.error("feature lazy loadClass hook failed method=" + loadClass, throwable);
        }
    }

    private synchronized void maybeInstallSemanticClass(Class<?> type) {
        long expectedGeneration = generation;
        if (type == null || type.getClassLoader() != activeLoader) {
            return;
        }
        String className = type.getName();
        if (REPLY_HOLDER_CLASS.equals(className)) {
            if (plan.resolveReplyHolder
                    && entityListHooks.installReplyHolder(type) > 0) {
                installState.markFallbackHook(expectedGeneration, KEY_REPLY_HOLDER);
                notifySemanticClassDiscovered(KEY_REPLY_HOLDER, type);
            }
            retireLazyResolversIfComplete();
            return;
        }
        if ("com.coolapk.market.viewholder.RelatedDataViewHolder".equals(className)) {
            if (plan.resolveRelatedData) {
                installDedicatedHolder(TargetResolver.KEY_RELATED_DATA, type,
                        expectedGeneration);
            }
            retireLazyResolversIfComplete();
            return;
        }
        if ("com.coolapk.market.view.ad.SponsorSelfDrawDetailViewHolder"
                .equals(className)) {
            if (plan.resolveDetailSponsor) {
                installDedicatedHolder(TargetResolver.KEY_DETAIL_SPONSOR, type,
                        expectedGeneration);
            }
            retireLazyResolversIfComplete();
            return;
        }
        if (("com.coolapk.market.view.cardlist.component."
                + "RecyclerViewItemFullVisibleControllerKt").equals(className)) {
            if (!plan.resolveAutoComment) {
                return;
            }
            List<Method> candidates = new ArrayList<>();
            for (Method method : type.getDeclaredMethods()) {
                if (TargetVerifier.isAutoCommentEntry(method)) {
                    candidates.add(method);
                }
            }
            if (candidates.size() == 1) {
                installHook(TargetResolver.KEY_AUTO_COMMENT, candidates.get(0),
                        expectedGeneration);
            } else if (candidates.size() > 1) {
                log.info("feature lazy auto-comment refused candidates="
                        + candidates.size() + " reason=ambiguous");
            }
            return;
        }
        if (!plan.resolveTopicRecommend || !className.startsWith(
                "com.coolapk.market.view.feedv8.component.TopicRecommend")) {
            return;
        }
        for (Method method : type.getDeclaredMethods()) {
            String lower = method.getName().toLowerCase(java.util.Locale.ROOT);
            boolean enableGetter = (method.getReturnType() == boolean.class
                    || method.getReturnType() == Boolean.class)
                    && lower.contains("topicrecommend") && lower.contains("enable");
            boolean dedicatedSetup = "setupView".equals(method.getName())
                    && className.endsWith("TopicRecommendKt");
            if (enableGetter || dedicatedSetup) {
                installHook(TargetResolver.KEY_TOPIC_RECOMMEND, method,
                        expectedGeneration);
            }
        }
        retireLazyResolversIfComplete();
    }

    private synchronized void retireLazyResolversIfComplete() {
        if (!lazyHooks.isActive() || !allSemanticTargetsInstalled()) {
            return;
        }
        retireLazyResolvers("targetsInstalled");
    }

    /** Terminal lifecycle boundary: discovery-only global hooks never persist. */
    synchronized LazyHookRegistry.RetireResult retireLazyResolvers(String reason) {
        int before = lazyHooks.size();
        LazyHookRegistry.RetireResult result = lazyHooks.retire();
        retireLazyLedgerEntries(result, reason, false);
        log.info("feature lazy class resolver retired reason=" + reason
                + " handlesBefore=" + before
                + " unhookedThisClose=" + result.unhookedThisClose
                + " failedThisClose=" + result.failedThisClose
                + " totalUnhooked=" + result.totalUnhooked
                + " totalFailures=" + result.totalFailures
                + " remaining=" + result.remaining
                + " frameworkActive=" + result.isActive()
                + " logicalEnabled=" + result.logicalEnabled);
        return result;
    }

    /** Terminal is a permanent discovery boundary for this process. */
    synchronized LazyHookRegistry.RetireResult retireLazyResolversPermanently(
            String reason) {
        lazyDiscoveryPermanentlyDisabled = true;
        int before = lazyHooks.size();
        LazyHookRegistry.RetireResult result = lazyHooks.retirePermanently();
        retireLazyLedgerEntries(result, reason, true);
        log.info("feature lazy class resolver retired reason=" + reason
                + " handlesBefore=" + before
                + " unhookedThisClose=" + result.unhookedThisClose
                + " failedThisClose=" + result.failedThisClose
                + " totalUnhooked=" + result.totalUnhooked
                + " totalFailures=" + result.totalFailures
                + " remaining=" + result.remaining
                + " frameworkActive=" + result.isActive()
                + " logicalEnabled=" + result.logicalEnabled
                + " permanent=true");
        return result;
    }

    private void retireLazyLedgerEntries(LazyHookRegistry.RetireResult result,
                                         String reason, boolean permanent) {
        if (result.isActive()) {
            // A failed unhook stays active in the ledger: frameworkActiveHooks
            // must keep reporting it instead of pretending it is gone.
            return;
        }
        String detail = reason + (permanent ? " permanent=true" : "")
                + " unhooked=" + result.unhookedThisClose
                + " failed=" + result.failedThisClose;
        ledger.retire("feature-lazy-loadClass-1", detail);
        ledger.retire("feature-lazy-loadClass-2", detail);
    }

    /**
     * Cache persistence callback for the reply holder: the only semantic
     * target that has no DexKit resolver and is therefore only discoverable
     * through the temporary loadClass hooks. Failures never break discovery.
     */
    private void notifySemanticClassDiscovered(String cacheKey, Class<?> type) {
        SemanticClassDiscoveredListener listener = discoveryListener;
        if (listener == null) {
            return;
        }
        try {
            listener.onSemanticClassDiscovered(cacheKey,
                    DescriptorUtils.classDescriptorOf(type),
                    type.getClassLoader(), generation);
        } catch (Throwable throwable) {
            log.error("semantic discovery listener failed key=" + cacheKey, throwable);
        }
    }

    /** Used only for a newly built, not-yet-published terminal configuration. */
    synchronized void disableLazyDiscoveryBeforePublication() {
        lazyDiscoveryPermanentlyDisabled = true;
        lazyHooks.retirePermanently();
    }

    synchronized boolean hasActiveLazyResolvers() {
        return lazyHooks.isActive();
    }

    boolean areLazyResolversLogicallyEnabled() {
        return lazyHooks.isLogicalEnabled();
    }

    synchronized long generation() {
        return generation;
    }

    private boolean allSemanticTargetsInstalled() {
        return (!plan.resolveReplyHolder || installState.hasFallbackHook(KEY_REPLY_HOLDER))
                && (!plan.resolveAutoComment
                || installState.hasPrimaryHook(TargetResolver.KEY_AUTO_COMMENT))
                && (!plan.resolveTopicRecommend
                || installState.hasPrimaryHook(TargetResolver.KEY_TOPIC_RECOMMEND))
                && (!plan.resolveRelatedData
                || installState.hasFallbackHook(TargetResolver.KEY_RELATED_DATA))
                && (!plan.resolveDetailSponsor
                || installState.hasFallbackHook(TargetResolver.KEY_DETAIL_SPONSOR));
    }

    private void installDedicatedHolder(String key, Class<?> holderClass,
                                        long expectedGeneration) {
        for (Constructor<?> constructor : holderClass.getDeclaredConstructors()) {
            String siteKey = key + "|constructor";
            if (handles.contains(siteKey, constructor)) {
                installState.markFallbackHook(expectedGeneration, key);
                continue;
            }
            try {
                HookHandle handle = module.hook(constructor)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .setId("coolapk-" + key + "-constructor-"
                                + Integer.toHexString(
                                constructor.toGenericString().hashCode()))
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            if (shouldBlock(key)) {
                                guardDedicatedHolder(chain.getThisObject(), key);
                            }
                            return result;
                        });
                handles.put(siteKey, constructor, handle);
                installState.markFallbackHook(expectedGeneration, key);
                ledger.record(HookLedger.Layer.BUSINESS, "feature",
                        "feature-" + key + "-constructor", constructor.toGenericString());
                log.info("dedicated issue2 constructor hook installed key=" + key
                        + " constructor=" + constructor);
            } catch (Throwable throwable) {
                log.error("dedicated issue2 constructor hook failed key=" + key
                        + " constructor=" + constructor, throwable);
            }
        }
        for (Method method : holderClass.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                    || method.getReturnType() != void.class
                    || !method.getParameterTypes()[0].getName().startsWith(
                    "com.coolapk.market.model.")) {
                continue;
            }
            String siteKey = key + "|holder";
            if (handles.contains(siteKey, method)) {
                installState.markFallbackHook(expectedGeneration, key);
                continue;
            }
            try {
                HookHandle handle = module.hook(method)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .setId("coolapk-" + key + "-holder-"
                                + Integer.toHexString(method.toGenericString().hashCode()))
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            if (shouldBlock(key)) {
                                collapseHolder(chain.getThisObject());
                                log.info("removed dedicated issue2 holder key=" + key
                                        + " method=" + method);
                            }
                            return result;
                        });
                handles.put(siteKey, method, handle);
                installState.markFallbackHook(expectedGeneration, key);
                ledger.record(HookLedger.Layer.BUSINESS, "feature",
                        "feature-" + key + "-holder", method.toGenericString());
                log.info("dedicated issue2 holder hook installed key=" + key
                        + " method=" + method);
            } catch (Throwable throwable) {
                log.error("dedicated issue2 holder hook failed key=" + key
                        + " method=" + method, throwable);
            }
        }
    }

    private void guardDedicatedHolder(Object holder, String key) throws Exception {
        Field itemViewField = holder.getClass().getField("itemView");
        Object candidate = itemViewField.get(holder);
        if (!(candidate instanceof View)) {
            return;
        }
        View itemView = (View) candidate;
        synchronized (guardedHolderViews) {
            if (guardedHolderViews.add(itemView)) {
                itemView.addOnAttachStateChangeListener(
                        new View.OnAttachStateChangeListener() {
                            @Override
                            public void onViewAttachedToWindow(View view) {
                                if (shouldBlock(key)) {
                                    view.post(() -> collapseView(view));
                                }
                            }

                            @Override
                            public void onViewDetachedFromWindow(View view) {
                            }
                        });
                itemView.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                     oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (shouldBlock(key)) {
                        collapseView(view);
                    }
                });
            }
        }
        collapseView(itemView);
        log.info("removed dedicated issue2 holder key=" + key
                + " class=" + holder.getClass().getName());
    }

    private static void collapseHolder(Object holder) throws Exception {
        Field itemViewField = holder.getClass().getField("itemView");
        Object candidate = itemViewField.get(holder);
        if (!(candidate instanceof View)) {
            return;
        }
        collapseView((View) candidate);
    }

    private static void collapseView(View view) {
        view.setVisibility(View.GONE);
        view.setMinimumHeight(0);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && params.height != 0) {
            params.height = 0;
            view.setLayoutParams(params);
        }
    }

    private void installMethod(ResolvedTarget target, ClassLoader loader,
                               long expectedGeneration) {
        if (target == null) {
            return;
        }
        Method method = DescriptorUtils.methodForDescriptor(target.methodDescriptor, loader);
        if (method == null) {
            log.info("feature hook pending key=" + target.key + " reason=methodNotLoadable");
            return;
        }
        installHook(target.key, method, expectedGeneration);
    }

    /**
     * T4 is removed by the feed-list/entityTemplate data path only after this
     * build's unique semantic predicate has verified. The predicate is not
     * altered; its verification is the runtime deletion permission gate.
     */
    private synchronized void installSemanticEvidence(
            ResolvedTarget target, ClassLoader loader, long expectedGeneration) {
        if (!isCurrent(expectedGeneration, loader)) {
            return;
        }
        if (target == null || installState.hasSemanticEvidence(
                TargetResolver.KEY_SAME_TOPIC_FEED)) {
            return;
        }
        Method method = DescriptorUtils.methodForDescriptor(
                target.methodDescriptor, loader);
        if (method == null || !TargetVerifier.isSameTopicTemplatePredicate(method)) {
            log.info("feature semantic evidence pending key="
                    + TargetResolver.KEY_SAME_TOPIC_FEED);
            return;
        }
        if (!installState.markSemanticEvidence(
                expectedGeneration, TargetResolver.KEY_SAME_TOPIC_FEED)) {
            return;
        }
        entityListHooks.setSameTopicSemanticVerified(expectedGeneration, true);
        log.info("feature semantic evidence verified key="
                + TargetResolver.KEY_SAME_TOPIC_FEED + " method=" + method);
    }

    private synchronized void installHook(String key, Method method,
                                          long expectedGeneration) {
        if (!isCurrent(expectedGeneration, method.getDeclaringClass().getClassLoader())) {
            return;
        }
        if (handles.contains(key, method)) {
            installState.markPrimaryHook(expectedGeneration, key);
            return;
        }
        try {
            HookHandle handle = module.hook(method)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-" + key + "-" + Integer.toHexString(
                            method.toGenericString().hashCode()))
                    .intercept(chain -> {
                        boolean block = shouldBlock(key);
                        if (!block) {
                            return chain.proceed();
                        }
                        if (TargetResolver.KEY_AUTO_COMMENT.equals(key)) {
                            return defaultValue(method.getReturnType());
                        }
                        if (TargetResolver.KEY_TOPIC_RECOMMEND.equals(key)) {
                            return defaultValue(method.getReturnType());
                        }
                        if (TargetResolver.KEY_DETAIL_SPONSOR.equals(key)) {
                            return null;
                        }
                        return chain.proceed();
                    });
            handles.put(key, method, handle);
            installState.markPrimaryHook(expectedGeneration, key);
            ledger.record(HookLedger.Layer.BUSINESS, "feature",
                    "feature-" + key, method.toGenericString());
            log.info("feature hook installed key=" + key + " method=" + method);
        } catch (Throwable throwable) {
            log.error("feature hook install failed key=" + key + " method=" + method,
                    throwable);
        }
    }

    private boolean shouldBlock(String key) {
        if (TargetResolver.KEY_AUTO_COMMENT.equals(key)) {
            return config.isEffectiveEnabled(PurifierConfig.Feature.AUTO_COMMENT, coolapkMajor);
        }
        if (TargetResolver.KEY_TOPIC_RECOMMEND.equals(key)) {
            return config.isEffectiveEnabled(
                    PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND, coolapkMajor);
        }
        if (TargetResolver.KEY_RELATED_DATA.equals(key)) {
            return config.isEffectiveEnabled(PurifierConfig.Feature.RELATED_DATA, coolapkMajor);
        }
        if (TargetResolver.KEY_DETAIL_SPONSOR.equals(key)) {
            return config.isEffectiveEnabled(PurifierConfig.Feature.DETAIL_SPONSOR, coolapkMajor);
        }
        return false;
    }

    boolean requiredTargetsReady(Map<String, ResolvedTarget> targets) {
        return missingRequiredTargets(targets).isEmpty();
    }

    List<String> missingRequiredTargets(Map<String, ResolvedTarget> targets) {
        return FeatureTargetReadiness.missing(
                config, coolapkMajor, targets, installState);
    }

    private boolean isCurrent(long expectedGeneration, ClassLoader loader) {
        return expectedGeneration == generation && loader == activeLoader;
    }

    private static Object defaultValue(Class<?> type) {
        if ("kotlin.Unit".equals(type.getName())) {
            try {
                return type.getField("INSTANCE").get(null);
            } catch (Throwable ignored) {
                return null;
            }
        }
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        return null;
    }
}
