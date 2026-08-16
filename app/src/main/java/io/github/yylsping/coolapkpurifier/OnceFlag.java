package io.github.yylsping.coolapkpurifier;

import java.util.concurrent.atomic.AtomicBoolean;

/** Thread-safe one-shot flag used for first-activity trace semantics. */
final class OnceFlag {
    private final AtomicBoolean fired = new AtomicBoolean();

    boolean tryOnce() {
        return fired.compareAndSet(false, true);
    }
}
