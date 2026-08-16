package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TraceGateTest {
    @Test
    public void freezeIsOneWayAndIdempotent() {
        TraceGate gate = new TraceGate();
        assertFalse(gate.isFrozen());
        gate.freeze();
        gate.freeze();
        assertTrue(gate.isFrozen());
    }
}
