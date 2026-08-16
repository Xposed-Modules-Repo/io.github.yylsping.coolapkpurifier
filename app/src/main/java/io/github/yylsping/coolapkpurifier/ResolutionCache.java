package io.github.yylsping.coolapkpurifier;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-version resolver cache.
 *
 * <p>Stores up to {@link CachePolicy#MAX_ENTRIES} successful identities with
 * total bytes at most {@link CachePolicy#MAX_TOTAL_BYTES}. Entries are keyed
 * by stableTargetIdentity, never by versionCode. Reads never clear other
 * identity entries.
 */
final class ResolutionCache {
    static final int RESOLVER_SCHEMA_VERSION = 1;

    private static final String FILE_NAME = "coolapk_purifier_cache_v3.json";

    private final File file;
    private final Object lock = new Object();

    ResolutionCache(Context appContext) {
        this.file = new File(appContext.getFilesDir(), FILE_NAME);
    }

    Map<String, ResolvedTarget> loadTargets(TargetIdentity identity) {
        CacheEntry entry = loadEntry(identity);
        return entry == null ? new LinkedHashMap<>() : new LinkedHashMap<>(entry.targets);
    }

    boolean isRecoveryAttempted(TargetIdentity identity) {
        synchronized (lock) {
            JSONObject root = readJson();
            if (root == null) {
                return false;
            }
            JSONArray recovered = root.optJSONArray("recoveryAttempted");
            if (recovered == null) {
                return false;
            }
            for (int i = 0; i < recovered.length(); i++) {
                if (identity.token.equals(recovered.optString(i, null))) {
                    return true;
                }
            }
            return false;
        }
    }

    void markRecoveryAttempted(TargetIdentity identity) {
        synchronized (lock) {
            try {
                JSONObject root = readJson();
                if (root == null) {
                    root = new JSONObject();
                    root.put("schema", RESOLVER_SCHEMA_VERSION);
                    root.put("entries", new JSONArray());
                }
                JSONArray recovered = root.optJSONArray("recoveryAttempted");
                if (recovered == null) {
                    recovered = new JSONArray();
                    root.put("recoveryAttempted", recovered);
                }
                boolean exists = false;
                for (int i = 0; i < recovered.length(); i++) {
                    if (identity.token.equals(recovered.optString(i, null))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    recovered.put(identity.token);
                }
                writeJson(root);
            } catch (Throwable ignored) {
            }
        }
    }

    void saveTargets(TargetIdentity identity, Map<String, ResolvedTarget> targets) {
        synchronized (lock) {
            try {
                JSONObject root = readJson();
                if (root == null) {
                    root = new JSONObject();
                    root.put("schema", RESOLVER_SCHEMA_VERSION);
                    root.put("entries", new JSONArray());
                }
                if (root.optInt("schema", 0) != RESOLVER_SCHEMA_VERSION) {
                    root = new JSONObject();
                    root.put("schema", RESOLVER_SCHEMA_VERSION);
                    root.put("entries", new JSONArray());
                }

                JSONArray entries = root.optJSONArray("entries");
                if (entries == null) {
                    entries = new JSONArray();
                    root.put("entries", entries);
                }

                JSONObject replacement = encodeEntry(identity, targets, System.currentTimeMillis());
                JSONArray merged = new JSONArray();
                merged.put(replacement);
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject candidate = entries.optJSONObject(i);
                    TargetIdentity candidateIdentity =
                            TargetIdentity.fromJson(candidate == null ? null
                                    : candidate.optJSONObject("identity"));
                    if (candidateIdentity != null
                            && identity.token.equals(candidateIdentity.token)) {
                        continue;
                    }
                    merged.put(candidate);
                }
                entries = evictEntries(merged, identity.token);
                root.put("entries", entries);
                writeJson(root);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Invalidates only the current identity; other entries stay untouched. */
    void removeTargets(TargetIdentity identity) {
        synchronized (lock) {
            try {
                JSONObject root = readJson();
                if (root == null) {
                    return;
                }
                JSONArray entries = root.optJSONArray("entries");
                if (entries == null) {
                    return;
                }
                JSONArray kept = new JSONArray();
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject candidate = entries.optJSONObject(i);
                    TargetIdentity candidateIdentity =
                            TargetIdentity.fromJson(candidate == null ? null
                                    : candidate.optJSONObject("identity"));
                    if (candidateIdentity != null
                            && identity.token.equals(candidateIdentity.token)) {
                        continue;
                    }
                    kept.put(candidate);
                }
                root.put("entries", kept);
                writeJson(root);
            } catch (Throwable ignored) {
            }
        }
    }

    private CacheEntry loadEntry(TargetIdentity identity) {
        synchronized (lock) {
            JSONObject root = readJson();
            if (root == null || root.optInt("schema", 0) != RESOLVER_SCHEMA_VERSION) {
                return null;
            }
            JSONArray entries = root.optJSONArray("entries");
            if (entries == null) {
                return null;
            }
            for (int i = 0; i < entries.length(); i++) {
                JSONObject candidate = entries.optJSONObject(i);
                if (candidate == null) {
                    continue;
                }
                TargetIdentity candidateIdentity =
                        TargetIdentity.fromJson(candidate.optJSONObject("identity"));
                if (candidateIdentity == null || !identity.sameTarget(candidateIdentity)) {
                    continue;
                }
                Map<String, ResolvedTarget> targets = decodeTargets(candidate);
                long lastUsedAt = candidate.optLong("lastUsedAt", 0L);
                touch(identity, lastUsedAt);
                return new CacheEntry(targets, lastUsedAt);
            }
            return null;
        }
    }

    private void touch(TargetIdentity identity, long oldLastUsedAt) {
        try {
            JSONObject root = readJson();
            if (root == null) {
                return;
            }
            JSONArray entries = root.optJSONArray("entries");
            if (entries == null) {
                return;
            }
            boolean changed = false;
            for (int i = 0; i < entries.length(); i++) {
                JSONObject candidate = entries.optJSONObject(i);
                TargetIdentity candidateIdentity =
                        TargetIdentity.fromJson(candidate == null ? null
                                : candidate.optJSONObject("identity"));
                if (candidateIdentity != null && identity.token.equals(candidateIdentity.token)
                        && oldLastUsedAt == candidate.optLong("lastUsedAt", 0L)) {
                    candidate.put("lastUsedAt", System.currentTimeMillis());
                    changed = true;
                    break;
                }
            }
            if (changed) {
                writeJson(root);
            }
        } catch (Throwable ignored) {
        }
    }

    private JSONArray evictEntries(JSONArray entries, String protectKey) throws JSONException {
        List<CachePolicy.Record> records = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            String key = entry.optJSONObject("identity") == null ? ""
                    : entry.optJSONObject("identity").optString("token", "");
            records.add(new CachePolicy.Record(
                    key,
                    entry.optLong("lastUsedAt", 0L),
                    entry.toString().getBytes(StandardCharsets.UTF_8).length));
        }
        List<CachePolicy.Record> kept = CachePolicy.evict(records, protectKey);
        JSONArray result = new JSONArray();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            String key = entry == null || entry.optJSONObject("identity") == null ? ""
                    : entry.optJSONObject("identity").optString("token", "");
            for (CachePolicy.Record record : kept) {
                if (record.key.equals(key)) {
                    result.put(entry);
                    break;
                }
            }
        }
        return result;
    }

    private JSONObject encodeEntry(TargetIdentity identity,
                                   Map<String, ResolvedTarget> targets,
                                   long lastUsedAt) throws JSONException {
        JSONObject entry = new JSONObject();
        entry.put("identity", identity.toJson());
        entry.put("lastUsedAt", lastUsedAt);
        JSONArray array = new JSONArray();
        for (ResolvedTarget target : targets.values()) {
            array.put(target.toJson());
        }
        entry.put("targets", array);
        return entry;
    }

    private Map<String, ResolvedTarget> decodeTargets(JSONObject entry) {
        Map<String, ResolvedTarget> targets = new LinkedHashMap<>();
        JSONArray array = entry == null ? null : entry.optJSONArray("targets");
        if (array == null) {
            return targets;
        }
        for (int i = 0; i < array.length(); i++) {
            ResolvedTarget target = ResolvedTarget.fromJson(array.optJSONObject(i));
            if (target != null && target.key != null && !target.key.isEmpty()) {
                targets.put(target.key, target);
            }
        }
        return targets;
    }

    private JSONObject readJson() {
        if (!file.isFile()) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) Math.min(file.length(), CachePolicy.MAX_TOTAL_BYTES + 1)];
            int offset = 0;
            while (offset < bytes.length) {
                int read = in.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            if (offset == 0) {
                return null;
            }
            return new JSONObject(new String(bytes, 0, offset, StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void writeJson(JSONObject root) {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(root.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.getFD().sync();
        } catch (Throwable ignored) {
            return;
        }
        try {
            if (file.exists() && !file.delete() && file.length() > 0) {
                return;
            }
            if (!temp.renameTo(file)) {
                try (OutputStream out = new FileOutputStream(file)) {
                    out.write(root.toString().getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static final class CacheEntry {
        final Map<String, ResolvedTarget> targets;
        final long lastUsedAt;

        CacheEntry(Map<String, ResolvedTarget> targets, long lastUsedAt) {
            this.targets = targets;
            this.lastUsedAt = lastUsedAt;
        }
    }
}
