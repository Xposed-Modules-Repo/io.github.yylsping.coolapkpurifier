package io.github.yylsping.coolapkpurifier;

/**
 * Pure policy for the Mode A-ZF Phase 1 Application.attach one-shot: the
 * bootstrap attach hook may only be unhooked after the coordinator received
 * everything it needs to continue on its own. A failed handoff must keep the
 * hook installed — never retire silently.
 */
final class AttachHandoffPolicy {
    private AttachHandoffPolicy() {
    }

    static final class HandoffState {
        final boolean contextSaved;
        final boolean configInitialized;
        final boolean settingsLifecycleRegistered;

        HandoffState(boolean contextSaved, boolean configInitialized,
                     boolean settingsLifecycleRegistered) {
            this.contextSaved = contextSaved;
            this.configInitialized = configInitialized;
            this.settingsLifecycleRegistered = settingsLifecycleRegistered;
        }
    }

    static boolean canRetireAttach(HandoffState state) {
        return state.contextSaved && state.configInitialized
                && state.settingsLifecycleRegistered;
    }

    /** First unsatisfied condition, for the failure log; null when ready. */
    static String missingCondition(HandoffState state) {
        if (!state.contextSaved) {
            return "contextMissing";
        }
        if (!state.configInitialized) {
            return "configMissing";
        }
        if (!state.settingsLifecycleRegistered) {
            return "settingsLifecycleMissing";
        }
        return null;
    }
}
