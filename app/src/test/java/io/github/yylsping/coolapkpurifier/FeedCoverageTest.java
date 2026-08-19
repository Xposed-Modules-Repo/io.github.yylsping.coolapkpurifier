package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Per-anchor full-harvest semantics: "the class has at least one hook" is
 * NOT coverage — every feed-shaped method discovered for the class in the
 * current scan must be live-installed before the anchor counts as COMPLETE.
 */
public final class FeedCoverageTest {
    private static final String AD_HELPER = "Lcom/coolapk/market/view/ad/EntityAdHelper;";
    private static final String FRAGMENT =
            "Lcom/coolapk/market/view/cardlist/EntityListFragment;";

    private static FeedCoverage.Anchor anchor(String descriptor, boolean loadable,
                                              List<String> discovered,
                                              Set<String> installed) {
        return FeedCoverage.anchor(descriptor, loadable, discovered, installed::contains);
    }

    /** Case 1/2: one anchor has two feed methods but only one live hook. */
    @Test
    public void partialAnchorKeepsCoverageUnsettledUntilRetrySucceeds() {
        FeedCoverage.Anchor partial =
                anchor(AD_HELPER, true, Arrays.asList("A", "B"), set("A"));
        FeedCoverage.Anchor complete =
                anchor(FRAGMENT, true, Collections.singletonList("C"), set("C"));

        assertEquals(FeedCoverage.AnchorState.PARTIAL, partial.state());
        assertEquals(FeedCoverage.AnchorState.COMPLETE, complete.state());
        assertFalse(FeedCoverage.settledByAnchors(Arrays.asList(partial, complete)));

        // Retry installs method B too → settled.
        FeedCoverage.Anchor retried =
                anchor(AD_HELPER, true, Arrays.asList("A", "B"), set("A", "B"));
        assertTrue(FeedCoverage.settledByAnchors(Arrays.asList(retried, complete)));
    }

    /** Case 3: anchor loadable but declares no feed-shaped method (version refactor). */
    @Test
    public void loadableAnchorWithoutFeedMethodsIsComplete() {
        FeedCoverage.Anchor noMethods =
                anchor(FRAGMENT, true, Collections.emptyList(), set());
        FeedCoverage.Anchor adHelperComplete = Helper.adHelperComplete();

        assertEquals(FeedCoverage.AnchorState.COMPLETE, noMethods.state());
        assertTrue(FeedCoverage.settledByAnchors(
                Arrays.asList(adHelperComplete, noMethods)));
    }

    /** Case 4: an old version genuinely missing one anchor settles only at the deadline. */
    @Test
    public void absentAnchorStaysUnsettledForDeadlineSettlement() {
        FeedCoverage.Anchor absent = anchor(FRAGMENT, false, Collections.emptyList(), set());
        FeedCoverage.Anchor present =
                anchor(AD_HELPER, true, Collections.singletonList("A"), set("A"));

        assertEquals(FeedCoverage.AnchorState.NOT_LOADABLE, absent.state());
        assertFalse(FeedCoverage.settledByAnchors(Arrays.asList(present, absent)));
    }

    /** Case 5: a persisted descriptor whose hook failed to install is not live coverage. */
    @Test
    public void discoveredButFailedInstallIsNotCoverage() {
        FeedCoverage.Anchor failedInstall =
                anchor(AD_HELPER, true, Collections.singletonList("A"), set());
        assertEquals(FeedCoverage.AnchorState.PARTIAL, failedInstall.state());
        assertFalse(FeedCoverage.settledByAnchors(Collections.singletonList(failedInstall)));
    }

    @Test
    public void describeSeparatesAnchorStates() {
        List<FeedCoverage.Anchor> anchors = Arrays.asList(
                anchor(AD_HELPER, true, Arrays.asList("A", "B"), set("A", "B")),
                anchor(FRAGMENT, false, Collections.emptyList(), set()));
        String described = FeedCoverage.describe(anchors);
        assertTrue(described, described.contains("COMPLETE(2/2)"));
        assertTrue(described, described.contains("NOT_LOADABLE(0/0)"));
    }

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    /** Small local helper for building the counterpart anchor in case 3. */
    private static final class Helper {
        static FeedCoverage.Anchor adHelperComplete() {
            return FeedCoverage.anchor(AD_HELPER, true,
                    Collections.singletonList("A"), d -> true);
        }
    }
}
