package io.github.yylsping.coolapkpurifier;

/**
 * Immutable terminal-state gate for the bootstrap lifecycle. Once READY or
 * DEGRADED is entered the state is FROZEN: a late resolution session can
 * neither flip READY→DEGRADED (background exception) nor DEGRADED→READY
 * (late success). Re-marking the SAME terminal state is reported as
 * IDEMPOTENT so callers can run their one-shot cleanup safely.
 */
final class TerminalStateGate {
    enum Transition { APPLIED, IDEMPOTENT, REJECTED }

    private BootstrapState state;

    TerminalStateGate(BootstrapState initial) {
        this.state = initial;
    }

    synchronized Transition mark(BootstrapState next) {
        if (state.isTerminal()) {
            return state == next ? Transition.IDEMPOTENT : Transition.REJECTED;
        }
        if (state == next) {
            return Transition.IDEMPOTENT;
        }
        state = next;
        return Transition.APPLIED;
    }

    synchronized BootstrapState state() {
        return state;
    }

    synchronized boolean isTerminal() {
        return state.isTerminal();
    }
}
