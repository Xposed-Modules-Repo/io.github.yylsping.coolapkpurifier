package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class EntityListHooksGenerationTest {
    @Test
    public void sameTopicGateClosesImmediatelyAndRejectsLateOldEvidence() {
        EntityListHooks hooks = new EntityListHooks(null, new ModuleLog(null), new HookLedger());
        ClassLoader first = new ClassLoader() { };
        ClassLoader second = new ClassLoader() { };
        hooks.beginGeneration(1, first);
        hooks.setSameTopicSemanticVerified(1, true);
        assertTrue(hooks.isSameTopicSemanticVerified());

        hooks.beginGeneration(2, second);
        assertFalse(hooks.isSameTopicSemanticVerified());
        hooks.setSameTopicSemanticVerified(1, true);
        assertFalse(hooks.isSameTopicSemanticVerified());
        hooks.setSameTopicSemanticVerified(2, true);
        assertTrue(hooks.isSameTopicSemanticVerified());
    }
}
