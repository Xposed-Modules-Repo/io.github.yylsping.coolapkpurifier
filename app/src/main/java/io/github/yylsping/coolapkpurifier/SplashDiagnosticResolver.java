package io.github.yylsping.coolapkpurifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

/** Diagnostic-only evidence. No obfuscated name fallback and no result override. */
final class SplashDiagnosticResolver {
    static final String DECISION = "diagnostic.splashDecision";
    static final String HOST_START = "diagnostic.splashHostStart";
    static final String FRAGMENT_SHOW = "diagnostic.splashFragmentShow";
    static final String SOURCE = "splash_observation_semantics_v1";
    static final String MAIN = SplashDecisionResolver.MAIN;
    static final String FRAGMENT = SplashDecisionResolver.FRAGMENT;
    static Map<String, ResolvedTarget> resolve(DexKitBridge bridge, ClassLoader loader,
                                                ModuleLog log) {
        Map<String, ResolvedTarget> result = new LinkedHashMap<>();
        ResolvedTarget resolved = SplashDecisionResolver.resolve(bridge, loader, log);
        if (resolved == null) return result;
        ResolvedTarget decision = new ResolvedTarget(DECISION, SOURCE,
                resolved.classDescriptor, resolved.methodDescriptor);
        result.put(decision.key, decision);
        addUnique(result, bridge, loader, HOST_START, MethodMatcher.create().declaredClass(MAIN)
                .returnType("void").paramCount(4)
                .addUsingString("event=host_start_requested, host=MainActivity, source=", StringMatchType.Equals)
                .addUsingString("event=host_loader_start, source=", StringMatchType.Equals));
        addUnique(result, bridge, loader, FRAGMENT_SHOW, MethodMatcher.create().declaredClass(FRAGMENT)
                .returnType("void").paramTypes("java.lang.Object")
                .addUsingString("event=show_ad_start, source=", StringMatchType.Equals)
                .addUsingString("event=show_ad_return, source=", StringMatchType.Equals));
        return result;
    }

    private static void addUnique(Map<String, ResolvedTarget> out, DexKitBridge bridge,
                                  ClassLoader loader, String key, MethodMatcher matcher) {
        List<ResolvedTarget> candidates = new ArrayList<>();
        for (MethodData data : bridge.findMethod(FindMethod.create().matcher(matcher))) {
            ResolvedTarget target = target(key, data);
            if (verify(target, loader)) candidates.add(target);
        }
        ResolvedTarget target = UniqueTargetSelector.only(candidates);
        if (target != null) out.put(key, target);
    }

    private static ResolvedTarget target(String key, MethodData data) {
        return new ResolvedTarget(key, SOURCE,
                "L" + data.getDeclaredClassName().replace('.', '/') + ";", data.getDescriptor());
    }

    static boolean decisionShape(Method method) {
        return SplashDecisionResolver.decisionShape(method);
    }

    static boolean verify(ResolvedTarget target, ClassLoader loader) {
        try {
            if (target == null || !SOURCE.equals(target.source)) return false;
            Method method = DescriptorUtils.methodForDescriptor(target.methodDescriptor, loader);
            if (method == null || !org.luckypray.dexkit.util.DexSignUtil.getDescriptor(method)
                    .equals(target.methodDescriptor)
                    || !DescriptorUtils.classDescriptorOf(method.getDeclaringClass())
                    .equals(target.classDescriptor)) return false;
            if (DECISION.equals(target.key)) return decisionShape(method);
            int flags = method.getModifiers();
            if (!Modifier.isPublic(flags) || !Modifier.isFinal(flags) || Modifier.isStatic(flags)
                    || Modifier.isNative(flags) || Modifier.isAbstract(flags)
                    || method.getReturnType() != void.class) return false;
            Class<?>[] p = method.getParameterTypes();
            if (HOST_START.equals(target.key)) {
                return MAIN.equals(method.getDeclaringClass().getName()) && p.length == 4
                        && SplashDecisionResolver.sourceShape(p[0]) && p[1] == boolean.class
                        && "kotlin.jvm.functions.Function0".equals(p[2].getName()) && p[3] == String.class;
            }
            return FRAGMENT_SHOW.equals(target.key) && FRAGMENT.equals(method.getDeclaringClass().getName())
                    && Arrays.equals(p, new Class<?>[]{Object.class});
        } catch (Throwable failure) { return false; }
    }
}
