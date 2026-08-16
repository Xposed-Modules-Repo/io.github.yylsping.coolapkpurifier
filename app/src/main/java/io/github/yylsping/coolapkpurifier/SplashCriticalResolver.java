package io.github.yylsping.coolapkpurifier;

import android.app.Activity;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.MethodData;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves only the splash target. This resolver runs before feed/getter
 * resolution and never waits for the normal resolver.
 */
final class SplashCriticalResolver {
    static final String SOURCE_FINGERPRINT_STRONG = "fingerprint_strong";
    static final String SOURCE_FINGERPRINT_WEAK = "fingerprint_weak";

    private final DexKitBridge bridge;
    private final ClassLoader loader;
    private final ModuleLog log;

    SplashCriticalResolver(DexKitBridge bridge, ClassLoader loader, ModuleLog log) {
        this.bridge = bridge;
        this.loader = loader;
        this.log = log;
    }

    ResolvedTarget resolve() {
        ClassData type = queryStrong();
        if (type != null) {
            return target(type, SOURCE_FINGERPRINT_STRONG);
        }
        type = queryWeak();
        if (type != null) {
            return target(type, SOURCE_FINGERPRINT_WEAK);
        }
        log.info("resolver splash path=all candidates=0 failClosed=true"
                + " frameworkFirstLaunchGate=true");
        return null;
    }

    private ClassData queryStrong() {
        try {
            ClassDataList raw = bridge.findClass(FindClass.create()
                    .searchPackages("com.coolapk.market.view.splash")
                    .matcher(ClassMatcher.create()
                            .source("Splash", StringMatchType.Contains, false)));
            List<ClassData> candidates = activityCandidates(raw);
            logCandidates("strong", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            ClassData candidate = candidates.get(0);
            if (!verifyClass(candidate)) {
                log.info("resolver splash path=fingerprint_strong rejected=verification class="
                        + candidate.getDescriptor());
                return null;
            }
            return candidate;
        } catch (Throwable throwable) {
            log.error("resolver splash path=fingerprint_strong queryFailed", throwable);
            return null;
        }
    }

    private ClassData queryWeak() {
        try {
            ClassDataList raw = bridge.findClass(FindClass.create()
                    .searchPackages("com.coolapk.market.view.splash")
                    .matcher(ClassMatcher.create()
                            .className("Splash", StringMatchType.Contains, false)));
            List<ClassData> candidates = activityCandidates(raw);
            logCandidates("weak", candidates);
            if (candidates.size() != 1) {
                return null;
            }
            ClassData candidate = candidates.get(0);
            if (!verifyClass(candidate)) {
                log.info("resolver splash path=fingerprint_weak rejected=verification class="
                        + candidate.getDescriptor());
                return null;
            }
            return candidate;
        } catch (Throwable throwable) {
            log.error("resolver splash path=fingerprint_weak queryFailed", throwable);
            return null;
        }
    }

    private List<ClassData> activityCandidates(ClassDataList raw) {
        List<ClassData> candidates = new ArrayList<>();
        if (raw == null) {
            return candidates;
        }
        for (ClassData data : raw) {
            try {
                Class<?> type = data.getInstance(loader);
                if (type != null && Activity.class.isAssignableFrom(type)) {
                    candidates.add(data);
                }
            } catch (Throwable ignored) {
            }
        }
        return candidates;
    }

    private boolean verifyClass(ClassData data) {
        try {
            Class<?> type = data.getInstance(loader);
            if (type == null || !Activity.class.isAssignableFrom(type)) {
                return false;
            }
            return TargetVerifier.findOnCreate(type) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ResolvedTarget target(ClassData type, String source) {
        MethodData onCreate = null;
        if (type.getMethods() != null) {
            for (MethodData method : type.getMethods()) {
                if ("onCreate".equals(method.getName())
                        && method.getParamTypeNames() != null
                        && method.getParamTypeNames().size() == 1
                        && "android.os.Bundle".equals(method.getParamTypeNames().get(0))) {
                    onCreate = method;
                    break;
                }
            }
        }
        ResolvedTarget target = new ResolvedTarget(
                TargetResolver.KEY_SPLASH_BASE,
                source,
                type.getDescriptor(),
                onCreate == null ? "" : onCreate.getDescriptor());
        String problem = TargetVerifier.verify(target, loader);
        if (problem != null) {
            log.info("resolver splash path=" + source + " rejected=verification reason="
                    + problem + " target=" + target.describe());
            return null;
        }
        log.info("resolver splash path=" + source + " candidates=1 descriptor="
                + target.describe());
        return target;
    }

    private void logCandidates(String stage, List<ClassData> candidates) {
        StringBuilder sb = new StringBuilder();
        for (ClassData candidate : candidates) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(candidate);
        }
        log.info("resolver splash path=fingerprint_" + stage
                + " candidates=" + candidates.size()
                + (sb.length() == 0 ? "" : " list=" + sb));
    }
}
