package io.github.yylsping.coolapkpurifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/**
 * SEARCH_HOT_RANKINGS conservative implementation: a single exact terminal
 * binder collapse of the search-page hot-list card ("数码热榜/资讯热榜/品牌
 * 实时榜/话题热榜" section). UI-layer only; data and host flow untouched.
 */
final class SearchHotRankingsDelta {
    static final String UI_HOOK_ID = "coolapk-search-hot-rankings-ui";

    private final XposedModule module;
    private final ModuleLog log;
    private final FeatureGate gate;
    private final FeatureExposureLedger exposureLedger;

    private final AtomicInteger observedCount = new AtomicInteger();
    private final AtomicInteger suppressedCount = new AtomicInteger();
    private final AtomicBoolean observedLogged = new AtomicBoolean();
    private final AtomicBoolean suppressedLogged = new AtomicBoolean();

    private volatile HookHandle uiHandle;

    SearchHotRankingsDelta(XposedModule module, ModuleLog log, FeatureGate gate,
                           FeatureExposureLedger exposureLedger) {
        this.module = module;
        this.log = log;
        this.gate = gate;
        this.exposureLedger = exposureLedger;
    }

    synchronized InstallResult install(SearchUiCardTargetSpec spec, ClassLoader loader) {
        if (uiHandle != null) {
            return InstallResult.ALREADY_INSTALLED;
        }
        if (spec == null || loader == null) {
            log.info("search_hot_rankings_hook_registered=false reason=target_missing");
            return InstallResult.TARGET_MISSING;
        }
        SearchUiCardHooks.Runtime runtime = SearchUiCardHooks.resolve(
                spec, loader, log, "hot_rankings_search_page");
        if (runtime == null) {
            return InstallResult.TARGET_MISSING;
        }
        try {
            runtime.binder.setAccessible(true);
            uiHandle = module.hook(runtime.binder)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId(UI_HOOK_ID)
                    .intercept(chain -> SearchUiCardHooks.onBinder(chain,
                            gateEnabled(), runtime,
                            PurifierConfig.Feature.SEARCH_HOT_RANKINGS, exposureLedger,
                            observedCount, suppressedCount,
                            observedLogged, suppressedLogged,
                            log, "hot_rankings_search_page"));
            log.info("search_hot_rankings_hook_registered=true source=manifest_exact"
                    + " layer=hot_rankings_search_page mode=binder_hide"
                    + " descriptor=" + runtime.spec.descriptor()
                    + " carveOut=" + runtime.spec.entityTemplate);
        } catch (Throwable failure) {
            uiHandle = null;
            log.error("search_hot_rankings_hook_registered=false"
                    + " reason=installation_failure layer=hot_rankings_search_page"
                    + " descriptor=" + runtime.spec.descriptor(), failure);
            return InstallResult.INSTALL_FAILED;
        }
        if (exposureLedger != null) {
            exposureLedger.recordHookInstalled(PurifierConfig.Feature.SEARCH_HOT_RANKINGS);
        }
        return InstallResult.INSTALLED;
    }

    private boolean gateEnabled() {
        return gate != null
                && gate.isEffectiveEnabled(PurifierConfig.Feature.SEARCH_HOT_RANKINGS);
    }

    synchronized boolean uiInstalled() {
        return uiHandle != null;
    }

    String summaryLine() {
        return "search_hot_rankings_ui_summary"
                + " observed=" + observedCount.get()
                + " suppressed=" + suppressedCount.get();
    }
}
