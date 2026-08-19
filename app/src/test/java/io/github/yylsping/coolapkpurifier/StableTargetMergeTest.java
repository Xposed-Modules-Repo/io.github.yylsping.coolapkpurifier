package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Multi-target keys must be descriptor-stable across sessions. Positional
 * numbering used to let a later session re-key an existing descriptor and
 * overwrite an unrelated persisted entry (A,B → B lost A).
 */
public final class StableTargetMergeTest {

    private static ResolvedTarget feed(String descriptor) {
        return new ResolvedTarget(TargetResolver.KEY_FEED, "fingerprint_strong",
                descriptor, descriptor + "->m(Ljava/util/List;Z)Ljava/util/List;");
    }

    private static ResolvedTarget splash(String descriptor) {
        return new ResolvedTarget(TargetResolver.KEY_SPLASH_BASE, "fingerprint_strong",
                descriptor, "");
    }

    private static Map<String, ResolvedTarget> session(ResolvedTarget... targets) {
        Map<String, ResolvedTarget> incoming = new LinkedHashMap<>();
        for (int i = 0; i < targets.length; i++) {
            String key = targets[i].key.equals(TargetResolver.KEY_FEED)
                    ? TargetResolver.indexedKey(TargetResolver.KEY_FEED, countFeeds(incoming))
                    : TargetResolver.indexedKey(TargetResolver.KEY_SPLASH_BASE,
                            countSplashes(incoming));
            incoming.put(key, targets[i].withKey(key));
        }
        return incoming;
    }

    private static int countFeeds(Map<String, ResolvedTarget> map) {
        int count = 0;
        for (String key : map.keySet()) {
            if (TargetResolver.isFeedKey(key)) {
                count++;
            }
        }
        return count;
    }

    private static int countSplashes(Map<String, ResolvedTarget> map) {
        int count = 0;
        for (String key : map.keySet()) {
            if (TargetResolver.isSplashKey(key)) {
                count++;
            }
        }
        return count;
    }

    @Test
    public void feedOrderAndSubsetChangesKeepEveryDescriptor() {
        Map<String, ResolvedTarget> live = new LinkedHashMap<>();

        TargetResolver.mergeTargets(live, session(feed("LA;"), feed("LB;")));
        assertEquals(2, live.size());
        assertEquals("feed", keyOfDescriptor(live, "LA;->m(Ljava/util/List;Z)Ljava/util/List;"));
        assertEquals("feed#2", keyOfDescriptor(live, "LB;->m(Ljava/util/List;Z)Ljava/util/List;"));

        // Session 2 sees only B: B must keep feed#2, A must survive.
        TargetResolver.mergeTargets(live, session(feed("LB;")));
        assertEquals(2, live.size());
        assertFalse(live.get("feed").methodDescriptor
                .contains("LB;"));

        // Session 3 sees B, A and a new C in a different order.
        TargetResolver.mergeTargets(live, session(feed("LB;"), feed("LA;"), feed("LC;")));
        assertEquals(3, live.size());
        assertEquals("feed#3", keyOfDescriptor(live, "LC;->m(Ljava/util/List;Z)Ljava/util/List;"));
        assertEquals("feed", keyOfDescriptor(live, "LA;->m(Ljava/util/List;Z)Ljava/util/List;"));
        assertEquals("feed#2", keyOfDescriptor(live, "LB;->m(Ljava/util/List;Z)Ljava/util/List;"));
    }

    @Test
    public void splashMergesStablyByClassDescriptor() {
        Map<String, ResolvedTarget> live = new LinkedHashMap<>();

        TargetResolver.mergeTargets(live, session(splash("Lsplash/SplashActivity;")));
        TargetResolver.mergeTargets(live, session(splash("Lsplash/SplashAdActivity;")));
        assertEquals(2, live.size());

        // Superset session in reversed order: keys stay bound to descriptors.
        TargetResolver.mergeTargets(live, session(
                splash("Lsplash/SplashAdActivity;"), splash("Lsplash/SplashActivity;")));
        assertEquals(2, live.size());
        assertEquals("splash_base",
                keyOfClassDescriptor(live, "Lsplash/SplashActivity;"));
        assertEquals("splash_base#2",
                keyOfClassDescriptor(live, "Lsplash/SplashAdActivity;"));
    }

    @Test
    public void duplicateDescriptorInsideOneBatchCollapsesToOneEntry() {
        Map<String, ResolvedTarget> live = new LinkedHashMap<>();
        Map<String, ResolvedTarget> incoming = new LinkedHashMap<>();
        incoming.put(TargetResolver.KEY_FEED, feed("LA;"));
        incoming.put("feed#2", feed("LA;"));

        TargetResolver.mergeTargets(live, incoming);

        assertEquals(1, live.size());
    }

    @Test
    public void getterKeysStayFixedAndSplashDoesNotStealFeedKeys() {
        Map<String, ResolvedTarget> live = new LinkedHashMap<>();
        Map<String, ResolvedTarget> incoming = new LinkedHashMap<>();
        incoming.put(TargetResolver.KEY_GETTER_TITLE, new ResolvedTarget(
                TargetResolver.KEY_GETTER_TITLE, "fingerprint_strong",
                "LE;", "LE;->getTitle()Ljava/lang/String;"));

        TargetResolver.mergeTargets(live, incoming);
        TargetResolver.mergeTargets(live, session(splash("Ls/SplashActivity;")));
        TargetResolver.mergeTargets(live, session(feed("LA;")));

        assertEquals(3, live.size());
        assertEquals("LE;->getTitle()Ljava/lang/String;",
                live.get(TargetResolver.KEY_GETTER_TITLE).methodDescriptor);
        assertEquals("splash_base", keyOfClassDescriptor(live, "Ls/SplashActivity;"));
        assertEquals("feed", keyOfDescriptor(live, "LA;->m(Ljava/util/List;Z)Ljava/util/List;"));
    }

    private static String keyOfDescriptor(Map<String, ResolvedTarget> live, String method) {
        for (Map.Entry<String, ResolvedTarget> entry : live.entrySet()) {
            if (method.equals(entry.getValue().methodDescriptor)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String keyOfClassDescriptor(Map<String, ResolvedTarget> live, String cls) {
        for (Map.Entry<String, ResolvedTarget> entry : live.entrySet()) {
            if (cls.equals(entry.getValue().classDescriptor)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
