package io.github.yylsping.coolapkpurifier;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ReplyDiscoveryBudgetTest {
    private final ReplyDiscoveryBudget budget = new ReplyDiscoveryBudget(0);
    private final FeatureRuntimeHealth health = new FeatureRuntimeHealth();
    private final AtomicInteger unregisters = new AtomicInteger();
    private final AtomicInteger cancels = new AtomicInteger();

    private boolean finish(boolean installed, long now) {
        return budget.finishIfNeeded(installed, now, unregisters::incrementAndGet,
                cancels::incrementAndGet, health);
    }
    @Test public void timedLaneHasFourAttemptsAndStopsObservers() {
        health.configure(true, true, true);
        for (int i = 0; i < 4; i++) assertTrue(budget.tryTimed(i));
        assertFalse(budget.tryTimed(5));
        assertTrue(finish(false, 5));
        assertEquals(1, unregisters.get());
        assertEquals(1, cancels.get());
        assertEquals(FeatureRuntimeHealth.Status.UNAVAILABLE, health.replyStatus());
    }
    @Test public void resumeLaneHasThreeAttemptsWithThrottle() {
        assertTrue(budget.tryResume(0));
        assertFalse(budget.tryResume(1));
        assertTrue(budget.tryResume(30_000));
        assertTrue(budget.tryResume(60_000));
        assertFalse(budget.tryResume(90_000));
        assertTrue(finish(false, 90_000));
        assertFalse(budget.tryTimed(90_001));
    }
    @Test public void wallClockDeadlineStopsWithoutAnyUiEvent() {
        health.configure(true, true, true);
        assertFalse(finish(false, 119_999));
        assertTrue(finish(false, 120_000));
        assertEquals(1, unregisters.get());
        assertEquals(FeatureRuntimeHealth.Status.UNAVAILABLE, health.replyStatus());
    }
    @Test public void installedStopsAndLateQueueCannotRearm() {
        health.configure(true, true, true);
        assertTrue(finish(true, 1));
        assertTrue(finish(false, 120_000));
        assertEquals(1, unregisters.get());
        assertEquals(1, cancels.get());
        assertFalse(budget.tryResume(120_001));
        assertFalse(budget.tryTimed(120_001));
        assertEquals(FeatureRuntimeHealth.Status.INSTALLED, health.replyStatus());
    }
}
