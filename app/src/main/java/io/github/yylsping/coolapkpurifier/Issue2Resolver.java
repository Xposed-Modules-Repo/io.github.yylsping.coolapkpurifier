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

/** Resolves the optional Issue #2 targets. */
final class Issue2Resolver {
    private static final String AUTO_COMMENT_CLASS =
            "com.coolapk.market.view.cardlist.component.RecyclerViewItemFullVisibleControllerKt";
    private static final String TOPIC_CONFIG_CLASS =
            "com.coolapk.market.view.feedv8.component.TopicRecommendConfig";
    private static final String RELATED_HOLDER_CLASS =
            "com.coolapk.market.viewholder.RelatedDataViewHolder";
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
        if (coolapkMajor < 15) {
            return targets;
        }
        if (config.isEnabled(PurifierConfig.Feature.AUTO_COMMENT)) {
            put(targets, resolveAutoCommentEntry());
        }
        if (config.isEnabled(PurifierConfig.Feature.TOPIC_DEVICE_RECOMMEND)) {
            put(targets, resolveTopicRecommendToggle());
        }
        if (config.isEnabled(PurifierConfig.Feature.RELATED_DATA)) {
            ResolvedTarget getter = resolveRelatedDataGetter();
            put(targets, getter != null ? getter : resolveClassTarget(
                    TargetResolver.KEY_RELATED_DATA, RELATED_HOLDER_CLASS));
        }
        if (config.isEnabled(PurifierConfig.Feature.SAME_TOPIC_FEED)) {
            put(targets, resolveSameTopicTemplatePredicate());
        }
        if (config.isEnabled(PurifierConfig.Feature.DETAIL_SPONSOR)) {
            put(targets, resolveDetailSponsorGetter());
        }
        return targets;
    }

    private ResolvedTarget resolveAutoCommentEntry() {
        List<MethodData> live = new ArrayList<>();
        try {
            ClassDataList classes = bridge.findClass(FindClass.create()
                    .searchPackages(packageName(AUTO_COMMENT_CLASS))
                    .matcher(ClassMatcher.create()
                            .source("RecyclerViewItemFullVisibleController.kt",
                                    StringMatchType.Equals, false)
                            .className(AUTO_COMMENT_CLASS)));
            if (classes.size() == 1) {
                MethodDataList methods = bridge.findMethod(FindMethod.create()
                        .searchInClass(java.util.Collections.singleton(classes.get(0)))
                        .matcher(MethodMatcher.create()
                                .returnType("void")
                                .paramCount(1)));
                for (MethodData method : methods) {
                    Method reflected = liveMethod(method);
                    if (reflected != null
                            && TargetVerifier.isAutoCommentEntry(reflected)) {
                        live.add(method);
                    }
                }
            }
        } catch (Throwable throwable) {
            log.info("resolver target=autoComment sourceQueryFailed=" + throwable);
        }
        log.info("resolver target=autoComment entryCandidates=" + live.size()
                + " descriptors=" + describeMethods(live));
        MethodData unique = UniqueTargetSelector.only(live);
        if (unique != null) {
            return methodTarget(TargetResolver.KEY_AUTO_COMMENT,
                    "issue2_kotlin_source_shape", unique);
        }
        try {
            Class<?> type = Class.forName(AUTO_COMMENT_CLASS, false, loader);
            Method matched = null;
            for (Method method : type.getDeclaredMethods()) {
                if (TargetVerifier.isAutoCommentEntry(method)) {
                    if (matched != null) {
                        log.info("resolver target=autoComment reflectionAmbiguous class="
                                + AUTO_COMMENT_CLASS);
                        return null;
                    }
                    matched = method;
                }
            }
            return reflectedMethodTarget(TargetResolver.KEY_AUTO_COMMENT,
                    matched, "issue2_reflection_shape");
        } catch (Throwable throwable) {
            log.info("resolver target=autoComment reflectionUnavailable=" + throwable);
            return null;
        }
    }

    private ResolvedTarget resolveTopicRecommendToggle() {
        List<MethodData> live = new ArrayList<>();
        try {
            MethodData targetRowGetter = bridge.getMethodData(
                    "Lcom/coolapk/market/model/Feed;->getTargetRow()"
                            + "Lcom/coolapk/market/model/FeedTarget;");
            if (targetRowGetter != null) {
                for (MethodData caller : targetRowGetter.getCallers()) {
                    if (isTopicTargetRowAssembler(caller)) {
                        live.add(caller);
                    }
                }
            }
            log.info("resolver target=topicRecommend targetRowAssemblers=" + live.size()
                    + " descriptors=" + describeMethods(live));
            if (live.size() == 1) {
                return methodTarget(TargetResolver.KEY_TOPIC_RECOMMEND,
                        "issue2_target_row_assembler", live.get(0));
            }
        } catch (Throwable throwable) {
            log.info("resolver target=topicRecommend targetRowQueryFailed=" + throwable);
        }
        live.clear();
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

    private ResolvedTarget resolveDetailSponsorGetter() {
        List<MethodData> live = new ArrayList<>();
        try {
            MethodDataList methods = bridge.findMethod(FindMethod.create()
                    .searchPackages("com.coolapk.market.model")
                    .matcher(MethodMatcher.create()
                            .name("getDetailSponsorCard", StringMatchType.Equals, false)
                            .returnType("com.coolapk.market.model.Entity")
                            .paramCount(0)));
            for (MethodData method : methods) {
                Method reflected = liveMethod(method);
                if (reflected != null && !Modifier.isAbstract(reflected.getModifiers())
                        && inheritsFrom(reflected.getDeclaringClass(),
                        "com.coolapk.market.model.Feed")) {
                    live.add(method);
                }
            }
        } catch (Throwable throwable) {
            log.info("resolver target=detailSponsor getterQueryFailed=" + throwable);
        }
        log.info("resolver target=detailSponsor getterCandidates=" + live.size()
                + " descriptors=" + describeMethods(live));
        return live.size() == 1 ? methodTarget(TargetResolver.KEY_DETAIL_SPONSOR,
                "issue2_detail_model_getter", live.get(0)) : null;
    }

    private ResolvedTarget resolveRelatedDataGetter() {
        List<MethodData> live = new ArrayList<>();
        try {
            MethodDataList methods = bridge.findMethod(FindMethod.create()
                    .searchPackages("com.coolapk.market.model")
                    .matcher(MethodMatcher.create()
                            .name("getRelatedData", StringMatchType.Equals, false)
                            .returnType("java.util.List").paramCount(0)));
            for (MethodData method : methods) {
                Method reflected = liveMethod(method);
                if (reflected != null && TargetVerifier.isRelatedDataGetter(reflected)) {
                    live.add(method);
                }
            }
        } catch (Throwable throwable) {
            log.info("resolver target=relatedData getterQueryFailed=" + throwable);
        }
        log.info("resolver target=relatedData getterCandidates=" + live.size()
                + " descriptors=" + describeMethods(live));
        MethodData unique = UniqueTargetSelector.only(live);
        return unique == null ? null : methodTarget(TargetResolver.KEY_RELATED_DATA,
                "issue2_related_model_getter", unique);
    }

    private ResolvedTarget resolveSameTopicTemplatePredicate() {
        List<MethodData> live = new ArrayList<>();
        try {
            MethodDataList methods = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                            // Server entity-template metadata, not display text.
                            .usingStrings("feedRecommendListCard")
                            .returnType("boolean")
                            .paramCount(1)));
            for (MethodData method : methods) {
                Method reflected = liveMethod(method);
                if (reflected != null && isSameTopicTemplatePredicate(reflected)) {
                    live.add(method);
                }
            }
        } catch (Throwable throwable) {
            log.info("resolver target=sameTopicFeed templateQueryFailed=" + throwable);
        }
        log.info("resolver target=sameTopicFeed templatePredicates=" + live.size()
                + " descriptors=" + describeMethods(live));
        MethodData unique = UniqueTargetSelector.only(live);
        return unique == null ? null : methodTarget(TargetResolver.KEY_SAME_TOPIC_FEED,
                "issue2_entity_template_predicate", unique);
    }

    private boolean isSameTopicTemplatePredicate(Method method) {
        Class<?>[] params = method.getParameterTypes();
        return Modifier.isStatic(method.getModifiers())
                && !Modifier.isAbstract(method.getModifiers())
                && method.getReturnType() == boolean.class
                && params.length == 1
                && params[0] == Object.class
                && inheritsFrom(method.getDeclaringClass(),
                "com.coolapk.market.view.cardlist.EntityListFragment");
    }

    private boolean isTopicTargetRowAssembler(MethodData method) {
        Method reflected = liveMethod(method);
        if (reflected == null || !Modifier.isStatic(reflected.getModifiers())
                || Modifier.isAbstract(reflected.getModifiers())
                || !"kotlin.Unit".equals(reflected.getReturnType().getName())) {
            return false;
        }
        Class<?>[] params = reflected.getParameterTypes();
        return params.length == 4
                && "com.coolapk.market.model.Feed".equals(params[0].getName())
                && params[1] == reflected.getDeclaringClass()
                && "androidx.compose.runtime.Composer".equals(params[2].getName())
                && params[3] == int.class;
    }

    private static boolean inheritsFrom(Class<?> type, String parentName) {
        Class<?> cursor = type;
        while (cursor != null) {
            if (parentName.equals(cursor.getName())) {
                return true;
            }
            cursor = cursor.getSuperclass();
        }
        return false;
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
