package io.github.yylsping.coolapkpurifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Live descriptor set for exactly one runtime loader generation. */
final class CurrentGenerationTargets {
    private long generation;
    private final Map<String, ResolvedTarget> targets = new LinkedHashMap<>();

    CurrentGenerationTargets(long initialGeneration) {
        generation = initialGeneration;
    }

    synchronized void beginGeneration(long nextGeneration) {
        generation = nextGeneration;
        targets.clear();
    }

    synchronized boolean merge(long expectedGeneration,
                               Map<String, ResolvedTarget> incoming) {
        if (expectedGeneration != generation) {
            return false;
        }
        TargetResolver.mergeTargets(targets, incoming);
        return true;
    }

    synchronized boolean replace(long expectedGeneration,
                                 Map<String, ResolvedTarget> verified) {
        if (expectedGeneration != generation) {
            return false;
        }
        targets.clear();
        targets.putAll(verified);
        return true;
    }

    synchronized Map<String, ResolvedTarget> snapshot(long expectedGeneration) {
        if (expectedGeneration != generation) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(targets);
    }

    synchronized long generation() {
        return generation;
    }
}
