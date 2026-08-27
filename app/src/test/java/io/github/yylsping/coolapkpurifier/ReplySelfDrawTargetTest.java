package io.github.yylsping.coolapkpurifier;

import android.view.View;
import androidx.databinding.DataBindingComponent;
import androidx.recyclerview.widget.RecyclerView;
import com.coolapk.market.model.Entity;
import com.coolapk.market.view.ad.EntityAdHelper;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public final class ReplySelfDrawTargetTest {
    private final ClassLoader loader = getClass().getClassLoader();

    public abstract static class Base extends RecyclerView.ViewHolder {
        protected Base(View view) { super(view); }
        public abstract void a(Object entity);
    }
    public static final class First extends Base {
        public static int resource = com.coolapk.market.R.layout.item_reply_self_draw;
        private Entity entity;
        private EntityAdHelper helper;
        public First(View view, EntityAdHelper helper, DataBindingComponent component) { super(view); }
        @Override public void a(Object value) { }
        public final void helper(Object value) { }
        public void payload(Object entity, Object payload) { }
    }
    public static final class Second extends Base {
        public static int other = com.coolapk.market.R.layout.item_reply_self_draw;
        private Entity entity;
        private EntityAdHelper helper;
        public Second(View view, EntityAdHelper helper, DataBindingComponent component) { super(view); }
        @Override public void a(Object value) { }
    }
    public static final class NoConstructor extends Base {
        public static int resource = com.coolapk.market.R.layout.item_reply_self_draw;
        private Entity entity;
        private EntityAdHelper helper;
        public NoConstructor(View view) { super(view); }
        @Override public void a(Object value) { }
    }
    public abstract static class AmbiguousBase extends Base {
        protected AmbiguousBase(View view) { super(view); }
        @Override public abstract void a(Object value);
        public abstract void b(Object value);
    }
    public static final class Ambiguous extends AmbiguousBase {
        public static int resource = com.coolapk.market.R.layout.item_reply_self_draw;
        private Entity entity;
        private EntityAdHelper helper;
        public Ambiguous(View view, EntityAdHelper helper, DataBindingComponent component) { super(view); }
        @Override public void a(Object value) { }
        @Override public void b(Object value) { }
    }

    private Method bind(Class<?> type) throws Exception { return type.getDeclaredMethod("a", Object.class); }

    private ReplySelfDrawResolver.Candidate candidate(Class<?> type, boolean registered, boolean factory,
                                                    String... strings) throws Exception {
        return new ReplySelfDrawResolver.Candidate(bind(type), registered, factory, Arrays.asList(strings));
    }

    @Test public void acceptsOnlyUniqueParentBinderAndRejectsHelpersAndPayload() throws Exception {
        assertTrue(ReplySelfDrawTarget.isBindMethod(bind(First.class), loader));
        assertFalse(ReplySelfDrawTarget.isBindMethod(First.class.getDeclaredMethod("helper", Object.class), loader));
        assertFalse(ReplySelfDrawTarget.isBindMethod(First.class.getDeclaredMethod("payload", Object.class, Object.class), loader));
        assertFalse(ReplySelfDrawTarget.isBindMethod(bind(Base.class), loader));
        assertFalse(ReplySelfDrawTarget.isBindMethod(bind(Ambiguous.class), loader));
    }

    @Test public void requiresLayoutAndConstructorEvenWhenShapeMatches() throws Exception {
        assertFalse(ReplySelfDrawTarget.isBindMethod(bind(NoConstructor.class), loader));
        int original = First.resource;
        try {
            First.resource = original + 1;
            assertFalse(ReplySelfDrawTarget.isBindMethod(bind(First.class), loader));
        } finally { First.resource = original; }
    }

    @Test public void resolverRequiresRegistrationFactoryAndBothBindMarkers() throws Exception {
        for (boolean registered : new boolean[] {false, true}) {
            for (boolean factory : new boolean[] {false, true}) {
                ResolvedTarget result = ReplySelfDrawResolver.select(Collections.singletonList(
                        candidate(First.class, registered, factory, "sponsorStyle", "rewardVideoVisibleInLayout")), loader);
                assertEquals(registered && factory, result != null);
            }
        }
        assertNull(ReplySelfDrawResolver.select(Collections.singletonList(
                candidate(First.class, true, true, "sponsorStyle")), loader));
    }

    @Test public void resolverRejectsTwoValidHoldersButDeduplicatesSameMethod() throws Exception {
        ReplySelfDrawResolver.Candidate first = candidate(First.class, true, true,
                "sponsorStyle", "rewardVideoVisibleInLayout");
        assertNotNull(ReplySelfDrawResolver.select(Arrays.asList(first, first), loader));
        assertNull(ReplySelfDrawResolver.select(Arrays.asList(first, candidate(Second.class, true, true,
                "sponsorStyle", "rewardVideoVisibleInLayout")), loader));
    }

    @Test public void cachedMethodMustBelongToVerifiedHolderAndCannotBeClassOnly() throws Exception {
        ResolvedTarget target = ReplySelfDrawResolver.select(Collections.singletonList(candidate(
                First.class, true, true, "sponsorStyle", "rewardVideoVisibleInLayout")), loader);
        assertNotNull(target);
        assertNull(TargetVerifier.verify(target, loader));
        assertNotNull(TargetVerifier.verify(new ResolvedTarget(target.key, "cache",
                DescriptorUtils.classDescriptorOf(Second.class), target.methodDescriptor), loader));
        assertNotNull(TargetVerifier.verify(new ResolvedTarget(target.key, "cache", target.classDescriptor, ""), loader));
    }
}
