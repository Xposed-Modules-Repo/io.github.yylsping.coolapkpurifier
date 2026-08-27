package io.github.yylsping.coolapkpurifier;

/**
 * Pure policy for the Mode A-ZF post-READY reply discovery retry. The reply
 * holder lives in a dex chunk the protected shell may append only after READY;
 * since the temporary loadClass hooks are gone by then, a bounded sequence of
 * plain Class.forName retries closes the gap without any framework hook.
 */
final class ReplyDiscoveryRetryPolicy {
    private ReplyDiscoveryRetryPolicy() {
    }

    /** Bounded retry schedule; quitting after the last attempt. */
    static final long[] RETRY_DELAYS_MILLIS = {2_000L, 8_000L, 20_000L, 60_000L};

    /** Minimum spacing between UI-resume-driven attempts (log/noise bound). */
    static final long RESUME_ATTEMPT_MIN_INTERVAL_MILLIS = 30_000L;

    static boolean shouldRetry(boolean replySelected, boolean replyInstalled,
                               int attemptIndex) {
        return replySelected && !replyInstalled
                && attemptIndex >= 0 && attemptIndex < RETRY_DELAYS_MILLIS.length;
    }

    static long delayFor(int attemptIndex) {
        return RETRY_DELAYS_MILLIS[Math.max(0, Math.min(attemptIndex,
                RETRY_DELAYS_MILLIS.length - 1))];
    }
}
