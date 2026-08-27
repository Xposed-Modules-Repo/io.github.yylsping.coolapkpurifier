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
     * when a selected reply holder is either installed or simply absent from
     * the persisted cache. Blocking is reserved for the case a previous
     * process PROVED the holder reachable (persisted target) but this boot
     * has not installed it yet — only then can a discovery session actually
     * change the outcome. On Coolapk builds whose dex layout never exposes
     * the holder through the runtime loader (16.6.1 drift), this keeps the
     * cache-hit fast path fast instead of paying a DexKit rescan every boot.
     */
    static boolean blocksCacheFastPath(boolean replySelected, boolean replyInstalled,
                                       boolean cachedHasReplyTarget) {
        return replySelected && !replyInstalled && cachedHasReplyTarget;
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
