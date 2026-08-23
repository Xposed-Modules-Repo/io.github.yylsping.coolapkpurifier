package io.github.yylsping.coolapkpurifier;

import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/** Installs the cached method-level feature hooks and evaluates selected coverage. */
final class FeatureHooks {
    private final XposedModule module;
    private final ModuleLog log;
    private final PurifierConfig config;
    private final int coolapkMajor;
    private final EntityListHooks entityListHooks;
    private final Map<String, HookHandle> handles = new HashMap<>();
    private final Set<String> installedFeatureKeys = new HashSet<>();
    private final Set<View> guardedHolderViews = java.util.Collections.newSetFromMap(
            new WeakHashMap<>());
    private final List<HookHandle> lazyClassHandles = new ArrayList<>();
    private boolean lazyResolverInstalled;

    FeatureHooks(XposedModule module, ModuleLog log, PurifierConfig config,
                 int coolapkMajor, EntityListHooks entityListHooks) {
        this.module = module;
        this.log = log;
        this.config = config;
        this.coolapkMajor = coolapkMajor;
        this.entityListHooks = entityListHooks;
    }

    synchronized void installTargets(Map<String, ResolvedTarget> targets, ClassLoader loader) {
        installSemanticIfLoadable(loader,
                "com.coolapk.market.view.cardlist.component."
                        + "RecyclerViewItemFullVisibleControllerKt");
        installSemanticIfLoadable(loader,
                "com.coolapk.market.view.feedv8.component.TopicRecommendConfig");
        installSemanticIfLoadable(loader,
                "com.coolapk.market.view.feedv8.component.TopicRecommendKt");
        installSemanticIfLoadable(loader,
                "com.coolapk.market.viewholder.MultiFeedReplyViewHolder");
        installSemanticIfLoadable(loader,
                "com.coolapk.market.viewholder.RelatedDataViewHolder");
        installSemanticIfLoadable(loader,
                "com.coolapk.market.view.ad.SponsorSelfDrawDetailViewHolder");
        installMethod(targets.get(TargetResolver.KEY_SPLASH_DECISION), loader);
        installMethod(targets.get(TargetResolver.KEY_AUTO_COMMENT), loader);
        installMethod(targets.get(TargetResolver.KEY_TOPIC_RECOMMEND), loader);
    }

    private void installSemanticIfLoadable(ClassLoader loader, String className) {
        try {
            maybeInstallSemanticClass(Class.forName(className, false, loader));
        } catch (Throwable ignored) {
            // The persistent loadClass hook handles this staged dex later.
        }
    }

    /** Hooks semantic business classes as protected Coolapk appends their dex. */
    synchronized void installLazyResolvers() {
        if (lazyResolverInstalled || coolapkMajor < 15) {
            return;
        }
        try {
            hookLoadClass(ClassLoader.class.getDeclaredMethod("loadClass", String.class));
            hookLoadClass(ClassLoader.class.getDeclaredMethod(
                    "loadClass", String.class, boolean.class));
            lazyResolverInstalled = !lazyClassHandles.isEmpty();
            log.info("feature lazy class resolver installed handles="
                    + lazyClassHandles.size());
        } catch (Throwable throwable) {
            log.error("feature lazy class resolver install failed", throwable);
        }
    }

    private void hookLoadClass(Method loadClass) {
        try {
            HookHandle handle = module.hook(loadClass)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-issue2-lazy-" + loadClass.getParameterCount())
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof Class<?>) {
                            maybeInstallSemanticClass((Class<?>) result);
                        }
                        return result;
                    });
            lazyClassHandles.add(handle);
        } catch (Throwable throwable) {
            log.error("feature lazy loadClass hook failed method=" + loadClass, throwable);
        }
    }

    private synchronized void maybeInstallSemanticClass(Class<?> type) {
        String className = type.getName();
        if ("com.coolapk.market.viewholder.MultiFeedReplyViewHolder".equals(className)) {
            entityListHooks.installReplyHolder(type);
            return;
        }
        if ("com.coolapk.market.viewholder.RelatedDataViewHolder".equals(className)) {
            installDedicatedHolder(TargetResolver.KEY_RELATED_DATA, type);
            return;
        }
        if ("com.coolapk.market.view.ad.SponsorSelfDrawDetailViewHolder"
                .equals(className)) {
            installDedicatedHolder(TargetResolver.KEY_DETAIL_SPONSOR, type);
            return;
        }
        if (("com.coolapk.market.view.cardlist.component."
                + "RecyclerViewItemFullVisibleControllerKt").equals(className)) {
            for (Method method : type.getDeclaredMethods()) {
                if ("addAutoShowFeedCommentView".equals(method.getName())) {
                    installHook(TargetResolver.KEY_AUTO_COMMENT, method);
                }
            }
            return;
        }
        if (!className.startsWith(
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
                installHook(TargetResolver.KEY_TOPIC_RECOMMEND, method);
            }
        }
    }

    private void installDedicatedHolder(String key, Class<?> holderClass) {
        for (Constructor<?> constructor : holderClass.getDeclaredConstructors()) {
            String handleKey = key + "|constructor|" + constructor.toGenericString();
            if (handles.containsKey(handleKey)) {
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
                handles.put(handleKey, handle);
                installedFeatureKeys.add(key);
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
            String handleKey = key + "|holder|" + method.toGenericString();
            if (handles.containsKey(handleKey)) {
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
                handles.put(handleKey, handle);
                installedFeatureKeys.add(key);
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

    private void installMethod(ResolvedTarget target, ClassLoader loader) {
        if (target == null) {
            return;
        }
        Method method = DescriptorUtils.methodForDescriptor(target.methodDescriptor, loader);
        if (method == null) {
            log.info("feature hook pending key=" + target.key + " reason=methodNotLoadable");
            return;
        }
        installHook(target.key, method);
    }

    private synchronized void installHook(String key, Method method) {
        String handleKey = key + "|" + method.toGenericString();
        if (handles.containsKey(handleKey)) {
            return;
        }
        try {
            HookHandle handle = module.hook(method)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-" + key + "-" + Integer.toHexString(
                            method.toGenericString().hashCode()))
                    .intercept(chain -> {
                        if (!shouldBlock(key)) {
                            return chain.proceed();
                        }
                        if (TargetResolver.KEY_SPLASH_DECISION.equals(key)) {
                            log.info("blocked splash decision via " + method);
                            return false;
                        }
                        if (TargetResolver.KEY_AUTO_COMMENT.equals(key)) {
                            return defaultValue(method.getReturnType());
                        }
                        if (TargetResolver.KEY_TOPIC_RECOMMEND.equals(key)) {
                            return defaultValue(method.getReturnType());
                        }
                        return chain.proceed();
                    });
            handles.put(handleKey, handle);
            installedFeatureKeys.add(key);
            log.info("feature hook installed key=" + key + " method=" + method);
        } catch (Throwable throwable) {
            log.error("feature hook install failed key=" + key + " method=" + method,
                    throwable);
        }
    }

    private boolean shouldBlock(String key) {
        if (TargetResolver.KEY_SPLASH_DECISION.equals(key)) {
            return config.isEffectiveEnabled(PurifierConfig.Feature.SPLASH, coolapkMajor);
        }
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
        List<String> missing = new ArrayList<>();
        requireMethod(targets, TargetResolver.KEY_SPLASH_DECISION,
                config.isEffectiveEnabled(PurifierConfig.Feature.SPLASH, coolapkMajor), missing);
        requireMethod(targets, TargetResolver.KEY_AUTO_COMMENT,
                config.isEffectiveEnabled(PurifierConfig.Feature.AUTO_COMMENT, coolapkMajor),
                missing);
        requireMethod(targets, TargetResolver.KEY_TOPIC_RECOMMEND,
                config.isEffectiveEnabled(
                        PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND, coolapkMajor), missing);
        return missing;
    }

    private void requireMethod(Map<String, ResolvedTarget> targets, String key,
                               boolean required, List<String> missing) {
        if (!required) {
            return;
        }
        if (TargetResolver.KEY_AUTO_COMMENT.equals(key)
                || TargetResolver.KEY_TOPIC_RECOMMEND.equals(key)) {
            if (!lazyResolverInstalled && !installedFeatureKeys.contains(key)) {
                missing.add(key);
            }
            return;
        }
        ResolvedTarget target = targets.get(key);
        if (target == null || target.methodDescriptor == null
                || target.methodDescriptor.isEmpty()
                || !installedFeatureKeys.contains(key)) {
            missing.add(key);
        }
    }

    private static Object defaultValue(Class<?> type) {
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
