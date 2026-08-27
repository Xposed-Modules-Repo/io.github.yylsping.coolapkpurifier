package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LazyDiscoveryPolicyTest {

    @Test
    public void selectedReplyHolderBlocksCacheFastPathUntilInstalled() {
        assertTrue(LazyDiscoveryPolicy.blocksCacheFastPath(true, false));
    }

    @Test
    public void installedReplyHolderAllowsCacheFastPath() {
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(true, true));
    }

    @Test
    public void unselectedReplyHolderNeverBlocksCacheFastPath() {
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(false, false));
        assertFalse(LazyDiscoveryPolicy.blocksCacheFastPath(false, true));
    }

    @Test
    public void lazyDiscoveryRequiresPlanMissingAndNotDisabled() {
        assertTrue(LazyDiscoveryPolicy.shouldInstallLazyDiscovery(true, true, false));
        assertFalse(LazyDiscoveryPolicy.shouldInstallLazyDiscovery(false, true, false));
        assertFalse(LazyDiscoveryPolicy.shouldInstallLazyDiscovery(true, false, false));
        assertFalse(LazyDiscoveryPolicy.shouldInstallLazyDiscovery(true, true, true));
    }
}
