package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public final class TerminalTransactionTest {
    @Test
    public void g1ReadyDeadlineCompletesBeforeConcurrentG2Reset() throws Exception {
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        ClassLoader l1 = loader();
        ClassLoader l2 = loader();
        epoch.transition(l1);
        TerminalTransaction transaction = new TerminalTransaction(epoch);
        StateHarness state = new StateHarness();
        CountDownLatch readinessRead = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        AtomicReference<TerminalTransaction.Result> result = new AtomicReference<>();

        Thread deadline = new Thread(() -> result.set(transaction.commitDeadline(
                "deadline", (generation, activeLoader) -> {
                    readinessRead.countDown();
                    await(allowCommit);
                    return readiness(true, "g1:none");
                }, state)), "deadline-g1-ready");
        deadline.start();
        assertTrue(readinessRead.await(5, TimeUnit.SECONDS));

        Thread loaderSwitch = new Thread(
                () -> transitionUnlessTerminal(epoch, state, l2), "loader-switch-g2");
        loaderSwitch.start();
        allowCommit.countDown();
        deadline.join(5_000L);
        loaderSwitch.join(5_000L);

        TerminalSnapshot snapshot = result.get().snapshot;
        assertEquals(BootstrapState.READY, state.state);
        assertEquals(1L, snapshot.generation);
        assertSame(l1, snapshot.loader);
        // The terminal state is visible when the queued loader transaction
        // obtains the epoch, so it cannot reset to G2 after a G1 commit.
        assertEquals(1L, epoch.generation());
        assertSame(l1, epoch.loader());
    }

    @Test
    public void g1ReadyCannotBeCommittedAfterG2ResetWinsEpoch() {
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        ClassLoader l1 = loader();
        ClassLoader l2 = loader();
        epoch.transition(l1);
        StateHarness state = new StateHarness();
        transitionUnlessTerminal(epoch, state, l2);

        TerminalTransaction.Result result = new TerminalTransaction(epoch).commitDeadline(
                "deadline", (generation, activeLoader) ->
                        readiness(generation == 1L, "g" + generation + ":notReady"), state);

        assertEquals(BootstrapState.DEGRADED, state.state);
        assertEquals(2L, result.snapshot.generation);
        assertSame(l2, result.snapshot.loader);
        assertFalse(result.snapshot.coreReady);
    }

    @Test
    public void g1FalseCannotDegradeReadyG2() {
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        ClassLoader l1 = loader();
        ClassLoader l2 = loader();
        epoch.transition(l1);
        StateHarness state = new StateHarness();
        transitionUnlessTerminal(epoch, state, l2);

        TerminalTransaction.Result result = new TerminalTransaction(epoch).commitDeadline(
                "deadline", (generation, activeLoader) ->
                        readiness(generation == 2L, "g" + generation + ":none"), state);

        assertEquals(BootstrapState.READY, state.state);
        assertEquals(2L, result.snapshot.generation);
        assertSame(l2, result.snapshot.loader);
        assertTrue(result.snapshot.coreReady);
    }

    @Test
    public void terminalSnapshotGenerationLoaderAndMissingComeFromOneProbe() {
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        ClassLoader l1 = loader();
        epoch.transition(l1);
        StateHarness state = new StateHarness();

        TerminalSnapshot snapshot = new TerminalTransaction(epoch).commitDeadline(
                "deadline", (generation, activeLoader) -> readiness(false,
                        "missing@g" + generation + ":l"
                                + System.identityHashCode(activeLoader)), state).snapshot;

        assertEquals(1L, snapshot.generation);
        assertSame(l1, snapshot.loader);
        assertEquals(Collections.singletonList("missing@g1:l"
                        + System.identityHashCode(l1)),
                snapshot.missingRequired);
        assertEquals(BootstrapState.DEGRADED, snapshot.terminalState);
    }

    @Test
    public void staleSessionCannotCommitTerminalIntoNewGeneration() {
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        epoch.transition(loader());
        ResolutionSessionContext g1 = epoch.capture(1L);
        epoch.transition(loader());
        StateHarness state = new StateHarness();

        TerminalTransaction.Result result = new TerminalTransaction(epoch).commitSession(
                g1, TerminalTransaction.Intent.FORCE_DEGRADED, "error",
                (generation, activeLoader) -> readiness(false, "missing"), state);

        assertFalse(result.sessionCurrent);
        assertNull(result.snapshot);
        assertEquals(BootstrapState.BOOTSTRAP, state.state);
    }

    private static TerminalSnapshot.Readiness readiness(boolean ready, String missing) {
        return new TerminalSnapshot.Readiness(ready,
                ready ? Collections.emptyList() : Collections.singletonList(missing));
    }

    private static void transitionUnlessTerminal(
            ResolutionEpoch epoch, StateHarness state, ClassLoader loader) {
        epoch.exclusive(() -> {
            if (!state.isTerminal()) {
                epoch.transition(loader);
            }
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static ClassLoader loader() {
        return new ClassLoader(null) {
        };
    }

    private static final class StateHarness implements TerminalTransaction.StateAccess {
        private final TerminalStateGate gate =
                new TerminalStateGate(BootstrapState.BOOTSTRAP);
        private volatile BootstrapState state = BootstrapState.BOOTSTRAP;

        @Override
        public boolean isTerminal() {
            return state.isTerminal();
        }

        @Override
        public TerminalStateGate.Transition mark(BootstrapState next) {
            TerminalStateGate.Transition transition = gate.mark(next);
            if (transition == TerminalStateGate.Transition.APPLIED) {
                state = next;
            }
            return transition;
        }
    }
}
