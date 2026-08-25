package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class RuntimeConfigurationTransactionTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void initializerCannotWriteCapturedG1AfterLoaderMovesToG2() throws Exception {
        ClassLoader l1 = loader();
        ClassLoader l2 = loader();
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        epoch.transition(l1);
        RuntimeConfigurationTransaction transaction =
                new RuntimeConfigurationTransaction(epoch);
        FeatureInstallState installState = new FeatureInstallState();
        installState.beginGeneration(1L);
        EntityListHooks entityHooks = new EntityListHooks(null, new ModuleLog(null));
        entityHooks.beginGeneration(1L, l1);
        FeatureHooks localFeatureHooks = featureHooks(entityHooks, installState);
        AtomicLong initializerObserved = new AtomicLong();
        CountDownLatch initializerBuilt = new CountDownLatch(1);
        CountDownLatch resumeInitializer = new CountDownLatch(1);

        Thread initializer = new Thread(() -> {
            initializerObserved.set(epoch.generation());
            initializerBuilt.countDown();
            await(resumeInitializer);
            transaction.publish(() -> false,
                    (generation, activeLoader, activated, terminal) -> {
                        assertTrue(activated);
                        assertFalse(terminal);
                        localFeatureHooks.beginGeneration(generation, activeLoader);
                        entityHooks.beginGeneration(generation, activeLoader);
                    });
        }, "configuration-initializer");
        initializer.start();
        assertTrue(initializerBuilt.await(5, TimeUnit.SECONDS));
        assertEquals(1L, initializerObserved.get());

        epoch.exclusive(() -> {
            ResolutionEpoch.Transition transition = epoch.transition(l2);
            assertTrue(transition.changed);
            installState.beginGeneration(transition.generation);
            entityHooks.beginGeneration(transition.generation, transition.loader);
        });
        resumeInitializer.countDown();
        initializer.join(5_000L);

        assertEquals(2L, epoch.generation());
        assertSame(l2, epoch.loader());
        assertEquals(2L, installState.generation());
        assertEquals(2L, localFeatureHooks.generation());
        assertEquals(2L, entityHooks.generation());

        // Component-level monotonic guards remain defensive against a future
        // caller accidentally replaying the initializer's old observation.
        localFeatureHooks.beginGeneration(1L, l1);
        entityHooks.beginGeneration(1L, l1);
        assertEquals(2L, localFeatureHooks.generation());
        assertEquals(2L, entityHooks.generation());
    }

    @Test
    public void terminalDuringInitializationCannotResurrectLazyDiscovery()
            throws Exception {
        ClassLoader l1 = loader();
        ResolutionEpoch epoch = new ResolutionEpoch(loader());
        epoch.transition(l1);
        RuntimeConfigurationTransaction transaction =
                new RuntimeConfigurationTransaction(epoch);
        FeatureHooks featureHooks = featureHooks(
                new EntityListHooks(null, new ModuleLog(null)),
                new FeatureInstallState());
        CountDownLatch publishedBeforeLazyInstall = new CountDownLatch(1);
        CountDownLatch resumeInitializer = new CountDownLatch(1);

        Thread initializer = new Thread(() -> {
            transaction.publish(() -> false,
                    (generation, activeLoader, activated, terminal) ->
                            featureHooks.beginGeneration(generation, activeLoader));
            publishedBeforeLazyInstall.countDown();
            await(resumeInitializer);
            featureHooks.installLazyResolvers();
        }, "configuration-initializer");
        initializer.start();
        assertTrue(publishedBeforeLazyInstall.await(5, TimeUnit.SECONDS));

        LazyHookRegistry.RetireResult cleanup =
                featureHooks.retireLazyResolversPermanently("terminal:test");
        assertFalse(cleanup.logicalEnabled);
        resumeInitializer.countDown();
        initializer.join(5_000L);

        assertFalse(featureHooks.areLazyResolversLogicallyEnabled());
        assertFalse(featureHooks.hasActiveLazyResolvers());

        // Also cover terminal winning before publication: the newly built
        // hooks are permanently disabled inside the epoch before publishing.
        AtomicBoolean terminal = new AtomicBoolean(true);
        FeatureHooks lateFeatureHooks = featureHooks(
                new EntityListHooks(null, new ModuleLog(null)),
                new FeatureInstallState());
        transaction.publish(terminal::get,
                (generation, activeLoader, activated, isTerminal) -> {
                    assertTrue(isTerminal);
                    lateFeatureHooks.disableLazyDiscoveryBeforePublication();
                });
        lateFeatureHooks.beginGeneration(epoch.generation(), epoch.loader());
        lateFeatureHooks.installLazyResolvers();
        assertFalse(lateFeatureHooks.areLazyResolversLogicallyEnabled());
        assertFalse(lateFeatureHooks.hasActiveLazyResolvers());
    }

    private FeatureHooks featureHooks(EntityListHooks entityHooks,
                                      FeatureInstallState installState)
            throws Exception {
        ModuleLog log = new ModuleLog(null);
        PurifierConfig config = new PurifierConfig(
                temporaryFolder.newFolder(), CacheAtomicWriter.RENAME_REPLACE, log);
        HookInstallPlan plan = HookInstallPlan.from(config, 16);
        return new FeatureHooks(null, log, config, 16, entityHooks, plan, installState);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static ClassLoader loader() {
        return new ClassLoader(null) {
        };
    }
}
