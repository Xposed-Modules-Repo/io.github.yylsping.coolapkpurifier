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
}
