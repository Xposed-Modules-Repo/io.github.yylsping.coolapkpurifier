package io.github.yylsping.coolapkpurifier;

/** High level startup state machine. Never use fixed sleeps to advance it. */
enum BootstrapState {
    BOOTSTRAP,
    WAIT_RUNTIME_DEX,
    CACHE_VERIFY,
    SPLASH_CRITICAL,
    SPLASH_READY,
    FULL_RESOLVE,
    READY,
    DEGRADED;

    boolean isAtLeast(BootstrapState state) {
        return ordinal() >= state.ordinal();
    }

    boolean isTerminal() {
        return this == READY || this == DEGRADED;
    }
}
