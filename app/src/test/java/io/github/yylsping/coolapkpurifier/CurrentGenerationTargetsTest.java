package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public final class CurrentGenerationTargetsTest {
    @Test
    public void g1OnlyTargetCannotEnterG2Snapshot() {
        CurrentGenerationTargets store = new CurrentGenerationTargets(1L);
        store.merge(1L, target("A", "LA;"));

        store.beginGeneration(2L);

        assertTrue(store.snapshot(2L).isEmpty());
        assertTrue(store.snapshot(1L).isEmpty());
    }

    @Test
    public void g2CacheCandidateContainsBOnly() {
        CurrentGenerationTargets store = new CurrentGenerationTargets(1L);
        store.merge(1L, target("A", "LA;"));
        store.beginGeneration(2L);
        store.merge(2L, target("B", "LB;"));

        Map<String, ResolvedTarget> g2 = store.snapshot(2L);
        assertEquals(Collections.singleton("B"), g2.keySet());
        assertFalse(g2.containsKey("A"));
    }

    @Test
    public void sameDescriptorMustBeReintroducedByG2() {
        CurrentGenerationTargets store = new CurrentGenerationTargets(1L);
        store.merge(1L, target("A", "LSame;"));
        store.beginGeneration(2L);
        assertTrue(store.snapshot(2L).isEmpty());

        assertTrue(store.merge(2L, target("A", "LSame;")));
        assertTrue(store.snapshot(2L).containsKey("A"));
        assertFalse(store.merge(1L, target("stale", "LStale;")));
    }

    private static Map<String, ResolvedTarget> target(String key, String descriptor) {
        return Collections.singletonMap(key,
                new ResolvedTarget(key, "test", descriptor, descriptor + "->m()V"));
    }
}
