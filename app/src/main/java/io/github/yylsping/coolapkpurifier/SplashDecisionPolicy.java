package io.github.yylsping.coolapkpurifier;

import java.util.function.BooleanSupplier;

/** Preserve the original execution, exception and OFF result; only override after completion. */
final class SplashDecisionPolicy {
    interface Proceed { Object call() throws Throwable; }
    interface Observation { void completed(Object original, Object returned, boolean enabled); }

    static Object intercept(Proceed proceed, BooleanSupplier enabled, Observation observation)
            throws Throwable {
        Object original = proceed.call(); // Exactly once. Never catch/retry a host exception.
        boolean suppress;
        try {
            suppress = enabled.getAsBoolean();
        } catch (Throwable unavailable) {
            suppress = false; // A module/configuration failure must leave the host result intact.
        }
        Object returned = suppress && Boolean.TRUE.equals(original) ? Boolean.FALSE : original;
        try {
            observation.completed(original, returned, suppress);
        } catch (Throwable ignored) { /* Logging cannot change the result or replay the host. */ }
        return returned;
    }

    static boolean ready(boolean activityInstalled, boolean embeddedHost, boolean decisionInstalled) {
        return activityInstalled && (!embeddedHost || decisionInstalled);
    }

    private SplashDecisionPolicy() { }
}
