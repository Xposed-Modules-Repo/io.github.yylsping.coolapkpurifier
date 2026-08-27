package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.app.Application;
import com.coolapk.market.view.main.MainActivity;
import com.coolapk.market.view.splash.SplashActivity;
import com.coolapk.market.view.splash.SplashAdActivity;
import com.coolapk.market.view.splash.FullScreenAdActivity;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class SplashLifecycleGuardTest {
    private final SplashGate gate = new SplashGate();
    private final AtomicInteger finishes = new AtomicInteger();
    private final SplashLifecycleGuard guard = new SplashLifecycleGuard(gate, () -> true,
            activity -> finishes.incrementAndGet(), new ModuleLog(null));

    @Test public void legacySplashFinishesAfterMain() {
        gate.markMainActivity();
        guard.onActivityCreated(new SplashActivity(), null);
        assertEquals(1, finishes.get());
    }
    @Test public void splashAdFinishes() {
        guard.onActivityCreated(new SplashAdActivity(), null);
        assertEquals(1, finishes.get());
    }
    @Test public void fullscreenFinishesAfterMain() {
        gate.markMainActivity();
        guard.onActivityCreated(new FullScreenAdActivity(), null);
        assertEquals(1, finishes.get());
    }
    @Test public void mainIsPreservedAndEndsLooseStartup() {
        guard.onActivityCreated(new MainActivity(), null);
        assertEquals(0, finishes.get());
        assertTrue(gate.isMainActivitySeen());
    }
    @Test public void unknownSplashNameIsNeverEnough() {
        guard.onActivityCreated(new UnknownSplashActivity(), null);
        gate.markMainActivity();
        guard.onActivityCreated(new UnknownSplashActivity(), null);
        assertEquals(0, finishes.get());
    }
    @Test public void dynamicResolvedClassFinishesAfterMain() {
        gate.addResolvedSplashClass(DynamicAd.class);
        gate.markMainActivity();
        guard.onActivityCreated(new DynamicAd(), null);
        assertEquals(1, finishes.get());
    }
    @Test public void disabledChoicePreservesSplash() {
        new SplashLifecycleGuard(gate, () -> false,
                activity -> finishes.incrementAndGet(), new ModuleLog(null))
                .onActivityCreated(new SplashAdActivity(), null);
        assertEquals(0, finishes.get());
    }
    @Test public void registrationIsIdempotentAndMigratesApplication() {
        FakeApplication first = new FakeApplication();
        FakeApplication second = new FakeApplication();
        assertTrue(guard.install(first));
        assertTrue(guard.install(first));
        assertEquals(1, first.registered);
        assertTrue(guard.install(second));
        assertEquals(1, first.unregistered);
        assertEquals(1, second.registered);
    }
    @Test public void registrationFailureRetainsInstrumentation() {
        FakeApplication application = new FakeApplication();
        application.fail = true;
        assertFalse(guard.install(application));
        assertFalse(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.READY, true, guard.isInstalled()));
        assertEquals("splashLifecycleGuardMissing", FrameworkRetirePolicy.retainReason(
                BootstrapState.READY, true, guard.isInstalled()));
    }
    @Test public void registeredGuardAllowsReadyRetireButNotDegraded() {
        assertTrue(guard.install(new FakeApplication()));
        assertTrue(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.READY, true, guard.isInstalled()));
        assertFalse(FrameworkRetirePolicy.shouldRetireInstrumentationSafety(
                BootstrapState.DEGRADED, true, guard.isInstalled()));
    }
    @Test public void adPackageLegacyAliasesAreExact() {
        assertTrue(gate.isLegacySplashName("com.coolapk.market.view.ad.SplashAdActivity"));
        assertTrue(gate.isLegacySplashName("com.coolapk.market.view.ad.FullScreenAdActivity"));
        assertFalse(gate.isLegacySplashName("other.FullScreenAdActivity"));
    }

    static class UnknownSplashActivity extends Activity { }
    static class DynamicAd extends Activity { }
    static class FakeApplication extends Application {
        int registered;
        int unregistered;
        boolean fail;
        @Override public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks c) {
            if (fail) throw new IllegalStateException("test registration failure");
            registered++;
        }
        @Override public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks c) {
            unregistered++;
        }
    }
}
