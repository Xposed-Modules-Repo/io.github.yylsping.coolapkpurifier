package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class EntityListFilterTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private EntityClassifier classifier;
    private EntityListFilter filter;

    @Before
    public void setUp() throws Exception {
        classifier = new EntityClassifier();
        classifier.setAccessors(new EntityAccessors(
                method("getEntityTemplate"),
                method("getEntityId"),
                method("getTitle"),
                method("getEntityType")));
        filter = new EntityListFilter(classifier);
    }

    private static Method method(String name) throws Exception {
        return Entity.class.getMethod(name);
    }

    @Test
    public void structuralSponsorMetadataIsRecognized() {
        assertTrue(classifier.isSponsored(new Entity("SponsorCard", "", "", "")));
        assertTrue(classifier.isSponsored(new Entity(
                "Feed_Detail_Reply_Sponsor_Card", "", "", "")));
        assertTrue(classifier.isSponsored(new Entity("NativeAdCard", "", "", "")));
        assertTrue(classifier.isSponsored(new Entity("advert_banner", "", "", "")));
        assertTrue(classifier.isSponsored(new Entity("replyAdCard", "", "", "")));
        assertTrue(classifier.isSponsored(new Entity("", "", "", "Advertisement")));
        assertTrue(classifier.isSponsored(new Entity("", "", "", "NATIVE_AD")));
    }

    @Test
    public void userTitleAndIsolatedEntityIdCannotTriggerSponsorRemoval() {
        assertFalse(classifier.isSponsored(new Entity(
                "feed", "SPONSORED-42", "ordinary", "feed")));
        assertFalse(classifier.isSponsored(new Entity(
                "feed", "42", "My sponsor experience", "feed")));
    }

    @Test
    public void selfDrawRequiresExactTemplateAndHonorsReplySwitch() throws Exception {
        PurifierConfig config = newConfig();
        config.setEnabled(PurifierConfig.Feature.REPLY_SPONSOR, true);
        classifier.setConfig(config);
        Entity sponsor = new Entity("feedDetailReplySponsorCard", "", "", "entityCard");
        assertTrue(classifier.shouldRemoveReplySelfDraw(sponsor));
        for (String template : Arrays.asList("feedReply", "reply", "subReply", "feed", "sponsorCard",
                "feedDetailReplySponsorCardSuffix", "FeedDetailReplySponsorCard", "")) {
            assertFalse(classifier.shouldRemoveReplySelfDraw(new Entity(
                    template, "sponsor", "feedDetailReplySponsorCard", "native_ad")));
        }
        config.setEnabled(PurifierConfig.Feature.REPLY_SPONSOR, false);
        config.setEnabled(PurifierConfig.Feature.DETAIL_SPONSOR, true);
        assertFalse(classifier.shouldRemoveReplySelfDraw(sponsor));
        assertFalse(classifier.shouldRemoveReplySelfDraw(null));
        assertFalse(classifier.shouldRemoveReplySelfDraw(new Object()));
    }

    @Test
    public void selfDrawPreservesContentWithoutVerifiedGettersOrConfig() throws Exception {
        Entity sponsor = new Entity("feedDetailReplySponsorCard", "", "", "");
        assertFalse(classifier.shouldRemoveReplySelfDraw(sponsor));
        classifier.setConfig(newConfig());
        classifier.setAccessors(null);
        assertFalse(classifier.shouldRemoveReplySelfDraw(sponsor));
    }

    @Test
    public void enabledReplySponsorDoesNotDependOnDisabledDetailSponsor() throws Exception {
        PurifierConfig config = newConfig();
        config.setEnabled(PurifierConfig.Feature.REPLY_SPONSOR, true);
        classifier.setConfig(config);
        Entity replyCard = new Entity("feed_detail_reply_sponsor_card", "", "", "");

        assertTrue(classifier.shouldRemove(replyCard, EntityClassifier.Context.REPLY));
        assertFalse(classifier.shouldRemove(replyCard, EntityClassifier.Context.DETAIL));
    }

    @Test
    public void replyAndDetailSwitchesAreIndependentInEveryCombination() throws Exception {
        Entity replyCard = new Entity("feed_detail_reply_sponsor_card", "", "", "");
        Entity detailCard = new Entity("detailSponsorCard", "", "promotion", "");
        boolean[][] combinations = {
                {true, false}, {false, true}, {true, true}, {false, false}
        };
        for (boolean[] combination : combinations) {
            PurifierConfig config = newConfig();
            config.setEnabled(PurifierConfig.Feature.REPLY_SPONSOR, combination[0]);
            config.setEnabled(PurifierConfig.Feature.DETAIL_SPONSOR, combination[1]);
            classifier.setConfig(config);

            assertEquals(combination[0], classifier.shouldRemove(
                    replyCard, EntityClassifier.Context.REPLY));
            assertEquals(combination[1], classifier.shouldRemove(
                    detailCard, EntityClassifier.Context.DETAIL));
        }
    }

    @Test
    public void sameSponsorEntityUsesTheHookContextSwitch() throws Exception {
        PurifierConfig config = newConfig();
        config.setEnabled(PurifierConfig.Feature.FEED_SPONSOR, false);
        config.setEnabled(PurifierConfig.Feature.REPLY_SPONSOR, true);
        config.setEnabled(PurifierConfig.Feature.DETAIL_SPONSOR, false);
        classifier.setConfig(config);
        Entity entity = new Entity("sponsorCard", "", "", "");

        assertFalse(classifier.shouldRemove(entity, EntityClassifier.Context.FEED));
        assertTrue(classifier.shouldRemove(entity, EntityClassifier.Context.REPLY));
        assertFalse(classifier.shouldRemove(entity, EntityClassifier.Context.DETAIL));
    }

    @Test
    public void ordinaryReplyAndDetailContentAreNeverRemoved() throws Exception {
        PurifierConfig config = newConfig();
        config.setEnabled(PurifierConfig.Feature.DETAIL_SPONSOR, true);
        classifier.setConfig(config);
        Entity ordinaryReply = new Entity("feed_reply", "42", "normal", "reply");
        Entity ordinaryDetail = new Entity("feed_detail", "43", "normal", "feed");

        assertFalse(classifier.shouldRemove(
                ordinaryReply, EntityClassifier.Context.REPLY));
        assertFalse(classifier.shouldRemove(
                ordinaryDetail, EntityClassifier.Context.DETAIL));
    }

    @Test
    public void exactSameTopicEntityTemplateIsFilteredUpstreamWhenEnabled()
            throws Exception {
        PurifierConfig config = newConfig();
        config.setEnabled(PurifierConfig.Feature.SAME_TOPIC_FEED, true);
        classifier.setConfig(config);
        classifier.setSameTopicSemanticVerified(true);
        Entity recommendCard = new Entity(
                "feedRecommendListCard", "card-1", "anything", "entity_card");

        assertTrue(classifier.isSameTopicRecommendation(recommendCard));
        assertTrue(classifier.shouldRemove(recommendCard, EntityClassifier.Context.FEED));
    }

    @Test
    public void sameTopicExactTemplateIsPreservedWithoutVerifiedEvidence()
            throws Exception {
        PurifierConfig config = newConfig();
        config.setEnabled(PurifierConfig.Feature.SAME_TOPIC_FEED, true);
        classifier.setConfig(config);
        Entity recommendCard = new Entity(
                "feedRecommendListCard", "card-1", "anything", "entity_card");

        assertTrue(classifier.isSameTopicRecommendation(recommendCard));
        assertFalse(classifier.shouldRemove(recommendCard, EntityClassifier.Context.FEED));
    }

    @Test
    public void sameTopicTemplateIsExactAndSwitchScoped() throws Exception {
        PurifierConfig disabled = newConfig();
        classifier.setConfig(disabled);
        Entity exact = new Entity("feedRecommendListCard", "1", "", "entity_card");
        Entity nearMiss = new Entity(
                "feedRecommendListCardUserPost", "2", "", "entity_card");

        assertFalse(classifier.shouldRemove(exact, EntityClassifier.Context.FEED));
        disabled.setEnabled(PurifierConfig.Feature.SAME_TOPIC_FEED, true);
        classifier.setSameTopicSemanticVerified(true);
        assertFalse(classifier.isSameTopicRecommendation(nearMiss));
        assertFalse(classifier.shouldRemove(nearMiss, EntityClassifier.Context.FEED));
        assertFalse(classifier.shouldRemove(exact, EntityClassifier.Context.DETAIL));
        assertFalse(classifier.shouldRemove(exact, EntityClassifier.Context.REPLY));
    }

    private PurifierConfig newConfig() throws Exception {
        return new PurifierConfig(folder.newFolder(), (temp, destination) -> {
            try {
                Files.copy(temp.toPath(), destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }, null);
    }

    @Test
    public void nullMissingAndThrowingGettersFailOpen() {
        assertFalse(classifier.isSponsored(null));
        assertFalse(classifier.isSponsored(new Object()));
        assertFalse(classifier.isSponsored(new ThrowingEntity()));
    }

    @Test
    public void noAdReturnsTheOriginalList() {
        List<Entity> source = Arrays.asList(
                new Entity("feed", "1", "one", "feed"),
                new Entity("reply", "2", "two", "reply"));

        assertSame(source, filter.filter(source));
    }

    @Test
    public void filtersFirstMiddleLastAndMultipleAdsWithoutMutatingSource() {
        Entity first = new Entity("sponsor", "a", "ad-1", "feed");
        Entity one = new Entity("feed", "1", "one", "feed");
        Entity middle = new Entity("nativeAd", "b", "ad-2", "feed");
        Entity two = new Entity("feed", "2", "two", "feed");
        Entity last = new Entity("lastAdCard", "c", "ad-3", "feed");
        ArrayList<Entity> source = new ArrayList<>(Arrays.asList(first, one, middle, two, last));

        List<?> result = filter.filter(source);

        assertEquals(Arrays.asList(one, two), result);
        assertEquals(Arrays.asList(first, one, middle, two, last), source);
    }

    public static final class Entity {
        private final String template;
        private final String id;
        private final String title;
        private final String type;

        Entity(String template, String id, String title, String type) {
            this.template = template;
            this.id = id;
            this.title = title;
            this.type = type;
        }

        public String getEntityTemplate() {
            return template;
        }

        public String getEntityId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getEntityType() {
            return type;
        }
    }

    public static final class ThrowingEntity {
        public String getEntityTemplate() {
            throw new IllegalStateException("boom");
        }

        public String getEntityId() {
            return null;
        }

        public String getTitle() {
            return "ordinary";
        }

        public String getEntityType() {
            throw new IllegalStateException("boom");
        }
    }
}
