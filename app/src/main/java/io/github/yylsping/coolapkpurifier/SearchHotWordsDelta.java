package io.github.yylsping.coolapkpurifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/**
 * SEARCH_HOT_WORDS conservative implementation.
 *
 * <p>Two independently verified terminal binders, both UI-layer collapses of
 * exact hot-word cards: the home-ranking page (V9_HOME_TAB_RANKING) hot-word
 * capsule and the search-page "热门搜索" section. Each layer resolves on its
 * own; a missing layer degrades the feature to an honest PARTIAL instead of
 * failing the other layer.</p>
 */
final class SearchHotWordsDelta {
    static final String CAPSULE_UI_HOOK_ID = "coolapk-search-hot-words-capsule-ui";
    static final String HOT_WORDS_UI_HOOK_ID = "coolapk-search-hot-words-ui";

    private final XposedModule module;
    private final ModuleLog log;
    private final FeatureGate gate;
    private final FeatureExposureLedger exposureLedger;

    private final AtomicInteger capsuleObservedCount = new AtomicInteger();
    private final AtomicInteger capsuleSuppressedCount = new AtomicInteger();
    private final AtomicInteger hotWordsObservedCount = new AtomicInteger();
    private final AtomicInteger hotWordsSuppressedCount = new AtomicInteger();
    private final AtomicBoolean capsuleObservedLogged = new AtomicBoolean();
    private final AtomicBoolean capsuleSuppressedLogged = new AtomicBoolean();
    private final AtomicBoolean hotWordsObservedLogged = new AtomicBoolean();
    private final AtomicBoolean hotWordsSuppressedLogged = new AtomicBoolean();

    private volatile HookHandle capsuleUiHandle;
    private volatile HookHandle hotWordsUiHandle;

    SearchHotWordsDelta(XposedModule module, ModuleLog log, FeatureGate gate,
                        FeatureExposureLedger exposureLedger) {
        this.module = module;
        this.log = log;
        this.gate = gate;
        this.exposureLedger = exposureLedger;
    }

    synchronized InstallResult install(SearchUiCardTargetSpec capsuleSpec,
                                       SearchUiCardTargetSpec hotWordsSpec,
                                       ClassLoader loader) {
        if (capsuleUiHandle != null || hotWordsUiHandle != null) {
            return InstallResult.ALREADY_INSTALLED;
        }
        if (loader == null || (capsuleSpec == null && hotWordsSpec == null)) {
            log.info("search_hot_words_hook_registered=false reason=target_missing");
            return InstallResult.TARGET_MISSING;
        }

        SearchUiCardHooks.Runtime capsule = SearchUiCardHooks.resolve(
                capsuleSpec, loader, log, "hot_words_capsule");
        SearchUiCardHooks.Runtime hotWords = SearchUiCardHooks.resolve(
                hotWordsSpec, loader, log, "hot_words_search_page");

        int installed = 0;
        if (capsule != null) {
            SearchUiCardHooks.Runtime runtime = capsule;
            try {
                runtime.binder.setAccessible(true);
                capsuleUiHandle = module.hook(runtime.binder)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .setId(CAPSULE_UI_HOOK_ID)
                        .intercept(chain -> SearchUiCardHooks.onBinder(chain,
                                gateEnabled(), runtime,
                                PurifierConfig.Feature.SEARCH_HOT_WORDS, exposureLedger,
                                capsuleObservedCount, capsuleSuppressedCount,
                                capsuleObservedLogged, capsuleSuppressedLogged,
                                log, "hot_words_capsule"));
                installed++;
                log.info("search_hot_words_hook_registered=true source=manifest_exact"
                        + " layer=hot_words_capsule mode=binder_hide"
                        + " descriptor=" + runtime.spec.descriptor()
                        + " carveOut=" + runtime.spec.entityTemplate + "/"
                        + runtime.spec.urlContains);
            } catch (Throwable failure) {
                capsuleUiHandle = null;
                log.error("search_hot_words_hook_registered=false"
                        + " reason=installation_failure layer=hot_words_capsule"
                        + " descriptor=" + runtime.spec.descriptor(), failure);
            }
        }
        if (hotWords != null) {
            SearchUiCardHooks.Runtime runtime = hotWords;
            try {
                runtime.binder.setAccessible(true);
                hotWordsUiHandle = module.hook(runtime.binder)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .setId(HOT_WORDS_UI_HOOK_ID)
                        .intercept(chain -> SearchUiCardHooks.onBinder(chain,
                                gateEnabled(), runtime,
                                PurifierConfig.Feature.SEARCH_HOT_WORDS, exposureLedger,
                                hotWordsObservedCount, hotWordsSuppressedCount,
                                hotWordsObservedLogged, hotWordsSuppressedLogged,
                                log, "hot_words_search_page"));
                installed++;
                log.info("search_hot_words_hook_registered=true source=manifest_exact"
                        + " layer=hot_words_search_page mode=binder_hide"
                        + " descriptor=" + runtime.spec.descriptor()
                        + " carveOut=" + runtime.spec.entityTemplate);
            } catch (Throwable failure) {
                hotWordsUiHandle = null;
                log.error("search_hot_words_hook_registered=false"
                        + " reason=installation_failure layer=hot_words_search_page"
                        + " descriptor=" + runtime.spec.descriptor(), failure);
            }
        }

        if (installed > 0 && exposureLedger != null) {
            exposureLedger.recordHookInstalled(PurifierConfig.Feature.SEARCH_HOT_WORDS);
        }
        log.info("search_hot_words_coverage="
                + (installed == 2 ? "FULL" : installed == 1 ? "PARTIAL" : "NONE")
                + " capsule=" + (capsuleUiHandle != null ? "AVAILABLE" : "UNAVAILABLE")
                + " searchPage=" + (hotWordsUiHandle != null ? "AVAILABLE" : "UNAVAILABLE"));
        if (installed == 0) {
            return InstallResult.TARGET_MISSING;
        }
        return installed == 2 ? InstallResult.INSTALLED : InstallResult.PARTIAL;
    }

    private boolean gateEnabled() {
        return gate != null
                && gate.isEffectiveEnabled(PurifierConfig.Feature.SEARCH_HOT_WORDS);
    }

    synchronized boolean capsuleUiInstalled() {
        return capsuleUiHandle != null;
    }

    synchronized boolean hotWordsUiInstalled() {
        return hotWordsUiHandle != null;
    }

    String summaryLine() {
        return "search_hot_words_ui_summary"
                + " capsule_observed=" + capsuleObservedCount.get()
                + " capsule_suppressed=" + capsuleSuppressedCount.get()
                + " search_page_observed=" + hotWordsObservedCount.get()
                + " search_page_suppressed=" + hotWordsSuppressedCount.get();
    }
}
