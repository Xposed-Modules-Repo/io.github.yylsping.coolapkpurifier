package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Cross-object lifecycle timing: the 20s deadline firing while a session is
 * still running, with runtime-dex/watchdog triggers coalesced meanwhile.
 * The terminal decision must stand, pending must be dropped, and the late
 * session must not flip or duplicate anything.
 */
public final class ResolutionLifecycleTest {
    private static final class Launched {
        final String trigger;
        final boolean followUp;

        Launched(String trigger, boolean followUp) {
            this.trigger = trigger;
            this.followUp = followUp;
        }
    }

    private static final class Lifecycle {
        final TerminalStateGate gate = new TerminalStateGate(BootstrapState.BOOTSTRAP);
        final SessionScheduler scheduler = new SessionScheduler();
        final OnceFlag terminalCleaned = new OnceFlag();
        final List<Launched> launched = new ArrayList<>();
        int cleanups;

        final SessionScheduler.SessionStarter starter = (trigger, followUp) ->
                launched.add(new Launched(trigger, followUp));

        void submit(String trigger) {
            scheduler.submit(trigger, gate.isTerminal(), starter);
        }

        void finishRunningSession() {
            scheduler.onFinished(gate.isTerminal(), starter);
        }

        /** Mirrors finishReady(): only the APPLIED transition cleans up. */
        void sessionFinishesReady() {
            if (gate.mark(BootstrapState.READY) == TerminalStateGate.Transition.APPLIED) {
                cleanupTerminal();
            }
        }

        /** Mirrors the session catch block: DEGRADED via the gate. */
        void sessionThrows() {
            gate.mark(BootstrapState.DEGRADED);
            cleanupTerminal();
        }

        /** Mirrors the deadline handler: terminal decision + pending cancel. */
        void deadline(boolean coreReady) {
            BootstrapState terminal = ReadinessPolicy.deadlineTerminalState(coreReady);
            if (terminal == BootstrapState.READY) {
                if (gate.mark(BootstrapState.READY)
                        == TerminalStateGate.Transition.APPLIED) {
                    cleanupTerminal();
                }
            } else {
                gate.mark(BootstrapState.DEGRADED);
                cleanupTerminal();
            }
            scheduler.cancelPending();
        }

        private void cleanupTerminal() {
            if (terminalCleaned.tryOnce()) {
                cleanups++;
            }
        }
    }

    @Test
    public void deadlineDegradedDuringRunningSessionFreezesEverything() {
        Lifecycle lifecycle = new Lifecycle();
        // markState progression to FULL_RESOLVE, then a session starts.
        lifecycle.gate.mark(BootstrapState.FULL_RESOLVE);
        lifecycle.submit("runtimeDex:loadClass");
        assertEquals(1, lifecycle.launched.size());

        // runtime-dex + watchdog triggers arrive while the session runs.
        lifecycle.submit("runtimeDex:EntityListFragment");
        lifecycle.submit("watchdog 8s");
        assertTrue(lifecycle.scheduler.hasPending());

        // 20s deadline: core not ready → DEGRADED, pending cancelled.
        lifecycle.deadline(false);
        assertEquals(BootstrapState.DEGRADED, lifecycle.gate.state());
        assertFalse(lifecycle.scheduler.hasPending());

        // The running session finishes READY (late success) — rejected.
        lifecycle.sessionFinishesReady();
        assertEquals(BootstrapState.DEGRADED, lifecycle.gate.state());
        // Terminal finish drops any pending that sneaked in and launches nothing.
        lifecycle.submit("runtimeDex:late");
        lifecycle.finishRunningSession();
        assertEquals(1, lifecycle.launched.size());
        assertEquals(1, lifecycle.cleanups);
    }

    @Test
    public void deadlineReadyDuringRunningSessionSurvivesLateException() {
        Lifecycle lifecycle = new Lifecycle();
        lifecycle.gate.mark(BootstrapState.FULL_RESOLVE);
        lifecycle.submit("runtimeDex:loadClass");
        lifecycle.submit("watchdog 8s");

        // 20s deadline: core ready → READY (settled by deadline).
        lifecycle.deadline(true);
        assertEquals(BootstrapState.READY, lifecycle.gate.state());

        // The running session throws afterwards — READY stays.
        lifecycle.sessionThrows();
        assertEquals(BootstrapState.READY, lifecycle.gate.state());
        lifecycle.finishRunningSession();
        assertEquals(1, lifecycle.launched.size());
        assertEquals(1, lifecycle.cleanups);
    }

    @Test
    public void pendingFollowUpStillWorksWhenNoDeadlineInterferes() {
        Lifecycle lifecycle = new Lifecycle();
        lifecycle.submit("sessionA");
        lifecycle.submit("runtimeDex:EntityListFragment");
        lifecycle.finishRunningSession();

        assertEquals(2, lifecycle.launched.size());
        assertTrue(lifecycle.launched.get(1).followUp);
        assertTrue(lifecycle.launched.get(1).trigger.contains("+pending"));
        assertEquals(0, lifecycle.cleanups);

        // The follow-up finishes READY normally.
        lifecycle.sessionFinishesReady();
        lifecycle.finishRunningSession();
        assertEquals(BootstrapState.READY, lifecycle.gate.state());
        assertEquals(1, lifecycle.cleanups);
    }
}
