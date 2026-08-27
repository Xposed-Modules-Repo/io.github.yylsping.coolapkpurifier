package io.github.yylsping.coolapkpurifier;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.UsingFieldData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Resolves one dedicated renderer using business evidence, never a name guess. */
final class ReplySelfDrawResolver {
    static final String SOURCE_MARKER =
            "com.coolapk.market.view.feed.reply.FeedReplySelfDrawViewHolder.";
    static final String RENDER_MARKER = "[FeedReplySelfDraw.renderSelfDrawAd] mainImageUrl: ";
    private final DexKitBridge bridge;
    private final ClassLoader loader;
    private final ModuleLog log;

    ReplySelfDrawResolver(DexKitBridge bridge, ClassLoader loader, ModuleLog log) {
        this.bridge = bridge;
        this.loader = loader;
        this.log = log;
    }

    ResolvedTarget resolve() {
        List<Candidate> candidates = new ArrayList<>();
        try {
            List<MethodData> registrations = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create().declaredClass(ReplySelfDrawTarget.FRAGMENT)
                            .name("onActivityCreated").returnType("void")
                            .paramTypes("android.os.Bundle")
                            .addUsingString(ReplySelfDrawTarget.TEMPLATE, StringMatchType.Equals)));
            if (registrations.size() != 1) return null;
            MethodData registration = registrations.get(0);
            for (ClassData type : bridge.findClass(FindClass.create().matcher(ClassMatcher.create()
                    .addUsingString(SOURCE_MARKER, StringMatchType.StartsWith)
                    .addUsingString(RENDER_MARKER, StringMatchType.Equals)))) {
                Class<?> live = type.getInstance(loader);
                Field layout = ReplySelfDrawTarget.layoutField(live, loader);
                boolean registeredLayout = false;
                for (UsingFieldData used : registration.getUsingFields()) {
                    if (layout != null && layout.equals(used.getField().getFieldInstance(loader))) {
                        registeredLayout = true;
                    }
                }
                boolean replyFactory = false;
                for (MethodData method : type.getMethods()) {
                    if (!"<init>".equals(method.getName())) continue;
                    for (MethodData caller : method.getCallers()) {
                        if (ReplySelfDrawTarget.FRAGMENT.equals(caller.getDeclaredClassName())
                                && ReplySelfDrawTarget.VIEW_HOLDER.equals(caller.getReturnTypeName())
                                && caller.getParamTypeNames().equals(java.util.Arrays.asList(
                                ReplySelfDrawTarget.FRAGMENT, "android.view.View"))) {
                            replyFactory = true;
                        }
                    }
                }
                for (MethodData method : type.getMethods()) {
                    if (!"void".equals(method.getReturnTypeName())
                            || !method.getParamTypeNames().equals(
                            java.util.Collections.singletonList("java.lang.Object"))) continue;
                    candidates.add(new Candidate(method.getMethodInstance(loader),
                            registeredLayout, replyFactory, method.getUsingStrings()));
                }
            }
        } catch (Throwable failure) {
            log.info("resolver replySelfDraw evidenceUnavailable=" + failure);
            return null;
        }
        ResolvedTarget target = select(candidates, loader);
        log.info("resolver replySelfDraw candidates=" + candidates.size()
                + " verified=" + (target == null ? "noneOrAmbiguous" : target.describe()));
        return target;
    }

    static final class Candidate {
        final Method method;
        final boolean registeredLayout;
        final boolean replyFactory;
        final List<String> strings;

        Candidate(Method method, boolean registeredLayout, boolean replyFactory, List<String> strings) {
            this.method = method;
            this.registeredLayout = registeredLayout;
            this.replyFactory = replyFactory;
            this.strings = strings;
        }
    }

    static ResolvedTarget select(List<Candidate> candidates, ClassLoader loader) {
        LinkedHashMap<String, ResolvedTarget> verified = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            if (!candidate.registeredLayout || !candidate.replyFactory
                    || !candidate.strings.contains("sponsorStyle")
                    || !candidate.strings.contains("rewardVideoVisibleInLayout")
                    || !ReplySelfDrawTarget.isBindMethod(candidate.method, loader)) continue;
            Method method = candidate.method;
            ResolvedTarget target = new ResolvedTarget(TargetResolver.KEY_REPLY_SELF_DRAW,
                    "reply_self_draw_registration_v1", DescriptorUtils.classDescriptorOf(
                    method.getDeclaringClass()), org.luckypray.dexkit.util.DexSignUtil.getDescriptor(method));
            verified.put(target.methodDescriptor, target);
        }
        return UniqueTargetSelector.only(new ArrayList<>(verified.values()));
    }
}
