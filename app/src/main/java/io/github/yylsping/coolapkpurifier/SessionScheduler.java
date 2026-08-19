package io.github.yylsping.coolapkpurifier;

/**
 * Coalescing gate for resolution sessions: at most one session runs at a
 * time, and every trigger that arrives while a session is running is folded
 * into exactly ONE follow-up session instead of being dropped.
 *
 * <p>This closes the lost-trigger dead zone: a runtime-dex event (observer
 * already single-shot closed) or an 8s watchdog firing while a session is
 * scanning with a stale bridge snapshot must still produce a next session
 * that consumes {@code lastResolutionIncomplete} and bumps the DexKit
 * generation. Follow-ups are event-driven only — a new follow-up is launched
 * exclusively when a NEW trigger arrived during the previous session, so
 * there is no busy loop.
 */
final class SessionScheduler {
    interface SessionStarter {
        void start(String trigger, boolean followUp);
    }

    enum SubmitResult { STARTED, COALESCED, REJECTED_TERMINAL }

    private final Object lock = new Object();
    private boolean running;
    private boolean pending;
    private String pendingTrigger;
    private int coalesced;

    /** Submits a trigger; starts the session immediately when none is running. */
    SubmitResult submit(String trigger, boolean terminal, SessionStarter starter) {
        synchronized (lock) {
            if (terminal) {
                return SubmitResult.REJECTED_TERMINAL;
            }
            if (running) {
                if (!pending) {
                    pending = true;
                    pendingTrigger = trigger;
                }
                coalesced++;
                return SubmitResult.COALESCED;
            }
            running = true;
        }
        starter.start(trigger, false);
        return SubmitResult.STARTED;
    }

    /**
     * Finishes the running session and launches exactly one follow-up when
     * triggers were coalesced meanwhile. A terminal finisher drops pending
     * instead of launching.
     */
    void onFinished(boolean terminal, SessionStarter starter) {
        String trigger;
        int extra;
        synchronized (lock) {
            if (!running) {
                return;
            }
            running = false;
            if (terminal || !pending) {
                pending = false;
                pendingTrigger = null;
                coalesced = 0;
                return;
            }
            trigger = pendingTrigger;
            extra = coalesced - 1;
            pending = false;
            pendingTrigger = null;
            coalesced = 0;
            running = true;
        }
        starter.start(label(trigger, extra), true);
    }

    /** Drops any coalesced trigger; used when the coordinator goes terminal. */
    void cancelPending() {
        synchronized (lock) {
            pending = false;
            pendingTrigger = null;
            coalesced = 0;
        }
    }

    boolean isRunning() {
        synchronized (lock) {
            return running;
        }
    }

    boolean hasPending() {
        synchronized (lock) {
            return pending;
        }
    }

    private static String label(String trigger, int extraTriggers) {
        String base = trigger == null ? "unknown" : trigger;
        return extraTriggers > 0 ? base + "+pending" + extraTriggers : base + "+pending";
    }
}
