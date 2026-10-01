package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Bundled 16.6.2 (versionCode 2609151) profile contract: exact descriptors
 * adjudicated from the 16.6.2 runtime dex dump. D1/D6 unchanged from 16.6.1;
 * D2 fn4->qx4, D3 yl6->py6 + semanticMethod, D5 d14->vb4.
 */
public final class TargetManifest1662ProfileTest {
    private static TargetProfile profile1662() {
        TargetProfile profile =
                TestManifests.manifest().validatedProfileFor(TestManifests.COOLAPK_16_6_2);
        if (profile == null) {
            throw new AssertionError("bundled manifest must validate 16.6.2");
        }
        return profile;
    }

    @Test
    public void detailSponsorDescriptorUnchangedFrom1661() {
        assertEquals("Lcom/coolapk/market/model/$$AutoValue_Feed;->"
                        + "getDetailSponsorCard()Lcom/coolapk/market/model/Entity;",
                profile1662().detailSponsor.descriptor());
    }

    @Test
    public void detailSponsorUiDescriptorUnchangedFrom1661() {
        DetailSponsorUiTargetSpec spec = profile1662().detailSponsorUi;
        assertEquals("Lcom/coolapk/market/view/ad/ֈ;->ވ(Ljava/lang/Object;)V",
                spec.descriptor());
        assertEquals("ކ", spec.layoutField);
        assertEquals("sponsorForFeedDetail", spec.entityTemplate);
        assertEquals("com.coolapk.market.model.Entity", spec.entityClass);
        assertEquals("com.coolapk.market.view.ad.EntityAdHelper", spec.adHelperClass);
    }

    @Test
    public void replySponsorDescriptorUsesAdjudicatedQx4() {
        ReplySponsorTargetSpec spec = profile1662().replySponsor;
        assertEquals("Lqx4;->ވ(Ljava/lang/Object;)V", spec.descriptor());
        assertEquals("މ", spec.layoutField);
        assertEquals("feedDetailReplySponsorCard", spec.entityTemplate);
        assertEquals("com.coolapk.market.model.Entity", spec.entityClass);
        assertEquals("com.coolapk.market.view.ad.EntityAdHelper", spec.adHelperClass);
    }

    @Test
    public void sameTopicDescriptorUsesPy6Event() {
        SameTopicTargetSpec spec = profile1662().sameTopic;
        assertEquals("Lcom/coolapk/market/view/cardlist/MainV8ListFragment;->"
                        + "ཥ(Ljava/lang/Object;)Z",
                spec.semanticDescriptor());
        assertEquals("Lcom/coolapk/market/view/cardlist/MainV8ListFragment;->"
                        + "onInsertRecommendListEvent(Lpy6;)V",
                spec.eventDescriptor());
        assertEquals("feedRecommendListCard", spec.entityTemplate);
        assertEquals("Ϳ", spec.anchorGetter);
        assertEquals("Ԩ", spec.cardGetter);
    }

    @Test
    public void topicDeviceRecommendDescriptorUsesVb4() {
        assertEquals("Lvb4;->ޓ(Lcom/coolapk/market/model/Feed;Lvb4;"
                        + "Landroidx/compose/runtime/Composer;I)Lkotlin/Unit;",
                profile1662().topicDeviceRecommend.descriptor());
    }

    @Test
    public void topicDeviceRecommendUiDescriptorUsesCs4() {
        assertEquals("Lcs4;->ԭ(Landroidx/compose/ui/Modifier;"
                        + "Lcom/coolapk/market/model/FeedTarget;"
                        + "Lcom/coolapk/market/view/feed/reply/FeedDetailV13ViewModel;"
                        + "Landroidx/compose/runtime/Composer;I)V",
                profile1662().topicDeviceRecommendUi.descriptor());
    }

    @Test
    public void autoCommentDescriptorUnchangedFrom1661() {
        assertEquals("Lcom/coolapk/market/view/cardlist/component/"
                        + "RecyclerViewItemFullVisibleControllerKt;->"
                        + "Ϳ(Lcom/coolapk/market/view/cardlist/EntityListFragment;)V",
                profile1662().autoComment.descriptor());
    }

    @Test
    public void autoCommentPromptDescriptorUnchangedFrom1661() {
        assertEquals("Lcom/coolapk/market/view/cardlist/component/"
                        + "RecyclerViewItemFullVisibleControllerKt"
                        + "$addAutoShowFeedCommentView$1;->Ԫ("
                        + "Lcom/coolapk/market/view/cardlist/EntityListFragment;"
                        + "Lkotlin/ranges/IntRange;)Lkotlin/Unit;",
                profile1662().autoCommentPrompt.descriptor());
    }

    @Test
    public void relatedDataDescriptorsAreExactAndUnchangedFrom1661() {
        TargetProfile profile = profile1662();
        assertEquals("Lcom/coolapk/market/model/$$AutoValue_Feed;->"
                        + "getRelatedData()Ljava/util/List;",
                profile.relatedData.descriptor());
        assertEquals("Lcom/coolapk/market/viewholder/ޕ;->ވ(Ljava/lang/Object;)V",
                profile.relatedIconListUi.descriptor());
        assertEquals("com.coolapk.market.viewholder.ؠ",
                profile.relatedIconListUi.holderClass);
        assertEquals("iconListCard", profile.relatedIconListUi.cardTemplate);
        assertEquals("goods", profile.relatedIconListUi.promotionEntityType);
        assertEquals("topic", profile.relatedIconListUi.singleRecommendEntityType);
        assertEquals("Lcom/coolapk/market/view/feed/reply/Ԯ;->"
                        + "ޚ(Lcom/coolapk/market/model/HolderItem;)V",
                profile.relatedContentUi.descriptor());
        assertEquals("ENTITY_TYPE_BIND_GOODS", profile.relatedContentUi.entityType);
    }

    @Test
    public void searchHotUiDescriptorsAreExact() {
        TargetProfile profile = profile1662();
        SearchUiCardTargetSpec capsule = profile.searchHotCapsuleUi;
        assertEquals("Leu;->ވ(Ljava/lang/Object;)V", capsule.descriptor());
        assertEquals("capsuleListCard", capsule.entityTemplate);
        assertEquals("contentEntityType=hotSearch", capsule.urlContains);
        assertEquals(java.util.Arrays.asList("android.view.View",
                        "androidx.databinding.DataBindingComponent"),
                capsule.constructorParams);
        SearchUiCardTargetSpec words = profile.searchHotWordsUi;
        assertEquals("Lene;->ވ(Ljava/lang/Object;)V", words.descriptor());
        assertEquals("hotSearch", words.entityTemplate);
        assertEquals("", words.urlContains);
        assertEquals(java.util.Arrays.asList("android.view.View",
                        "androidx.databinding.DataBindingComponent", "v27"),
                words.constructorParams);
        SearchUiCardTargetSpec rankings = profile.searchHotRankingsUi;
        assertEquals("Ldne;->ވ(Ljava/lang/Object;)V", rankings.descriptor());
        assertEquals("searchHotListCard", rankings.entityTemplate);
        assertEquals(java.util.Arrays.asList("android.view.View",
                        "androidx.databinding.DataBindingComponent", "v27", "pu3"),
                rankings.constructorParams);
        for (SearchUiCardTargetSpec spec : new SearchUiCardTargetSpec[]{
                capsule, words, rankings}) {
            assertEquals("java.lang.Object", spec.dataClass);
            assertEquals("com.coolapk.market.model.Card", spec.cardClass);
            assertEquals("getEntityTemplate", spec.entityTemplateGetter);
            assertEquals("getUrl", spec.urlGetter);
            assertEquals("androidx.recyclerview.widget.RecyclerView$ViewHolder",
                    spec.viewHolderClass);
        }
    }

    @Test
    public void searchHotUiAbsentFrom1661Profile() {
        TargetProfile profile1661 =
                TestManifests.manifest().validatedProfileFor(TestManifests.COOLAPK_16_6_1);
        if (profile1661 == null) {
            throw new AssertionError("bundled manifest must validate 16.6.1");
        }
        org.junit.Assert.assertNull(profile1661.searchHotCapsuleUi);
        org.junit.Assert.assertNull(profile1661.searchHotWordsUi);
        org.junit.Assert.assertNull(profile1661.searchHotRankingsUi);
    }

    @Test
    public void profileIdentity() {
        TargetProfile profile = profile1662();
        assertEquals(2_609_151L, profile.versionCode);
        assertEquals("16.6.2", profile.versionName);
        assertEquals(TargetProfile.Status.VALIDATED, profile.status);
    }
}
