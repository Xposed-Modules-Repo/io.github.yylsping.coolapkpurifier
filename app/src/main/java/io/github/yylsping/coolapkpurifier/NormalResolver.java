package io.github.yylsping.coolapkpurifier;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.MatchType;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.AnnotationElementMatcher;
import org.luckypray.dexkit.query.matchers.AnnotationMatcher;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.util.DexSignUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves feed and model getters. This runs after splash readiness and is
 * allowed to take as long as it needs in the background.
 */
final class NormalResolver {
    static final String SOURCE_FINGERPRINT_STRONG = "fingerprint_strong";
    static final String SOURCE_FINGERPRINT_WEAK = "fingerprint_weak";
    static final String SOURCE_FALLBACK = "fallback_compat";

    private static final String AD_HELPER_CLASS = "com.coolapk.market.view.ad.EntityAdHelper";
    private static final String ENTITY_CLASS = "com.coolapk.market.model.Entity";
    private static final String SERIALIZED_NAME_ANNOTATION =
            "com.google.gson.annotations.SerializedName";

    private final DexKitBridge bridge;
    private final ClassLoader loader;
    private final ModuleLog log;

    NormalResolver(DexKitBridge bridge, ClassLoader loader, ModuleLog log) {
        this.bridge = bridge;
        this.loader = loader;
        this.log = log;
    }

    Map<String, ResolvedTarget> resolve() {
        Map<String, ResolvedTarget> targets = new LinkedHashMap<>();
        ResolvedTarget feed = resolveFeed();
        if (feed != null) {
            targets.put(TargetResolver.KEY_FEED, feed);
        }
        ClassData entityClass = resolveEntityClass();
        if (entityClass != null) {
            putGetter(targets, TargetResolver.KEY_GETTER_TEMPLATE, entityClass,
                    "getEntityTemplate", "entityTemplate");
            putGetter(targets, TargetResolver.KEY_GETTER_ENTITY_ID, entityClass,
                    "getEntityId", "entityId");
            putGetter(targets, TargetResolver.KEY_GETTER_TITLE, entityClass,
                    "getTitle", "title");
            putGetter(targets, TargetResolver.KEY_GETTER_ENTITY_TYPE, entityClass,
                    "getEntityType", "entityType");
        } else {
            log.info("resolver target=getters path=all failed=entityClassUnresolved"
                    + " failClosed=true");
        }
        return targets;
    }

    private ResolvedTarget resolveFeed() {
        MethodData method = queryFeedStrong();
        if (method != null) {
            return methodTarget(TargetResolver.KEY_FEED, SOURCE_FINGERPRINT_STRONG, method);
        }
        method = queryFeedWeak();
        if (method != null) {
            return methodTarget(TargetResolver.KEY_FEED, SOURCE_FINGERPRINT_WEAK, method);
        }
        ReflectedMethod reflected = queryFeedFallback();
        if (reflected != null) {
            return reflectedTarget(TargetResolver.KEY_FEED, SOURCE_FALLBACK, reflected);
        }
        log.info("resolver target=feed path=all failed=unresolved failClosed=true");
        return null;
    }

    private MethodData queryFeedStrong() {
        try {
            MethodDataList raw = bridge.findMethod(FindMethod.create()
                    .searchPackages("com.coolapk.market")
                    .matcher(MethodMatcher.create()
                            .returnType("java.util.List")
                            .paramTypes("java.util.List", "boolean")
                            .usingStrings("sponsorTemplates")));
            List<MethodData> candidates = filterFeed(raw, true);
            logCandidates("feed", "strong", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            MethodData candidate = candidates.get(0);
            if (!verifyFeed(candidate)) {
                log.info("resolver target=feed path=fingerprint_strong"
                        + " rejected=verification descriptor=" + candidate.getDescriptor());
                return null;
            }
            return candidate;
        } catch (Throwable throwable) {
            log.error("resolver target=feed path=fingerprint_strong queryFailed", throwable);
            return null;
        }
    }

    private MethodData queryFeedWeak() {
        try {
            ClassDataList classes = bridge.findClass(FindClass.create()
                    .searchPackages("com.coolapk.market")
                    .matcher(ClassMatcher.create().className(AD_HELPER_CLASS)));
            if (classes.size() != 1) {
                logCandidates("feed", "weakClass", classes);
                return null;
            }
            MethodDataList methods = bridge.findMethod(FindMethod.create()
                    .searchInClass(Collections.singleton(classes.get(0)))
                    .matcher(MethodMatcher.create()
                            .returnType("java.util.List")
                            .paramTypes("java.util.List", "boolean")));
            List<MethodData> candidates = filterFeed(methods, false);
            logCandidates("feed", "weak", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            MethodData candidate = candidates.get(0);
            if (!verifyFeed(candidate)) {
                log.info("resolver target=feed path=fingerprint_weak"
                        + " rejected=verification descriptor=" + candidate.getDescriptor());
                return null;
            }
            return candidate;
        } catch (Throwable throwable) {
            log.error("resolver target=feed path=fingerprint_weak queryFailed", throwable);
            return null;
        }
    }

    private ReflectedMethod queryFeedFallback() {
        try {
            Class<?> type = Class.forName(AD_HELPER_CLASS, false, loader);
            List<Method> candidates = new ArrayList<>();
            for (Method method : type.getDeclaredMethods()) {
                if (TargetVerifier.isFeedShape(method)
                        && !Modifier.isAbstract(method.getModifiers())) {
                    candidates.add(method);
                }
            }
            log.info("resolver target=feed path=fallback_compat candidates="
                    + candidates.size() + " class=" + type.getName());
            if (candidates.size() != 1) {
                return null;
            }
            return new ReflectedMethod(candidates.get(0));
        } catch (Throwable throwable) {
            log.info("resolver target=feed path=fallback_compat unavailable reason=" + throwable);
            return null;
        }
    }

    private List<MethodData> filterFeed(MethodDataList raw, boolean requireSponsorString) {
        List<MethodData> candidates = new ArrayList<>();
        if (raw == null) {
            return candidates;
        }
        for (MethodData method : raw) {
            if (!"java.util.List".equals(method.getReturnTypeName())) {
                continue;
            }
            List<String> params = method.getParamTypeNames();
            if (params == null || params.size() != 2
                    || !"java.util.List".equals(params.get(0))
                    || !"boolean".equals(params.get(1))) {
                continue;
            }
            if (requireSponsorString
                    && !contains(method.getUsingStrings(), "sponsorTemplates")) {
                continue;
            }
            candidates.add(method);
        }
        return candidates;
    }

    private boolean verifyFeed(MethodData method) {
        try {
            Method instance = method.getMethodInstance(loader);
            return instance != null
                    && TargetVerifier.isFeedShape(instance)
                    && !Modifier.isAbstract(instance.getModifiers());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ClassData resolveEntityClass() {
        try {
            ClassDataList raw = bridge.findClass(FindClass.create()
                    .searchPackages("com.coolapk.market.model")
                    .matcher(ClassMatcher.create()
                            .className(ENTITY_CLASS)
                            .modifiers(Modifier.INTERFACE, MatchType.Contains)));
            List<ClassData> candidates = new ArrayList<>();
            for (ClassData data : raw) {
                if (Modifier.isInterface(data.getModifiers())) {
                    candidates.add(data);
                }
            }
            logCandidates("getterEntityClass", "strong", candidates);
            if (candidates.size() == 1) {
                Class<?> type = candidates.get(0).getInstance(loader);
                if (type != null && type.isInterface()) {
                    return candidates.get(0);
                }
            }
        } catch (Throwable throwable) {
            log.error("resolver target=getterEntityClass path=fingerprint_strong queryFailed",
                    throwable);
        }
        try {
            Class<?> type = Class.forName(ENTITY_CLASS, false, loader);
            if (type.isInterface()) {
                log.info("resolver target=getterEntityClass path=fallback_compat"
                        + " candidates=1 class=" + type.getName());
                return bridge.getClassData(type);
            }
        } catch (Throwable throwable) {
            log.info("resolver target=getterEntityClass path=fallback_compat unavailable reason="
                    + throwable);
        }
        return null;
    }

    private void putGetter(Map<String, ResolvedTarget> targets, String key,
                           ClassData entityClass, String methodName, String serializedName) {
        MethodData method = queryGetterStrong(entityClass, serializedName);
        String source = SOURCE_FINGERPRINT_STRONG;
        if (method == null) {
            method = queryGetterWeak(entityClass, methodName);
            source = SOURCE_FINGERPRINT_WEAK;
        }
        ReflectedMethod reflected = null;
        if (method == null) {
            reflected = queryGetterFallback(entityClass, methodName);
            source = SOURCE_FALLBACK;
        }
        if (method == null && reflected == null) {
            log.info("resolver target=getter key=" + key + " path=all failed=unresolved"
                    + " failClosed=true serializedName=" + serializedName
                    + " methodName=" + methodName);
            return;
        }
        if (method != null) {
            if (!verifyGetter(method)) {
                log.info("resolver target=getter key=" + key + " path=" + source
                        + " rejected=verification descriptor=" + method.getDescriptor());
                return;
            }
            ResolvedTarget target = methodTarget(key, source, method);
            if (target != null) {
                targets.put(key, target);
            }
        } else {
            ResolvedTarget target = reflectedTarget(key, source, reflected);
            if (target != null) {
                targets.put(key, target);
            }
        }
    }

    private MethodData queryGetterStrong(ClassData entityClass, String serializedName) {
        try {
            AnnotationMatcher annotation = AnnotationMatcher.create()
                    .type(SERIALIZED_NAME_ANNOTATION)
                    .addElement(AnnotationElementMatcher.create()
                            .name("value")
                            .stringValue(serializedName, StringMatchType.Equals, false));
            MethodDataList raw = bridge.findMethod(FindMethod.create()
                    .searchInClass(Collections.singleton(entityClass))
                    .matcher(MethodMatcher.create()
                            .returnType("java.lang.String")
                            .paramTypes()
                            .addAnnotation(annotation)));
            List<MethodData> candidates = new ArrayList<>(raw);
            logCandidates("getter." + serializedName, "strong", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            return verifyGetter(candidates.get(0)) ? candidates.get(0) : null;
        } catch (Throwable throwable) {
            log.info("resolver target=getter serializedName=" + serializedName
                    + " path=fingerprint_strong queryFailed reason=" + throwable);
            return null;
        }
    }

    private MethodData queryGetterWeak(ClassData entityClass, String methodName) {
        try {
            MethodDataList raw = bridge.findMethod(FindMethod.create()
                    .searchInClass(Collections.singleton(entityClass))
                    .matcher(MethodMatcher.create()
                            .name(methodName, StringMatchType.Equals, false)
                            .returnType("java.lang.String")
                            .paramTypes()));
            List<MethodData> candidates = new ArrayList<>(raw);
            logCandidates("getter." + methodName, "weak", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            return verifyGetter(candidates.get(0)) ? candidates.get(0) : null;
        } catch (Throwable throwable) {
            log.info("resolver target=getter methodName=" + methodName
                    + " path=fingerprint_weak queryFailed reason=" + throwable);
            return null;
        }
    }

    private ReflectedMethod queryGetterFallback(ClassData entityClass, String methodName) {
        try {
            Class<?> type = entityClass.getInstance(loader);
            Method method = type.getMethod(methodName);
            log.info("resolver target=getter methodName=" + methodName
                    + " path=fallback_compat candidates=1 method=" + method);
            return verifyGetter(method) ? new ReflectedMethod(method) : null;
        } catch (Throwable throwable) {
            log.info("resolver target=getter methodName=" + methodName
                    + " path=fallback_compat unavailable reason=" + throwable);
            return null;
        }
    }

    private boolean verifyGetter(MethodData method) {
        try {
            return verifyGetter(method.getMethodInstance(loader));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean verifyGetter(Method method) {
        return method != null
                && method.getParameterTypes().length == 0
                && method.getReturnType() == String.class;
    }

    private ResolvedTarget methodTarget(String key, String source, MethodData method) {
        ResolvedTarget target = new ResolvedTarget(key, source,
                method.getDeclaredClassName(), method.getDescriptor());
        String problem = TargetVerifier.verify(target, loader);
        if (problem != null) {
            log.info("resolver target=" + key + " path=" + source
                    + " rejected=verification reason=" + problem + " target=" + target.describe());
            return null;
        }
        log.info("resolver target=" + key + " path=" + source
                + " candidates=1 descriptor=" + target.describe());
        return target;
    }

    private ResolvedTarget reflectedTarget(String key, String source, ReflectedMethod candidate) {
        ResolvedTarget target = new ResolvedTarget(key, source,
                DexSignUtil.getClassDescriptor(candidate.method.getDeclaringClass()),
                DexSignUtil.getDescriptor(candidate.method));
        String problem = TargetVerifier.verify(target, loader);
        if (problem != null) {
            log.info("resolver target=" + key + " path=" + source
                    + " rejected=verification reason=" + problem + " target=" + target.describe());
            return null;
        }
        log.info("resolver target=" + key + " path=" + source
                + " candidates=1 descriptor=" + target.describe());
        return target;
    }

    private boolean contains(List<String> strings, String expected) {
        if (strings == null) {
            return false;
        }
        for (String value : strings) {
            if (value != null && value.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private void logCandidates(String target, String stage, List<?> candidates) {
        StringBuilder sb = new StringBuilder();
        for (Object candidate : candidates) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(String.valueOf(candidate));
        }
        log.info("resolver target=" + target + " path=fingerprint_" + stage
                + " candidates=" + candidates.size()
                + (sb.length() == 0 ? "" : " list=" + sb));
    }

    private static final class ReflectedMethod {
        final Method method;

        ReflectedMethod(Method method) {
            this.method = method;
        }
    }
}
