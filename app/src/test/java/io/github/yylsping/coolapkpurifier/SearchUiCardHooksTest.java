package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.databinding.DataBindingComponent;
import androidx.recyclerview.widget.RecyclerView;

import com.coolapk.market.model.Card;

import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * SEARCH_HOT_WORDS / SEARCH_HOT_RANKINGS: exact holder contract verification
 * and render-time template/url classification.
 */
public final class SearchUiCardHooksTest {

    /** Fixture mirroring the host capsule holder contract. */
    public static final class FakeCapsuleHolder extends RecyclerView.ViewHolder {
        public FakeCapsuleHolder(View itemView, DataBindingComponent component) {
            super(itemView);
        }

        public void bind(Object data) {
        }
    }

    /** Non-final owner must be rejected. */
    public static class OpenHolder extends RecyclerView.ViewHolder {
        public OpenHolder(View itemView, DataBindingComponent component) {
            super(itemView);
        }

        public void bind(Object data) {
        }
    }

    /** Missing the pinned (View, DataBindingComponent) ctor. */
    public static final class WrongCtorHolder extends RecyclerView.ViewHolder {
        public WrongCtorHolder(View itemView) {
            super(itemView);
        }

        public void bind(Object data) {
        }
    }

    private static final class FakeCard implements Card {
        private final String template;
        private final String url;

        FakeCard(String template, String url) {
            this.template = template;
            this.url = url;
        }

        @Override
        public String getEntityTemplate() {
            return template;
        }

        @Override
        public String getUrl() {
            return url;
        }
    }

    private static JSONObject specJson(String ownerClass) throws Exception {
        JSONObject json = new JSONObject()
                .put("ownerClass", ownerClass)
                .put("methodName", "bind")
                .put("dataClass", "java.lang.Object")
                .put("cardClass", "com.coolapk.market.model.Card")
                .put("entityTemplateGetter", "getEntityTemplate")
                .put("entityTemplate", "capsuleListCard")
                .put("urlGetter", "getUrl")
                .put("urlContains", "contentEntityType=hotSearch")
                .put("constructorParams", new org.json.JSONArray()
                        .put("android.view.View")
                        .put("androidx.databinding.DataBindingComponent"))
                .put("viewHolderClass",
                        "androidx.recyclerview.widget.RecyclerView$ViewHolder");
        return json;
    }

    private static SearchUiCardTargetSpec spec(Class<?> owner) throws Exception {
        return SearchUiCardTargetSpec.parse(specJson(owner.getName()), "testKind");
    }

    @Test
    public void exactContractAcceptsFixtureHolder() throws Exception {
        Method binder = FakeCapsuleHolder.class.getDeclaredMethod("bind", Object.class);
        assertTrue(SearchUiCardHooks.isExactTarget(spec(FakeCapsuleHolder.class),
                binder, getClass().getClassLoader()));
    }

    @Test
    public void contractRejectsNearbyHolders() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Method binder = FakeCapsuleHolder.class.getDeclaredMethod("bind", Object.class);
        // Owner in the spec differs from the declaring class.
        assertFalse(SearchUiCardHooks.isExactTarget(spec(OpenHolder.class),
                binder, loader));
        // Non-final owner.
        assertFalse(SearchUiCardHooks.isExactTarget(spec(OpenHolder.class),
                OpenHolder.class.getDeclaredMethod("bind", Object.class), loader));
        // Missing pinned ctor shape.
        assertFalse(SearchUiCardHooks.isExactTarget(spec(WrongCtorHolder.class),
                WrongCtorHolder.class.getDeclaredMethod("bind", Object.class), loader));
        // Null spec / method / loader are fail-closed.
        assertFalse(SearchUiCardHooks.isExactTarget(null, binder, loader));
        assertFalse(SearchUiCardHooks.isExactTarget(spec(FakeCapsuleHolder.class),
                null, loader));
    }

    private static SearchUiCardHooks.Runtime runtime(String template,
                                                     String urlContains)
            throws Exception {
        JSONObject json = specJson(FakeCapsuleHolder.class.getName())
                .put("entityTemplate", template)
                .put("urlContains", urlContains);
        SearchUiCardTargetSpec spec = SearchUiCardTargetSpec.parse(json, "testKind");
        Method binder = FakeCapsuleHolder.class.getDeclaredMethod("bind", Object.class);
        return new SearchUiCardHooks.Runtime(spec, binder, Card.class,
                Card.class.getMethod("getEntityTemplate"),
                Card.class.getMethod("getUrl"), null);
    }

    @Test
    public void classificationUsesTemplateAndUrl() throws Exception {
        SearchUiCardHooks.Runtime capsule =
                runtime("capsuleListCard", "contentEntityType=hotSearch");
        assertTrue(SearchUiCardHooks.matches(capsule, new FakeCard("capsuleListCard",
                "coolmarket://feed?contentEntityType=hotSearch&id=1")));
        assertFalse(SearchUiCardHooks.matches(capsule, new FakeCard("capsuleListCard",
                "coolmarket://feed?contentEntityType=album&id=1")));
        assertFalse(SearchUiCardHooks.matches(capsule, new FakeCard("capsuleListCard",
                null)));
        assertFalse(SearchUiCardHooks.matches(capsule, new FakeCard("hotSearch",
                "coolmarket://feed?contentEntityType=hotSearch&id=1")));
    }

    @Test
    public void classificationWithoutUrlConstraintNeedsTemplateOnly() throws Exception {
        SearchUiCardHooks.Runtime words = runtime("hotSearch", "");
        assertTrue(SearchUiCardHooks.matches(words, new FakeCard("hotSearch", null)));
        assertFalse(SearchUiCardHooks.matches(words,
                new FakeCard("searchHotListCard", "anything")));
        // Fail-closed on foreign values and nulls.
        assertFalse(SearchUiCardHooks.matches(words, new Object()));
        assertFalse(SearchUiCardHooks.matches(words, null));
        assertFalse(SearchUiCardHooks.matches(null, new FakeCard("hotSearch", null)));
    }
}
