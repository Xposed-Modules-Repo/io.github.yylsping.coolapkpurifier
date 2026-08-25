package io.github.yylsping.coolapkpurifier;

import android.content.Context;

import org.luckypray.dexkit.DexKitBridge;

/**
 * Immutable runtime token for one resolver worker invocation.
 *
 * <p>The generation and loader are captured before the worker is dispatched
 * and never read back from coordinator globals. The context also owns the
 * DexKit session it creates; superseding a context only makes commits inert.
 * The owning worker closes the bridge from its {@code finally} block, so a
 * loader-transition thread can never release a native handle mid-query.</p>
 */
final class ResolutionSessionContext implements AutoCloseable {
    interface OwnedResource {
        void close();
    }

    final long sessionId;
    final long generation;
    final ClassLoader loader;
    final Context appContext;

    private final OwnedResource injectedResource;
    private volatile boolean superseded;
    private volatile boolean terminalized;
    private boolean closed;
    private DexKitSession dexKitSession;

    ResolutionSessionContext(long sessionId, long generation, ClassLoader loader) {
        this(sessionId, generation, loader, null, null);
    }

    ResolutionSessionContext(long sessionId, long generation, ClassLoader loader,
                             OwnedResource injectedResource) {
        this(sessionId, generation, loader, null, injectedResource);
    }

    ResolutionSessionContext(long sessionId, long generation, ClassLoader loader,
                             Context appContext, OwnedResource injectedResource) {
        if (loader == null) {
            throw new IllegalArgumentException("loader == null");
        }
        this.sessionId = sessionId;
        this.generation = generation;
        this.loader = loader;
        this.appContext = appContext;
        this.injectedResource = injectedResource;
    }

    boolean isSuperseded() {
        return superseded;
    }

    boolean isInvalidated() {
        return superseded || terminalized;
    }

    void supersede() {
        superseded = true;
    }

    void terminalize() {
        terminalized = true;
    }

    synchronized DexKitBridge ensureBridge(ModuleLog log, BootstrapTrace trace,
                                            String trigger) {
        if (closed || isInvalidated()) {
            return null;
        }
        if (dexKitSession == null) {
            dexKitSession = new DexKitSession(log, trace, loader, appContext);
            dexKitSession.notifyLoaderGenerationChanged(
                    "resolutionSession:" + sessionId + ":" + trigger);
        }
        return dexKitSession.ensureBridge(trigger);
    }

    synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (dexKitSession != null) {
            dexKitSession.close();
            dexKitSession = null;
        }
        if (injectedResource != null) {
            injectedResource.close();
        }
    }

    String describe() {
        return "sessionId=" + sessionId
                + " generation=" + generation
                + " loaderIdentity=" + System.identityHashCode(loader)
                + " status=" + (terminalized ? "TERMINAL"
                : (superseded ? "SUPERSEDED" : "CURRENT"));
    }
}
