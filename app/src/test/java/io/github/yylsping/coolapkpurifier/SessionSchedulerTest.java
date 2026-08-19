package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Trigger coalescing: a runtime-dex or watchdog trigger arriving while a
 * session is running used to be dropped completely; with the observer
 * single-shot closed that produced a "new DEX appeared but no next scan"
 * dead zone. It must now be folded into exactly one follow-up session.
 */
public final class SessionSchedulerTest {
    private static final class Launched {
        final String trigger;
        final boolean followUp;

        Launched(String trigger, boolean followUp) {
            this.trigger = trigger;
            this.followUp = followUp;
        }
    }

    private SessionScheduler scheduler;
    private List<Launched> launched;

    @Before
    public void setUp() {
        scheduler = new SessionScheduler();
        launched = new ArrayList<>();
    }

    private final SessionScheduler.SessionStarter recorder = (trigger, followUp) ->
            launched.add(new Launched(trigger, followUp));

    @Test
    public void submitsWhileRunningAreCoalescedIntoExactlyOneFollowUp() {
        assertEquals(SessionScheduler.SubmitResult.STARTED,
                scheduler.submit("sessionA", false, recorder));
        assertTrue(scheduler.isRunning());
        assertFalse(scheduler.hasPending());

        // runtime-dex trigger and two watchdog triggers arrive while A runs.
        assertEquals(SessionScheduler.SubmitResult.COALESCED,
                scheduler.submit("runtimeDex:EntityListFragment", false, recorder));
        assertEquals(SessionScheduler.SubmitResult.COALESCED,
                scheduler.submit("watchdog 8s", false, recorder));
        assertEquals(SessionScheduler.SubmitResult.COALESCED,
                scheduler.submit("activityPre:MainActivity", false, recorder));
        assertTrue(scheduler.hasPending());

        scheduler.onFinished(false, recorder);

        // Exactly one follow-up launched, marked as such, running again.
        assertEquals(2, launched.size());
        assertTrue(launched.get(1).followUp);
        assertTrue(scheduler.isRunning());
        assertFalse(scheduler.hasPending());
    }

    @Test
    public void finishingWithoutPendingTriggersLaunchesNothing() {
        scheduler.submit("sessionA", false, recorder);
        scheduler.onFinished(false, recorder);

        assertEquals(1, launched.size());
        assertFalse(scheduler.isRunning());
    }

    @Test
    public void pendingIsClearedAfterTheFollowUpLaunches() {
        scheduler.submit("sessionA", false, recorder);
        scheduler.submit("runtimeDex:X", false, recorder);
        scheduler.onFinished(false, recorder);

        // No new trigger during the follow-up: nothing further launches.
        scheduler.onFinished(false, recorder);

        assertEquals(2, launched.size());
        assertFalse(scheduler.isRunning());
        assertFalse(scheduler.hasPending());
    }

    @Test
    public void coalescingDoesNotCascadeIntoABusyLoop() {
        // Only external triggers create follow-ups; finishing repeatedly
        // without new submits never launches anything new.
        scheduler.submit("sessionA", false, recorder);
        for (int i = 0; i < 20; i++) {
            scheduler.onFinished(false, recorder);
        }
        assertEquals(1, launched.size());
    }

    @Test
    public void terminalSubmitRejectsInsteadOfCoalescing() {
        scheduler.submit("sessionA", false, recorder);
        assertEquals(SessionScheduler.SubmitResult.REJECTED_TERMINAL,
                scheduler.submit("runtimeDex:X", true, recorder));
        assertFalse(scheduler.hasPending());
    }

    @Test
    public void terminalFinishDropsPendingAndLaunchesNothing() {
        scheduler.submit("sessionA", false, recorder);
        scheduler.submit("watchdog 8s", false, recorder);
        assertTrue(scheduler.hasPending());

        scheduler.onFinished(true, recorder);

        assertEquals(1, launched.size());
        assertFalse(scheduler.isRunning());
        assertFalse(scheduler.hasPending());
    }

    @Test
    public void cancelPendingDropsCoalescedTriggers() {
        scheduler.submit("sessionA", false, recorder);
        scheduler.submit("runtimeDex:X", false, recorder);
        assertTrue(scheduler.hasPending());

        // Deadline path: pending cancelled while the session still runs; the
        // later finish must not launch a follow-up.
        scheduler.cancelPending();

        assertFalse(scheduler.hasPending());
        scheduler.onFinished(false, recorder);
        assertEquals(1, launched.size());
        assertFalse(scheduler.isRunning());
    }

    @Test
    public void followUpTriggerLabelCarriesPendingMarker() {
        scheduler.submit("sessionA", false, recorder);
        scheduler.submit("runtimeDex:EntityListFragment", false, recorder);
        scheduler.submit("watchdog 8s", false, recorder);
        scheduler.onFinished(false, recorder);

        String label = launched.get(1).trigger;
        assertTrue(label, label.contains("runtimeDex:EntityListFragment"));
        assertTrue(label, label.contains("+pending"));
    }
}
