package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ContentLayoutHooksTest {
    @Test
    public void commentChildWithoutParentLayoutIsNotFallbackReady() {
        assertFalse(ContentLayoutHooks.hasAutoCommentFallbackEvidence(
                true, 1, 0, 0));
    }

    @Test
    public void v8ParentAndCommentChildAreFallbackReady() {
        assertTrue(ContentLayoutHooks.hasAutoCommentFallbackEvidence(
                true, 1, 2, 0));
    }

    @Test
    public void v8LiteParentAndCommentChildAreFallbackReady() {
        assertTrue(ContentLayoutHooks.hasAutoCommentFallbackEvidence(
                true, 1, 0, 3));
    }

    @Test
    public void resourceEvidenceWithoutInflaterHookIsNotReady() {
        assertFalse(ContentLayoutHooks.hasAutoCommentFallbackEvidence(
                false, 1, 2, 3));
    }
}
