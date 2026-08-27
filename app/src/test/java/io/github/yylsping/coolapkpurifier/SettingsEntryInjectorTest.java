package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import com.coolapk.market.view.settings.SettingEntranceComposeFragment;
import com.coolapk.market.view.settings.SettingEntranceComposeFragment.Row;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import static org.junit.Assert.*;

public final class SettingsEntryInjectorTest {
    @Test public void insertsNativeModelAsFirstLazyColumnGroupAndPreservesNativeObjects() throws Exception {
        SettingEntranceComposeFragment fragment = new SettingEntranceComposeFragment();
        fragment.initData();
        Object originalGroup = fragment.groups.get(0);
        Object originalRow = ((List<?>) originalGroup).get(0);
        assertTrue(SettingsEntryInjector.inject(fragment, 42, activity -> { }));
        assertEquals(2, fragment.groups.size());
        assertSame(originalGroup, fragment.groups.get(1));
        assertSame(originalRow, ((List<?>) fragment.groups.get(1)).get(0));
        List<?> entryGroup = (List<?>) fragment.groups.get(0);
        assertEquals(1, entryGroup.size());
        assertEquals("酷安净化", ((Row) entryGroup.get(0)).title);
        assertEquals(42, ((Row) entryGroup.get(0)).icon);
        assertSame(Row.class, entryGroup.get(0).getClass());
    }

    @Test public void repeatedInjectionAndHostDataRebuildDoNotAccumulateEntries() throws Exception {
        SettingEntranceComposeFragment fragment = new SettingEntranceComposeFragment();
        fragment.initData();
        assertTrue(SettingsEntryInjector.inject(fragment, 42, activity -> { }));
        assertTrue(SettingsEntryInjector.inject(fragment, 42, activity -> { }));
        assertEquals(2, fragment.groups.size());
        fragment.initData();
        assertEquals(1, fragment.groups.size());
        assertTrue(SettingsEntryInjector.inject(fragment, 42, activity -> { }));
        assertEquals(2, fragment.groups.size());
    }

    @Test public void nativeCallbackUsesPassedActivityAndReturnsHostUnit() throws Exception {
        SettingEntranceComposeFragment fragment = new SettingEntranceComposeFragment();
        fragment.initData();
        List<Activity> opened = new ArrayList<>();
        assertTrue(SettingsEntryInjector.inject(fragment, 42, opened::add));
        Row row = (Row) ((List<?>) fragment.groups.get(0)).get(0);
        Activity owner = new Activity();
        assertSame(Unit.INSTANCE, row.click.invoke(owner));
        assertSame(owner, opened.get(0));
        row.click.invoke(new Object());
        assertEquals(1, opened.size());
        assertTrue(row.click.equals(row.click));
        assertFalse(row.click.equals(new Object()));
    }

    @Test public void unrelatedFragmentsAndMixedOrEmptyDataAreRejectedWithoutMutation() throws Exception {
        assertNull(SettingsEntryInjector.findInitData(Lookalike.class));
        SettingEntranceComposeFragment fragment = new SettingEntranceComposeFragment();
        assertFalse(SettingsEntryInjector.inject(fragment, 42, activity -> { }));
        fragment.initData();
        assertFalse(SettingsEntryInjector.inject(fragment, 0, activity -> { }));
        assertEquals(1, fragment.groups.size());
        fragment.groups.add(Arrays.asList("not-a-native-row"));
        Object first = fragment.groups.get(0);
        assertFalse(SettingsEntryInjector.inject(fragment, 42, activity -> { }));
        assertEquals(2, fragment.groups.size());
        assertSame(first, fragment.groups.get(0));
    }

    @Test public void ambiguousListFieldsAndMutableModelLookalikesAreRejected() throws Exception {
        assertNull(SettingsEntryInjector.listField(Ambiguous.class));
        assertNull(SettingsEntryInjector.modelConstructor(MutableRow.class, Function1.class));
        assertNotNull(SettingsEntryInjector.modelConstructor(Row.class, Function1.class));
    }

    public static class Lookalike { public final void initData() { } }
    public static class Ambiguous { public List<Object> a, b; public final void initData() { } }
    public static final class MutableRow {
        public String title, summary;
        public int icon;
        public Function1<Object, Unit> click;
        public MutableRow(String title, int icon, String summary, Function1<Object, Unit> click) { }
    }
}
