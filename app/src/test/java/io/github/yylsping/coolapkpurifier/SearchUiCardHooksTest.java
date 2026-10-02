package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingComponent;
import androidx.recyclerview.widget.RecyclerView;

import com.coolapk.market.model.Card;
import com.coolapk.market.model.HolderItem;

import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface.Chain;

/**
 * SEARCH_HOT_WORDS / SEARCH_HOT_RANKINGS: exact holder contract verification
 * and render-time template/url classification.
 */
public final class SearchUiCardHooksTest {

    /** Fixture mirroring the host capsule holder contract. */
    public static final class FakeCapsuleHolder extends RecyclerView.ViewHolder {
        // The verified contract: an owner-declared, non-static Card field.
        public Card card;

        public FakeCapsuleHolder(View itemView, DataBindingComponent component) {
            super(itemView);
        }

        public void bind(Object data) {
        }
    }

    /** e66-like sibling: same shape, but the data field is HolderItem. */
    public static final class HolderItemOnlyHolder extends RecyclerView.ViewHolder {
        public HolderItem item;

        public HolderItemOnlyHolder(View itemView, DataBindingComponent component) {
            super(itemView);
        }

        public void bind(Object data) {
        }
    }

    /** Static Card fields do not satisfy the instance-field contract. */
    public static final class StaticCardOnlyHolder extends RecyclerView.ViewHolder {
        public static Card CARD;

        public StaticCardOnlyHolder(View itemView, DataBindingComponent component) {
            super(itemView);
        }

        public void bind(Object data) {
        }
    }

    /** A non-Card-typed field never satisfies the exact-type contract. */
    public static final class ObjectFieldHolder extends RecyclerView.ViewHolder {
        public Object card;

        public ObjectFieldHolder(View itemView, DataBindingComponent component) {
            super(itemView);
        }

        public void bind(Object data) {
        }
    }

    /** Card field declared on a base class; not inherited by contract. */
    public static class BaseCardHolder extends RecyclerView.ViewHolder {
        public Card card;

        public BaseCardHolder(View itemView, DataBindingComponent component) {
            super(itemView);
        }
    }

    public static final class InheritedCardFieldHolder extends BaseCardHolder {
        public InheritedCardFieldHolder(View itemView, DataBindingComponent component) {
            super(itemView, component);
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

    @Test
    public void contractRequiresOwnerDeclaredCardInstanceField() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        // Valid holder: owner-declared non-static Card field.
        assertTrue(SearchUiCardHooks.isExactTarget(spec(FakeCapsuleHolder.class),
                FakeCapsuleHolder.class.getDeclaredMethod("bind", Object.class),
                loader));
        // e66-like sibling: same shape/layout, but the data field is HolderItem.
        assertFalse(SearchUiCardHooks.isExactTarget(spec(HolderItemOnlyHolder.class),
                HolderItemOnlyHolder.class.getDeclaredMethod("bind", Object.class),
                loader));
        // A static Card field never satisfies the instance-field contract.
        assertFalse(SearchUiCardHooks.isExactTarget(spec(StaticCardOnlyHolder.class),
                StaticCardOnlyHolder.class.getDeclaredMethod("bind", Object.class),
                loader));
        // Exact type required: an Object-typed field does not qualify.
        assertFalse(SearchUiCardHooks.isExactTarget(spec(ObjectFieldHolder.class),
                ObjectFieldHolder.class.getDeclaredMethod("bind", Object.class),
                loader));
        // All three validated profiles declare the Card field on the owner
        // class itself; inherited fields are deliberately not accepted.
        assertFalse(SearchUiCardHooks.isExactTarget(spec(InheritedCardFieldHolder.class),
                InheritedCardFieldHolder.class.getDeclaredMethod("bind", Object.class),
                loader));
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

    // ------------------------------------------------------------------
    // Binder policy: proceed/collapse/restore/reuse, gate independence.
    // ------------------------------------------------------------------

    private static SearchUiCardHooks.Runtime policyRuntime(String template,
                                                           String urlContains)
            throws Exception {
        JSONObject json = specJson(FakeCapsuleHolder.class.getName())
                .put("entityTemplate", template)
                .put("urlContains", urlContains);
        SearchUiCardTargetSpec spec = SearchUiCardTargetSpec.parse(json, "testKind");
        Method binder = FakeCapsuleHolder.class.getDeclaredMethod("bind", Object.class);
        return new SearchUiCardHooks.Runtime(spec, binder, Card.class,
                Card.class.getMethod("getEntityTemplate"),
                Card.class.getMethod("getUrl"),
                new RelatedDataDelta.HolderController(
                        RecyclerView.ViewHolder.class.getField("itemView")));
    }

    private static final class BinderProbe {
        final AtomicInteger observed = new AtomicInteger();
        final AtomicInteger suppressed = new AtomicInteger();
        final AtomicBoolean observedLogged = new AtomicBoolean();
        final AtomicBoolean suppressedLogged = new AtomicBoolean();
        final ModuleLog log = new ModuleLog(null);
    }

    private static Object onBinder(FakeChain chain, boolean enabled,
                                   SearchUiCardHooks.Runtime runtime,
                                   PurifierConfig.Feature feature,
                                   BinderProbe probe) throws Throwable {
        return SearchUiCardHooks.onBinder(chain, enabled, runtime, feature, null,
                probe.observed, probe.suppressed,
                probe.observedLogged, probe.suppressedLogged,
                probe.log, "test");
    }

    @Test
    public void disabledFeatureProceedsTargetCardWithoutCollapse() throws Throwable {
        SearchUiCardHooks.Runtime words = policyRuntime("hotSearch", "");
        StatefulView view = new StatefulView();
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(100, 100);
        params.height = 48; // android.jar stub ctors are no-ops
        view.setLayoutParams(params);
        FakeCapsuleHolder holder = new FakeCapsuleHolder(view, null);
        BinderProbe probe = new BinderProbe();

        FakeChain chain = new FakeChain(holder, null, new FakeCard("hotSearch", null));
        onBinder(chain, false, words, PurifierConfig.Feature.SEARCH_HOT_WORDS, probe);

        assertEquals(1, chain.proceedCalls.get());
        assertEquals(View.VISIBLE, view.getVisibility());
        assertEquals(48, view.getLayoutParams().height);
        assertEquals(1, probe.observed.get());
        assertEquals(0, probe.suppressed.get());
    }

    @Test
    public void enabledFeatureProceedsThenCollapsesExactCard() throws Throwable {
        SearchUiCardHooks.Runtime words = policyRuntime("hotSearch", "");
        StatefulView view = new StatefulView();
        view.setMinimumHeight(9);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(100, 100);
        params.height = 48; // android.jar stub ctors are no-ops
        view.setLayoutParams(params);
        FakeCapsuleHolder holder = new FakeCapsuleHolder(view, null);
        BinderProbe probe = new BinderProbe();

        FakeChain chain = new FakeChain(holder, null, new FakeCard("hotSearch", null));
        onBinder(chain, true, words, PurifierConfig.Feature.SEARCH_HOT_WORDS, probe);

        assertEquals(1, chain.proceedCalls.get());
        assertEquals(View.GONE, view.getVisibility());
        assertEquals(0, view.getMinimumHeight());
        assertEquals(0, view.getLayoutParams().height);
        assertEquals(1, probe.observed.get());
        assertEquals(1, probe.suppressed.get());
    }

    @Test
    public void nonTargetCardPassesThroughEvenWhenEnabled() throws Throwable {
        SearchUiCardHooks.Runtime words = policyRuntime("hotSearch", "");
        StatefulView view = new StatefulView();
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(100, 100);
        params.height = 48; // android.jar stub ctors are no-ops
        view.setLayoutParams(params);
        FakeCapsuleHolder holder = new FakeCapsuleHolder(view, null);
        BinderProbe probe = new BinderProbe();

        FakeChain chain = new FakeChain(holder, null,
                new FakeCard("searchHotListCard", null));
        onBinder(chain, true, words, PurifierConfig.Feature.SEARCH_HOT_WORDS, probe);

        assertEquals(1, chain.proceedCalls.get());
        assertEquals(View.VISIBLE, view.getVisibility());
        assertEquals(48, view.getLayoutParams().height);
        assertEquals(0, probe.observed.get());
        assertEquals(0, probe.suppressed.get());
    }

    @Test
    public void recycledHolderIsRestoredBeforeANonTargetRebind() throws Throwable {
        SearchUiCardHooks.Runtime words = policyRuntime("hotSearch", "");
        StatefulView view = new StatefulView();
        view.setMinimumHeight(9);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(100, 100);
        params.height = 48; // android.jar stub ctors are no-ops
        view.setLayoutParams(params);
        FakeCapsuleHolder holder = new FakeCapsuleHolder(view, null);
        BinderProbe probe = new BinderProbe();

        onBinder(new FakeChain(holder, null, new FakeCard("hotSearch", null)),
                true, words, PurifierConfig.Feature.SEARCH_HOT_WORDS, probe);
        assertEquals(View.GONE, view.getVisibility());

        // RecyclerView reuses the collapsed holder for a non-target card:
        // the rebind must restore the original layout state first.
        onBinder(new FakeChain(holder, null, new FakeCard("searchHotListCard", null)),
                true, words, PurifierConfig.Feature.SEARCH_HOT_WORDS, probe);
        assertEquals(View.VISIBLE, view.getVisibility());
        assertEquals(9, view.getMinimumHeight());
        assertEquals(48, view.getLayoutParams().height);
    }

    @Test
    public void wordsAndRankingsGatesDoNotCross() throws Throwable {
        SearchUiCardHooks.Runtime words = policyRuntime("hotSearch", "");
        SearchUiCardHooks.Runtime rankings = policyRuntime("searchHotListCard", "");
        StatefulView wordsView = new StatefulView();
        StatefulView rankingsView = new StatefulView();
        BinderProbe wordsProbe = new BinderProbe();
        BinderProbe rankingsProbe = new BinderProbe();

        // Words gate OFF: its exact card is not collapsed.
        onBinder(new FakeChain(new FakeCapsuleHolder(wordsView, null), null,
                        new FakeCard("hotSearch", null)),
                false, words, PurifierConfig.Feature.SEARCH_HOT_WORDS, wordsProbe);
        assertEquals(View.VISIBLE, wordsView.getVisibility());
        assertEquals(0, wordsProbe.suppressed.get());

        // Rankings gate ON: only its own card collapses.
        onBinder(new FakeChain(new FakeCapsuleHolder(rankingsView, null), null,
                        new FakeCard("searchHotListCard", null)),
                true, rankings, PurifierConfig.Feature.SEARCH_HOT_RANKINGS,
                rankingsProbe);
        assertEquals(View.GONE, rankingsView.getVisibility());
        assertEquals(1, rankingsProbe.suppressed.get());
        // The rankings invocation never touched the words counters.
        assertEquals(1, wordsProbe.observed.get());
        assertEquals(0, wordsProbe.suppressed.get());
    }

    private static final class FakeChain implements Chain {
        final AtomicInteger proceedCalls = new AtomicInteger();
        private final Object thisObject;
        private final Object result;
        private final Object arg0;

        FakeChain(Object thisObject, Object result, Object arg0) {
            this.thisObject = thisObject;
            this.result = result;
            this.arg0 = arg0;
        }

        @Override public java.lang.reflect.Executable getExecutable() { return null; }
        @Override public Object getThisObject() { return thisObject; }
        @Override public java.util.List<Object> getArgs() {
            return java.util.Collections.singletonList(arg0);
        }
        @Override public Object getArg(int index) { return index == 0 ? arg0 : null; }
        @Override public Object proceed() { proceedCalls.incrementAndGet(); return result; }
        @Override public Object proceed(Object... args) {
            proceedCalls.incrementAndGet(); return result;
        }
        @Override public Object proceedWith(Object thisObject) {
            proceedCalls.incrementAndGet(); return result;
        }
        @Override public Object proceedWith(Object thisObject, Object... args) {
            proceedCalls.incrementAndGet(); return result;
        }
    }

    private static final class StatefulView extends View {
        private int visibility = View.VISIBLE;
        private int minimumHeight;
        private ViewGroup.LayoutParams layoutParams;

        StatefulView() {
            super(null);
        }

        @Override public void setVisibility(int visibility) {
            this.visibility = visibility;
        }

        @Override public int getVisibility() {
            return visibility;
        }

        @Override public void setMinimumHeight(int minimumHeight) {
            this.minimumHeight = minimumHeight;
        }

        @Override public int getMinimumHeight() {
            return minimumHeight;
        }

        @Override public void setLayoutParams(ViewGroup.LayoutParams params) {
            this.layoutParams = params;
        }

        @Override public ViewGroup.LayoutParams getLayoutParams() {
            return layoutParams;
        }
    }
}
