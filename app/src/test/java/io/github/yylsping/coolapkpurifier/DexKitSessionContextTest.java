package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public final class DexKitSessionContextTest {
    @Test
    public void capturedAppContextIsUsedWhenCurrentApplicationIsUnavailable() {
        Context context = new ContextWrapper(null);
        AtomicReference<Context> nativeContext = new AtomicReference<>();
        AtomicBoolean bridgeOpenAttempted = new AtomicBoolean();
        DexKitSession session = new DexKitSession(
                new ModuleLog(null), null, loader(), context,
                nativeContext::set,
                ignored -> {
                    bridgeOpenAttempted.set(true);
                    return null;
                });

        // The host JVM has no live ActivityThread/Application. Initialization
        // still reaches the bridge opener with the captured attach Context.
        assertNull(HookCoordinator.currentApplication());
        assertNull(session.ensureBridge("test"));
        assertSame(context, nativeContext.get());
        assertTrue(bridgeOpenAttempted.get());
    }

    @Test
    public void missingAppContextFailsGracefullyBeforeNativeInitialization() {
        AtomicBoolean nativeLoadAttempted = new AtomicBoolean();
        AtomicBoolean bridgeOpenAttempted = new AtomicBoolean();
        DexKitSession session = new DexKitSession(
                new ModuleLog(null), null, loader(), null,
                ignored -> nativeLoadAttempted.set(true),
                ignored -> {
                    bridgeOpenAttempted.set(true);
                    return null;
                });

        assertNull(session.ensureBridge("missing-context"));
        assertTrue(!nativeLoadAttempted.get());
        assertTrue(!bridgeOpenAttempted.get());
    }

    @Test
    public void resolutionSessionCarriesApplicationContextIntoDexKitOwnership() {
        Context context = new ContextWrapper(null);
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        epoch.transition(loader());

        ResolutionSessionContext session = epoch.capture(7L, context);

        assertSame(context, session.appContext);
    }

    private static ClassLoader loader() {
        return new ClassLoader(null) {
        };
    }

    @Test
    public void nativeFailurePropagatesOnceAndNeverCallsBridgeOrRetries() {
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        DexKitSession session = new DexKitSession(new ModuleLog(null), null, loader(),
                new ContextWrapper(null), ignored -> {
                    attempts.incrementAndGet();
                    throw new UnsatisfiedLinkError("dlopen namespace failure");
                }, ignored -> { throw new AssertionError("bridge must not open"); });
        DexKitNativeLoader.LoadFailure failure = org.junit.Assert.assertThrows(
                DexKitNativeLoader.LoadFailure.class, () -> session.ensureBridge("native-failure"));
        assertSame(failure, org.junit.Assert.assertThrows(DexKitNativeLoader.LoadFailure.class,
                () -> session.ensureBridge("watchdog")));
        org.junit.Assert.assertEquals(1, attempts.get());
        org.junit.Assert.assertEquals(DexKitNativeLoader.FAILURE_REASON,
                HookCoordinator.sessionFailureReason(failure));
    }

    @Test
    public void bridgeFailureAfterNativeSuccessIsNotMisclassifiedAsNativeFailure() {
        java.util.concurrent.atomic.AtomicInteger opens = new java.util.concurrent.atomic.AtomicInteger();
        DexKitSession session = new DexKitSession(new ModuleLog(null), null, loader(),
                new ContextWrapper(null), ignored -> {}, ignored -> {
                    opens.incrementAndGet();
                    throw new IllegalStateException("bridge unavailable");
                });
        assertNull(session.ensureBridge("bridge-error"));
        assertNull(session.ensureBridge("runtime-change"));
        org.junit.Assert.assertEquals(2, opens.get());
    }
}
