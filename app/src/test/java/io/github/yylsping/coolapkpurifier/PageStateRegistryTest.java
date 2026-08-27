package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PageStateRegistryTest {
    @Test
    public void childDismissPreservesOwnerAndAllowsReentryWithoutDuplicatePage() {
        OwnedSettingsPages<Object, Object> pages = new OwnedSettingsPages<>();
        Object nativeActivity = new Object();
        Object first = new Object();
        Object second = new Object();
        assertTrue(pages.open(nativeActivity, first));
        assertFalse(pages.open(nativeActivity, second));
        assertTrue(pages.close(nativeActivity, first));
        assertFalse(pages.close(nativeActivity, first));
        assertTrue(pages.open(nativeActivity, second));
        assertFalse(pages.close(nativeActivity, first));
        assertTrue(pages.contains(nativeActivity));
        assertSame(second, pages.removeOwner(nativeActivity));
        assertFalse(pages.contains(nativeActivity));
    }

    @Test
    public void destroyingOneNativeOwnerDoesNotCloseAnotherOwnersPage() {
        OwnedSettingsPages<Object, Object> pages = new OwnedSettingsPages<>();
        Object firstOwner = new Object(), secondOwner = new Object();
        Object firstPage = new Object(), secondPage = new Object();
        pages.open(firstOwner, firstPage);
        pages.open(secondOwner, secondPage);
        assertSame(firstPage, pages.removeOwner(firstOwner));
        assertFalse(pages.close(firstOwner, firstPage));
        assertTrue(pages.contains(secondOwner));
        assertSame(secondPage, pages.removeOwner(secondOwner));
    }

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
