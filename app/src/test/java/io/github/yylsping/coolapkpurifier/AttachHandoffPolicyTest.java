package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AttachHandoffPolicyTest {

    private static AttachHandoffPolicy.HandoffState state(boolean context,
                                                          boolean config,
                                                          boolean lifecycle) {
        return new AttachHandoffPolicy.HandoffState(context, config, lifecycle);
    }

    @Test
    public void retiresOnlyAfterCompleteHandoff() {
        assertTrue(AttachHandoffPolicy.canRetireAttach(state(true, true, true)));
    }

    @Test
    public void missingContextBlocksRetire() {
        assertFalse(AttachHandoffPolicy.canRetireAttach(state(false, true, true)));
        assertEquals("contextMissing",
                AttachHandoffPolicy.missingCondition(state(false, true, true)));
    }

    @Test
    public void missingConfigBlocksRetire() {
        assertFalse(AttachHandoffPolicy.canRetireAttach(state(true, false, true)));
        assertEquals("configMissing",
                AttachHandoffPolicy.missingCondition(state(true, false, true)));
    }

    @Test
    public void missingSettingsLifecycleBlocksRetire() {
        assertFalse(AttachHandoffPolicy.canRetireAttach(state(true, true, false)));
        assertEquals("settingsLifecycleMissing",
                AttachHandoffPolicy.missingCondition(state(true, true, false)));
    }

    @Test
    public void completeHandoffHasNoMissingCondition() {
        assertNull(AttachHandoffPolicy.missingCondition(state(true, true, true)));
    }
}
