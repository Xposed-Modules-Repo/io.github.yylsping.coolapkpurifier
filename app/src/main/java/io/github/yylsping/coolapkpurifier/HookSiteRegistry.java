package io.github.yylsping.coolapkpurifier;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.Map;

/**
 * Deduplicates hooks by reflected member and logical feature. Method/Constructor
 * equality includes the declaring Class object, so equal descriptors from L1
 * and L2 remain distinct while repeated reflection in one loader is stable.
 */
final class HookSiteRegistry<T> {
    private final Map<Executable, Map<String, T>> sites = new HashMap<>();

    synchronized boolean contains(String featureKey, Executable executable) {
        Map<String, T> features = sites.get(executable);
        return features != null && features.containsKey(featureKey);
    }

    synchronized void put(String featureKey, Executable executable, T value) {
        sites.computeIfAbsent(executable, ignored -> new HashMap<>()).put(featureKey, value);
    }

    synchronized int size() {
        int count = 0;
        for (Map<String, T> features : sites.values()) {
            count += features.size();
        }
        return count;
    }
}
