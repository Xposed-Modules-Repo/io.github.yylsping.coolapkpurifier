package io.github.yylsping.coolapkpurifier;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import io.github.libxposed.api.XposedInterface.HookHandle;

/** Owns discovery-only global hooks so terminal cleanup is explicit and testable. */
final class LazyHookRegistry {
    enum HookSite {
        LOAD_CLASS_ONE_ARG,
        LOAD_CLASS_TWO_ARG
    }

    static final class RetireResult {
        final int unhookedThisClose;
        final int failedThisClose;
        final int totalUnhooked;
        final int totalFailures;
        final int remaining;
        final boolean logicalEnabled;

        RetireResult(int unhooked, int failed, int remaining) {
            this(unhooked, failed, unhooked, failed, remaining, false);
        }

        RetireResult(int unhookedThisClose, int failedThisClose,
                     int totalUnhooked, int totalFailures,
                     int remaining, boolean logicalEnabled) {
            this.unhookedThisClose = unhookedThisClose;
            this.failedThisClose = failedThisClose;
            this.totalUnhooked = totalUnhooked;
            this.totalFailures = totalFailures;
            this.remaining = remaining;
            this.logicalEnabled = logicalEnabled;
        }

        boolean isActive() {
            return remaining > 0;
        }
    }

    private final Map<HookSite, HookHandle> handles = new EnumMap<>(HookSite.class);
    private volatile boolean logicalEnabled;
    private volatile boolean permanentlyDisabled;
    private int totalUnhooked;
    private int totalFailures;

    synchronized boolean activate() {
        if (permanentlyDisabled) {
            logicalEnabled = false;
            return false;
        }
        logicalEnabled = true;
        return true;
    }

    boolean isLogicalEnabled() {
        return logicalEnabled;
    }

    synchronized void put(HookSite site, HookHandle handle) {
        if (handle != null) {
            if (handles.containsKey(site)) {
                throw new IllegalStateException("duplicate hook site " + site);
            }
            handles.put(site, handle);
        }
    }

    synchronized boolean contains(HookSite site) {
        return handles.containsKey(site);
    }

    synchronized Set<HookSite> missingSites() {
        java.util.EnumSet<HookSite> missing = java.util.EnumSet.allOf(HookSite.class);
        missing.removeAll(handles.keySet());
        return missing;
    }

    synchronized boolean isActive() {
        return !handles.isEmpty();
    }

    synchronized int size() {
        return handles.size();
    }

    synchronized RetireResult retire() {
        // Make every residual framework interceptor inert before attempting
        // unhook. A failed unhook can therefore never perform discovery work.
        logicalEnabled = false;
        int retired = 0;
        int failed = 0;
        Map<HookSite, HookHandle> unresolved = new EnumMap<>(HookSite.class);
        for (Map.Entry<HookSite, HookHandle> entry : handles.entrySet()) {
            try {
                entry.getValue().unhook();
                retired++;
            } catch (Throwable ignored) {
                // Keep the reference: an exception cannot honestly be
                // reported as an inactive framework hook.
                failed++;
                unresolved.put(entry.getKey(), entry.getValue());
            }
        }
        handles.clear();
        handles.putAll(unresolved);
        totalUnhooked += retired;
        totalFailures += failed;
        return new RetireResult(retired, failed, totalUnhooked, totalFailures,
                handles.size(), logicalEnabled);
    }

    synchronized RetireResult retirePermanently() {
        permanentlyDisabled = true;
        return retire();
    }
}
