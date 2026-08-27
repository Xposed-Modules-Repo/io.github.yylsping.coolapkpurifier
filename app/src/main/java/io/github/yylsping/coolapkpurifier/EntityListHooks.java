package io.github.yylsping.coolapkpurifier;

import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/**
 * Feed filter hooks. 2.0.1 covered every (List, boolean) -> List transformer
 * in both EntityAdHelper and EntityListFragment; this class keeps that shape:
 * every newly resolved business entry is hooked additively and existing hooks
 * are never removed, so partial resolutions cannot shrink coverage.
 */
final class EntityListHooks {
    private final XposedModule module;
    private final ModuleLog log;
    private final HookLedger ledger;
    private final EntityClassifier classifier = new EntityClassifier();
    private final EntityListFilter filter = new EntityListFilter(classifier);
    private final HookedFeedRegistry hooked = new HookedFeedRegistry();
    private final Map<Method, HookHandle> handles = new HashMap<>();
    private final Map<View, ReplyViewState> collapsedReplyViews = new WeakHashMap<>();
    private volatile boolean accessorsComplete;
    private volatile ClassLoader activeLoader;
    private volatile long generation;

    EntityListHooks(XposedModule module, ModuleLog log, HookLedger ledger) {
        this.module = module;
        this.log = log;
        this.ledger = ledger;
    }

    void setConfig(PurifierConfig config, int coolapkMajor) {
        classifier.setConfig(config);
        classifier.setCoolapkMajor(coolapkMajor);
    }

    synchronized void setSameTopicSemanticVerified(long expectedGeneration, boolean verified) {
        if (expectedGeneration == generation) {
            classifier.setSameTopicSemanticVerified(verified);
        }
    }

    boolean isSameTopicSemanticVerified() {
        return classifier.isSameTopicSemanticVerified();
    }

    synchronized void beginGeneration(long nextGeneration, ClassLoader loader) {
        if (nextGeneration < generation) {
            log.info("entity-list generation rollback rejected current=" + generation
                    + " attempted=" + nextGeneration);
            return;
        }
        generation = nextGeneration;
        activeLoader = loader;
        accessorsComplete = false;
        classifier.setSameTopicSemanticVerified(false);
        classifier.setAccessors(new EntityAccessors(null, null, null, null));
    }

    synchronized void updateAccessors(Map<String, ResolvedTarget> targets, ClassLoader loader,
                                      long expectedGeneration) {
        if (expectedGeneration != generation || loader != activeLoader) {
            return;
        }
        EntityAccessors accessors = EntityAccessors.fromTargets(targets, loader);
        classifier.setAccessors(accessors);
        accessorsComplete = accessors.isComplete();
    }

    boolean isAccessorsComplete() {
        return accessorsComplete;
    }

    /** Live-anchor probe for coverage settling; sees installed hooks only. */
    boolean hasHookedInClass(String classDescriptor) {
        return hooked.hasHookedInClass(classDescriptor);
    }

    /** Live per-method probe for the coverage snapshot (installed hooks only). */
    boolean isHooked(Method method) {
        return hooked.contains(method);
    }

    /**
     * Hooks one more list transformer. Returns how many hooks were added by
     * this call (0 when the method was already hooked or is unusable).
     */
    synchronized int install(Method method) {
        if (method == null || hooked.contains(method)) {
            return 0;
        }
        try {
            HookHandle handle = module.hook(method)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .setId("coolapk-feed-filter")
                    .intercept(chain -> {
                        Object original = chain.proceed();
                        if (!(original instanceof List<?>)) {
                            return original;
                        }
                        try {
                            List<?> source = (List<?>) original;
                            List<?> filtered = filter.filter(source);
                            if (filtered != source) {
                                log.info("removed " + (source.size() - filtered.size())
                                        + " filtered item(s) via " + method);
                            }
                            return filtered;
                        } catch (Throwable throwable) {
                            log.error("ad filtering failed; preserving original list", throwable);
                            return original;
                        }
                    });
            handles.put(method, handle);
            hooked.add(method);
            ledger.record(HookLedger.Layer.BUSINESS, "feed",
                    "feed-filter-" + Integer.toHexString(method.toGenericString().hashCode()),
                    method.toGenericString());
            log.info("installed feed filter hook method=" + method);
            return 1;
        } catch (Throwable throwable) {
            log.error("feed filter hook install failed method=" + method, throwable);
            return 0;
        }
    }

    synchronized int installAll(Collection<Method> methods) {
        int installed = 0;
        if (methods == null) {
            return 0;
        }
        for (Method method : methods) {
            installed += install(method);
        }
        return installed;
    }

    /**
     * Covers detail reply ads inserted after the normal list transformer. The
     * dedicated 15/16 holder is stable even when its bind methods are obfuscated.
     */
    synchronized int installReplyHolder(Class<?> holderClass) {
        if (!TargetVerifier.isReplyHolderClass(holderClass)) {
            return 0;
        }
        int installed = 0;
        for (Method method : holderClass.getDeclaredMethods()) {
            if (!TargetVerifier.isReplyBindMethod(method)) {
                continue;
            }
            if (handles.containsKey(method)) {
                continue;
            }
            try {
                HookHandle handle = module.hook(method)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .setId("coolapk-reply-sponsor-holder-"
                                + Integer.toHexString(method.toGenericString().hashCode()))
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            try {
                                boolean sponsored = classifier.shouldRemove(chain.getArg(0),
                                        EntityClassifier.Context.REPLY);
                                updateReplyHolder(chain.getThisObject(), sponsored);
                                if (sponsored) {
                                    log.info("removed reply sponsor via " + method);
                                }
                            } catch (Throwable throwable) {
                                log.error("reply sponsor holder filtering failed", throwable);
                            }
                            return result;
                        });
                handles.put(method, handle);
                installed++;
                ledger.record(HookLedger.Layer.BUSINESS, "feed",
                        "reply-holder-" + Integer.toHexString(
                                method.toGenericString().hashCode()),
                        method.toGenericString());
                log.info("installed reply sponsor holder hook method=" + method);
            } catch (Throwable throwable) {
                log.error("reply sponsor holder hook install failed method=" + method,
                        throwable);
            }
        }
        return installed;
    }

    private void updateReplyHolder(Object holder, boolean sponsored) throws Exception {
        Field itemViewField = holder.getClass().getField("itemView");
        Object candidate = itemViewField.get(holder);
        if (!(candidate instanceof View)) {
            return;
        }
        View itemView = (View) candidate;
        if (!sponsored) {
            ReplyViewState state = collapsedReplyViews.remove(itemView);
            if (state != null) {
                state.restore(itemView);
            }
            return;
        }
        if (!collapsedReplyViews.containsKey(itemView)) {
            collapsedReplyViews.put(itemView, ReplyViewState.capture(itemView));
        }
        itemView.setVisibility(View.GONE);
        itemView.setMinimumHeight(0);
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        if (params != null && params.height != 0) {
            params.height = 0;
            itemView.setLayoutParams(params);
        }
    }

    synchronized int hookedMethodCount() {
        return hooked.sizeForLoader(activeLoader);
    }

    synchronized long generation() {
        return generation;
    }

    private static final class ReplyViewState {
        private final int visibility;
        private final int minimumHeight;
        private final int layoutHeight;

        private ReplyViewState(int visibility, int minimumHeight, int layoutHeight) {
            this.visibility = visibility;
            this.minimumHeight = minimumHeight;
            this.layoutHeight = layoutHeight;
        }

        static ReplyViewState capture(View view) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            return new ReplyViewState(view.getVisibility(), view.getMinimumHeight(),
                    params == null ? Integer.MIN_VALUE : params.height);
        }

        void restore(View view) {
            view.setVisibility(visibility);
            view.setMinimumHeight(minimumHeight);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null && layoutHeight != Integer.MIN_VALUE
                    && params.height != layoutHeight) {
                params.height = layoutHeight;
                view.setLayoutParams(params);
            }
        }
    }
}
