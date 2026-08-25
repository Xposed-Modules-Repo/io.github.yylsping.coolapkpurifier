package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable evidence captured by the same epoch transaction as terminal commit. */
final class TerminalSnapshot {
    static final class Readiness {
        final boolean coreReady;
        final List<String> missingRequired;

        Readiness(boolean coreReady, List<String> missingRequired) {
            this.coreReady = coreReady;
            this.missingRequired = immutableCopy(missingRequired);
        }
    }

    final long generation;
    final ClassLoader loader;
    final BootstrapState terminalState;
    final boolean coreReady;
    final List<String> missingRequired;
    final String source;

    TerminalSnapshot(long generation, ClassLoader loader,
                     BootstrapState terminalState, Readiness readiness,
                     String source) {
        this.generation = generation;
        this.loader = loader;
        this.terminalState = terminalState;
        this.coreReady = readiness.coreReady;
        this.missingRequired = immutableCopy(readiness.missingRequired);
        this.source = source;
    }

    String describe() {
        return "terminalGeneration=" + generation
                + " terminalLoaderIdentity=" + System.identityHashCode(loader)
                + " terminal=" + terminalState
                + " coreReady=" + coreReady
                + " missingRequired=" + missingRequired
                + " source=" + source;
    }

    private static List<String> immutableCopy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
