package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Coverage settling probes LIVE installed hooks by declaring-class
 * descriptor, so the registry must track the class of every hook that was
 * actually installed (a resolved-but-not-installable descriptor must not
 * count as coverage).
 */
public final class HookedFeedRegistryTest {
    public static final class AnchorA {
        public List<Object> first(List<Object> source, boolean flag) {
            return source;
        }

        public List<Object> second(List<Object> source, boolean flag) {
            return source;
        }
    }

    public static final class AnchorB {
        public List<Object> only(List<Object> source, boolean flag) {
            return source;
        }
    }

    @Test
    public void tracksDeclaringClassDescriptorsOfLiveHooks() throws Exception {
        HookedFeedRegistry registry = new HookedFeedRegistry();

        registry.add(AnchorA.class.getMethod("first", List.class, boolean.class));

        assertTrue(registry.hasHookedInClass(
                DescriptorUtils.classDescriptorOf(AnchorA.class)));
        assertFalse(registry.hasHookedInClass(
                DescriptorUtils.classDescriptorOf(AnchorB.class)));
        assertEquals(1, registry.size());
    }

    @Test
    public void severalMethodsOfOneClassCountOnceForCoverage() throws Exception {
        HookedFeedRegistry registry = new HookedFeedRegistry();
        registry.add(AnchorA.class.getMethod("first", List.class, boolean.class));
        registry.add(AnchorA.class.getMethod("second", List.class, boolean.class));

        assertEquals(2, registry.size());
        assertTrue(registry.hasHookedInClass(
                DescriptorUtils.classDescriptorOf(AnchorA.class)));
    }

    @Test
    public void duplicateAddsAreIgnored() throws Exception {
        HookedFeedRegistry registry = new HookedFeedRegistry();
        java.lang.reflect.Method method =
                AnchorA.class.getMethod("first", List.class, boolean.class);

        assertTrue(registry.add(method));
        assertFalse(registry.add(method));
        assertTrue(registry.contains(method));
        assertEquals(1, registry.size());
    }

    @Test
    public void nullAndUnknownDescriptorsAreRejected() {
        HookedFeedRegistry registry = new HookedFeedRegistry();

        assertFalse(registry.add(null));
        assertFalse(registry.contains(null));
        assertFalse(registry.hasHookedInClass(null));
        assertFalse(registry.hasHookedInClass("Lnever/Installed;"));
        assertEquals(0, registry.size());
    }

    /**
     * Coordinator-shaped coverage probe: an anchor is COMPLETE only when
     * every discovered feed-shaped method of the class carries a live hook.
     */
    @Test
    public void anchorSemanticsSettleOnlyWhenEveryDiscoveredMethodIsLive()
            throws Exception {
        HookedFeedRegistry registry = new HookedFeedRegistry();

        // AnchorA declares two feed methods; only one hooked → PARTIAL.
        registry.add(AnchorA.class.getMethod("first", List.class, boolean.class));
        assertFalse(anchorCoverage(registry));

        // Both anchors fully harvested → settled.
        registry.add(AnchorA.class.getMethod("second", List.class, boolean.class));
        registry.add(AnchorB.class.getMethod("only", List.class, boolean.class));
        assertTrue(anchorCoverage(registry));
    }

    private static boolean anchorCoverage(HookedFeedRegistry registry) {
        java.util.List<FeedCoverage.Anchor> snapshot = new java.util.ArrayList<>();
        for (Class<?> anchor : java.util.Arrays.asList(AnchorA.class, AnchorB.class)) {
            java.util.List<String> discovered = new java.util.ArrayList<>();
            java.util.List<String> installed = new java.util.ArrayList<>();
            for (java.lang.reflect.Method method : anchor.getDeclaredMethods()) {
                discovered.add(method.getName());
                if (registry.contains(method)) {
                    installed.add(method.getName());
                }
            }
            snapshot.add(FeedCoverage.anchor(
                    DescriptorUtils.classDescriptorOf(anchor), true,
                    discovered, installed::contains));
        }
        return FeedCoverage.settledByAnchors(snapshot);
    }
}
