package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

/** An owner-bound page using the host's native activity enter/exit animations. */
final class SettingsPageDialog extends Dialog {
    private final Activity owner;
    private final View page;
    private boolean exiting;
    private final Runnable finishExit = this::dismissImmediately;

    SettingsPageDialog(Activity owner, View page, int background) {
        super(owner, android.R.style.Theme_DeviceDefault_NoActionBar);
        this.owner = owner;
        this.page = page;
        setOwnerActivity(owner);
        page.setClickable(true);
        page.setFitsSystemWindows(false);
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            // The opaque page includes the system-bar areas; only its children
            // are inset. A transparent navigation bar must never expose the
            // owner's settings list below a shorter content view.
            if (Build.VERSION.SDK_INT >= 30) {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return WindowInsets.CONSUMED;
            }
            int left = insets.getSystemWindowInsetLeft();
            int top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight();
            int bottom = insets.getSystemWindowInsetBottom();
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                left = Math.max(left, cutout.getSafeInsetLeft());
                top = Math.max(top, cutout.getSafeInsetTop());
                right = Math.max(right, cutout.getSafeInsetRight());
                bottom = Math.max(bottom, cutout.getSafeInsetBottom());
            }
            view.setPadding(left, top, right, bottom);
            return insets.consumeSystemWindowInsets().consumeDisplayCutout();
        });
        Window window = getWindow();
        if (window != null) {
            // Reveal the native settings underneath the moving page. Activity
            // animation styles have no dialog enter/exit attributes, so play
            // their view animations without a second window animation.
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setWindowAnimations(0);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            boolean light = Color.red(background) * .299 + Color.green(background) * .587
                    + Color.blue(background) * .114 >= 128;
            int appearanceFlags = light
                    ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0;
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | appearanceFlags);
            if (Build.VERSION.SDK_INT >= 29) {
                window.setStatusBarContrastEnforced(false);
                window.setNavigationBarContrastEnforced(false);
            }
            if (Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false);
                if (window.getInsetsController() != null) {
                    int appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    window.getInsetsController().setSystemBarsAppearance(light ? appearance : 0, appearance);
                }
            }
        }
    }

    @Override public void show() {
        super.show();
        page.requestApplyInsets();
        Animation enter = loadAnimation("right_in");
        if (enter != null) page.startAnimation(enter);
    }

    @Override public void dismiss() {
        // Destruction must release the window immediately, even mid-animation.
        if (!isShowing() || owner.isFinishing() || owner.isDestroyed()
                || !page.isAttachedToWindow()) {
            dismissImmediately();
            return;
        }
        if (exiting) return;
        Animation exit = loadAnimation("right_out");
        if (exit == null) {
            dismissImmediately();
            return;
        }
        exiting = true;
        exit.setFillAfter(true);
        exit.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationRepeat(Animation animation) { }
            @Override public void onAnimationEnd(Animation animation) {
                // Removing a window during traversal can interrupt its draw.
                page.post(finishExit);
            }
        });
        page.startAnimation(exit);
        // Also release if the window stops drawing before the last frame.
        page.postDelayed(finishExit, exit.computeDurationHint() + 100);
    }

    private Animation loadAnimation(String name) {
        try {
            float scale = Settings.Global.getFloat(owner.getContentResolver(),
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 1f);
            if (scale <= 0 || !Float.isFinite(scale)) return null;
            int resource = owner.getResources().getIdentifier(name, "anim", "com.coolapk.market");
            if (resource == 0) return null;
            Animation animation = AnimationUtils.loadAnimation(owner, resource);
            animation.scaleCurrentDuration(scale);
            return animation;
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private void dismissImmediately() {
        page.removeCallbacks(finishExit);
        page.clearAnimation();
        super.dismiss();
    }

    @Override protected void onStop() {
        page.removeCallbacks(finishExit);
        page.clearAnimation();
        super.onStop();
    }
}
