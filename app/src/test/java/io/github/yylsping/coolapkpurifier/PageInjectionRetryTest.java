package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.Test;

public final class PageInjectionRetryTest {
    @Test public void lateViewTreeIsRetriedThenStopsOnSuccess() {
        Fixture f = new Fixture(3);
        f.retry.start("settings");
        f.drain();
        assertEquals(3, f.calls);
        assertEquals(0, f.exhausted);
        assertEquals(java.util.Arrays.asList(0L, 100L, 250L), f.delays);
    }

    @Test public void missingViewTreeHasFiniteBudgetAndReleasesQueue() {
        Fixture f = new Fixture(Integer.MAX_VALUE);
        f.retry.start("settings");
        f.drain();
        assertEquals(5, f.calls);
        assertEquals(1, f.exhausted);
        assertEquals(1850L, f.delays.stream().mapToLong(Long::longValue).sum());
        assertTrue(f.queue.isEmpty());
    }

    @Test public void pauseOrDestroyCancelsQueuedAndAlreadyDequeuedWork() {
        Fixture f = new Fixture(Integer.MAX_VALUE);
        f.retry.start("settings");
        Runnable stale = f.queue.remove();
        f.retry.cancel("settings");
        stale.run();
        f.drain();
        assertEquals(0, f.calls);
        assertEquals(0, f.exhausted);
    }

    @Test public void repeatedResumeReplacesPendingAttemptRatherThanDuplicatingIt() {
        Fixture f = new Fixture(1);
        f.retry.start("settings");
        Runnable stale = f.queue.remove();
        f.retry.start("settings");
        stale.run();
        f.drain();
        assertEquals(1, f.calls);
    }

    @Test public void aLaterResumeCanRecoverAfterPreviousBudgetExhaustion() {
        Fixture f = new Fixture(6);
        f.retry.start("settings");
        f.drain();
        assertEquals(1, f.exhausted);
        f.retry.start("settings");
        f.drain();
        assertEquals(6, f.calls);
        assertEquals(1, f.exhausted);
    }

    @Test public void cancellingOnePageDoesNotCancelAnother() {
        Fixture f = new Fixture(1);
        f.retry.start("first");
        f.retry.start("second");
        f.retry.cancel("first");
        f.drain();
        assertEquals(1, f.calls);
    }

    private static final class Fixture {
        final Queue<Runnable> queue = new ArrayDeque<>();
        final List<Long> delays = new ArrayList<>();
        int calls;
        int exhausted;
        final PageInjectionRetry<String> retry;

        Fixture(int succeedsAt) {
            retry = new PageInjectionRetry<>((r, delay) -> {
                queue.add(r);
                delays.add(delay);
            }, queue::remove, page -> ++calls >= succeedsAt, page -> exhausted++);
        }

        void drain() {
            int guard = 0;
            while (!queue.isEmpty()) {
                assertTrue("unbounded retry", ++guard < 20);
                queue.remove().run();
            }
        }
    }
}
