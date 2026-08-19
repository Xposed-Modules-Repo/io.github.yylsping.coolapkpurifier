package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Terminal freeze: READY/DEGRADED must be immutable once entered — a late
 * background exception must not flip READY→DEGRADED and a late session
 * success must not flip DEGRADED→READY.
 */
public final class TerminalStateGateTest {
    @Test
    public void nonTerminalTransitionsApplyNormally() {
        TerminalStateGate gate = new TerminalStateGate(BootstrapState.BOOTSTRAP);

        assertEquals(TerminalStateGate.Transition.APPLIED,
                gate.mark(BootstrapState.WAIT_RUNTIME_DEX));
        assertEquals(TerminalStateGate.Transition.APPLIED,
                gate.mark(BootstrapState.FULL_RESOLVE));
        assertEquals(BootstrapState.FULL_RESOLVE, gate.state());
        assertFalse(gate.isTerminal());
    }

    @Test
    public void sameStateMarksAreIdempotent() {
        TerminalStateGate gate = new TerminalStateGate(BootstrapState.BOOTSTRAP);

        assertEquals(TerminalStateGate.Transition.APPLIED,
                gate.mark(BootstrapState.WAIT_RUNTIME_DEX));
        assertEquals(TerminalStateGate.Transition.IDEMPOTENT,
                gate.mark(BootstrapState.WAIT_RUNTIME_DEX));
        assertEquals(BootstrapState.WAIT_RUNTIME_DEX, gate.state());

        // Marking the initial state again after moving on is a real change.
        assertEquals(TerminalStateGate.Transition.APPLIED,
                gate.mark(BootstrapState.BOOTSTRAP));
    }

    @Test
    public void readyCannotBeFlippedToDegradedByALateWorker() {
        TerminalStateGate gate = new TerminalStateGate(BootstrapState.FULL_RESOLVE);

        assertEquals(TerminalStateGate.Transition.APPLIED, gate.mark(BootstrapState.READY));
        // Late session throws after READY was decided.
        assertEquals(TerminalStateGate.Transition.REJECTED, gate.mark(BootstrapState.DEGRADED));
        assertEquals(BootstrapState.READY, gate.state());
        assertTrue(gate.isTerminal());
    }

    @Test
    public void degradedCannotBeFlippedToReadyByALateSession() {
        TerminalStateGate gate = new TerminalStateGate(BootstrapState.WAIT_RUNTIME_DEX);

        assertEquals(TerminalStateGate.Transition.APPLIED, gate.mark(BootstrapState.DEGRADED));
        // Deadline said DEGRADED; the still-running session finishes READY later.
        assertEquals(TerminalStateGate.Transition.REJECTED, gate.mark(BootstrapState.READY));
        assertEquals(BootstrapState.DEGRADED, gate.state());
    }

    @Test
    public void sameTerminalStateIsIdempotentForOneShotCleanup() {
        TerminalStateGate gate = new TerminalStateGate(BootstrapState.READY);

        assertEquals(TerminalStateGate.Transition.IDEMPOTENT, gate.mark(BootstrapState.READY));
        assertEquals(BootstrapState.READY, gate.state());

        TerminalStateGate degraded = new TerminalStateGate(BootstrapState.DEGRADED);
        assertEquals(TerminalStateGate.Transition.IDEMPOTENT,
                degraded.mark(BootstrapState.DEGRADED));
    }

    @Test
    public void terminalBlocksAllIntermediateStatesToo() {
        TerminalStateGate gate = new TerminalStateGate(BootstrapState.DEGRADED);

        assertEquals(TerminalStateGate.Transition.REJECTED,
                gate.mark(BootstrapState.FULL_RESOLVE));
        assertEquals(TerminalStateGate.Transition.REJECTED,
                gate.mark(BootstrapState.WAIT_RUNTIME_DEX));
        assertEquals(BootstrapState.DEGRADED, gate.state());
    }
}
