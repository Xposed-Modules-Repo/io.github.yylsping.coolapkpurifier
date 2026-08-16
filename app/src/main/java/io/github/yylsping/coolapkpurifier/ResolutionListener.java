package io.github.yylsping.coolapkpurifier;

import java.util.Map;

/** Immutable resolution events delivered on the coordinator worker thread. */
interface ResolutionListener {
    void onSplashResolved(ResolvedTarget target);

    void onNormalResolved(Map<String, ResolvedTarget> targets);

    void onFullResolved();

    void onResolutionFailed(String reason);
}
