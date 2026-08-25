package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Executable;

import org.junit.Test;

import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedInterface.Hooker;

public final class LazyHookRegistryTest {
    @Test
    public void terminalRetireUnhooksEveryDiscoveryHandle() {
        LazyHookRegistry registry = new LazyHookRegistry();
        FakeHandle first = new FakeHandle();
        FakeHandle second = new FakeHandle();
        registry.put(LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG, first);
        registry.put(LazyHookRegistry.HookSite.LOAD_CLASS_TWO_ARG, second);
        registry.activate();

        assertTrue(registry.isActive());
        LazyHookRegistry.RetireResult result = registry.retire();
        assertEquals(2, result.unhookedThisClose);
        assertEquals(0, result.failedThisClose);
        assertEquals(2, result.totalUnhooked);
        assertEquals(0, result.remaining);
        assertTrue(first.unhooked);
        assertTrue(second.unhooked);
        assertFalse(registry.isActive());
        assertFalse(registry.isLogicalEnabled());
        assertEquals(0, registry.retire().unhookedThisClose);
    }

    @Test
    public void failedUnhookRemainsActiveAndVisible() {
        LazyHookRegistry registry = new LazyHookRegistry();
        FakeHandle success = new FakeHandle();
        FakeHandle failure = new FakeHandle(true);
        registry.put(LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG, success);
        registry.put(LazyHookRegistry.HookSite.LOAD_CLASS_TWO_ARG, failure);
        registry.activate();

        LazyHookRegistry.RetireResult result = registry.retire();

        assertEquals(1, result.unhookedThisClose);
        assertEquals(1, result.failedThisClose);
        assertEquals(1, result.remaining);
        assertTrue(result.isActive());
        assertTrue(registry.isActive());
        assertEquals(1, registry.size());
        assertFalse(registry.isLogicalEnabled());
        assertFalse(result.logicalEnabled);
    }

    @Test
    public void partialUnhookRearmRestoresMissingSiteWithoutDuplicatingResidual() {
        LazyHookRegistry registry = new LazyHookRegistry();
        FakeHandle removed = new FakeHandle();
        FakeHandle residual = new FakeHandle(true);
        registry.put(LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG, removed);
        registry.put(LazyHookRegistry.HookSite.LOAD_CLASS_TWO_ARG, residual);
        registry.activate();

        LazyHookRegistry.RetireResult retired = registry.retire();
        assertEquals(1, retired.unhookedThisClose);
        assertEquals(1, retired.failedThisClose);
        assertTrue(registry.missingSites().contains(
                LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG));
        assertFalse(registry.missingSites().contains(
                LazyHookRegistry.HookSite.LOAD_CLASS_TWO_ARG));

        registry.activate();
        FakeHandle replacement = new FakeHandle();
        registry.put(LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG, replacement);

        assertEquals(2, registry.size());
        assertTrue(registry.contains(LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG));
        assertTrue(registry.contains(LazyHookRegistry.HookSite.LOAD_CLASS_TWO_ARG));
        assertTrue(registry.isLogicalEnabled());
    }

    @Test
    public void permanentRetireKeepsFailedResidualInertAndRejectsReactivation() {
        LazyHookRegistry registry = new LazyHookRegistry();
        FakeHandle residual = new FakeHandle(true);
        registry.put(LazyHookRegistry.HookSite.LOAD_CLASS_ONE_ARG, residual);
        registry.activate();

        LazyHookRegistry.RetireResult retired = registry.retirePermanently();
        assertEquals(1, retired.failedThisClose);
        assertTrue(retired.isActive());
        assertFalse(retired.logicalEnabled);

        assertFalse(registry.activate());
        assertTrue(registry.isActive());
        assertFalse(registry.isLogicalEnabled());
    }

    private static final class FakeHandle implements HookHandle {
        private final boolean fail;
        boolean unhooked;

        FakeHandle() {
            this(false);
        }

        FakeHandle(boolean fail) {
            this.fail = fail;
        }

        @Override
        public Executable getExecutable() {
            return null;
        }

        @Override
        public void unhook() {
            if (fail) {
                throw new IllegalStateException("unhook failed");
            }
            unhooked = true;
        }

        @Override
        public String getId() {
            return "fake";
        }

        @Override
        public HookHandle replaceHook(Hooker hooker) {
            return this;
        }
    }
}
