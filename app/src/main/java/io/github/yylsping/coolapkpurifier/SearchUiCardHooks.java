package io.github.yylsping.coolapkpurifier;

import android.view.View;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface.Chain;

/**
 * Shared machinery for the search hot-word / hot-ranking UI terminals: exact
 * binder contract verification, render-time card classification and the
 * collapse/restore interception body. UI-layer only — the bound data and the
 * host invocation are never modified.
 */
final class SearchUiCardHooks {
    private SearchUiCardHooks() {
    }

    static final class Runtime {
        final SearchUiCardTargetSpec spec;
        final Method binder;
        final Class<?> cardClass;
        final Method templateGetter;
        final Method urlGetter;
        final RelatedDataDelta.HolderController controller;

        Runtime(SearchUiCardTargetSpec spec, Method binder, Class<?> cardClass,
                Method templateGetter, Method urlGetter,
                RelatedDataDelta.HolderController controller) {
            this.spec = spec;
            this.binder = binder;
            this.cardClass = cardClass;
            this.templateGetter = templateGetter;
            this.urlGetter = urlGetter;
            this.controller = controller;
        }
    }

    /** Resolves and verifies one terminal; logs and returns null when unavailable. */
    static Runtime resolve(SearchUiCardTargetSpec spec, ClassLoader loader,
                           ModuleLog log, String layerLabel) {
        if (spec == null) {
            log.info("search_ui_target_available=false layer=" + layerLabel
                    + " reason=target_missing");
            return null;
        }
        try {
            Class<?> owner = Class.forName(spec.ownerClass, false, loader);
            Class<?> dataClass = Class.forName(spec.dataClass, false, loader);
            Class<?> cardClass = Class.forName(spec.cardClass, false, loader);
            Method binder = owner.getDeclaredMethod(spec.methodName, dataClass);
            if (!isExactTarget(spec, binder, loader)) {
                log.info("search_ui_target_available=false layer=" + layerLabel
                        + " reason=contract_mismatch"
                        + (hasCardInstanceField(owner, cardClass)
                                ? "" : " detail=missing_card_instance_field")
                        + " owner=" + spec.ownerClass
                        + " cardClass=" + spec.cardClass
                        + " descriptor=" + spec.descriptor());
                return null;
            }
            Method templateGetter = cardClass.getMethod(spec.entityTemplateGetter);
            Method urlGetter = cardClass.getMethod(spec.urlGetter);
            templateGetter.setAccessible(true);
            urlGetter.setAccessible(true);
            return new Runtime(spec, binder, cardClass, templateGetter, urlGetter,
                    new RelatedDataDelta.HolderController(owner.getField("itemView")));
        } catch (Throwable failure) {
            log.info("search_ui_target_available=false layer=" + layerLabel
                    + " reason=target_missing descriptor=" + spec.descriptor()
                    + " error=" + failure);
            return null;
        }
    }

    static boolean isExactTarget(SearchUiCardTargetSpec spec, Method method,
                                 ClassLoader loader) {
        if (spec == null || method == null || loader == null
                || !spec.ownerClass.equals(method.getDeclaringClass().getName())
                || !spec.methodName.equals(method.getName())) {
            return false;
        }
        try {
            Class<?> owner = method.getDeclaringClass();
            Class<?> data = Class.forName(spec.dataClass, false, loader);
            int flags = method.getModifiers();
            if (!Modifier.isFinal(owner.getModifiers())
                    || !Modifier.isPublic(flags) || Modifier.isStatic(flags)
                    || Modifier.isAbstract(flags)
                    || method.getReturnType() != void.class
                    || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != data) {
                return false;
            }
            Class<?>[] ctorTypes = new Class<?>[spec.constructorParams.size()];
            for (int i = 0; i < ctorTypes.length; i++) {
                ctorTypes[i] = Class.forName(spec.constructorParams.get(i), false, loader);
            }
            if (!Modifier.isPublic(
                    owner.getDeclaredConstructor(ctorTypes).getModifiers())) {
                return false;
            }
            java.lang.reflect.Field itemView = owner.getField("itemView");
            if (itemView.getType() != View.class
                    || !spec.viewHolderClass.equals(
                    itemView.getDeclaringClass().getName())) {
                return false;
            }
            Class<?> card = Class.forName(spec.cardClass, false, loader);
            Method template = card.getMethod(spec.entityTemplateGetter);
            Method url = card.getMethod(spec.urlGetter);
            if (template.getParameterCount() != 0
                    || template.getReturnType() != String.class
                    || url.getParameterCount() != 0
                    || url.getReturnType() != String.class) {
                return false;
            }
            // e66/wne guard (16.6.4): sibling holders can share layout and
            // holder contract while binding HolderItem instead of Card. The
            // holder must declare its own non-static instance field of the
            // exact card type; all three validated profiles declare it on the
            // owner class (parent is the shared i5/ob base), so inherited
            // fields are deliberately not accepted.
            return hasCardInstanceField(owner, card);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Exact-type, non-static, owner-declared instance field check. */
    static boolean hasCardInstanceField(Class<?> owner, Class<?> cardClass) {
        if (owner == null || cardClass == null) {
            return false;
        }
        for (java.lang.reflect.Field field : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    && field.getType() == cardClass) {
                return true;
            }
        }
        return false;
    }

    /** Render-time classification of the bound card; never mutates anything. */
    static boolean matches(Runtime runtime, Object value)
            throws ReflectiveOperationException {
        if (runtime == null || value == null || !runtime.cardClass.isInstance(value)
                || !runtime.spec.entityTemplate.equals(
                runtime.templateGetter.invoke(value))) {
            return false;
        }
        if (!runtime.spec.urlContains.isEmpty()) {
            Object url = runtime.urlGetter.invoke(value);
            return url instanceof String && ((String) url).contains(runtime.spec.urlContains);
        }
        return true;
    }

    /**
     * Conservative interception body: restore recycled state, proceed
     * untouched, classify the bound card, collapse only on an exact match.
     */
    static Object onBinder(Chain chain, boolean enabled, Runtime runtime,
                           PurifierConfig.Feature feature,
                           FeatureExposureLedger exposureLedger,
                           AtomicInteger observedCounter, AtomicInteger suppressedCounter,
                           AtomicBoolean observedLogged, AtomicBoolean suppressedLogged,
                           ModuleLog log, String layerLabel) throws Throwable {
        Object holder = chain.getThisObject();
        try {
            runtime.controller.update(holder, false);
        } catch (Throwable restoreFailure) {
            log.error("search_ui recycled holder restore failed layer=" + layerLabel
                    + "; continuing host binder", restoreFailure);
        }
        Object original = chain.proceed();
        try {
            if (!matches(runtime, chain.getArg(0))) {
                return original;
            }
            if (exposureLedger != null) {
                exposureLedger.recordEntered(feature);
                exposureLedger.recordSampleSeen(feature);
            }
            observedCounter.incrementAndGet();
            if (observedLogged.compareAndSet(false, true)) {
                log.info("search_ui_observed=true layer=" + layerLabel
                        + " enabled=" + enabled
                        + " descriptor=" + runtime.spec.descriptor());
            }
            if (enabled) {
                runtime.controller.update(holder, true);
                runtime.controller.collapseResidualSpacingAfterLayout(
                        holder, log, layerLabel);
                int suppressed = suppressedCounter.incrementAndGet();
                if (exposureLedger != null) {
                    exposureLedger.recordModified(feature);
                }
                if (suppressedLogged.compareAndSet(false, true)) {
                    log.info("search_ui_suppressed=true layer=" + layerLabel
                            + " count=" + suppressed
                            + " visibility=gone minimumHeight=0 layoutHeight=0"
                            + " residualSpacingScheduled=true");
                }
            }
        } catch (Throwable failure) {
            try {
                runtime.controller.update(holder, false);
            } catch (Throwable restoreFailure) {
                failure.addSuppressed(restoreFailure);
            }
            log.error("search_ui decision failed layer=" + layerLabel
                    + "; preserving holder state", failure);
        }
        return original;
    }
}
