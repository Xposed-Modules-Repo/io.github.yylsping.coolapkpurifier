package io.github.yylsping.coolapkpurifier;

import android.content.Context;
import android.os.SystemClock;

import org.luckypray.dexkit.DexKitBridge;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Session-local DexKitBridge lifecycle. The containing
 * {@link ResolutionSessionContext} fixes the runtime ClassLoader and is the
 * sole owner/closer. The internal revision/budget remains defensive, but a
 * normal resolver transaction creates exactly one bridge and never shares it
 * with another generation.
 */
final class DexKitSession {
    interface NativeLibraryLoader {
        void ensureLoaded(Context appContext);
    }

    interface BridgeOpener {
        DexKitBridge create(ClassLoader loader);
    }

    private final ModuleLog log;
    private final BootstrapTrace trace;
    private final ClassLoader loader;
    private final Context appContext;
    private final NativeLibraryLoader nativeLibraryLoader;
    private final BridgeOpener bridgeOpener;
    private final Object lock = new Object();
    private final AtomicInteger generation = new AtomicInteger();

    private DexKitBridge bridge;
    private int bridgeGeneration = -1;
    private int rebuildCount;
    private long loaderIdentity = -1L;
    private DexKitNativeLoader.LoadFailure nativeFailure;

    DexKitSession(ModuleLog log, BootstrapTrace trace, ClassLoader loader,
                  Context appContext) {
        this(log, trace, loader, appContext,
                context -> DexKitNativeLoader.ensureLoaded(context, log, trace),
                candidateLoader -> DexKitBridge.create(candidateLoader, true));
    }

    DexKitSession(ModuleLog log, BootstrapTrace trace, ClassLoader loader,
                  Context appContext, NativeLibraryLoader nativeLibraryLoader,
                  BridgeOpener bridgeOpener) {
        this.log = log;
        this.trace = trace;
        this.loader = loader;
        this.appContext = appContext;
        this.nativeLibraryLoader = nativeLibraryLoader;
        this.bridgeOpener = bridgeOpener;
    }

    int getGeneration() {
        return generation.get();
    }

    long getLoaderIdentity() {
        return System.identityHashCode(loader);
    }

    void notifyLoaderGenerationChanged(String reason) {
        generation.incrementAndGet();
        trace("loaderGeneration", "reason=" + reason
                + " generation=" + generation.get()
                + " loaderIdentity=" + getLoaderIdentity());
        log.info("resolver loaderGeneration reason=" + reason
                + " generation=" + generation.get()
                + " loaderIdentity=" + getLoaderIdentity());
    }

    DexKitBridge ensureBridge(String trigger) {
        synchronized (lock) {
            if (nativeFailure != null) {
                throw nativeFailure;
            }
            long loaderId = System.identityHashCode(loader);
            if (bridge != null && bridge.isValid()
                    && bridgeGeneration == generation.get()
                    && loaderIdentity == loaderId) {
                return bridge;
            }
            // Bounded rebuild budget: the runtime loader may append DEX in
            // several stages, and each incomplete session bumps the
            // generation to force one rescan.
            if (rebuildCount >= 4) {
                log.info("resolver dexkit rebuild refused rebuildCount=" + rebuildCount
                        + " trigger=" + trigger);
                return bridge != null && bridge.isValid() ? bridge : null;
            }
            closeBridge();
            rebuildCount++;
            bridgeGeneration = generation.get();
            loaderIdentity = loaderId;
            long start = SystemClock.elapsedRealtime();
            try {
                if (appContext == null) {
                    trace("bridgeCreateEnd", "trigger=" + trigger
                            + " failed=applicationContextUnavailable");
                    log.info("resolver dexkit bridge creation skipped trigger=" + trigger
                            + " reason=applicationContextUnavailable");
                    return null;
                }
                try {
                    nativeLibraryLoader.ensureLoaded(appContext);
                } catch (Exception | LinkageError failure) {
                    nativeFailure = failure instanceof DexKitNativeLoader.LoadFailure
                            ? (DexKitNativeLoader.LoadFailure) failure
                            : new DexKitNativeLoader.LoadFailure("nativeLibraryLoader", failure);
                    trace("nativeBootstrapFailed", nativeFailure.getMessage());
                    log.error("resolver NATIVE_BOOTSTRAP_FAILED", nativeFailure);
                    throw nativeFailure;
                }
                trace("bridgeCreateStart", "trigger=" + trigger
                        + " generation=" + bridgeGeneration
                        + " loaderIdentity=" + loaderIdentity);
                bridge = bridgeOpener.create(loader);
                if (bridge == null) {
                    trace("bridgeCreateEnd", "trigger=" + trigger
                            + " failed=bridgeOpenerReturnedNull");
                    return null;
                }
                long end = SystemClock.elapsedRealtime();
                trace("bridgeCreateEnd", "trigger=" + trigger
                        + " elapsedMs=" + (end - start)
                        + " dexNum=" + (bridge.isValid() ? bridge.getDexNum() : -1)
                        + " rebuild=" + rebuildCount);
                if (bridge.isValid()) {
                    bridge.setThreadNum(2);
                    log.info("resolver dexkit bridge created trigger=" + trigger
                            + " generation=" + bridgeGeneration
                            + " loaderIdentity=" + loaderIdentity
                            + " elapsedMs=" + (end - start)
                            + " dexNum=" + bridge.getDexNum()
                            + " rebuild=" + rebuildCount);
                }
                return bridge.isValid() ? bridge : null;
            } catch (DexKitNativeLoader.LoadFailure failure) {
                throw failure;
            } catch (Throwable throwable) {
                trace("bridgeCreateEnd", "trigger=" + trigger + " failed=" + throwable);
                log.error("resolver dexkit bridge creation failed trigger=" + trigger, throwable);
                return null;
            }
        }
    }

    boolean maybeRebuildIfStale(int expectedGeneration) {
        synchronized (lock) {
            if (bridge == null || expectedGeneration != bridgeGeneration) {
                closeBridge();
                return true;
            }
            return false;
        }
    }

    void close() {
        synchronized (lock) {
            if (bridge != null) {
                trace("resolverClosed", "dexNum="
                        + (bridge.isValid() ? bridge.getDexNum() : -1));
            }
            closeBridge();
        }
    }

    private void closeBridge() {
        if (bridge != null) {
            try {
                bridge.close();
            } catch (Throwable ignored) {
            }
            bridge = null;
        }
    }

    private void trace(String event, String detail) {
        BootstrapTrace current = trace;
        if (current != null) {
            current.mark(event, detail);
        }
    }

}
