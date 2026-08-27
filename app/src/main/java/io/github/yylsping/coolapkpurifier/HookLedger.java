package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central hook lifecycle ledger (Mode A-ZF Phase 5).
 *
 * <p>Every framework and business hook installation reports here; retirement
 * closes the entry with a reason. The terminal READY/DEGRADED summary is the
 * verifiable answer to "are any framework hooks still active after READY".
 *
 * <p>Entries are idempotent by id: re-recording an existing id keeps the
 * original install time, and retiring twice is a no-op. Failed unhooks stay
 * active — an entry whose unhook threw must not be reported as retired.
 */
final class HookLedger {
    enum Layer {
        FRAMEWORK, BUSINESS
    }

    static final class Entry {
        final String id;
        final Layer layer;
        final String owner;
        final String target;
        final long installedAt;
        private Long retiredAt;
        private String retireReason;

        private Entry(String id, Layer layer, String owner, String target) {
            this.id = id;
            this.layer = layer;
            this.owner = owner;
            this.target = target;
            this.installedAt = System.currentTimeMillis();
        }

        boolean isActive() {
            return retiredAt == null;
        }

        String retireReason() {
            return retireReason;
        }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    /** Idempotent: re-recording an existing id keeps the original entry. */
    synchronized void record(Layer layer, String owner, String id, String target) {
        if (id == null || id.isEmpty()) {
            return;
        }
        entries.putIfAbsent(id, new Entry(id, layer, owner, target));
    }

    /** @return true when an active entry was closed by this call. */
    synchronized boolean retire(String id, String reason) {
        Entry entry = entries.get(id);
        if (entry == null || !entry.isActive()) {
            return false;
        }
        entry.retiredAt = System.currentTimeMillis();
        entry.retireReason = reason;
        return true;
    }

    synchronized boolean isActive(String id) {
        Entry entry = entries.get(id);
        return entry != null && entry.isActive();
    }

    synchronized List<String> activeIds(Layer layer) {
        List<String> active = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (entry.layer == layer && entry.isActive()) {
                active.add(entry.id);
            }
        }
        return active;
    }

    synchronized int count() {
        return entries.size();
    }

    synchronized boolean hasActiveFrameworkHooks() {
        return !activeIds(Layer.FRAMEWORK).isEmpty();
    }

    /** Machine-readable one-liner emitted at terminal states. */
    synchronized String summaryLine(String stateLabel) {
        return "hook ledger state=" + stateLabel
                + " frameworkActive=" + hasActiveFrameworkHooks()
                + " frameworkActiveHooks=" + activeIds(Layer.FRAMEWORK)
                + " businessActiveHooks=" + activeIds(Layer.BUSINESS);
    }

    /** Human-readable listing for diagnostics. */
    synchronized String describe() {
        StringBuilder sb = new StringBuilder();
        for (Entry entry : entries.values()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(entry.layer).append(' ').append(entry.id)
                    .append(" owner=").append(entry.owner)
                    .append(" target=").append(entry.target)
                    .append(" active=").append(entry.isActive());
            if (!entry.isActive()) {
                sb.append(" retiredBecause=").append(entry.retireReason);
            }
        }
        return sb.toString();
    }
}
