package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PageStateRegistryTest {
    @Test
    public void repeatedEntryDoesNotRequireDuplicateState() {
        PageStateRegistry<Object, Object> registry = new PageStateRegistry<>();
        Object activity = new Object();
        Object state = new Object();
        registry.put(activity, state);

        assertTrue(registry.contains(activity));
        assertSame(state, registry.get(activity));
        assertEquals(1, registry.size());
    }

    @Test
    public void destroyExplicitlyBreaksTheRegistryReferenceChain() {
        PageStateRegistry<Object, Object> registry = new PageStateRegistry<>();
        Object activity = new Object();
        Object state = new Object();
        registry.put(activity, state);

        assertSame(state, registry.remove(activity));
        assertFalse(registry.contains(activity));
        assertNull(registry.get(activity));
        assertEquals(0, registry.size());
    }
}
