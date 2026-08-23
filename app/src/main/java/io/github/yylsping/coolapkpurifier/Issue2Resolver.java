package io.github.yylsping.coolapkpurifier;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Resolves the optional Issue #2 targets and the 15/16 splash decision. */
final class Issue2Resolver {
    private static final String AUTO_COMMENT_CLASS =
            "com.coolapk.market.view.cardlist.component.RecyclerViewItemFullVisibleControllerKt";
    private static final String TOPIC_CONFIG_CLASS =
            "com.coolapk.market.view.feedv8.component.TopicRecommendConfig";
    private static final String RELATED_HOLDER_CLASS =
            "com.coolapk.market.viewholder.RelatedDataViewHolder";
    private static final String RECOMMEND_FEED_CLASS =
            "com.coolapk.market.view.feed.post.RecommendFeed";
    private static final String DETAIL_SPONSOR_HOLDER_CLASS =
            "com.coolapk.market.view.ad.SponsorSelfDrawDetailViewHolder";

    private final DexKitBridge bridge;
    private final ClassLoader loader;
    private final ModuleLog log;

    Issue2Resolver(DexKitBridge bridge, ClassLoader loader, ModuleLog log) {
        this.bridge = bridge;
        this.loader = loader;
        this.log = log;
    }

    Map<String, ResolvedTarget> resolve(PurifierConfig config, int coolapkMajor) {
        Map<String, ResolvedTarget> targets = new LinkedHashMap<>();
        if (coolapkMajor >= 15 && config.isEnabled(PurifierConfig.Feature.SPLASH)) {
            put(targets, resolveSplashDecision());
        }
        if (coolapkMajor < 15) {
            return targets;
        }
        if (config.isEnabled(PurifierConfig.Feature.AUTO_COMMENT)) {
            put(targets, resolveNamedMethod(TargetResolver.KEY_AUTO_COMMENT,
                    AUTO_COMMENT_CLASS, "addAutoShowFeedCommentView"));
        }
        if (config.isEnabled(PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND)) {
            put(targets, resolveTopicRecommendToggle());
        }
        if (config.isEnabled(PurifierConfig.Feature.RELATED_DATA)) {
            put(targets, resolveClassTarget(TargetResolver.KEY_RELATED_DATA,
                    RELATED_HOLDER_CLASS));
        }
        if (config.isEnabled(PurifierConfig.Feature.SAME_TOPIC_FEED)) {
            put(targets, resolveClassTarget(TargetResolver.KEY_SAME_TOPIC_FEED,
                    RECOMMEND_FEED_CLASS));
        }
        if (config.isEnabled(PurifierConfig.Feature.DETAIL_SPONSOR)) {
            put(targets, resolveClassTarget(TargetResolver.KEY_DETAIL_SPONSOR,
                    DETAIL_SPONSOR_HOLDER_CLASS));
        }
        return targets;
    }

    private ResolvedTarget resolveSplashDecision() {
        List<MethodData> verified = new ArrayList<>();
        try {
            // FullScreenAdUtils is package-obfuscated (kc5.* on 16.6.1), so
            // first locate the single Kotlin source class globally, then run
            // the string query only inside that tiny class set.
            ClassDataList classes = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                            .source("FullScreenAdUtils.kt",
                                    StringMatchType.Equals, false)
                            .usingStrings("[shouldShowAd] start",
                                    "everything is fine, show splash",
                                    "in no ad meantime")));
            if (!classes.isEmpty()) {
                MethodDataList methods = bridge.findMethod(FindMethod.create()
                        .searchInClass(new ArrayList<>(classes))
                        .matcher(MethodMatcher.create().usingStrings(
                                "[shouldShowAd] start",
                                "everything is fine, show splash",
                                "in no ad meantime")));
                for (MethodData candidate : methods) {
                    if (isLiveBoolean(candidate)) {
                        verified.add(candidate);
                    }
                }
            }
            log.info("resolver target=splashDecision sourceClasses=" + classes.size());
        } catch (Throwable throwable) {
            log.info("resolver target=splashDecision queryFailed=" + throwable);
        }
        log.info("resolver target=splashDecision candidates=" + verified.size()
                + " descriptors=" + describeMethods(verified));
        if (verified.size() != 1) {
            return null;
        }
        return methodTarget(TargetResolver.KEY_SPLASH_DECISION,
                "issue2_splash_strings", verified.get(0));
    }

    private ResolvedTarget resolveNamedMethod(String key, String className,
                                               String methodName) {
        try {
            ClassDataList classes = bridge.findClass(FindClass.create()
                    .searchPackages(packageName(className))
                    .matcher(ClassMatcher.create().className(className)));
            if (classes.size() == 1) {
                MethodDataList methods = bridge.findMethod(FindMethod.create()
                        .searchInClass(java.util.Collections.singleton(classes.get(0)))
                        .matcher(MethodMatcher.create().name(
                                methodName, StringMatchType.Equals, false)));
                List<MethodData> live = new ArrayList<>();
                for (MethodData method : methods) {
                    if (liveMethod(method) != null) {
                        live.add(method);
                    }
                }
                log.info("resolver target=" + key + " candidates=" + live.size()
                        + " descriptors=" + describeMethods(live));
                if (live.size() == 1) {
                    return methodTarget(key, "issue2_semantic_name", live.get(0));
                }
            }
        } catch (Throwable throwable) {
            log.info("resolver target=" + key + " namedQueryFailed=" + throwable);
        }
        try {
            Class<?> type = Class.forName(className, false, loader);
            Method matched = null;
            for (Method method : type.getDeclaredMethods()) {
                if (methodName.equals(method.getName())
                        && !Modifier.isAbstract(method.getModifiers())) {
                    if (matched != null) {
                        log.info("resolver target=" + key
                                + " reflectionAmbiguous class=" + className);
                        return null;
                    }
                    matched = method;
                }
            }
            return reflectedMethodTarget(key, matched, "issue2_reflection_name");
        } catch (Throwable throwable) {
            log.info("resolver target=" + key + " reflectionUnavailable=" + throwable);
            return null;
        }
    }

    private ResolvedTarget resolveTopicRecommendToggle() {
        List<MethodData> live = new ArrayList<>();
        try {
            ClassDataList classes = bridge.findClass(FindClass.create()
                    .searchPackages("com.coolapk.market.view.feedv8.component")
                    .matcher(ClassMatcher.create().className(
                            "TopicRecommend", StringMatchType.Contains, false)));
            List<ClassData> classList = new ArrayList<>(classes);
            if (!classList.isEmpty()) {
                MethodDataList methods = bridge.findMethod(FindMethod.create()
                        .searchInClass(classList)
                        .matcher(MethodMatcher.create().returnType("boolean")));
                for (MethodData method : methods) {
                    String name = method.getName().toLowerCase(Locale.ROOT);
                    if (name.contains("topicrecommend") && name.contains("enable")
                            && isLiveBoolean(method)) {
                        live.add(method);
                    }
                }
            }
        } catch (Throwable throwable) {
            log.info("resolver target=topicRecommend queryFailed=" + throwable);
        }
        log.info("resolver target=topicRecommend candidates=" + live.size()
                + " descriptors=" + describeMethods(live));
        if (live.size() == 1) {
            return methodTarget(TargetResolver.KEY_TOPIC_RECOMMEND,
                    "issue2_topic_toggle", live.get(0));
        }
        try {
            Class<?> type = Class.forName(TOPIC_CONFIG_CLASS, false, loader);
            Method matched = null;
            for (Method method : type.getDeclaredMethods()) {
                String name = method.getName().toLowerCase(Locale.ROOT);
                boolean returnsBoolean = method.getReturnType() == boolean.class
                        || method.getReturnType() == Boolean.class;
                if (returnsBoolean && name.contains("topicrecommend")
                        && name.contains("enable")) {
                    if (matched != null) {
                        return null;
                    }
                    matched = method;
                }
            }
            return reflectedMethodTarget(TargetResolver.KEY_TOPIC_RECOMMEND,
                    matched, "issue2_topic_reflection");
        } catch (Throwable throwable) {
            log.info("resolver target=topicRecommend reflectionUnavailable=" + throwable);
            return null;
        }
    }

    private ResolvedTarget resolveClassTarget(String key, String className) {
        try {
            ClassDataList classes = bridge.findClass(FindClass.create()
                    .searchPackages(packageName(className))
                    .matcher(ClassMatcher.create().className(className)));
            if (classes.size() == 1) {
                ResolvedTarget target = new ResolvedTarget(key,
                        "issue2_semantic_class", classes.get(0).getDescriptor(), "");
                if (TargetVerifier.verify(target, loader) == null) {
                    log.info("resolver target=" + key + " candidates=1 descriptor="
                            + target.describe());
                    return target;
                }
            }
        } catch (Throwable throwable) {
            log.info("resolver target=" + key + " classQueryFailed=" + throwable);
        }
        try {
            Class<?> type = Class.forName(className, false, loader);
            ResolvedTarget target = new ResolvedTarget(key,
                    "issue2_reflection_class", DescriptorUtils.classDescriptorOf(type), "");
            if (TargetVerifier.verify(target, loader) == null) {
                log.info("resolver target=" + key + " reflection=1 descriptor="
                        + target.describe());
                return target;
            }
        } catch (Throwable throwable) {
            log.info("resolver target=" + key + " reflectionUnavailable=" + throwable);
        }
        return null;
    }

    private ResolvedTarget methodTarget(String key, String source, MethodData method) {
        ResolvedTarget target = new ResolvedTarget(
                key, source, method.getDeclaredClassName(), method.getDescriptor());
        String problem = TargetVerifier.verify(target, loader);
        if (problem != null) {
            log.info("resolver target=" + key + " rejected=" + problem
                    + " descriptor=" + target.describe());
            return null;
        }
        return target;
    }

    private ResolvedTarget reflectedMethodTarget(String key, Method method, String source) {
        if (method == null) {
            return null;
        }
        ResolvedTarget target = new ResolvedTarget(key, source,
                DexSignUtil.getClassDescriptor(method.getDeclaringClass()),
                DexSignUtil.getDescriptor(method));
        return TargetVerifier.verify(target, loader) == null ? target : null;
    }

    private boolean isLiveBoolean(MethodData method) {
        Method live = liveMethod(method);
        return live != null && (live.getReturnType() == boolean.class
                || live.getReturnType() == Boolean.class)
                && !Modifier.isAbstract(live.getModifiers());
    }

    private Method liveMethod(MethodData method) {
        try {
            return method.getMethodInstance(loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String packageName(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? className : className.substring(0, dot);
    }

    private static String describeMethods(List<MethodData> methods) {
        StringBuilder result = new StringBuilder();
        for (MethodData method : methods) {
            if (result.length() > 0) {
                result.append('|');
            }
            result.append(method.getDescriptor());
        }
        return result.toString();
    }

    private static void put(Map<String, ResolvedTarget> targets, ResolvedTarget target) {
        if (target != null) {
            targets.put(target.key, target);
        }
    }
}
