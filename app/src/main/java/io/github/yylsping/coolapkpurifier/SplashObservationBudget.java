package io.github.yylsping.coolapkpurifier;

/** One process window, independent of the (earlier) frozen bootstrap trace. */
final class SplashObservationBudget {
    static final long WINDOW_MS = 60_000;
    static final int MAX_EVENTS = 96;
    private final long startedAt;
    private int events;
    private boolean closed;

    SplashObservationBudget(long startedAt) { this.startedAt = startedAt; }

    synchronized boolean take(long now) {
        if (!active(now)) return false;
        events++;
        return true;
    }

    synchronized boolean active(long now) {
        return !closed && now >= startedAt && now - startedAt < WINDOW_MS
                && events < MAX_EVENTS;
    }

    synchronized void close() { closed = true; }
}
