package io.github.yylsping.coolapkpurifier;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/** Stable resource-name fallback for the dedicated Issue #2 card layouts. */
final class ContentLayoutHooks {
    private final XposedModule module;
    private final ModuleLog log;
    private final PurifierConfig config;
    private final int coolapkMajor;
    private final HookInstallPlan plan;
    private final FeatureInstallState installState;
    private final Resources resources;
    private HookHandle inflateHandle;
    private HookHandle layoutTagHandle;

    ContentLayoutHooks(XposedModule module, ModuleLog log, PurifierConfig config,
                       int coolapkMajor, HookInstallPlan plan,
                       FeatureInstallState installState, Resources resources) {
        this.module = module;
        this.log = log;
        this.config = config;
        this.coolapkMajor = coolapkMajor;
        this.plan = plan;
        this.installState = installState;
        this.resources = resources;
    }

    void install() {
        if (!plan.installLayoutInflater || inflateHandle != null) {
            return;
        }
        try {
            Method inflate = LayoutInflater.class.getDeclaredMethod(
                    "inflate", int.class, ViewGroup.class, boolean.class);
            inflateHandle = module.hook(inflate)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-issue2-layout-filter")
                    .intercept(chain -> {
                        int resourceId = (Integer) chain.getArg(0);
                        Object rootArg = chain.getArg(1);
                        ViewGroup parent = rootArg instanceof ViewGroup
                                ? (ViewGroup) rootArg : null;
                        boolean attach = Boolean.TRUE.equals(chain.getArg(2));
                        int previousChildren = parent == null ? -1 : parent.getChildCount();
                        Object original = chain.proceed();
                        View inflated = original instanceof View ? (View) original : null;
                        if (attach && parent != null
                                && parent.getChildCount() > previousChildren
                                && previousChildren >= 0) {
                            inflated = parent.getChildAt(previousChildren);
                        }
                        filter(resourceId, inflated);
                        return original;
                    });
            if (plan.installViewTag) {
                Method setTag = View.class.getDeclaredMethod("setTag", Object.class);
                layoutTagHandle = module.hook(setTag)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .setId("coolapk-issue2-layout-tag-filter")
                        .intercept(chain -> {
                            View view = (View) chain.getThisObject();
                            Object next = chain.getArg(0);
                            String tag = next instanceof String
                                    ? (String) next
                                    : (view.getTag() instanceof String
                                    ? (String) view.getTag() : "");
                            PurifierConfig.Feature taggedFeature = featureForLayoutTag(tag);
                            Object result = chain.proceed();
                            if (taggedFeature != null && isRemovalAllowed(taggedFeature)) {
                                collapse(view);
                                log.info("removed issue2 layoutTag=" + tag
                                        + " feature=" + taggedFeature.key);
                            }
                            return result;
                        });
            }
            log.info("issue2 layout filter hooks installed inflater=true viewTag="
                    + plan.installViewTag);
            long generation = installState.generation();
            if (generation > 0) {
                verifyFeatureFallbacks(generation);
            }
        } catch (Throwable throwable) {
            log.error("issue2 layout filter hook install failed", throwable);
        }
    }

    boolean isInstalled() {
        return (!plan.installLayoutInflater || inflateHandle != null)
                && (!plan.installViewTag || layoutTagHandle != null);
    }

    void verifyFeatureFallbacks(long expectedGeneration) {
        if (expectedGeneration <= 0 || inflateHandle == null || resources == null) {
            return;
        }
        int commentView = resourceId("comment_view", "id");
        int parentV8 = resourceId("item_feed_layout_v8", "layout");
        int parentV8Lite = resourceId("item_feed_layout_v8_lite", "layout");
        if (config.isEffectiveEnabled(PurifierConfig.Feature.AUTO_COMMENT, coolapkMajor)
                && hasAutoCommentFallbackEvidence(
                inflateHandle != null, commentView, parentV8, parentV8Lite)) {
            installState.markFallbackEvidence(
                    expectedGeneration, TargetResolver.KEY_AUTO_COMMENT);
        }
        if (config.isEffectiveEnabled(PurifierConfig.Feature.RELATED_DATA, coolapkMajor)
                && resourceId("item_related_data", "layout") != 0) {
            installState.markFallbackEvidence(
                    expectedGeneration, TargetResolver.KEY_RELATED_DATA);
        }
        log.info("feature layout fallback evidence generation=" + expectedGeneration
                + " autoComment=" + installState.hasFallbackEvidence(
                TargetResolver.KEY_AUTO_COMMENT)
                + " autoCommentChild=" + (commentView != 0)
                + " autoCommentParentV8=" + (parentV8 != 0)
                + " autoCommentParentV8Lite=" + (parentV8Lite != 0)
                + " relatedData=" + installState.hasFallbackEvidence(
                TargetResolver.KEY_RELATED_DATA));
    }

    static boolean hasAutoCommentFallbackEvidence(boolean inflaterInstalled,
                                                  int commentViewId,
                                                  int parentV8LayoutId,
                                                  int parentV8LiteLayoutId) {
        return inflaterInstalled && commentViewId != 0
                && (parentV8LayoutId != 0 || parentV8LiteLayoutId != 0);
    }

    private int resourceId(String name, String type) {
        try {
            return resources.getIdentifier(name, type, CoolapkModule.TARGET_PACKAGE);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void filter(int resourceId, View inflated) {
        if (inflated == null || coolapkMajor < 15) {
            return;
        }
        String name = resourceEntryName(inflated, resourceId);
        PurifierConfig.Feature feature = featureForLayout(name);
        if (feature != null && isRemovalAllowed(feature)) {
            collapse(inflated);
            log.info("removed issue2 layout=" + name + " feature=" + feature.key);
        }
        if (config.isEffectiveEnabled(PurifierConfig.Feature.AUTO_COMMENT, coolapkMajor)
                && ("item_feed_layout_v8".equals(name)
                || "item_feed_layout_v8_lite".equals(name))) {
            collapseNamedChild(inflated, "comment_view",
                    PurifierConfig.Feature.AUTO_COMMENT);
        }
        if (config.isEffectiveEnabled(
                PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND, coolapkMajor)
                && "item_relative_info_with_recommend".equals(name)) {
            // Keep the legitimate relative-info portion of the binding and
            // collapse only its dedicated TopicRecommend Compose host.
            collapseNamedChild(inflated, "compose_view",
                    PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND);
        }
    }

    private void collapseNamedChild(View inflated, String entryName,
                                    PurifierConfig.Feature feature) {
        View child = findResourceEntry(inflated, entryName);
        if (child != null) {
            collapse(child);
            log.info("removed issue2 child=" + entryName + " feature=" + feature.key);
        }
    }

    private PurifierConfig.Feature featureForLayout(String name) {
        if ("item_related_data".equals(name)) {
            return PurifierConfig.Feature.RELATED_DATA;
        }
        if ("item_recommend_feed_card".equals(name)) {
            return PurifierConfig.Feature.SAME_TOPIC_FEED;
        }
        if ("item_sponsor_self_draw_detail".equals(name) || "item_ads".equals(name)) {
            return PurifierConfig.Feature.DETAIL_SPONSOR;
        }
        return null;
    }

    private PurifierConfig.Feature featureForLayoutTag(String tag) {
        if (tag == null || !tag.startsWith("layout/")) {
            return null;
        }
        int suffix = tag.lastIndexOf('_');
        String layout = suffix > "layout/".length()
                ? tag.substring("layout/".length(), suffix)
                : tag.substring("layout/".length());
        return featureForLayout(layout);
    }

    private boolean isRemovalAllowed(PurifierConfig.Feature feature) {
        if (coolapkMajor < 15
                || !config.isEffectiveEnabled(feature, coolapkMajor)) {
            return false;
        }
        return feature != PurifierConfig.Feature.SAME_TOPIC_FEED
                || installState.hasSemanticEvidence(TargetResolver.KEY_SAME_TOPIC_FEED);
    }

    private static String resourceEntryName(View view, int resourceId) {
        if (resourceId == 0) {
            return "";
        }
        try {
            return view.getResources().getResourceEntryName(resourceId);
        } catch (Resources.NotFoundException ignored) {
            return "";
        }
    }

    private static View findResourceEntry(View view, String expected) {
        if (view.getId() != View.NO_ID) {
            try {
                if (expected.equals(view.getResources().getResourceEntryName(view.getId()))) {
                    return view;
                }
            } catch (Throwable ignored) {
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findResourceEntry(group.getChildAt(i), expected);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collapse(View view) {
        view.setVisibility(View.GONE);
        view.setMinimumHeight(0);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.height = 0;
            view.setLayoutParams(params);
        }
    }
}
