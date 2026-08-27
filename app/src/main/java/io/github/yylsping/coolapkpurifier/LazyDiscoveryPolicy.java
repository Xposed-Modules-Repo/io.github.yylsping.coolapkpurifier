package io.github.yylsping.coolapkpurifier;

/**
 * Pure policy for the Mode A-ZF Phase 3 lazy-discovery lifecycle. The two
 * temporary ClassLoader.loadClass hooks are no longer installed eagerly at
 * configuration time; they are only installed when a resolution session ends
 * with selected semantic targets still missing, and they retire as soon as
 * the last target is installed (or at terminal cleanup).
 */
final class LazyDiscoveryPolicy {
    private LazyDiscoveryPolicy() {
    }

    /**
     * A cached resolution may drive READY directly (no lazy hooks at all)
     * only when a selected reply holder is already installed. When it is
     * selected but missing, the session must fall through to the discovery
     * path so the temporary hooks get a chance to find it, matching the
     * pre-refactor discovery window.
     */
    static boolean blocksCacheFastPath(boolean replySelected, boolean replyInstalled) {
        return replySelected && !replyInstalled;
    }

    /**
     * Temporary discovery hooks are needed only when the plan still wants the
     * class-loader channel AND at least one selected semantic target is
     * missing. A permanently-disabled (terminal) discovery never re-arms.
     */
    static boolean shouldInstallLazyDiscovery(boolean planInstallClassLoader,
                                              boolean missingSemanticTargets,
                                              boolean permanentlyDisabled) {
        return planInstallClassLoader && missingSemanticTargets
                && !permanentlyDisabled;
    }
}
