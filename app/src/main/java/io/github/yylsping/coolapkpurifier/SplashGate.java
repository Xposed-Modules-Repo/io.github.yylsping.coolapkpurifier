package io.github.yylsping.coolapkpurifier;

import android.app.Activity;

/**
 * First-launch splash protection. Loose namespace/simple-name matching is
 * allowed only before MainActivity is seen. Resolved descriptors always win.
 */
final class SplashGate {
    private static final String SPLASH_PACKAGE = "com.coolapk.market.view.splash";

    private volatile Class<?> resolvedSplashClass;
    private volatile boolean mainActivitySeen;
    private volatile boolean firstActivitySeen;

    void setResolvedSplashClass(Class<?> type) {
        resolvedSplashClass = type;
    }

    boolean hasResolvedSplashClass() {
        return resolvedSplashClass != null;
    }

    void markFirstActivity() {
        firstActivitySeen = true;
    }

    boolean isFirstActivitySeen() {
        return firstActivitySeen;
    }

    void markMainActivity() {
        mainActivitySeen = true;
    }

    boolean isStartupPhase() {
        return !mainActivitySeen;
    }

    boolean isMainActivitySeen() {
        return mainActivitySeen;
    }

    boolean isResolvedSplash(Activity activity) {
        Class<?> resolved = resolvedSplashClass;
        return resolved != null && resolved.isAssignableFrom(activity.getClass());
    }

    boolean isFallbackSplashCandidate(Activity activity) {
        if (!isStartupPhase()) {
            return false;
        }
        return SPLASH_PACKAGE.equals(packageName(activity.getClass()))
                && isSplashSimpleName(simpleName(activity.getClass()));
    }

    boolean isLegacySplash(Activity activity) {
        String name = activity.getClass().getName();
        return "com.coolapk.market.view.splash.SplashActivity".equals(name)
                || "com.coolapk.market.view.splash.FullScreenAdActivity".equals(name);
    }

    boolean shouldFinishSplash(Activity activity) {
        return isResolvedSplash(activity)
                || isFallbackSplashCandidate(activity)
                || isLegacySplash(activity);
    }

    private static boolean isSplashSimpleName(String simpleName) {
        return simpleName != null
                && (simpleName.contains("Splash") || simpleName.contains("FullScreenAd"));
    }

    private static String packageName(Class<?> type) {
        Package pkg = type.getPackage();
        if (pkg != null) {
            return pkg.getName();
        }
        String name = type.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? name : name.substring(0, lastDot);
    }

    private static String simpleName(Class<?> type) {
        String name = type.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? name : name.substring(lastDot + 1);
    }
}
