package io.github.yylsping.coolapkpurifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Keeps discovery evidence, primary semantic hooks and UI fallbacks separate.
 * A fallback can improve degraded behaviour but can never satisfy primary
 * readiness for a resolved target.
 */
final class FeatureInstallState {
    private long generation;
    private final Set<String> primaryHooks = new HashSet<>();
    private final Set<String> fallbackHooks = new HashSet<>();
    private final Set<String> fallbackEvidence = new HashSet<>();
    private final Set<String> semanticEvidence = new HashSet<>();
    private final Set<String> splashClasses = new HashSet<>();

    synchronized void beginGeneration(long nextGeneration) {
        if (nextGeneration <= generation) {
            throw new IllegalArgumentException("generation must increase: current="
                    + generation + " next=" + nextGeneration);
        }
        generation = nextGeneration;
        primaryHooks.clear();
        fallbackHooks.clear();
        fallbackEvidence.clear();
        semanticEvidence.clear();
        splashClasses.clear();
    }

    synchronized long generation() {
        return generation;
    }

    synchronized void markPrimaryHook(String key) {
        primaryHooks.add(key);
    }

    synchronized boolean markPrimaryHook(long expectedGeneration, String key) {
        return expectedGeneration == generation && primaryHooks.add(key);
    }

    synchronized void markFallbackHook(String key) {
        fallbackHooks.add(key);
    }

    synchronized boolean markFallbackHook(long expectedGeneration, String key) {
        return expectedGeneration == generation && fallbackHooks.add(key);
    }

    synchronized void markFallbackEvidence(String key) {
        fallbackEvidence.add(key);
    }

    synchronized boolean markFallbackEvidence(long expectedGeneration, String key) {
        return expectedGeneration == generation && fallbackEvidence.add(key);
    }

    synchronized void markSemanticEvidence(String key) {
        semanticEvidence.add(key);
    }

    synchronized boolean markSemanticEvidence(long expectedGeneration, String key) {
        return expectedGeneration == generation && semanticEvidence.add(key);
    }

    synchronized boolean hasPrimaryHook(String key) {
        return primaryHooks.contains(key);
    }

    synchronized boolean hasFallbackHook(String key) {
        return fallbackHooks.contains(key);
    }

    synchronized boolean hasFallbackEvidence(String key) {
        return fallbackEvidence.contains(key);
    }

    synchronized boolean hasSemanticEvidence(String key) {
        return semanticEvidence.contains(key);
    }

    synchronized boolean markSplashHook(long expectedGeneration, String className) {
        return expectedGeneration == generation && splashClasses.add(className);
    }

    synchronized boolean hasSplashHook() {
        return !splashClasses.isEmpty();
    }

    synchronized boolean hasSplashClass(String className) {
        return splashClasses.contains(className);
    }

    synchronized Set<String> splashClasses() {
        return new java.util.TreeSet<>(splashClasses);
    }
}
