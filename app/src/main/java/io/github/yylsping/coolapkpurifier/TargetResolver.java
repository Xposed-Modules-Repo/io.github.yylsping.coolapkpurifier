package io.github.yylsping.coolapkpurifier;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

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
import java.util.Set;

/**
 * Resolves every hook target through the following ordered paths:
 *
 * 1. verified persistent cache (no DexKit bridge is created on a full cache hit)
 * 2. strong DexKit fingerprints (stable strings, types, annotations, call/shape semantics)
 * 3. weak DexKit fingerprints (semantic class names plus the same structural verification)
 * 4. existing compatible reflection fallback
 *
 * Every fingerprint stage is fail-closed: zero candidates, more than one candidate,
 * or a candidate that cannot be verified means no hook is installed for that target.
 */
final class TargetResolver {
    static final String KEY_FEED = "feed";
    static final String KEY_SPLASH_BASE = "splash_base";
    static final String KEY_GETTER_TEMPLATE = "getter.entityTemplate";
    static final String KEY_GETTER_ENTITY_ID = "getter.entityId";
    static final String KEY_GETTER_TITLE = "getter.title";
    static final String KEY_GETTER_ENTITY_TYPE = "getter.entityType";

    static final String SOURCE_CACHE = "cache";
    static final String SOURCE_FINGERPRINT_STRONG = "fingerprint_strong";
    static final String SOURCE_FINGERPRINT_WEAK = "fingerprint_weak";
    static final String SOURCE_FALLBACK = "fallback_compat";

    private static final String AD_HELPER_CLASS = "com.coolapk.market.view.ad.EntityAdHelper";
    private static final String ENTITY_CLASS = "com.coolapk.market.model.Entity";
    private static final String SERIALIZED_NAME_ANNOTATION = "com.google.gson.annotations.SerializedName";

    private final ModuleLog log;
    private final ClassLoader loader;
    private final Context appContext;
    private final ResolverCache cache;
    private final ResolverTrace trace;

    private final Map<String, ResolvedTarget> resolved = new LinkedHashMap<>();
    private TargetIdentity currentIdentity;
    private DexKitBridge bridge;
    private boolean cacheChecked;
    private boolean scanPerformed;
    private boolean cacheHitThisAttempt;
    private long lastAttemptAt;

    TargetResolver(ModuleLog log, ClassLoader loader, Context appContext) {
        this.log = log;
        this.loader = loader;
        this.appContext = appContext;
        this.cache = new ResolverCache(appContext);
        this.trace = new ResolverTrace(appContext);
    }

    /** Returns the resolved targets that are currently verified. */
    Map<String, ResolvedTarget> getResolved() {
        return resolved;
    }

    boolean isFeedResolved() {
        return resolved.containsKey(KEY_FEED);
    }

    boolean isSplashResolved() {
        return resolved.containsKey(KEY_SPLASH_BASE);
    }

    boolean areRequiredTargetsResolved() {
        return resolved.containsKey(KEY_FEED)
                && resolved.containsKey(KEY_GETTER_TEMPLATE)
                && resolved.containsKey(KEY_GETTER_ENTITY_ID)
                && resolved.containsKey(KEY_GETTER_TITLE)
                && resolved.containsKey(KEY_GETTER_ENTITY_TYPE);
    }

    /**
     * Runs one bounded resolution attempt. Callers schedule several attempts
     * because the protected Coolapk APK appends its real DEX after the
     * LSPosed package-ready callback.
     */
    synchronized void attempt() {
        lastAttemptAt = System.currentTimeMillis();
        try {
            if (currentIdentity == null) {
                currentIdentity = TargetIdentity.compute(appContext);
                info("resolver identity computed: " + currentIdentity.describe());
            }

            if (!cacheChecked) {
                applyCacheIfValid();
                cacheChecked = true;
            }

            if (areRequiredTargetsResolved()) {
                // A full cache hit must not create DexKit and must not rescan.
                if (cacheHitThisAttempt) {
                    info("resolver path=cache hit=true allRequiredTargets=verified dexkitScan=false targets="
                            + summarizeResolved());
                }
                return;
            }

            ensureBridge();
            if (bridge == null || !bridge.isValid()) {
                info("resolver path=fingerprint unavailable reason=dexkitBridgeInvalid dexNum="
                        + (bridge == null ? -1 : bridge.getDexNum())
                        + " willRetry=true resolvedTargets=" + resolved.size());
                return;
            }

            scanPerformed = true;
            if (!isFeedResolved()) {
                resolveFeed();
            }
            if (!isSplashResolved()) {
                resolveSplashBase();
            }
            if (!allGettersResolved()) {
                resolveEntityGetters();
            }
            persistResolved();
            info("resolver attempt finished dexkitScan=" + scanPerformed
                    + " resolvedTargets=" + summarizeResolved()
                    + " required=" + areRequiredTargetsResolved()
                    + " splash=" + isSplashResolved());
        } catch (Throwable throwable) {
            error("resolver attempt failed", throwable);
        }
    }

    private void applyCacheIfValid() {
        TargetIdentity cachedIdentity = cache.loadIdentity();
        Map<String, ResolvedTarget> cached = cache.loadEntries();
        if (cachedIdentity == null || cached.isEmpty()) {
            info("resolver path=cache hit=false reason=noCacheFile");
            return;
        }
        if (!cachedIdentity.sameTarget(currentIdentity)) {
            info("resolver path=cache hit=false reason=identityMismatch cached="
                    + cachedIdentity.describe() + " current=" + currentIdentity.describe());
            cache.clear();
            return;
        }

        int verified = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();
        for (ResolvedTarget target : cached.values()) {
            String problem = verify(target);
            if (problem == null) {
                resolved.put(target.key, target);
                verified++;
            } else {
                failed++;
                failures.add(target.key + ":" + problem);
            }
        }
        if (verified > 0) {
            cacheHitThisAttempt = true;
        }
        info("resolver path=cache hit=true identity=valid entries=" + cached.size()
                + " verified=" + verified + " failed=" + failed
                + (failed == 0 ? "" : " failures=" + failures)
                + " dexkitScan=" + (!areRequiredTargetsResolved()));
    }

    private void ensureBridge() {
        if (bridge != null && bridge.isValid()) {
            return;
        }
        closeBridge();
        long start = System.currentTimeMillis();
        try {
            DexKitNativeLoader.ensureLoaded(appContext);
            bridge = DexKitBridge.create(loader, true);
            if (bridge.isValid()) {
                bridge.setThreadNum(2);
                info("resolver dexkit bridge created useMemoryDexFile=true dexNum="
                        + bridge.getDexNum() + " elapsedMs=" + (System.currentTimeMillis() - start));
            }
        } catch (Throwable throwable) {
            bridge = null;
            error("resolver dexkit bridge creation failed", throwable);
        }
    }

    private void closeBridge() {
        if (bridge != null) {
            try {
                bridge.close();
            } catch (Throwable ignored) {
            }
            bridge = null;
        }
    }

    // ------------------------------------------------------------------
    // Feed list transformer
    // ------------------------------------------------------------------

    private void resolveFeed() {
        MethodData method = queryFeedStrong();
        if (method != null) {
            acceptMethod(KEY_FEED, SOURCE_FINGERPRINT_STRONG, method);
            return;
        }

        method = queryFeedWeak();
        if (method != null) {
            acceptMethod(KEY_FEED, SOURCE_FINGERPRINT_WEAK, method);
            return;
        }

        ReflectedMethod reflected = queryFeedFallback();
        if (reflected != null) {
            acceptReflectedMethod(KEY_FEED, SOURCE_FALLBACK, reflected);
            return;
        }
        info("resolver target=feed path=all failed=unresolved candidates=0 failClosed=true");
    }

    private MethodData queryFeedStrong() {
        try {
            FindMethod query = FindMethod.create()
                    .searchPackages("com.coolapk.market")
                    .matcher(MethodMatcher.create()
                            .returnType("java.util.List")
                            .paramTypes("java.util.List", "boolean")
                            .usingStrings("sponsorTemplates"));
            MethodDataList raw = bridge.findMethod(query);
            List<MethodData> candidates = filterFeedCandidates(raw, true);
            logCandidates("feed", "strong", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            MethodData candidate = candidates.get(0);
            if (!verifyFeedMethod(candidate)) {
                info("resolver target=feed path=fingerprint_strong rejected=verificationFailed descriptor="
                        + candidate.getDescriptor());
                return null;
            }
            return candidate;
        } catch (Throwable throwable) {
            error("resolver target=feed path=fingerprint_strong queryFailed", throwable);
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
            List<MethodData> candidates = filterFeedCandidates(methods, false);
            logCandidates("feed", "weak", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            MethodData candidate = candidates.get(0);
            if (!verifyFeedMethod(candidate)) {
                info("resolver target=feed path=fingerprint_weak rejected=verificationFailed descriptor="
                        + candidate.getDescriptor());
                return null;
            }
            return candidate;
        } catch (Throwable throwable) {
            error("resolver target=feed path=fingerprint_weak queryFailed", throwable);
            return null;
        }
    }

    private ReflectedMethod queryFeedFallback() {
        try {
            Class<?> type = Class.forName(AD_HELPER_CLASS, false, loader);
            List<Method> candidates = new ArrayList<>();
            for (Method method : type.getDeclaredMethods()) {
                if (isListBooleanListShape(method) && !Modifier.isAbstract(method.getModifiers())) {
                    candidates.add(method);
                }
            }
            info("resolver target=feed path=fallback_compat candidates=" + candidates.size()
                    + " class=" + type.getName());
            if (candidates.size() != 1) {
                return null;
            }
            Method method = candidates.get(0);
            info("resolver target=feed path=fallback_compat candidates=1 descriptor="
                    + DexSignUtil.getDescriptor(method));
            return new ReflectedMethod(method);
        } catch (Throwable throwable) {
            info("resolver target=feed path=fallback_compat unavailable reason=" + throwable);
            return null;
        }
    }

    private List<MethodData> filterFeedCandidates(MethodDataList raw, boolean requireSponsorString) {
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
            if (requireSponsorString && !containsString(method.getUsingStrings(), "sponsorTemplates")) {
                continue;
            }
            candidates.add(method);
        }
        return candidates;
    }

    private boolean verifyFeedMethod(MethodData method) {
        if (method == null || method.getDescriptor() == null || method.getDescriptor().isEmpty()) {
            return false;
        }
        try {
            Method instance = method.getMethodInstance(loader);
            return instance != null
                    && List.class.isAssignableFrom(instance.getReturnType())
                    && instance.getParameterTypes().length == 2
                    && List.class.isAssignableFrom(instance.getParameterTypes()[0])
                    && instance.getParameterTypes()[1] == boolean.class
                    && !Modifier.isAbstract(instance.getModifiers());
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Splash base
    // ------------------------------------------------------------------

    private void resolveSplashBase() {
        ClassData type = querySplashStrong();
        if (type != null) {
            MethodData onCreate = findOnCreateMethod(type);
            if (verifySplashClass(type)) {
                acceptSplash(type, onCreate, SOURCE_FINGERPRINT_STRONG);
                return;
            }
        }
        type = querySplashWeak();
        if (type != null) {
            MethodData onCreate = findOnCreateMethod(type);
            if (verifySplashClass(type)) {
                acceptSplash(type, onCreate, SOURCE_FINGERPRINT_WEAK);
                return;
            }
        }
        info("resolver target=splash path=all failed=unresolved candidates=0 failClosed=true"
                + " frameworkInstrumentationFallback=true");
    }

    private ClassData querySplashStrong() {
        try {
            ClassDataList raw = bridge.findClass(FindClass.create()
                    .searchPackages("com.coolapk.market.view.splash")
                    .matcher(ClassMatcher.create()
                            .source("SplashActivity", StringMatchType.Contains, false)));
            List<ClassData> candidates = new ArrayList<>();
            for (ClassData data : raw) {
                if (isActivityType(data)) {
                    candidates.add(data);
                }
            }
            logCandidates("splash", "strong", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            ClassData candidate = candidates.get(0);
            if (!verifySplashClass(candidate)) {
                info("resolver target=splash path=fingerprint_strong rejected=verificationFailed class="
                        + candidate.getDescriptor());
                return null;
            }
            return candidate;
        } catch (Throwable throwable) {
            error("resolver target=splash path=fingerprint_strong queryFailed", throwable);
            return null;
        }
    }

    private ClassData querySplashWeak() {
        try {
            ClassDataList raw = bridge.findClass(FindClass.create()
                    .searchPackages("com.coolapk.market.view.splash")
                    .matcher(ClassMatcher.create()
                            .className("Splash", StringMatchType.Contains, false)));
            List<ClassData> candidates = new ArrayList<>();
            for (ClassData data : raw) {
                if (isActivityType(data)) {
                    candidates.add(data);
                }
            }
            logCandidates("splash", "weak", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            ClassData candidate = candidates.get(0);
            if (!verifySplashClass(candidate)) {
                info("resolver target=splash path=fingerprint_weak rejected=verificationFailed class="
                        + candidate.getDescriptor());
                return null;
            }
            return candidate;
        } catch (Throwable throwable) {
            error("resolver target=splash path=fingerprint_weak queryFailed", throwable);
            return null;
        }
    }

    private boolean isActivityType(ClassData data) {
        if (data == null) {
            return false;
        }
        try {
            Class<?> type = data.getInstance(loader);
            return type != null && Activity.class.isAssignableFrom(type);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private List<ClassData> filterSplashCandidates(ClassDataList raw) {
        List<ClassData> candidates = new ArrayList<>();
        if (raw == null) {
            return candidates;
        }
        for (ClassData data : raw) {
            if (findOnCreateMethod(data) != null) {
                candidates.add(data);
            }
        }
        return candidates;
    }

    private MethodData findOnCreateMethod(ClassData data) {
        if (data == null || data.getMethods() == null) {
            return null;
        }
        for (MethodData method : data.getMethods()) {
            if ("onCreate".equals(method.getName())
                    && method.getParamTypeNames() != null
                    && method.getParamTypeNames().size() == 1
                    && "android.os.Bundle".equals(method.getParamTypeNames().get(0))) {
                return method;
            }
        }
        return null;
    }

    private boolean verifySplashClass(ClassData data) {
        if (data == null || data.getDescriptor() == null || data.getDescriptor().isEmpty()) {
            return false;
        }
        try {
            Class<?> type = data.getInstance(loader);
            if (type == null || !Activity.class.isAssignableFrom(type)) {
                return false;
            }
            Method onCreate = findOnCreateByReflection(type);
            return onCreate != null
                    && onCreate.getParameterTypes().length == 1
                    && onCreate.getParameterTypes()[0] == Bundle.class;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Method findOnCreateByReflection(Class<?> type) {
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            try {
                Method method = cursor.getDeclaredMethod("onCreate", Bundle.class);
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private void acceptSplash(ClassData type, MethodData onCreate, String source) {
        ResolvedTarget target = new ResolvedTarget(
                KEY_SPLASH_BASE, source, type.getDescriptor(),
                onCreate == null ? "" : onCreate.getDescriptor());
        if (verify(target) != null) {
            info("resolver target=splash path=" + source + " rejected=verificationFailed " + target.describe());
            return;
        }
        resolved.put(target.key, target);
        info("resolver target=splash path=" + source + " candidates=1 descriptor=" + target.describe());
    }

    // ------------------------------------------------------------------
    // Entity getters
    // ------------------------------------------------------------------

    private void resolveEntityGetters() {
        ClassData entityClass = resolveEntityClass();
        if (entityClass == null) {
            info("resolver target=getters path=all failed=entityClassUnresolved failClosed=true");
            return;
        }
        resolveGetter(KEY_GETTER_TEMPLATE, entityClass, "getEntityTemplate", "entityTemplate");
        resolveGetter(KEY_GETTER_ENTITY_ID, entityClass, "getEntityId", "entityId");
        resolveGetter(KEY_GETTER_TITLE, entityClass, "getTitle", "title");
        resolveGetter(KEY_GETTER_ENTITY_TYPE, entityClass, "getEntityType", "entityType");
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
            error("resolver target=getterEntityClass path=fingerprint_strong queryFailed", throwable);
        }

        // Weak/fallback: the model interface name has been stable and is only
        // used after the structural query above failed.
        try {
            Class<?> type = Class.forName(ENTITY_CLASS, false, loader);
            if (type.isInterface()) {
                info("resolver target=getterEntityClass path=fallback_compat candidates=1 class="
                        + type.getName());
                return bridge.getClassData(type);
            }
        } catch (Throwable throwable) {
            info("resolver target=getterEntityClass path=fallback_compat unavailable reason=" + throwable);
        }
        return null;
    }

    private void resolveGetter(String key, ClassData entityClass, String methodName, String serializedName) {
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
            info("resolver target=getter key=" + key + " path=all failed=unresolved failClosed=true"
                    + " serializedName=" + serializedName + " methodName=" + methodName);
            return;
        }
        if (method != null) {
            if (!verifyGetterMethod(method)) {
                info("resolver target=getter key=" + key + " path=" + source
                        + " rejected=verificationFailed descriptor=" + method.getDescriptor());
                return;
            }
            acceptMethod(key, source, method);
        } else {
            acceptReflectedMethod(key, source, reflected);
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
            MethodData candidate = candidates.get(0);
            return verifyGetterMethod(candidate) ? candidate : null;
        } catch (Throwable throwable) {
            info("resolver target=getter serializedName=" + serializedName
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
            MethodData candidate = candidates.get(0);
            return verifyGetterMethod(candidate) ? candidate : null;
        } catch (Throwable throwable) {
            info("resolver target=getter methodName=" + methodName
                    + " path=fingerprint_weak queryFailed reason=" + throwable);
            return null;
        }
    }

    private ReflectedMethod queryGetterFallback(ClassData entityClass, String methodName) {
        try {
            Class<?> type = entityClass.getInstance(loader);
            Method method = type.getMethod(methodName);
            info("resolver target=getter methodName=" + methodName
                    + " path=fallback_compat candidates=1 method=" + method);
            return verifyGetterMethod(method) ? new ReflectedMethod(method) : null;
        } catch (Throwable throwable) {
            info("resolver target=getter methodName=" + methodName
                    + " path=fallback_compat unavailable reason=" + throwable);
            return null;
        }
    }

    private boolean verifyGetterMethod(MethodData method) {
        if (method == null || method.getDescriptor() == null || method.getDescriptor().isEmpty()) {
            return false;
        }
        try {
            Method instance = method.getMethodInstance(loader);
            return verifyGetterMethod(instance);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean verifyGetterMethod(Method method) {
        return method != null
                && method.getParameterTypes().length == 0
                && method.getReturnType() == String.class;
    }

    private boolean allGettersResolved() {
        return resolved.containsKey(KEY_GETTER_TEMPLATE)
                && resolved.containsKey(KEY_GETTER_ENTITY_ID)
                && resolved.containsKey(KEY_GETTER_TITLE)
                && resolved.containsKey(KEY_GETTER_ENTITY_TYPE);
    }

    // ------------------------------------------------------------------
    // Cache / verification helpers
    // ------------------------------------------------------------------

    private void acceptMethod(String key, String source, MethodData method) {
        ResolvedTarget target = new ResolvedTarget(
                key, source, method.getDeclaredClassName(), method.getDescriptor());
        String problem = verify(target);
        if (problem != null) {
            info("resolver target=" + key + " path=" + source
                    + " rejected=verificationFailed reason=" + problem + " descriptor=" + target.describe());
            return;
        }
        resolved.put(key, target);
        info("resolver target=" + key + " path=" + source + " candidates=1 descriptor=" + target.describe());
    }

    private String verify(ResolvedTarget target) {
        if (target == null) {
            return "null target";
        }
        if (target.key == null || target.key.isEmpty()) {
            return "empty key";
        }
        try {
            Class<?> type = DescriptorUtils.classForName(target.classDescriptor, loader);
            if (type == null) {
                return "class not loadable";
            }
            if (target.methodDescriptor != null && !target.methodDescriptor.isEmpty()) {
                Method method = DescriptorUtils.methodForDescriptor(target.methodDescriptor, loader);
                if (method == null) {
                    return "method not loadable";
                }
                switch (target.key) {
                    case KEY_FEED:
                        return isListBooleanListShape(method) && !Modifier.isAbstract(method.getModifiers())
                                ? null : "feed shape mismatch";
                    case KEY_SPLASH_BASE:
                        return Activity.class.isAssignableFrom(type)
                                && "onCreate".equals(method.getName())
                                && method.getParameterTypes().length == 1
                                && method.getParameterTypes()[0] == Bundle.class
                                ? null : "splash shape mismatch";
                    default:
                        return method.getParameterTypes().length == 0
                                && method.getReturnType() == String.class
                                ? null : "getter shape mismatch";
                }
            }
            return null;
        } catch (Throwable throwable) {
            return String.valueOf(throwable);
        }
    }

    private void persistResolved() {
        if (!resolved.isEmpty()) {
            cache.save(currentIdentity, resolved);
            info("resolver cache saved entries=" + resolved.size() + " identity=" + currentIdentity.describe());
        }
    }

    private boolean isListBooleanListShape(Method method) {
        Class<?>[] params = method.getParameterTypes();
        return params.length == 2
                && List.class.isAssignableFrom(params[0])
                && params[1] == boolean.class
                && List.class.isAssignableFrom(method.getReturnType());
    }

    private boolean containsString(List<String> strings, String expected) {
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

    private String summarizeResolved() {
        StringBuilder sb = new StringBuilder();
        for (ResolvedTarget target : resolved.values()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(target.key).append('=').append(target.classDescriptor)
                    .append("->").append(target.methodDescriptor);
        }
        return sb.toString();
    }

    private void info(String message) {
        if (trace != null) {
            trace.info(message);
        }
        log.info(message);
    }

    private void error(String message, Throwable throwable) {
        if (trace != null) {
            trace.info(message + " : " + throwable);
        }
        log.error(message, throwable);
    }

    private void acceptReflectedMethod(String key, String source, ReflectedMethod candidate) {
        String classDescriptor = DexSignUtil.getClassDescriptor(candidate.method.getDeclaringClass());
        String methodDescriptor = DexSignUtil.getDescriptor(candidate.method);
        ResolvedTarget target = new ResolvedTarget(key, source, classDescriptor, methodDescriptor);
        String problem = verify(target);
        if (problem != null) {
            info("resolver target=" + key + " path=" + source
                    + " rejected=verificationFailed reason=" + problem + " descriptor=" + target.describe());
            return;
        }
        resolved.put(key, target);
        info("resolver target=" + key + " path=" + source + " candidates=1 descriptor=" + target.describe());
    }

    private void logCandidates(String target, String stage, List<?> candidates) {
        StringBuilder sb = new StringBuilder();
        for (Object candidate : candidates) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(String.valueOf(candidate));
        }
        info("resolver target=" + target + " path=fingerprint_" + stage
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
