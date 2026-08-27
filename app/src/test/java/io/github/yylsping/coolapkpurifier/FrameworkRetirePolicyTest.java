package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FrameworkRetirePolicyTest {

    @Test
    public void cleanReadyWithSpecificSplashRetiresSafetyGate() {
        assertTrue(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.READY, true));
    }

    @Test
    public void readyWithoutSpecificSplashRetainsSafetyGate() {
        assertFalse(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.READY, false));
        assertEquals("splashSpecificMissing",
                FrameworkRetirePolicy.retainReason(BootstrapState.READY, false));
    }

    @Test
    public void degradedRetainsSafetyGateEvenWithSpecificSplash() {
        assertFalse(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.DEGRADED, true));
        assertEquals("terminal:DEGRADED",
                FrameworkRetirePolicy.retainReason(BootstrapState.DEGRADED, true));
        assertEquals("terminal:DEGRADED",
                FrameworkRetirePolicy.retainReason(BootstrapState.DEGRADED, false));
    }

    @Test
    public void nonTerminalStatesNeverRetire() {
        for (BootstrapState state : BootstrapState.values()) {
            if (state.isTerminal()) {
                continue;
            }
            assertFalse(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                    state, true));
        }
    }
}
