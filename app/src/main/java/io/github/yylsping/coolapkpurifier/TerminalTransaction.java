package io.github.yylsping.coolapkpurifier;

/**
 * Linearizes terminal readiness and state commit with runtime-loader changes.
 * The readiness probe is always invoked while {@link ResolutionEpoch}'s
 * monitor is held; callers perform cleanup and logging after this method
 * returns.
 */
final class TerminalTransaction {
    enum Intent {
        REQUIRE_READY,
        DEADLINE,
        FORCE_DEGRADED
    }

    interface ReadinessProbe {
        TerminalSnapshot.Readiness read(long generation, ClassLoader loader);
    }

    interface StateAccess {
        boolean isTerminal();

        TerminalStateGate.Transition mark(BootstrapState next);
    }

    static final class Result {
        final boolean sessionCurrent;
        final TerminalSnapshot snapshot;

        Result(boolean sessionCurrent, TerminalSnapshot snapshot) {
            this.sessionCurrent = sessionCurrent;
            this.snapshot = snapshot;
        }
    }

    private final ResolutionEpoch epoch;

    TerminalTransaction(ResolutionEpoch epoch) {
        this.epoch = epoch;
    }

    Result commitDeadline(String source, ReadinessProbe readiness,
                          StateAccess stateAccess) {
        return commit(null, Intent.DEADLINE, source, readiness, stateAccess);
    }

    Result commitSession(ResolutionSessionContext sessionContext, Intent intent,
                         String source, ReadinessProbe readiness,
                         StateAccess stateAccess) {
        if (sessionContext == null) {
            throw new IllegalArgumentException("sessionContext == null");
        }
        if (intent == Intent.DEADLINE) {
            throw new IllegalArgumentException("deadline is not session-scoped");
        }
        return commit(sessionContext, intent, source, readiness, stateAccess);
    }

    private Result commit(ResolutionSessionContext sessionContext, Intent intent,
                          String source, ReadinessProbe readiness,
                          StateAccess stateAccess) {
        final TerminalSnapshot[] committed = {null};
        Runnable transaction = () -> {
            if (stateAccess.isTerminal()) {
                return;
            }
            long generation = epoch.generation();
            ClassLoader loader = epoch.loader();
            TerminalSnapshot.Readiness current = readiness.read(generation, loader);
            BootstrapState terminal;
            switch (intent) {
                case REQUIRE_READY:
                    if (!current.coreReady) {
                        return;
                    }
                    terminal = BootstrapState.READY;
                    break;
                case FORCE_DEGRADED:
                    terminal = BootstrapState.DEGRADED;
                    break;
                case DEADLINE:
                default:
                    terminal = ReadinessPolicy.deadlineTerminalState(current.coreReady);
                    break;
            }
            TerminalStateGate.Transition transition = stateAccess.mark(terminal);
            if (transition == TerminalStateGate.Transition.APPLIED) {
                committed[0] = new TerminalSnapshot(
                        generation, loader, terminal, current, source);
            }
        };

        boolean sessionCurrent;
        if (sessionContext == null) {
            epoch.exclusive(transaction);
            sessionCurrent = true;
        } else {
            sessionCurrent = epoch.commit(sessionContext, transaction);
        }
        return new Result(sessionCurrent, committed[0]);
    }
}
