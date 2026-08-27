package io.github.yylsping.coolapkpurifier;

/** Shared timed/resume budget; stopping revokes queued and future attempts once. */
final class ReplyDiscoveryBudget {
    static final int MAX_RESUME_ATTEMPTS = 3;
    static final long MAX_ELAPSED_MILLIS = 120_000L;
    private final long startedAt;
    private int timedAttempts;
    private int resumeAttempts;
    private long lastResumeAt = Long.MIN_VALUE;
    private boolean stopped;

    ReplyDiscoveryBudget(long startedAt) { this.startedAt = startedAt; }

    synchronized boolean exhausted(long now) {
        return timedAttempts >= ReplyDiscoveryRetryPolicy.RETRY_DELAYS_MILLIS.length
                || resumeAttempts >= MAX_RESUME_ATTEMPTS
                || now - startedAt >= MAX_ELAPSED_MILLIS;
    }

    synchronized boolean tryTimed(long now) {
        if (stopped || exhausted(now)) return false;
        timedAttempts++;
        return true;
    }

    synchronized boolean tryResume(long now) {
        if (stopped || exhausted(now)) return false;
        if (lastResumeAt != Long.MIN_VALUE && now - lastResumeAt
                < ReplyDiscoveryRetryPolicy.RESUME_ATTEMPT_MIN_INTERVAL_MILLIS) return false;
        lastResumeAt = now;
        resumeAttempts++;
        return true;
    }

    synchronized int timedAttempts() { return timedAttempts; }
    synchronized boolean isStopped() { return stopped; }

    boolean finishIfNeeded(boolean installed, long now, Runnable unregister,
                           Runnable cancel, FeatureRuntimeHealth health) {
        synchronized (this) {
            if (stopped) return true;
            if (!installed && !exhausted(now)) return false;
            stopped = true;
        }
        try {
            unregister.run();
        } finally {
            cancel.run();
            if (installed) health.replyInstalled();
            else health.replyUnavailable("retryBudgetExhausted");
        }
        return true;
    }
}
