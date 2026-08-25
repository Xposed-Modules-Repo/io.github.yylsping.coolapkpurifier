package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public final class ResolutionEpochTest {
    @Test
    public void loaderSwitchMidResolutionDiscardsEveryG1CommitAndClosesOnOwnerExit()
            throws Exception {
        ClassLoader fallback = loader();
        ClassLoader l1 = loader();
        ClassLoader l2 = loader();
        ResolutionEpoch epoch = new ResolutionEpoch(fallback);
        assertTrue(epoch.transition(l1).changed);

        AtomicInteger bridgeCloses = new AtomicInteger();
        AtomicInteger applies = new AtomicInteger();
        AtomicInteger cacheWrites = new AtomicInteger();
        AtomicInteger readyCommits = new AtomicInteger();
        AtomicInteger degradedCommits = new AtomicInteger();
        ResolutionSessionContext g1 = epoch.captureForTest(1L,
                bridgeCloses::incrementAndGet);
        CountDownLatch resolverMidpoint = new CountDownLatch(1);
        CountDownLatch releaseResolver = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            resolverMidpoint.countDown();
            try {
                releaseResolver.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            assertFalse(epoch.commit(g1, applies::incrementAndGet));
            assertFalse(epoch.commit(g1, cacheWrites::incrementAndGet));
            assertFalse(epoch.commit(g1, readyCommits::incrementAndGet));
            assertFalse(epoch.commit(g1, degradedCommits::incrementAndGet));
            g1.close();
            epoch.finish(g1);
        }, "g1-resolver");
        worker.start();
        assertTrue(resolverMidpoint.await(5, TimeUnit.SECONDS));

        ResolutionEpoch.Transition transition = epoch.transition(l2);
        assertTrue(transition.changed);
        assertEquals(2L, transition.generation);
        assertTrue(g1.isSuperseded());
        // Loader transition only marks stale; it never closes a bridge in use.
        assertEquals(0, bridgeCloses.get());
        releaseResolver.countDown();
        worker.join(5_000L);

        assertEquals(0, applies.get());
        assertEquals(0, cacheWrites.get());
        assertEquals(0, readyCommits.get());
        assertEquals(0, degradedCommits.get());
        assertEquals(1, bridgeCloses.get());
    }

    @Test
    public void g2FollowUpCanCommitReadyAfterG1WasSuperseded() {
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        ClassLoader l1 = loader();
        ClassLoader l2 = loader();
        epoch.transition(l1);
        ResolutionSessionContext g1 = epoch.capture(1L);
        epoch.transition(l2);
        assertFalse(epoch.commit(g1, () -> {
            throw new AssertionError("stale G1 committed");
        }));

        ResolutionSessionContext g2 = epoch.capture(2L);
        AtomicInteger ready = new AtomicInteger();
        assertTrue(epoch.commit(g2, ready::incrementAndGet));
        assertEquals(1, ready.get());
        assertEquals(2L, g2.generation);
        assertTrue(g2.loader == l2);
    }

    private static ClassLoader loader() {
        return new ClassLoader(null) {
        };
    }
}
