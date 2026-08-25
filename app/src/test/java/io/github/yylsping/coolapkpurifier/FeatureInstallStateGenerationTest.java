package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FeatureInstallStateGenerationTest {
    @Test
    public void newGenerationFailsClosedUntilEvidenceIsReestablished() {
        FeatureInstallState state = new FeatureInstallState();
        state.beginGeneration(1);
        assertTrue(state.markPrimaryHook(1, TargetResolver.KEY_DETAIL_SPONSOR));
        assertTrue(state.markSemanticEvidence(1, TargetResolver.KEY_SAME_TOPIC_FEED));
        assertTrue(state.markFallbackEvidence(1, TargetResolver.KEY_AUTO_COMMENT));
        assertTrue(state.markSplashHook(1, "com.coolapk.market.Splash"));

        state.beginGeneration(2);

        assertFalse(state.hasPrimaryHook(TargetResolver.KEY_DETAIL_SPONSOR));
        assertFalse(state.hasSemanticEvidence(TargetResolver.KEY_SAME_TOPIC_FEED));
        assertFalse(state.hasFallbackEvidence(TargetResolver.KEY_AUTO_COMMENT));
        assertFalse(state.hasSplashHook());
        // A late G1 install result cannot contaminate G2.
        assertFalse(state.markPrimaryHook(1, TargetResolver.KEY_DETAIL_SPONSOR));
        assertFalse(state.hasPrimaryHook(TargetResolver.KEY_DETAIL_SPONSOR));

        assertTrue(state.markSemanticEvidence(2, TargetResolver.KEY_SAME_TOPIC_FEED));
        assertTrue(state.hasSemanticEvidence(TargetResolver.KEY_SAME_TOPIC_FEED));
    }
}
