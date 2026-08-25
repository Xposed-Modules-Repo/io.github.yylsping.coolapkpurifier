package io.github.yylsping.coolapkpurifier;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Weak-key registry whose values are also explicitly released on host destruction. */
final class PageStateRegistry<A, S> {
    private final Map<A, S> states = Collections.synchronizedMap(new WeakHashMap<>());

    boolean contains(A activity) {
        return states.containsKey(activity);
    }

    S get(A activity) {
        return states.get(activity);
    }

    void put(A activity, S state) {
        states.put(activity, state);
    }

    S remove(A activity) {
        return states.remove(activity);
    }

    int size() {
        return states.size();
    }
}
