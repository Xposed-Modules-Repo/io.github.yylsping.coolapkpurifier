package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class SplashDecisionPolicyTest {
    @Test public void trueIsOverriddenOnlyAfterOriginalSideEffectsComplete() throws Throwable {
        List<String> order = new ArrayList<>();
        List<Object> observed = new ArrayList<>();
        Object result = SplashDecisionPolicy.intercept(() -> {
            order.add("original"); return true;
        }, () -> {
            order.add("config"); return true;
        }, (original, returned, enabled) -> {
            order.add("observe");
            observed.addAll(Arrays.asList(original, returned, enabled));
        });
        assertEquals(false, result);
        assertEquals(Arrays.asList("original", "config", "observe"), order);
        assertEquals(Arrays.asList(true, false, true), observed);
    }

    @Test public void offPreservesBothResultsAndCallsOriginalExactlyOnce() throws Throwable {
        for (Boolean original : new Boolean[]{true, false}) {
            AtomicInteger calls = new AtomicInteger();
            assertSame(original, SplashDecisionPolicy.intercept(() -> {
                calls.incrementAndGet(); return original;
            }, () -> false, (a, b, c) -> { }));
            assertEquals(1, calls.get());
        }
    }

    @Test public void originalFalseIsNeverChangedOrReplayed() throws Throwable {
        AtomicInteger calls = new AtomicInteger();
        assertEquals(false, SplashDecisionPolicy.intercept(() -> {
            calls.incrementAndGet(); return false;
        }, () -> true, (a, b, c) -> { }));
        assertEquals(1, calls.get());
    }

    @Test public void hostExceptionPropagatesWithoutConfigReadOrObservation() {
        Throwable hostFailure = new IllegalStateException("host");
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger configAndObservationCalls = new AtomicInteger();
        try {
            SplashDecisionPolicy.intercept(() -> {
                calls.incrementAndGet(); throw hostFailure;
            }, () -> { configAndObservationCalls.incrementAndGet(); return true; },
                    (a, b, c) -> configAndObservationCalls.incrementAndGet());
            fail("host exception lost");
        } catch (Throwable actual) {
            assertSame(hostFailure, actual);
        }
        assertEquals(1, calls.get());
        assertEquals(0, configAndObservationCalls.get());
    }

    @Test public void configFailureFailsOpenAndLoggingFailureCannotReplayHost() throws Throwable {
        AtomicInteger calls = new AtomicInteger();
        assertEquals(true, SplashDecisionPolicy.intercept(() -> {
            calls.incrementAndGet(); return true;
        }, () -> { throw new IllegalStateException("config"); },
                (a, b, c) -> { throw new IllegalStateException("log"); }));
        assertEquals(1, calls.get());
        assertEquals(false, SplashDecisionPolicy.intercept(() -> true, () -> true,
                (a, b, c) -> { throw new IllegalStateException("log"); }));
    }

    @Test public void switchIsReadAfterEachInvocationIncludingChangesDuringOriginal() throws Throwable {
        boolean[] enabled = {false};
        assertEquals(false, SplashDecisionPolicy.intercept(() -> {
            enabled[0] = true; return true;
        }, () -> enabled[0], (a, b, c) -> { }));
        enabled[0] = false;
        assertEquals(true, SplashDecisionPolicy.intercept(() -> true,
                () -> enabled[0], (a, b, c) -> { }));
    }

    @Test public void embeddedHostCannotBeReadyFromActivityHookOrStaleGenerationAlone() {
        FeatureInstallState state = new FeatureInstallState();
        state.beginGeneration(1);
        state.markSplashHook(1, "specific");
        assertFalse(SplashDecisionPolicy.ready(state.hasSplashHook(), true, false));
        state.markPrimaryHook(1, TargetResolver.KEY_SPLASH_DECISION);
        assertTrue(SplashDecisionPolicy.ready(state.hasSplashHook(), true,
                state.hasPrimaryHook(TargetResolver.KEY_SPLASH_DECISION)));
        state.beginGeneration(2);
        state.markSplashHook(2, "specific");
        assertFalse(state.markPrimaryHook(1, TargetResolver.KEY_SPLASH_DECISION));
        assertFalse(SplashDecisionPolicy.ready(state.hasSplashHook(), true,
                state.hasPrimaryHook(TargetResolver.KEY_SPLASH_DECISION)));
        assertTrue(SplashDecisionPolicy.ready(true, false, false));
        assertFalse(SplashDecisionPolicy.ready(false, true, true));
    }
}
