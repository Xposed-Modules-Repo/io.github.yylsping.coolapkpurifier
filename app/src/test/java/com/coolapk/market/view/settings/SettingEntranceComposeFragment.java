package com.coolapk.market.view.settings;

import androidx.compose.runtime.Composer;
import com.coolapk.market.view.settings.components.ComposeFragment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class SettingEntranceComposeFragment extends ComposeFragment {
    public final List<Object> groups = new ArrayList<>();
    public final void initData() {
        groups.clear();
        groups.add(new ArrayList<>(Arrays.asList(new Row("native-1", 1, "", activity -> Unit.INSTANCE),
                new Row("native-2", 2, "", activity -> Unit.INSTANCE))));
    }
    public final void render(Row row, Composer composer, int changed) { }
    public static final class Row {
        public final String title;
        public final int icon;
        public final String summary;
        public final Function1<Object, Unit> click;
        public Row(String title, int icon, String summary, Function1<Object, Unit> click) {
            this.title = title; this.icon = icon; this.summary = summary; this.click = click;
        }
    }
}
