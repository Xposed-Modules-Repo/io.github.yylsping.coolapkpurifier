package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import android.app.Application;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import io.github.libxposed.api.XposedInterface.HookHandle;

public class AttachHandoffPolicyTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private static AttachHandoffPolicy.HandoffState state(boolean context,
                                                          boolean config,
                                                          boolean lifecycle) {
        return new AttachHandoffPolicy.HandoffState(context, config, lifecycle);
    }

    @Test
    public void retiresOnlyAfterCompleteHandoff() {
        assertTrue(AttachHandoffPolicy.canRetireAttach(state(true, true, true)));
    }

    @Test
    public void missingContextBlocksRetire() {
        assertFalse(AttachHandoffPolicy.canRetireAttach(state(false, true, true)));
        assertEquals("contextMissing",
                AttachHandoffPolicy.missingCondition(state(false, true, true)));
    }

    @Test
    public void missingConfigBlocksRetire() {
        assertFalse(AttachHandoffPolicy.canRetireAttach(state(true, false, true)));
        assertEquals("configMissing",
                AttachHandoffPolicy.missingCondition(state(true, false, true)));
    }

    @Test
    public void missingSettingsLifecycleBlocksRetire() {
        assertFalse(AttachHandoffPolicy.canRetireAttach(state(true, true, false)));
        assertEquals("settingsLifecycleMissing",
                AttachHandoffPolicy.missingCondition(state(true, true, false)));
    }

    @Test
    public void completeHandoffHasNoMissingCondition() {
        assertNull(AttachHandoffPolicy.missingCondition(state(true, true, true)));
    }

    @Test
    public void terminalCleanupCannotBypassFailedLifecycleHandoff() throws Exception {
        HookCoordinator coordinator = coordinatorWithHandle(false, false);
        AtomicInteger calls = testCalls;
        retire(coordinator, "terminalCleanup:DEGRADED");
        assertEquals(0, calls.get());
        assertTrue(ledger(coordinator).isActive("application-attach"));

        // A failed precondition must not consume the one-shot retirement.
        set(field(coordinator, "settingsHooks"), "lifecycleCallbacksInstalled", true);
        retire(coordinator, "handoffComplete");
        assertEquals(1, calls.get());
        assertFalse(ledger(coordinator).isActive("application-attach"));
    }

    @Test
    public void terminalCleanupRetiresCancelledPostAndLatePostIsIdempotent() throws Exception {
        HookCoordinator coordinator = coordinatorWithHandle(true, false);
        AtomicInteger calls = testCalls;
        // The handoff post was queued but drained before it could execute.
        ((java.util.concurrent.atomic.AtomicBoolean) field(coordinator,
                "attachHookRetired")).set(true);
        retire(coordinator, "terminalCleanup:READY");
        retire(coordinator, "handoffComplete");
        assertEquals(1, calls.get());
        assertFalse(ledger(coordinator).hasActiveFrameworkHooks());
    }

    @Test
    public void failedUnhookRemainsInLedgerAndRetainsHandle() throws Exception {
        HookCoordinator coordinator = coordinatorWithHandle(true, true);
        retire(coordinator, "terminalCleanup:READY");
        assertEquals(1, testCalls.get());
        assertTrue(ledger(coordinator).hasActiveFrameworkHooks());
        assertEquals(1, ((List<?>) field(coordinator, "bootstrapHandles")).size());
    }

    private AtomicInteger testCalls;

    @SuppressWarnings("unchecked")
    private HookCoordinator coordinatorWithHandle(boolean lifecycleReady,
                                                   boolean failUnhook) throws Exception {
        ModuleLog log = new ModuleLog(null);
        HookCoordinator coordinator = new HookCoordinator(null, log, getClass().getClassLoader());
        PurifierConfig config = new PurifierConfig(folder.getRoot(),
                (source, destination) -> source.renameTo(destination), log);
        SettingsHooks settings = new SettingsHooks(null, new HookLedger(), log,
                config, 16);
        set(settings, "lifecycleCallbacksInstalled", lifecycleReady);
        set(coordinator, "appContext", new Application());
        set(coordinator, "config", config);
        set(coordinator, "settingsHooks", settings);
        AtomicInteger calls = new AtomicInteger();
        testCalls = calls;
        HookHandle handle = (HookHandle) Proxy.newProxyInstance(
                HookHandle.class.getClassLoader(), new Class<?>[]{HookHandle.class},
                (proxy, method, args) -> {
                    if ("unhook".equals(method.getName())) {
                        calls.incrementAndGet();
                        if (failUnhook) {
                            throw new IllegalStateException("test unhook failure");
                        }
                    }
                    return null;
                });
        ((List<HookHandle>) field(coordinator, "bootstrapHandles")).add(handle);
        ledger(coordinator).record(HookLedger.Layer.FRAMEWORK,
                "coordinator", "application-attach", "Application.attach(Context)");
        return coordinator;
    }

    private static HookLedger ledger(HookCoordinator coordinator) throws Exception {
        return (HookLedger) field(coordinator, "hookLedger");
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void set(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static void retire(HookCoordinator coordinator, String reason) throws Exception {
        Method method = HookCoordinator.class.getDeclaredMethod(
                "retireApplicationAttachNow", String.class);
        method.setAccessible(true);
        method.invoke(coordinator, reason);
    }
}
