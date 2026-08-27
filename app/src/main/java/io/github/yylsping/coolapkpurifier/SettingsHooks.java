package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.app.Application;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Injects the "酷安净化" entry into Coolapk's Compose settings host. */
final class SettingsHooks {
    static final String EXIT_MESSAGE = "请重启软件以动态适配更改选项";
    private static final String SIMPLE_ACTIVITY =
            "com.coolapk.market.view.base.SimpleActivity";

    private final ModuleLog log;
    private final PurifierConfig config;
    private final int coolapkMajor;
    private final FeatureRuntimeHealth runtimeHealth;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final PageStateRegistry<Activity, PageState> injected = new PageStateRegistry<>();
    private final PageInjectionRetry<Activity> injectionRetry;
    private volatile boolean lifecycleCallbacksInstalled;

    SettingsHooks(ModuleLog log, PurifierConfig config,
                  int coolapkMajor) {
        this(log, config, coolapkMajor, new FeatureRuntimeHealth());
    }

    SettingsHooks(ModuleLog log, PurifierConfig config,
                  int coolapkMajor, FeatureRuntimeHealth runtimeHealth) {
        this.log = log;
        this.config = config;
        this.coolapkMajor = coolapkMajor;
        this.runtimeHealth = runtimeHealth;
        this.injectionRetry = new PageInjectionRetry<>(
                (task, delay) -> mainHandler.postDelayed(task, delay),
                mainHandler::removeCallbacks, this::maybeInject,
                activity -> log.info("settings injection retry exhausted activity="
                        + activity.getClass().getName()));
    }

    void install(Context context) {
        if (lifecycleCallbacksInstalled) {
            return;
        }
        try {
            Context candidate = context;
            if (!(candidate instanceof Application) && context != null) {
                candidate = context.getApplicationContext();
            }
            if (!(candidate instanceof Application)) {
                log.info("settings lifecycle callbacks skipped reason=applicationMissing");
                return;
            }
            Application application = (Application) candidate;
            application.registerActivityLifecycleCallbacks(
                    new Application.ActivityLifecycleCallbacks() {
                        @Override
                        public void onActivityCreated(Activity activity, Bundle state) {
                        }

                        @Override
                        public void onActivityStarted(Activity activity) {
                        }

                        @Override
                        public void onActivityResumed(Activity activity) {
                            if (activity != null
                                    && SIMPLE_ACTIVITY.equals(activity.getClass().getName())) {
                                injectionRetry.start(activity);
                            }
                        }

                        @Override
                        public void onActivityPaused(Activity activity) {
                            injectionRetry.cancel(activity);
                        }

                        @Override
                        public void onActivityStopped(Activity activity) {
                        }

                        @Override
                        public void onActivitySaveInstanceState(
                                Activity activity, Bundle state) {
                        }

                        @Override
                        public void onActivityDestroyed(Activity activity) {
                            SettingsHooks.this.onActivityDestroyed(activity);
                        }
                    });
            lifecycleCallbacksInstalled = true;
            log.info("settings lifecycle callbacks registered frameworkHooks=0");
        } catch (Throwable throwable) {
            log.error("settings lifecycle callback registration failed", throwable);
        }
    }

    /** Attach-handoff precondition (Mode A-ZF Phase 1). */
    boolean isLifecycleCallbacksInstalled() {
        return lifecycleCallbacksInstalled;
    }

    /** True stops the retry; false means the host view tree is not ready yet. */
    private boolean maybeInject(Activity activity) {
        try {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()
                    || !SIMPLE_ACTIVITY.equals(activity.getClass().getName())
                    || injected.contains(activity)) {
                return true;
            }
            View decor = activity.getWindow().getDecorView();
            TextView toolbarTitle = findTextView(decor, "设置");
            if (toolbarTitle == null) {
                return false;
            }
            Resources resources = activity.getResources();
            int contentId = resources.getIdentifier(
                    "content_view", "id", CoolapkModule.TARGET_PACKAGE);
            View content = contentId == 0 ? null : activity.findViewById(contentId);
            if (!(content instanceof FrameLayout)) {
                log.info("settings injection skipped reason=contentViewNotFrame");
                return false;
            }
            FrameLayout frame = (FrameLayout) content;
            View compose = findByClassName(frame, "androidx.compose.ui.platform.ComposeView");
            if (compose == null || !(compose.getLayoutParams() instanceof FrameLayout.LayoutParams)) {
                log.info("settings injection skipped reason=composeViewMissing");
                return false;
            }

            FrameLayout.LayoutParams original =
                    (FrameLayout.LayoutParams) compose.getLayoutParams();
            int shift = dp(activity, 80);
            int originalTop = original.topMargin;
            FrameLayout.LayoutParams moved = new FrameLayout.LayoutParams(original);
            moved.topMargin = originalTop + shift;
            compose.setLayoutParams(moved);

            View entry = createEntry(activity);
            FrameLayout.LayoutParams entryParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 61));
            entryParams.leftMargin = dp(activity, 14);
            entryParams.rightMargin = dp(activity, 14);
            entryParams.topMargin = originalTop + dp(activity, 14);
            frame.addView(entry, entryParams);
            PageState state = new PageState(frame, compose, entry, toolbarTitle, originalTop);
            entry.setOnClickListener(view -> showConfigPage(activity, state));
            injected.put(activity, state);
            log.info("settings entry injected top=" + entryParams.topMargin
                    + " composeShift=" + shift + " coolapkMajor=" + coolapkMajor);
            return true;
        } catch (Throwable throwable) {
            log.error("settings injection skipped; core purifier unaffected", throwable);
            return true;
        }
    }

    private View createEntry(Activity activity) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 18), 0, dp(activity, 18), 0);
        row.setClickable(true);
        row.setFocusable(true);

        int cardColor = resolveColor(activity, android.R.attr.colorBackgroundFloating,
                Color.WHITE);
        GradientDrawable card = new GradientDrawable();
        card.setColor(cardColor);
        card.setCornerRadius(dp(activity, 12));
        row.setBackground(new RippleDrawable(
                ColorStateList.valueOf(0x18000000), card, null));

        ImageView icon = new ImageView(activity);
        Drawable iconDrawable = activity.getDrawable(android.R.drawable.ic_menu_manage);
        if (iconDrawable != null) {
            iconDrawable = iconDrawable.mutate();
            iconDrawable.setTint(resolveColor(activity, android.R.attr.colorAccent, 0xff00a88f));
            icon.setImageDrawable(iconDrawable);
        }
        row.addView(icon, new LinearLayout.LayoutParams(dp(activity, 24), dp(activity, 24)));

        TextView title = new TextView(activity);
        title.setText("酷安净化");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTextColor(resolveColor(activity, android.R.attr.textColorPrimary, Color.BLACK));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(activity, 14);
        row.addView(title, titleParams);

        TextView arrow = new TextView(activity);
        arrow.setText("›");
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        arrow.setGravity(Gravity.CENTER);
        arrow.setTextColor(resolveColor(activity, android.R.attr.textColorSecondary, 0xff777777));
        row.addView(arrow, new LinearLayout.LayoutParams(dp(activity, 24),
                ViewGroup.LayoutParams.MATCH_PARENT));
        return row;
    }

    private void showConfigPage(Activity activity, PageState state) {
        if (state.inConfig || state.transitioning) {
            return;
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(resolveColor(activity, android.R.attr.colorBackground,
                0xfff7f7fa));
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        int side = dp(activity, 14);
        list.setPadding(side, dp(activity, 14), side, dp(activity, 28));
        for (PurifierConfig.Feature feature : PurifierConfig.Feature.values()) {
            list.addView(createSwitchRow(activity, feature),
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.topMargin = state.originalComposeTop;
        scroll.setElevation(dp(activity, 8));
        state.frame.addView(scroll, params);
        state.configPage = scroll;
        state.exitNotified = false;
        state.toolbarTitle.setText("酷安净化");
        state.inConfig = true;
        state.transitioning = true;
        int width = Math.max(state.frame.getWidth(),
                activity.getResources().getDisplayMetrics().widthPixels);
        scroll.setTranslationX(width);
        scroll.animate()
                .translationX(0f)
                .setDuration(280L)
                .setInterpolator(AnimationUtils.loadInterpolator(activity,
                        android.R.interpolator.fast_out_slow_in))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (state.configPage == scroll && state.inConfig) {
                            state.compose.setVisibility(View.INVISIBLE);
                            state.entry.setVisibility(View.INVISIBLE);
                        }
                        state.transitioning = false;
                    }
                })
                .start();
        log.info("settings config page shown inline=true coolapkActivity="
                + activity.getClass().getName());
    }

    private void notifyConfigExit(Activity activity, PageState state) {
        if (state == null || state.exitNotified) {
            return;
        }
        state.exitNotified = true;
        Context application = activity.getApplicationContext();
        mainHandler.post(() -> Toast.makeText(application,
                EXIT_MESSAGE, Toast.LENGTH_LONG).show());
        log.info("settings config page exited toastRequested=true");
    }

    private void onActivityDestroyed(Activity activity) {
        injectionRetry.cancel(activity);
        PageState state = injected.remove(activity);
        if (state == null) {
            return;
        }
        if (state.inConfig) {
            notifyConfigExit(activity, state);
        }
        state.dispose();
        log.info("settings page state removed on destroy remaining=" + injected.size());
    }

    @SuppressWarnings("deprecation")
    private View createSwitchRow(Context context, PurifierConfig.Feature feature) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, 68));
        row.setPadding(dp(context, 18), dp(context, 8), dp(context, 12), dp(context, 8));

        GradientDrawable background = new GradientDrawable();
        background.setColor(resolveColor(context, android.R.attr.colorBackgroundFloating,
                Color.WHITE));
        background.setCornerRadius(dp(context, 8));
        row.setBackground(background);

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(context);
        title.setText(feature.title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTextColor(resolveColor(context, android.R.attr.textColorPrimary, Color.BLACK));
        labels.addView(title);
        boolean supported = !feature.requiresCoolapk15 || coolapkMajor >= 15;
        if (!supported) {
            TextView summary = new TextView(context);
            summary.setText("仅支持酷安 15.x 及以上");
            summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            summary.setTextColor(resolveColor(context,
                    android.R.attr.textColorSecondary, 0xff888888));
            labels.addView(summary);
        }
        if (feature == PurifierConfig.Feature.REPLY_SPONSOR && supported) {
            TextView status = new TextView(context);
            status.setText(runtimeHealth.replyMessage());
            status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            status.setTextColor(resolveColor(context,
                    android.R.attr.textColorSecondary, 0xff888888));
            Runnable refresh = () -> mainHandler.post(
                    () -> status.setText(runtimeHealth.replyMessage()));
            status.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View view) {
                    runtimeHealth.addListener(refresh);
                    refresh.run();
                }
                @Override public void onViewDetachedFromWindow(View view) {
                    runtimeHealth.removeListener(refresh);
                }
            });
            labels.addView(status);
        }
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(context);
        toggle.setChecked(config.isEnabled(feature));
        toggle.setEnabled(supported);
        toggle.setOnCheckedChangeListener((button, checked) -> {
            if (!config.setEnabled(feature, checked)) {
                Toast.makeText(context.getApplicationContext(),
                        "配置保存失败", Toast.LENGTH_SHORT).show();
                button.setChecked(config.isEnabled(feature));
            }
        });
        row.addView(toggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setEnabled(supported);
        row.setAlpha(supported ? 1f : 0.45f);
        row.setOnClickListener(view -> {
            if (supported) {
                toggle.setChecked(!toggle.isChecked());
            }
        });
        return row;
    }

    private static TextView findTextView(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findTextView(group.getChildAt(i), expected);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static View findByClassName(View view, String className) {
        if (className.equals(view.getClass().getName())) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findByClassName(group.getChildAt(i), className);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static int resolveColor(Context context, int attribute, int fallback) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attribute, value, true)) {
            if (value.resourceId != 0) {
                try {
                    return context.getColor(value.resourceId);
                } catch (Throwable ignored) {
                }
            }
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
        }
        return fallback;
    }

    private static final class PageState {
        final FrameLayout frame;
        final View compose;
        final View entry;
        final TextView toolbarTitle;
        final int originalComposeTop;
        View configPage;
        boolean inConfig;
        boolean exitNotified;
        boolean transitioning;

        PageState(FrameLayout frame, View compose, View entry, TextView toolbarTitle,
                  int originalComposeTop) {
            this.frame = frame;
            this.compose = compose;
            this.entry = entry;
            this.toolbarTitle = toolbarTitle;
            this.originalComposeTop = originalComposeTop;
        }

        void dispose() {
            entry.animate().cancel();
            entry.setOnClickListener(null);
            if (configPage != null) {
                configPage.animate().cancel();
                if (configPage.getParent() == frame) {
                    frame.removeView(configPage);
                }
                configPage = null;
            }
        }
    }
}
