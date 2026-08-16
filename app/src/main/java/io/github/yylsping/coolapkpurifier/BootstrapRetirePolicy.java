package io.github.yylsping.coolapkpurifier;

/**
 * Pure policy deciding when the generic startup Instrumentation hooks may be
 * retired. Degraded startup keeps the framework fallback until MainActivity
 * is definitely visible.
 */
final class BootstrapRetirePolicy {
    private BootstrapRetirePolicy() {
    }

    static boolean canRetire(BootstrapState state, boolean mainActivitySeen,
                             boolean specificSplashInstalled) {
        if (state == BootstrapState.READY) {
            return mainActivitySeen && specificSplashInstalled;
        }
        if (state == BootstrapState.DEGRADED) {
            return mainActivitySeen;
        }
        return false;
    }
}
