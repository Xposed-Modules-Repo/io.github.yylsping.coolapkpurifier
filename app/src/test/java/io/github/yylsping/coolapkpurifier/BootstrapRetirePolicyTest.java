package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BootstrapRetirePolicyTest {
    @Test
    public void readyRetiresOnlyAfterMainActivityAndSpecificSplash() {
        assertFalse(BootstrapRetirePolicy.canRetire(BootstrapState.READY, false, true));
        assertFalse(BootstrapRetirePolicy.canRetire(BootstrapState.READY, true, false));
        assertTrue(BootstrapRetirePolicy.canRetire(BootstrapState.READY, true, true));
    }

    @Test
    public void degradedKeepsFallbackUntilStartupWindowEnds() {
        assertFalse(BootstrapRetirePolicy.canRetire(BootstrapState.DEGRADED, false, false));
        assertTrue(BootstrapRetirePolicy.canRetire(BootstrapState.DEGRADED, true, false));
    }

    @Test
    public void nonTerminalStateNeverRetires() {
        assertFalse(BootstrapRetirePolicy.canRetire(BootstrapState.WAIT_RUNTIME_DEX, true, true));
        assertFalse(BootstrapRetirePolicy.canRetire(BootstrapState.FULL_RESOLVE, true, true));
    }
}
