package io.github.yylsping.coolapkpurifier;

import org.junit.Test;
import static org.junit.Assert.*;

public class FeatureRuntimeHealthTest {
    private final FeatureRuntimeHealth health = new FeatureRuntimeHealth();

    @Test public void selectedReplyIsDeferredUntilInstalled() {
        health.configure(true, true, true);
        assertEquals(FeatureRuntimeHealth.Status.DEFERRED, health.replyStatus());
        assertTrue(health.replyMessage().contains("尚未生效"));
    }
    @Test public void installedReplyCannotBeDowngradedByLateFailure() {
        health.configure(true, true, true);
        health.replyInstalled();
        health.replyUnavailable("lateFailure");
        assertEquals(FeatureRuntimeHealth.Status.INSTALLED, health.replyStatus());
        assertTrue(health.problems().isEmpty());
    }
    @Test public void disabledReplyNeverBecomesUnavailable() {
        health.configure(true, true, false);
        health.replyUnavailable("retryBudgetExhausted");
        assertEquals(FeatureRuntimeHealth.Status.DISABLED, health.replyStatus());
        assertTrue(health.problems().isEmpty());
    }
    @Test public void unavailableReplyIsVisibleSeparatelyFromInstalledCore() {
        health.configure(true, true, true);
        health.updateCore(true, true);
        health.replyUnavailable("retryBudgetExhausted");
        assertTrue(health.summary().contains("splash:INSTALLED, feedSponsor:INSTALLED"));
        assertEquals(java.util.Collections.singletonList("replySponsor:retryBudgetExhausted"),
                health.problems());
        assertTrue(health.replyMessage().contains("当前未生效"));
    }
    @Test public void listenersReceiveChangesAndCanDetach() {
        health.configure(true, true, true);
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        Runnable listener = calls::incrementAndGet;
        health.addListener(listener);
        health.replyUnavailable("retryBudgetExhausted");
        assertEquals(1, calls.get());
        health.removeListener(listener);
        health.replyInstalled();
        assertEquals(1, calls.get());
    }
    @Test public void unavailableReplyDoesNotChangeCoordinatorReady() throws Exception {
        HookCoordinator coordinator = new HookCoordinator(null, new ModuleLog(null),
                getClass().getClassLoader());
        java.lang.reflect.Field state = HookCoordinator.class.getDeclaredField("state");
        state.setAccessible(true);
        state.set(coordinator, BootstrapState.READY);
        java.lang.reflect.Field field = HookCoordinator.class.getDeclaredField("runtimeHealth");
        field.setAccessible(true);
        FeatureRuntimeHealth actual = (FeatureRuntimeHealth) field.get(coordinator);
        actual.configure(true, true, true);
        actual.replyUnavailable("retryBudgetExhausted");
        assertEquals(BootstrapState.READY, state.get(coordinator));
    }
}
