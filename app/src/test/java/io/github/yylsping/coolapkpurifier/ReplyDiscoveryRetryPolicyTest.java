package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReplyDiscoveryRetryPolicyTest {

    @Test
    public void retriesWhileSelectedMissingAndBounded() {
        assertTrue(ReplyDiscoveryRetryPolicy.shouldRetry(true, false, 0));
        assertTrue(ReplyDiscoveryRetryPolicy.shouldRetry(true, false,
                ReplyDiscoveryRetryPolicy.RETRY_DELAYS_MILLIS.length - 1));
        assertFalse(ReplyDiscoveryRetryPolicy.shouldRetry(true, false,
                ReplyDiscoveryRetryPolicy.RETRY_DELAYS_MILLIS.length));
    }

    @Test
    public void installedOrUnselectedStopsRetrying() {
        assertFalse(ReplyDiscoveryRetryPolicy.shouldRetry(true, true, 0));
        assertFalse(ReplyDiscoveryRetryPolicy.shouldRetry(false, false, 0));
    }

    @Test
    public void delaysAreBoundedAndMonotonic() {
        long[] delays = ReplyDiscoveryRetryPolicy.RETRY_DELAYS_MILLIS;
        assertEquals(4, delays.length);
        for (int i = 1; i < delays.length; i++) {
            assertTrue(delays[i] > delays[i - 1]);
        }
        assertEquals(delays[0], ReplyDiscoveryRetryPolicy.delayFor(0));
        assertEquals(delays[delays.length - 1],
                ReplyDiscoveryRetryPolicy.delayFor(delays.length + 5));
    }
}
