package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.os.Bundle;

import com.coolapk.market.view.splash.BoundarySplashActivity;
import com.coolapk.market.view.splash.CoolapkSplashParent;
import com.coolapk.market.view.splash.DirectFrameworkSplashActivity;
import com.coolapk.market.view.splash.InheritedSplashActivity;

import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Splash onCreate location semantics for inherited declarations, mirroring
 * Coolapk 16.5.1 where SplashActivity itself declares nothing and onCreate
 * lives in the obfuscated Coolapk parent.
 */
public final class TargetVerifierInheritedOnCreateTest {
    @Test
    public void inheritedOnCreateIsLocatedInTheCoolapkParent() throws Exception {
        Method expected = CoolapkSplashParent.class.getDeclaredMethod(
                "onCreate", Bundle.class);

        Method located = TargetVerifier.findOnCreate(InheritedSplashActivity.class);

        assertNotNull(located);
        assertEquals(expected, located);
    }

    @Test
    public void searchStopsAtTheCoolapkPackageBoundary() {
        // The parent declares onCreate but lives outside com.coolapk.market.*;
        // hooking it would hook an unrelated/framework hierarchy.
        assertNull(TargetVerifier.findOnCreate(BoundarySplashActivity.class));
    }

    @Test
    public void searchNeverFallsBackToTheFrameworkActivity() {
        // A coolapk activity extending android.app.Activity directly with no
        // Coolapk onCreate anywhere must yield null, so installSpecific()
        // skips (frameworkFallback=true) instead of hooking Activity.onCreate.
        assertNull(TargetVerifier.findOnCreate(DirectFrameworkSplashActivity.class));
    }

    /** Non-Coolapk parent declaring onCreate, mirroring an AndroidX base class. */
    public static class NonCoolapkParentWithOnCreate extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }
}
