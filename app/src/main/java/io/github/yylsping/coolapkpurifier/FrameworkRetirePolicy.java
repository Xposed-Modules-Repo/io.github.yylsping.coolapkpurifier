package io.github.yylsping.coolapkpurifier;

/**
 * Pure policy for the Mode A-ZF Phase 2 Instrumentation safety gate: on a
 * clean READY with a specific splash hook installed, the generic
 * Instrumentation fallback is retired; every other terminal outcome retains
 * it as the documented DEGRADED fallback.
 */
final class FrameworkRetirePolicy {
    private FrameworkRetirePolicy() {
    }

    static boolean shouldRetireInstrumentationSafety(BootstrapState terminalState,
                                                     boolean splashSpecificInstalled,
                                                     boolean lifecycleGuardInstalled) {
        return terminalState == BootstrapState.READY && splashSpecificInstalled
                && lifecycleGuardInstalled;
    }

    /** Human-readable retention reason for the retained branch. */
    static String retainReason(BootstrapState terminalState,
                               boolean splashSpecificInstalled,
                               boolean lifecycleGuardInstalled) {
        if (terminalState == BootstrapState.READY && !splashSpecificInstalled) {
            return "splashSpecificMissing";
        }
        if (terminalState == BootstrapState.READY && !lifecycleGuardInstalled) {
            return "splashLifecycleGuardMissing";
        }
        return "terminal:" + terminalState;
    }
}
