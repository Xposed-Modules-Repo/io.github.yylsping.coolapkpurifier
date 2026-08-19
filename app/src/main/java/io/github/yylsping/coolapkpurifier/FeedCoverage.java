package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Per-anchor feed coverage snapshot. An anchor class is COMPLETE only when
 * every feed-shaped method the CURRENT scan discovered for it carries a live
 * installed hook — "at least one hook in the class" is not coverage. A class
 * that is loadable and scanned but declares no feed-shaped method counts as
 * COMPLETE (that Coolapk version genuinely has no feed path there); a class
 * that is not loadable yet is NOT_LOADABLE and keeps coverage unsettled
 * until the deadline settles it.
 */
final class FeedCoverage {
    enum AnchorState { NOT_LOADABLE, PARTIAL, COMPLETE }

    static final class Anchor {
        final String classDescriptor;
        final boolean loadable;
        final int discovered;
        final int installed;

        Anchor(String classDescriptor, boolean loadable, int discovered, int installed) {
            this.classDescriptor = classDescriptor;
            this.loadable = loadable;
            this.discovered = discovered;
            this.installed = installed;
        }

        AnchorState state() {
            if (!loadable) {
                return AnchorState.NOT_LOADABLE;
            }
            return installed == discovered ? AnchorState.COMPLETE : AnchorState.PARTIAL;
        }

        String describe() {
            return classDescriptor + "=" + state() + "(" + installed + "/" + discovered + ")";
        }
    }

    private FeedCoverage() {
    }

    /**
     * Evaluates one anchor from the current session's resolver output.
     * {@code installedProbe} is asked per discovered method descriptor and
     * must reflect LIVE installed hooks, not persisted descriptors.
     */
    static Anchor anchor(String classDescriptor, boolean loadable,
                         List<String> discoveredMethodDescriptors,
                         Predicate<String> installedProbe) {
        int installed = 0;
        for (String descriptor : discoveredMethodDescriptors) {
            if (installedProbe.test(descriptor)) {
                installed++;
            }
        }
        return new Anchor(classDescriptor, loadable,
                discoveredMethodDescriptors.size(), installed);
    }

    /** Settled only when EVERY historically known anchor is COMPLETE. */
    static boolean settledByAnchors(List<Anchor> anchors) {
        for (Anchor anchor : anchors) {
            if (anchor.state() != AnchorState.COMPLETE) {
                return false;
            }
        }
        return true;
    }

    static String describe(List<Anchor> anchors) {
        List<String> parts = new ArrayList<>();
        for (Anchor anchor : anchors) {
            parts.add(anchor.describe());
        }
        return String.join(" ", parts);
    }
}
