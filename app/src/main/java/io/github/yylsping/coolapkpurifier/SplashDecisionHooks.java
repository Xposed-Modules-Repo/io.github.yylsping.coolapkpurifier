package io.github.yylsping.coolapkpurifier;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedModule;

/** One strictly resolved Coolapk business method. No Activity or SDK interception. */
final class SplashDecisionHooks {
    private final XposedModule module;
    private final ModuleLog log;
    private final HookLedger ledger;
    private final Map<Method, HookHandle> installed = new LinkedHashMap<>();
    private final AtomicInteger observations = new AtomicInteger();

    SplashDecisionHooks(XposedModule module, ModuleLog log, HookLedger ledger) {
        this.module = module;
        this.log = log;
        this.ledger = ledger;
    }

    synchronized boolean install(ResolvedTarget target, ClassLoader loader, BooleanSupplier enabled) {
        if (!SplashDecisionResolver.verify(target, loader)) return false;
        Method method = DescriptorUtils.methodForDescriptor(target.methodDescriptor, loader);
        if (method == null || method.getDeclaringClass().getClassLoader() != loader) return false;
        if (installed.containsKey(method)) return true;
        String id = "splash-decision-" + Integer.toHexString(System.identityHashCode(method));
        try {
            HookHandle handle = module.hook(method).setId(id).setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> SplashDecisionPolicy.intercept(chain::proceed, enabled,
                            (original, returned, suppress) -> {
                                // The hook remains active; only its logging is bounded.
                                if (observations.get() < 16 && observations.getAndIncrement() < 16) {
                                    log.info("splashDecision original=" + original + " returned=" + returned
                                            + " overrideApplied=" + !java.util.Objects.equals(original, returned)
                                            + " splashEnabled=" + suppress + " originalCompleted=true"
                                            + " mode=POST_RESULT target=" + target.methodDescriptor);
                                }
                            }));
            installed.put(method, handle);
            ledger.record(HookLedger.Layer.BUSINESS, "splash", id, target.methodDescriptor);
            log.info("splash decision installed mode=POST_RESULT target=" + target.methodDescriptor);
            return true;
        } catch (Throwable failure) {
            log.error("splash decision install failed; existing Activity protection retained", failure);
            return false;
        }
    }
}
