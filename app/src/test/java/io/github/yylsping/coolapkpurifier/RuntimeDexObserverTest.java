package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.coolapk.market.view.main.MainActivity;
import com.coolapk.market.shell.Stub;

import org.junit.Before;
import org.junit.Test;

/**
 * Regression tests for the observer lifecycle: rearm() must reinstall closed
 * hooks, an installation racing close() must be discarded, and no handle may
 * survive terminal cleanup.
 */
public final class RuntimeDexObserverTest {
    private RecordingInstaller installer;
    private RecordingListener listener;
    private RuntimeDexObserver observer;

    @Before
    public void setUp() {
        installer = new RecordingInstaller();
        listener = new RecordingListener();
        observer = new RuntimeDexObserver(new ModuleLog(null), listener, installer);
        FakeHandle.resetUnhooks();
    }

    @Test
    public void installArmsObserverAndInstallsHooksOnce() {
        observer.install();
        observer.install();

        assertEquals(1, installer.installCount);
        assertEquals(2, observer.publishedHandleCount());
        assertTrue(observer.isArmed());
    }

    @Test
    public void businessClassFiresOnceAndDisarms() {
        observer.install();

        observer.onClassLoaded(MainActivity.class);
        observer.onClassLoaded(MainActivity.class);

        assertEquals(1, listener.triggers.size());
        assertFalse(observer.isArmed());
        assertEquals(0, observer.publishedHandleCount());
    }

    @Test
    public void nonBusinessClassDoesNotConsumeTheArming() {
        observer.install();

        observer.onClassLoaded(String.class);
        observer.onClassLoaded(Stub.class);

        assertEquals(0, listener.triggers.size());
        assertTrue(observer.isArmed());
    }

    @Test
    public void rearmAfterFireReinstallsHooksAndCanFireAgain() {
        observer.install();
        observer.onClassLoaded(MainActivity.class);
        assertEquals(1, installer.installCount);

        observer.rearm();

        // The defect: rearm() used to leave the observer armed but hookless.
        assertEquals(2, installer.installCount);
        assertTrue(observer.isArmed());

        observer.onClassLoaded(MainActivity.class);
        assertEquals(2, listener.triggers.size());
    }

    @Test
    public void rearmWhileHooksInstalledDoesNotDuplicateHooks() {
        observer.install();

        observer.rearm();

        assertEquals(1, installer.installCount);
        assertTrue(observer.isArmed());
    }

    @Test
    public void notifyFirstActivityPreIsSingleShot() {
        observer.install();

        observer.notifyFirstActivityPre(getClass().getClassLoader());
        observer.notifyFirstActivityPre(getClass().getClassLoader());

        assertEquals(1, listener.triggers.size());
        assertEquals("firstActivityPre", listener.triggers.get(0));
    }

    /**
     * Tightened race: two threads rearming while the installer is still
     * running (handles not yet published) used to both observe an empty
     * handle list and install the loadClass hooks twice.
     */
    @Test
    public void concurrentRearmsInstallHooksExactlyOnce() throws Exception {
        installer.slowMode = true;
        observer.install();
        // Close the hooks (single-shot fire) so rearm actually has to
        // reinstall; the race window is between rearm() and handle
        // publication inside the still-running installer.
        observer.onClassLoaded(MainActivity.class);
        assertEquals(1, installer.installCount);

        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(2);
        Runnable rearm = () -> {
            try {
                barrier.await();
                observer.rearm();
            } catch (Exception ignored) {
            }
        };
        Thread first = new Thread(rearm, "rearm-1");
        Thread second = new Thread(rearm, "rearm-2");
        first.start();
        second.start();
        first.join(5_000);
        second.join(5_000);

        assertEquals(2, installer.installCount);
        assertTrue(observer.isArmed());
    }

    /**
     * P2-2 install/close race: close() during an in-flight installation must
     * discard (unhook) the freshly created handles instead of leaking live
     * ClassLoader hooks past terminal state, and must not fire the listener.
     */
    @Test
    public void closeDuringInFlightInstallDiscardsNewHandles() throws Exception {
        CountDownLatch installStarted = new CountDownLatch(1);
        CountDownLatch releaseGate = new CountDownLatch(1);
        installer.blocker = installStarted;
        installer.release = releaseGate;

        Thread installerThread = new Thread(() -> observer.install(), "observer-install");
        installerThread.start();
        assertTrue(installStarted.await(5, TimeUnit.SECONDS));

        // Terminal cleanup closes while module.hook() is still in flight.
        Thread releaser = new Thread(() -> {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            releaseGate.countDown();
        }, "observer-install-releaser");
        releaser.start();
        RuntimeDexObserver.CloseResult closeResult = observer.close();
        installerThread.join(5_000);
        releaser.join(5_000);

        assertEquals(1, installer.installCount);
        assertEquals(0, observer.publishedHandleCount());
        assertEquals(2, FakeHandle.unhookTotal()); // discarded in-flight handles unhooked
        assertEquals(0, listener.triggers.size());
        assertFalse(observer.isArmed());
        assertTrue(closeResult.summaryComplete);

        // A later rearm works under the new epoch.
        installer.blocker = null;
        FakeHandle.resetUnhooks();
        observer.rearm();
        assertEquals(2, installer.installCount);
        assertEquals(2, observer.publishedHandleCount());
        assertEquals(0, FakeHandle.unhookTotal());
    }

    @Test
    public void closeReportsPartialFailureAndMakesResidualHookInert() {
        RuntimeDexObserver partial = new RuntimeDexObserver(new ModuleLog(null), listener,
                missing -> handles(new FakeHandle(), new FakeHandle(true), missing));
        partial.install();

        RuntimeDexObserver.CloseResult result = partial.close();

        assertEquals(1, result.unhookedThisClose);
        assertEquals(1, result.failedThisClose);
        assertEquals(1, result.totalUnhooked);
        assertEquals(1, result.totalFailures);
        assertEquals(1, result.remaining);
        assertTrue(result.isFrameworkActive());
        assertFalse(result.logicalEnabled);
        assertFalse(partial.isLogicallyEnabled());
        partial.onClassLoaded(MainActivity.class);
        assertEquals(0, listener.triggers.size());
    }

    @Test
    public void closeReportsFullFailureWithoutClaimingWatcherRemoved() {
        RuntimeDexObserver failed = new RuntimeDexObserver(new ModuleLog(null), listener,
                missing -> handles(new FakeHandle(true), new FakeHandle(true), missing));
        failed.install();

        RuntimeDexObserver.CloseResult result = failed.close();

        assertEquals(0, result.unhookedThisClose);
        assertEquals(2, result.failedThisClose);
        assertEquals(2, result.remaining);
        assertTrue(result.isFrameworkActive());
        assertFalse(result.logicalEnabled);
    }

    @Test
    public void partialUnhookRearmInstallsOnlyMissingOverload() {
        List<Set<RuntimeDexObserver.HookSite>> requests = new ArrayList<>();
        AtomicInteger invocation = new AtomicInteger();
        RuntimeDexObserver partial = new RuntimeDexObserver(new ModuleLog(null), listener,
                missing -> {
                    requests.add(EnumSet.copyOf(missing));
                    int call = invocation.getAndIncrement();
                    return handles(new FakeHandle(), new FakeHandle(call == 0), missing);
                });
        partial.install();

        RuntimeDexObserver.CloseResult retired = partial.close();
        assertEquals(1, retired.remaining);
        partial.rearm();

        assertEquals(2, requests.size());
        assertEquals(EnumSet.of(RuntimeDexObserver.HookSite.LOAD_CLASS_ONE_ARG),
                requests.get(1));
        assertEquals(2, partial.publishedHandleCount());
        assertTrue(partial.isLogicallyEnabled());
    }

    @Test
    public void closeDuringInstallReportsDiscardUnhookFailures() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        RuntimeDexObserver racing = new RuntimeDexObserver(new ModuleLog(null), listener,
                missing -> {
                    started.countDown();
                    release.await(5, TimeUnit.SECONDS);
                    return handles(new FakeHandle(true), new FakeHandle(true), missing);
                });
        Thread installing = new Thread(racing::install, "failing-discard-install");
        installing.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        Thread releaser = new Thread(() -> {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            release.countDown();
        }, "failing-discard-release");
        releaser.start();

        RuntimeDexObserver.CloseResult result = racing.close();
        installing.join(5_000);
        releaser.join(5_000);

        assertTrue(result.summaryComplete);
        assertEquals(0, result.unhookedThisClose);
        assertEquals(2, result.failedThisClose);
        assertEquals(2, result.remaining);
        assertTrue(result.isFrameworkActive());
        assertFalse(result.logicalEnabled);
    }

    private static Map<RuntimeDexObserver.HookSite,
            io.github.libxposed.api.XposedInterface.HookHandle> handles(
            FakeHandle oneArg, FakeHandle twoArg,
            Set<RuntimeDexObserver.HookSite> requested) {
        Map<RuntimeDexObserver.HookSite,
                io.github.libxposed.api.XposedInterface.HookHandle> result =
                new EnumMap<>(RuntimeDexObserver.HookSite.class);
        if (requested.contains(RuntimeDexObserver.HookSite.LOAD_CLASS_ONE_ARG)) {
            result.put(RuntimeDexObserver.HookSite.LOAD_CLASS_ONE_ARG, oneArg);
        }
        if (requested.contains(RuntimeDexObserver.HookSite.LOAD_CLASS_TWO_ARG)) {
            result.put(RuntimeDexObserver.HookSite.LOAD_CLASS_TWO_ARG, twoArg);
        }
        return result;
    }

    private static final class RecordingInstaller implements RuntimeDexObserver.HookInstaller {
        int installCount;
        boolean slowMode;
        volatile CountDownLatch blocker;
        volatile CountDownLatch release;

        @Override
        public Map<RuntimeDexObserver.HookSite,
                io.github.libxposed.api.XposedInterface.HookHandle> installLoadClassHooks(
                Set<RuntimeDexObserver.HookSite> missingSites) {
            installCount++;
            if (slowMode) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            CountDownLatch startGate = blocker;
            if (startGate != null) {
                startGate.countDown();
            }
            CountDownLatch gate = release;
            if (gate != null) {
                try {
                    gate.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            // Mirror the real installer contract: build both handles and
            // return them for publish-once registration.
            return handles(new FakeHandle(), new FakeHandle(), missingSites);
        }
    }

    private static final class FakeHandle
            implements io.github.libxposed.api.XposedInterface.HookHandle {
        private static final AtomicInteger UNHOOKS = new AtomicInteger();
        private final boolean fail;

        FakeHandle() {
            this(false);
        }

        FakeHandle(boolean fail) {
            this.fail = fail;
        }

        static int unhookTotal() {
            return UNHOOKS.get();
        }

        static void resetUnhooks() {
            UNHOOKS.set(0);
        }

        @Override
        public java.lang.reflect.Executable getExecutable() {
            return null;
        }

        @Override
        public void unhook() {
            if (fail) {
                throw new IllegalStateException("unhook failed");
            }
            UNHOOKS.incrementAndGet();
        }

        @Override
        public String getId() {
            return "fake";
        }

        @Override
        public io.github.libxposed.api.XposedInterface.HookHandle replaceHook(
                io.github.libxposed.api.XposedInterface.Hooker hooker) {
            return this;
        }
    }

    private static final class RecordingListener implements RuntimeDexObserver.Listener {
        final List<String> triggers = new ArrayList<>();

        @Override
        public void onRuntimeDexReady(String trigger, ClassLoader runtimeClassLoader) {
            triggers.add(trigger == null ? "" : trigger.split(":")[0]);
        }
    }
}
