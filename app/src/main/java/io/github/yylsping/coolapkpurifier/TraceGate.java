package io.github.yylsping.coolapkpurifier;

import java.util.concurrent.atomic.AtomicBoolean;

/** Lightweight one-way terminal gate for BootstrapTrace file I/O. */
final class TraceGate {
    private final AtomicBoolean frozen = new AtomicBoolean();

    boolean isFrozen() {
        return frozen.get();
    }

    /** Freeze is idempotent. */
    void freeze() {
        frozen.set(true);
    }
}
