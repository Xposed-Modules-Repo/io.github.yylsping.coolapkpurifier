package com.coolapk.market.view.splash;

import android.app.Activity;
import android.os.Bundle;

/**
 * Test fixture mirroring the real brand splash activity of Coolapk 16.5.1:
 * top-level, historically known name, declares its own onCreate. Only
 * reflection metadata is used; lifecycle methods are never invoked.
 */
public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
