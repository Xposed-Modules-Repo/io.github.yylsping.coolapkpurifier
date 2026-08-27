package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.Bundle;

import com.coolapk.market.model.AutoValueFeed;
import com.coolapk.market.model.Entity;
import com.coolapk.market.model.FeedReply;
import com.coolapk.market.viewholder.MultiFeedReplyViewHolder;
import com.coolapk.market.view.feed.FeedBottomHolder;
import com.coolapk.market.view.cardlist.MainV8ListFragment;
import com.coolapk.market.view.cardlist.component.RecyclerViewItemFullVisibleControllerKt;

import java.util.List;

import org.junit.Test;

/** Prefix-aware verification for indexed feed#N / splash_base#N targets. */
public final class TargetVerifierTest {
    private static final String HELPER_D =
            "L" + Helper.class.getName().replace('.', '/') + ";";
    private static final String SPLASH_D =
            "L" + TestSplashActivity.class.getName().replace('.', '/') + ";";
    private static final String FEED_METHOD_D =
            HELPER_D + "->transform(Ljava/util/List;Z)Ljava/util/List;";
    private static final String ON_CREATE_D =
            SPLASH_D + "->onCreate(Landroid/os/Bundle;)V";

    @Test
    public void indexedFeedKeyVerifiesLikeBaseKey() {
        ResolvedTarget target = new ResolvedTarget(
                "feed#2", "fingerprint_strong", HELPER_D, FEED_METHOD_D);

        assertNull(TargetVerifier.verify(target, getClass().getClassLoader()));
    }

    @Test
    public void feedShapeMismatchIsRejected() {
        ResolvedTarget target = new ResolvedTarget(
                "feed", "fingerprint_strong", HELPER_D,
                HELPER_D + "->notFeed(Ljava/util/List;Z)Ljava/util/List;");

        assertNotNull(TargetVerifier.verify(target, getClass().getClassLoader()));
    }

    @Test
    public void indexedSplashKeyWithEmptyMethodDescriptorVerifies() {
        ResolvedTarget target = new ResolvedTarget(
                "splash_base#2", "fingerprint_strong", SPLASH_D, "");

        assertNull(TargetVerifier.verify(target, getClass().getClassLoader()));
    }

    @Test
    public void nonSplashKeyWithEmptyMethodDescriptorIsRejected() {
        ResolvedTarget target = new ResolvedTarget(
                "getter.title", "fingerprint_strong", SPLASH_D, "");

        assertNotNull(TargetVerifier.verify(target, getClass().getClassLoader()));
    }

    @Test
    public void classOnlyReplyTargetVerifiesWithoutRelaxingOtherKeys() {
        String descriptor = DescriptorUtils.classDescriptorOf(MultiFeedReplyViewHolder.class);
        assertNull(TargetVerifier.verify(new ResolvedTarget(
                TargetResolver.KEY_REPLY_HOLDER, "lazy_semantic_class", descriptor, ""),
                getClass().getClassLoader()));
        assertNotNull(TargetVerifier.verify(new ResolvedTarget(
                "getter.title", "cache", descriptor, ""), getClass().getClassLoader()));
    }

    @Test
    public void replyCacheRejectsAnUnrelatedClassEvenWithBindShape() {
        assertNotNull(TargetVerifier.verify(new ResolvedTarget(
                TargetResolver.KEY_REPLY_HOLDER, "cache",
                DescriptorUtils.classDescriptorOf(ReplyLookalike.class), ""),
                getClass().getClassLoader()));
        assertFalse(TargetVerifier.isReplyHolderClass(null));
    }

    @Test
    public void replyBinderAcceptsConcreteEntityAndProtectedFeedReply() throws Exception {
        assertTrue(TargetVerifier.isReplyBindMethod(MultiFeedReplyViewHolder.class
                .getDeclaredMethod("bind", Entity.class)));
        assertTrue(TargetVerifier.isReplyBindMethod(MultiFeedReplyViewHolder.class
                .getDeclaredMethod("bindReply", FeedReply.class)));
    }

    @Test
    public void replyBinderRejectsStaticAbstractAndReturningMethods() throws Exception {
        assertFalse(TargetVerifier.isReplyBindMethod(MultiFeedReplyViewHolder.class
                .getDeclaredMethod("staticBind", Entity.class)));
        assertFalse(TargetVerifier.isReplyBindMethod(AbstractReply.class
                .getDeclaredMethod("bind", Entity.class)));
        assertFalse(TargetVerifier.isReplyBindMethod(MultiFeedReplyViewHolder.class
                .getDeclaredMethod("returningBind", Entity.class)));
    }

    @Test
    public void replyBinderRejectsBroadOrWrongArityMethods() throws Exception {
        assertFalse(TargetVerifier.isReplyBindMethod(MultiFeedReplyViewHolder.class
                .getDeclaredMethod("broadBind", Object.class)));
        assertFalse(TargetVerifier.isReplyBindMethod(MultiFeedReplyViewHolder.class
                .getDeclaredMethod("extraArgument", Entity.class, boolean.class)));
        assertFalse(TargetVerifier.isReplyBindMethod(MultiFeedReplyViewHolder.class
                .getDeclaredMethod("noArguments")));
    }

    public static final class ReplyLookalike {
        public void bind(Entity entity) {
        }
    }

    public abstract static class AbstractReply {
        public abstract void bind(Entity entity);
    }

    @Test
    public void splashOnCreateShapeIsAccepted() {
        ResolvedTarget target = new ResolvedTarget(
                "splash_base", "fingerprint_strong", SPLASH_D, ON_CREATE_D);

        assertNull(TargetVerifier.verify(target, getClass().getClassLoader()));
    }

    @Test
    public void getterKeysStillVerify() {
        String entityD = "L" + EntityAccessorsTest.Entity.class.getName()
                .replace('.', '/') + ";";
        ResolvedTarget target = new ResolvedTarget(
                "getter.entityTemplate", "fingerprint_strong", entityD,
                entityD + "->getEntityTemplate()Ljava/lang/String;");

        assertNull(TargetVerifier.verify(target, getClass().getClassLoader()));
    }

    @Test
    public void feedShapeMatchesTheLegacySignature() throws Exception {
        assertTrue(TargetVerifier.isFeedShape(
                Helper.class.getMethod("transform", List.class, boolean.class)));
        assertEquals(false, TargetVerifier.isFeedShape(
                Helper.class.getMethod("notFeed", List.class, boolean.class)));
    }

    @Test
    public void topicTargetRowAssemblerShapeIsAccepted() throws Exception {
        assertTrue(TargetVerifier.isTopicRecommendMethod(
                FeedBottomHolder.class.getMethod("composeTargetRow",
                        com.coolapk.market.model.Feed.class,
                        FeedBottomHolder.class,
                        androidx.compose.runtime.Composer.class,
                        int.class)));
        assertEquals(false, TargetVerifier.isTopicRecommendMethod(
                FeedBottomHolder.class.getMethod("oldRenderer",
                        Object.class, Object.class, Object.class,
                        androidx.compose.runtime.Composer.class,
                        int.class)));
    }

    @Test
    public void concreteDetailSponsorGetterShapeIsAccepted() throws Exception {
        assertTrue(TargetVerifier.isDetailSponsorGetter(
                AutoValueFeed.class.getMethod("getDetailSponsorCard")));
        assertEquals(false, TargetVerifier.isDetailSponsorGetter(
                com.coolapk.market.model.Feed.class.getMethod(
                        "getDetailSponsorCard")));
    }

    @Test
    public void malformedCachedDetailSponsorDescriptorIsRejected() {
        String entityD = "L" + EntityAccessorsTest.Entity.class.getName()
                .replace('.', '/') + ";";
        ResolvedTarget wrongGetter = new ResolvedTarget(
                TargetResolver.KEY_DETAIL_SPONSOR, "cache", entityD,
                entityD + "->getEntityTemplate()Ljava/lang/String;");

        assertNotNull(TargetVerifier.verify(
                wrongGetter, getClass().getClassLoader()));
    }

    @Test
    public void sameTopicSemanticPredicateRequiresExactSafeShape() throws Exception {
        assertTrue(TargetVerifier.isSameTopicTemplatePredicate(
                MainV8ListFragment.class.getMethod(
                        "isRecommendCard", Object.class)));
        assertEquals(false, TargetVerifier.isSameTopicTemplatePredicate(
                MainV8ListFragment.class.getMethod(
                        "unsafeBroadPredicate", String.class)));
    }

    @Test
    public void obfuscatedAutoCommentEntryUsesSourceShapeNotMethodName() throws Exception {
        assertTrue(TargetVerifier.isAutoCommentEntry(
                RecyclerViewItemFullVisibleControllerKt.class.getMethod(
                        "m4970", com.coolapk.market.view.cardlist.EntityListFragment.class)));
        assertEquals(false, TargetVerifier.isAutoCommentEntry(
                RecyclerViewItemFullVisibleControllerKt.class.getMethod(
                        "unsafeInstance",
                        com.coolapk.market.view.cardlist.EntityListFragment.class)));
        assertEquals(false, TargetVerifier.isAutoCommentEntry(
                RecyclerViewItemFullVisibleControllerKt.class.getMethod(
                        "addAutoShowFeedCommentView", String.class)));
    }

    public static final class Helper {
        public List<Object> transform(List<Object> source, boolean flag) {
            return source;
        }

        public java.util.Collection<Object> notFeed(List<Object> source, boolean flag) {
            return source;
        }
    }

    public static final class TestSplashActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }
}
