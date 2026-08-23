package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class PurifierConfigTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private static final CacheAtomicWriter.ReplaceOperation REPLACE = (temp, destination) -> {
        try {
            Files.copy(temp.toPath(), destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    };

    @Test
    public void firstLoadPersistsExactlyThreeLegacyDefaults() throws Exception {
        PurifierConfig config = new PurifierConfig(folder.getRoot(), REPLACE, null);

        assertTrue(config.isEnabled(PurifierConfig.Feature.SPLASH));
        assertTrue(config.isEnabled(PurifierConfig.Feature.FEED_SPONSOR));
        assertTrue(config.isEnabled(PurifierConfig.Feature.REPLY_SPONSOR));
        assertFalse(config.isEnabled(PurifierConfig.Feature.AUTO_COMMENT));
        assertFalse(config.isEnabled(PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND));
        assertFalse(config.isEnabled(PurifierConfig.Feature.RELATED_DATA));
        assertFalse(config.isEnabled(PurifierConfig.Feature.SAME_TOPIC_FEED));
        assertFalse(config.isEnabled(PurifierConfig.Feature.DETAIL_SPONSOR));
        assertEquals(PurifierConfig.PendingKind.DEFAULT, config.pendingKind());
        assertFalse(config.hasNonDefaultSelections());
        assertTrue(folder.getRoot().toPath().resolve(PurifierConfig.FILE_NAME).toFile().isFile());

        PurifierConfig reloaded = new PurifierConfig(folder.getRoot(), REPLACE, null);
        assertEquals(PurifierConfig.PendingKind.DEFAULT, reloaded.pendingKind());
        assertTrue(reloaded.isEnabled(PurifierConfig.Feature.SPLASH));
        assertFalse(reloaded.isEnabled(PurifierConfig.Feature.AUTO_COMMENT));
    }

    @Test
    public void enablingIssueOptionIsImmediatelyDurableAndMarksSelectionPending()
            throws Exception {
        PurifierConfig config = new PurifierConfig(folder.getRoot(), REPLACE, null);

        assertTrue(config.setEnabled(PurifierConfig.Feature.AUTO_COMMENT, true));
        assertEquals(PurifierConfig.PendingKind.SELECTION, config.pendingKind());
        assertTrue(config.hasNonDefaultSelections());

        PurifierConfig reloaded = new PurifierConfig(folder.getRoot(), REPLACE, null);
        assertTrue(reloaded.isEnabled(PurifierConfig.Feature.AUTO_COMMENT));
        assertEquals(PurifierConfig.PendingKind.SELECTION, reloaded.pendingKind());
    }

    @Test
    public void issueOptionsAreIneffectiveBelowCoolapk15WithoutLosingChoice()
            throws Exception {
        PurifierConfig config = new PurifierConfig(folder.getRoot(), REPLACE, null);
        config.setEnabled(PurifierConfig.Feature.RELATED_DATA, true);

        assertFalse(config.isEffectiveEnabled(PurifierConfig.Feature.RELATED_DATA, 14));
        assertTrue(config.isEffectiveEnabled(PurifierConfig.Feature.RELATED_DATA, 15));
        assertTrue(config.isEnabled(PurifierConfig.Feature.RELATED_DATA));
    }

    @Test
    public void adaptationMarkerIsPersisted() throws Exception {
        PurifierConfig config = new PurifierConfig(folder.getRoot(), REPLACE, null);
        assertTrue(config.markAdapted());

        PurifierConfig reloaded = new PurifierConfig(folder.getRoot(), REPLACE, null);
        assertEquals(PurifierConfig.PendingKind.NONE, reloaded.pendingKind());
    }

    @Test
    public void failedWriteRollsBackInMemorySelection() throws Exception {
        PurifierConfig seed = new PurifierConfig(folder.getRoot(), REPLACE, null);
        assertFalse(seed.isEnabled(PurifierConfig.Feature.AUTO_COMMENT));

        PurifierConfig failing = new PurifierConfig(
                folder.getRoot(), (temp, destination) -> false, null);
        assertFalse(failing.setEnabled(PurifierConfig.Feature.AUTO_COMMENT, true));
        assertFalse(failing.isEnabled(PurifierConfig.Feature.AUTO_COMMENT));
    }
}
