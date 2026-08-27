package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class EntityListHooksGenerationTest {
    @Test
    public void replyViewRestoresOriginalStateWhenReboundToOrdinaryContent() throws Exception {
        EntityListHooks hooks = new EntityListHooks(null, new ModuleLog(null), new HookLedger());
        TestView view = new TestView();
        ReplySelfDrawTargetTest.First holder = new ReplySelfDrawTargetTest.First(view, null, null);
        hooks.updateReplyHolder(holder, false);
        org.junit.Assert.assertEquals(72, view.params.height);
        hooks.updateReplyHolder(holder, true);
        hooks.updateReplyHolder(holder, true);
        org.junit.Assert.assertEquals(android.view.View.GONE, view.visibility);
        org.junit.Assert.assertEquals(0, view.params.height);
        org.junit.Assert.assertEquals(0, view.minimum);
        hooks.updateReplyHolder(holder, false);
        org.junit.Assert.assertEquals(android.view.View.VISIBLE, view.visibility);
        org.junit.Assert.assertEquals(72, view.params.height);
        org.junit.Assert.assertEquals(24, view.minimum);
    }

    public static final class TestView extends android.view.View {
        int visibility = VISIBLE;
        int minimum = 24;
        android.view.ViewGroup.LayoutParams params = new android.view.ViewGroup.LayoutParams(100, 72);
        TestView() { super(null); params.height = 72; }
        @Override public int getVisibility() { return visibility; }
        @Override public void setVisibility(int value) { visibility = value; }
        @Override public int getMinimumHeight() { return minimum; }
        @Override public void setMinimumHeight(int value) { minimum = value; }
        @Override public android.view.ViewGroup.LayoutParams getLayoutParams() { return params; }
        @Override public void setLayoutParams(android.view.ViewGroup.LayoutParams value) { params = value; }
    }

    @Test
    public void sameTopicGateClosesImmediatelyAndRejectsLateOldEvidence() {
        EntityListHooks hooks = new EntityListHooks(null, new ModuleLog(null), new HookLedger());
        ClassLoader first = new ClassLoader() { };
        ClassLoader second = new ClassLoader() { };
        hooks.beginGeneration(1, first);
        hooks.setSameTopicSemanticVerified(1, true);
        assertTrue(hooks.isSameTopicSemanticVerified());

        hooks.beginGeneration(2, second);
        assertFalse(hooks.isSameTopicSemanticVerified());
        hooks.setSameTopicSemanticVerified(1, true);
        assertFalse(hooks.isSameTopicSemanticVerified());
        hooks.setSameTopicSemanticVerified(2, true);
        assertTrue(hooks.isSameTopicSemanticVerified());
    }
}
