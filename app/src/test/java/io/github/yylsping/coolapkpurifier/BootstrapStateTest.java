package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BootstrapStateTest {
    @Test
    public void criticalSplashPrecedesFullResolve() {
        assertTrue(BootstrapState.SPLASH_CRITICAL.isAtLeast(BootstrapState.CACHE_VERIFY));
        assertTrue(BootstrapState.SPLASH_READY.isAtLeast(BootstrapState.SPLASH_CRITICAL));
        assertTrue(BootstrapState.FULL_RESOLVE.isAtLeast(BootstrapState.SPLASH_READY));
        assertTrue(BootstrapState.READY.isAtLeast(BootstrapState.FULL_RESOLVE));
    }

    @Test
    public void terminalStatesAreExplicit() {
        assertFalse(BootstrapState.WAIT_RUNTIME_DEX.isTerminal());
        assertFalse(BootstrapState.SPLASH_CRITICAL.isTerminal());
        assertTrue(BootstrapState.READY.isTerminal());
        assertTrue(BootstrapState.DEGRADED.isTerminal());
    }
}
