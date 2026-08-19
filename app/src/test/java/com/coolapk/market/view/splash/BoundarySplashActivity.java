package com.coolapk.market.view.splash;

import io.github.yylsping.coolapkpurifier.TargetVerifierInheritedOnCreateTest.NonCoolapkParentWithOnCreate;

/**
 * Coolapk splash child whose onCreate-declaring parent lives OUTSIDE the
 * com.coolapk.market.* namespace: the verifier must stop at the package
 * boundary and return null instead of hooking a framework-side onCreate.
 */
public class BoundarySplashActivity extends NonCoolapkParentWithOnCreate {
}
