package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FrameworkRetirePolicyTest {

    @Test
    public void cleanReadyWithSpecificSplashRetiresSafetyGate() {
        assertTrue(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.READY, true, true));
    }

    @Test
    public void readyWithoutSpecificSplashRetainsSafetyGate() {
        assertFalse(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.READY, false, true));
        assertEquals("splashSpecificMissing",
                FrameworkRetirePolicy.retainReason(BootstrapState.READY, false, true));
    }

    @Test
    public void degradedRetainsSafetyGateEvenWithSpecificSplash() {
        assertFalse(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.DEGRADED, true, true));
        assertEquals("terminal:DEGRADED",
                FrameworkRetirePolicy.retainReason(BootstrapState.DEGRADED, true, true));
        assertEquals("terminal:DEGRADED",
                FrameworkRetirePolicy.retainReason(BootstrapState.DEGRADED, false, true));
    }

    @Test
    public void nonTerminalStatesNeverRetire() {
        for (BootstrapState state : BootstrapState.values()) {
            if (state.isTerminal()) {
                continue;
            }
            assertFalse(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                    state, true, true));
        }
    }
}
