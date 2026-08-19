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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the splash targets. This resolver runs before feed/getter
 * resolution and never waits for the normal resolver.
 *
 * <p>Coolapk 16.5.1 ships several splash-family activities (brand splash plus
 * ad splash) with independent onCreate hierarchies, so every verified
 * candidate is accepted instead of requiring exactly one. Which candidates
 * are observable depends on how much runtime DEX the loader has appended when
 * the query runs, so resolution is cumulative across sessions.
 *
 * <p>The three tiers are a genuine UNION, not a fallback chain: strong
 * fingerprint, weak class-name scan and legacy-name reflection all run every
 * session and results are deduplicated by class descriptor. A tier finding
 * one candidate must not stop the other tiers from discovering a second
 * splash activity that is already loadable (e.g. SplashActivity visible via
 * Class.forName while the DexKit snapshot only contains SplashAdActivity).
 * The strong tier merely contributes the higher-confidence source label.
 */
final class SplashCriticalResolver {
    static final String SOURCE_FINGERPRINT_STRONG = "fingerprint_strong";
    static final String SOURCE_FINGERPRINT_WEAK = "fingerprint_weak";
    static final String SOURCE_LEGACY_NAME = "legacy_name";

    /** DexKit-backed tier queries; injectable for union-order tests. */
    interface SplashQueries {
        List<ResolvedTarget> queryStrong();

        List<ResolvedTarget> queryWeak();
    }

    private final SplashQueries queries;
    private final ClassLoader loader;
    private final ModuleLog log;

    SplashCriticalResolver(DexKitBridge bridge, ClassLoader loader, ModuleLog log) {
        this(new DexKitSplashQueries(bridge, loader, log), loader, log);
    }

    SplashCriticalResolver(SplashQueries queries, ClassLoader loader, ModuleLog log) {
        this.queries = queries;
        this.loader = loader;
        this.log = log;
    }

    List<ResolvedTarget> resolve() {
        Map<String, ResolvedTarget> merged = new LinkedHashMap<>();
        for (ResolvedTarget target : queries.queryStrong()) {
            merged.putIfAbsent(target.classDescriptor, target);
        }
        for (ResolvedTarget target : queries.queryWeak()) {
            merged.putIfAbsent(target.classDescriptor, target);
        }
        // Legacy reflection bypasses the DexKit snapshot entirely: a class
        // the bridge cannot see yet may still be loadable right now.
        for (ResolvedTarget target : queryLegacyNames()) {
            merged.putIfAbsent(target.classDescriptor, target);
        }
        if (merged.isEmpty()) {
            log.info("resolver splash path=strong+weak+legacy candidates=0 failClosed=true"
                    + " frameworkFirstLaunchGate=true");
        }
        return new ArrayList<>(merged.values());
    }

    /** 2.0.1-parity reflection fallback over historically known names. */
    private List<ResolvedTarget> queryLegacyNames() {
        List<ResolvedTarget> targets = new ArrayList<>();
        for (String name : TargetResolver.LEGACY_SPLASH_CLASS_NAMES) {
            try {
                Class<?> type = Class.forName(name, false, loader);
                if (type == null || !Activity.class.isAssignableFrom(type)) {
                    continue;
                }
                if (TargetVerifier.findOnCreate(type) == null) {
                    continue;
                }
                ResolvedTarget target = new ResolvedTarget(
                        TargetResolver.KEY_SPLASH_BASE,
                        SOURCE_LEGACY_NAME,
                        DescriptorUtils.classDescriptorOf(type),
                        "");
                targets.add(target);
                log.info("resolver splash path=legacy_name candidates=1 descriptor="
                        + target.describe());
            } catch (Throwable ignored) {
                // Name not present in this Coolapk version; expected.
            }
        }
        return targets;
    }

    private static final class DexKitSplashQueries implements SplashQueries {
        private final DexKitBridge bridge;
        private final ClassLoader loader;
        private final ModuleLog log;

        DexKitSplashQueries(DexKitBridge bridge, ClassLoader loader, ModuleLog log) {
            this.bridge = bridge;
            this.loader = loader;
            this.log = log;
        }

        @Override
        public List<ResolvedTarget> queryStrong() {
            try {
                ClassDataList raw = bridge.findClass(FindClass.create()
                        .searchPackages("com.coolapk.market.view.splash")
                        .matcher(ClassMatcher.create()
                                .source("Splash", StringMatchType.Contains, false)));
                List<ClassData> candidates = activityCandidates(raw);
                logCandidates(log, "strong", candidates);
                List<ResolvedTarget> targets = new ArrayList<>();
                for (ClassData candidate : candidates) {
                    if (verifyClass(candidate)) {
                        ResolvedTarget resolved = target(candidate, SOURCE_FINGERPRINT_STRONG);
                        if (resolved != null) {
                            targets.add(resolved);
                        }
                    } else {
                        log.info("resolver splash path=fingerprint_strong rejected=verification"
                                + " class=" + candidate.getDescriptor());
                    }
                }
                return targets;
            } catch (Throwable throwable) {
                log.error("resolver splash path=fingerprint_strong queryFailed", throwable);
                return new ArrayList<>();
            }
        }

        @Override
        public List<ResolvedTarget> queryWeak() {
            try {
                ClassDataList raw = bridge.findClass(FindClass.create()
                        .searchPackages("com.coolapk.market.view.splash")
                        .matcher(ClassMatcher.create()
                                .className("Splash", StringMatchType.Contains, false)));
                List<ClassData> candidates = activityCandidates(raw);
                logCandidates(log, "weak", candidates);
                List<ResolvedTarget> targets = new ArrayList<>();
                for (ClassData candidate : candidates) {
                    if (verifyClass(candidate)) {
                        ResolvedTarget resolved = target(candidate, SOURCE_FINGERPRINT_WEAK);
                        if (resolved != null) {
                            targets.add(resolved);
                        }
                    } else {
                        log.info("resolver splash path=fingerprint_weak rejected=verification"
                                + " class=" + candidate.getDescriptor());
                    }
                }
                return targets;
            } catch (Throwable throwable) {
                log.error("resolver splash path=fingerprint_weak queryFailed", throwable);
                return new ArrayList<>();
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
                    // Not loadable in the current runtime DEX set; may appear in a
                    // later session after the loader appends more DEX.
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
            // The splash verifier accepts an empty method descriptor; a non-empty
            // one that fails verification means the record itself is unusable.
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

        private static void logCandidates(ModuleLog log, String stage,
                                          List<ClassData> candidates) {
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
}
