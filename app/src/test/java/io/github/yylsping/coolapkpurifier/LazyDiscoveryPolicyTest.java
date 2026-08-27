package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LazyDiscoveryPolicyTest {

    @Test
    public void modernUpgradeScansMissingTargetButInstalledAndDisabledKeepFastPath() {
        assertTrue(LazyDiscoveryPolicy.blocksCacheFastPath(true, false, false, true));
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(true, true, true, true));
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(false, false, false, true));
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(true, false, false, false));
    }

    @Test
    public void selectedReplyHolderBlocksOnlyWithPersistedTarget() {
        assertTrue(LazyDiscoveryPolicy.blocksCacheFastPath(true, false, true));
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(true, false, false));
    }

    @Test
    public void installedReplyHolderAllowsCacheFastPath() {
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(true, true, true));
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(true, true, false));
    }

    @Test
    public void unselectedReplyHolderNeverBlocksCacheFastPath() {
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(false, false, true));
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(false, true, false));
    }

    @Test
    public void lazyDiscoveryRequiresPlanMissingAndNotDisabled() {
        assertTrue(LazyDiscoveryPolicy.shouldInstallLazyDiscovery(true, true, false));
        assertFalse(LazyDiscoveryPolicy.shouldInstallLazyDiscovery(false, true, false));
        assertFalse(LazyDiscoveryPolicy.shouldInstallLazyDiscovery(true, false, false));
        assertFalse(LazyDiscoveryPolicy.shouldInstallLazyDiscovery(true, true, true));
    }
}
