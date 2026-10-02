package io.github.yylsping.manifestgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * e66/wne regression guard (16.6.4): two sibling holders can share the same
 * R.layout reference (search_hot) and pass every structural check while
 * binding different data types — wne carries an instance Card field, e66
 * carries HolderItem. The resolver must use the data-field type as a strict
 * discriminator so a HolderItem holder can never be adopted as the unique
 * CHANGED target.
 */
public final class SearchUiCardResolverTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private static final String[] STUB_CLASSES = {
            ".class public Landroid/view/View;\n.super Ljava/lang/Object;\n",
            ".class public Landroidx/databinding/DataBindingComponent;\n"
                    + ".super Ljava/lang/Object;\n",
            ".class public final Lcom/coolapk/market/model/HolderItem;\n"
                    + ".super Ljava/lang/Object;\n",
            ".class public final Lfake/R$layout;\n.super Ljava/lang/Object;\n"
                    + ".field public static final search_hot:I = 0x7f0a0001\n",
            ".class public abstract Landroidx/recyclerview/widget/RecyclerView$ViewHolder;\n"
                    + ".super Ljava/lang/Object;\n"
                    + ".field public final itemView:Landroid/view/View;\n"
                    + ".method public constructor <init>(Landroid/view/View;)V\n"
                    + "    .locals 0\n"
                    + "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n"
                    + "    return-void\n"
                    + ".end method\n",
            ".class public interface abstract Lcom/coolapk/market/model/Card;\n"
                    + ".super Ljava/lang/Object;\n"
                    + ".method public abstract getEntityTemplate()Ljava/lang/String;\n"
                    + ".end method\n"
                    + ".method public abstract getUrl()Ljava/lang/String;\n"
                    + ".end method\n",
    };

    /** Final holder over the search_hot layout; data field type varies. */
    private static String holder(String descriptor, String dataFieldType) {
        return ".class public final " + descriptor + "\n"
                + ".super Landroidx/recyclerview/widget/RecyclerView$ViewHolder;\n"
                + ".field public static layoutId:I\n"
                + ".field public data:" + dataFieldType + "\n"
                + ".method static constructor <clinit>()V\n"
                + "    .locals 1\n"
                + "    sget v0, Lfake/R$layout;->search_hot:I\n"
                + "    sput v0, " + descriptor + "->layoutId:I\n"
                + "    return-void\n"
                + ".end method\n"
                + ".method public constructor <init>(Landroid/view/View;"
                + "Landroidx/databinding/DataBindingComponent;)V\n"
                + "    .locals 0\n"
                + "    invoke-direct {p0, p1}, "
                + "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;"
                + "-><init>(Landroid/view/View;)V\n"
                + "    return-void\n"
                + ".end method\n"
                + ".method public bind(Ljava/lang/Object;)V\n"
                + "    .locals 0\n"
                + "    return-void\n"
                + ".end method\n";
    }

    private static final String CARD_HOLDER =
            holder("Laaa;", "Lcom/coolapk/market/model/Card;");
    private static final String HOLDER_ITEM_HOLDER =
            holder("Lbbb;", "Lcom/coolapk/market/model/HolderItem;");

    private DexIndex indexOfHolders(String... holders) throws Exception {
        String[] sources = new String[STUB_CLASSES.length + holders.length];
        System.arraycopy(STUB_CLASSES, 0, sources, 0, STUB_CLASSES.length);
        System.arraycopy(holders, 0, sources, STUB_CLASSES.length, holders.length);
        return indexOf(sources);
    }

    private DexIndex indexOf(String... smaliSources) throws Exception {
        File dir = folder.newFolder();
        List<String> files = new ArrayList<>();
        int i = 0;
        for (String source : smaliSources) {
            File smali = new File(dir, "fixture" + (i++) + ".smali");
            Files.write(smali.toPath(), source.getBytes(StandardCharsets.UTF_8));
            files.add(smali.getAbsolutePath());
        }
        File dex = new File(dir, "fixture.dex");
        org.jf.smali.SmaliOptions options = new org.jf.smali.SmaliOptions();
        options.outputDexFile = dex.getAbsolutePath();
        options.apiLevel = 28;
        assertTrue("smali assembly must succeed",
                org.jf.smali.Smali.assemble(options, files));
        return DexIndex.load(dex);
    }

    private static Specs.FeatureSpec wordsSpec() {
        return Specs.parse("searchHotWordsUi", new JSONObject()
                .put("ownerClass", "gone")
                .put("methodName", "bind")
                .put("dataClass", "java.lang.Object")
                .put("cardClass", "com.coolapk.market.model.Card")
                .put("entityTemplateGetter", "getEntityTemplate")
                .put("entityTemplate", "hotSearch")
                .put("urlGetter", "getUrl")
                .put("urlContains", "")
                .put("constructorParams", new JSONArray(Arrays.asList(
                        "android.view.View",
                        "androidx.databinding.DataBindingComponent")))
                .put("viewHolderClass",
                        "androidx.recyclerview.widget.RecyclerView$ViewHolder")
                .put("layoutName", "search_hot"));
    }

    @Test
    public void siblingHolderWithoutCardFieldIsDemotedOutOfTheStrictSet()
            throws Exception {
        DexIndex index = indexOfHolders(CARD_HOLDER, HOLDER_ITEM_HOLDER);
        Resolve.FeatureResult result =
                Resolve.forFeature("searchHotWordsUi").resolve(wordsSpec(), index);

        assertEquals(2, result.candidates.size());
        // Both holders share the layout; the data-field evidence decides the
        // strict set, so the report stays AMBIGUOUS with 1/2 strict.
        assertEquals(Resolve.Verdict.AMBIGUOUS, result.verdict);
        Resolve.Candidate chosen = result.chosenCandidate();
        assertNotNull(chosen);
        assertEquals("Laaa;->bind(Ljava/lang/Object;)V", chosen.descriptor);
        assertTrue(chosen.strictPass);
        assertTrue(chosen.evidence.stream().anyMatch(
                e -> e.startsWith("data field type:")));
        Resolve.Candidate demoted = result.candidates.get(1);
        assertEquals("Lbbb;->bind(Ljava/lang/Object;)V", demoted.descriptor);
        assertFalse(demoted.strictPass);
        assertTrue(demoted.evidence.stream().anyMatch(
                e -> e.startsWith("WARNING no instance field of card type")));
    }

    @Test
    public void holderItemOnlyHolderNeverBecomesTheUniqueTarget() throws Exception {
        DexIndex index = indexOfHolders(HOLDER_ITEM_HOLDER);
        Resolve.FeatureResult result =
                Resolve.forFeature("searchHotWordsUi").resolve(wordsSpec(), index);

        assertEquals(1, result.candidates.size());
        // The e66 failure mode: alone, a HolderItem holder must stay AMBIGUOUS
        // (0/1 strict) instead of being adopted as CHANGED.
        assertEquals(Resolve.Verdict.AMBIGUOUS, result.verdict);
        assertEquals("0/1 candidates pass strict contract", result.verification);
        assertFalse(result.candidates.get(0).strictPass);
    }

    @Test
    public void cardHolderAloneStillResolvesAsChanged() throws Exception {
        DexIndex index = indexOfHolders(CARD_HOLDER);
        Resolve.FeatureResult result =
                Resolve.forFeature("searchHotWordsUi").resolve(wordsSpec(), index);

        assertEquals(Resolve.Verdict.CHANGED, result.verdict);
        assertNotNull(result.chosenCandidate());
        assertNotNull(result.chosenCandidate().draftSpec);
    }
}
