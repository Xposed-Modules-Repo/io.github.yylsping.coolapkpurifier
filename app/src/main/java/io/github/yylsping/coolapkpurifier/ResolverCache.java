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
import java.util.LinkedHashMap;
import java.util.Map;

final class ResolverCache {
    private static final String FILE_NAME = "coolapk_purifier_resolved_v2.json";

    private final File file;

    ResolverCache(Context appContext) {
        this.file = new File(appContext.getFilesDir(), FILE_NAME);
    }

    TargetIdentity loadIdentity() {
        JSONObject root = readJson();
        if (root == null) {
            return null;
        }
        return TargetIdentity.fromJson(root.optJSONObject("identity"));
    }

    Map<String, ResolvedTarget> loadEntries() {
        Map<String, ResolvedTarget> entries = new LinkedHashMap<>();
        JSONObject root = readJson();
        if (root == null) {
            return entries;
        }
        JSONArray array = root.optJSONArray("targets");
        if (array == null) {
            return entries;
        }
        for (int i = 0; i < array.length(); i++) {
            ResolvedTarget target = ResolvedTarget.fromJson(array.optJSONObject(i));
            if (target != null && target.key != null && !target.key.isEmpty()) {
                entries.put(target.key, target);
            }
        }
        return entries;
    }

    void save(TargetIdentity identity, Map<String, ResolvedTarget> targets) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 2);
            root.put("identity", identity.toJson());
            JSONArray array = new JSONArray();
            for (ResolvedTarget target : targets.values()) {
                array.put(target.toJson());
            }
            root.put("targets", array);

            File temp = new File(file.getParentFile(), file.getName() + ".tmp");
            try (OutputStream out = new FileOutputStream(temp)) {
                out.write(root.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (file.exists() && !file.delete()) {
                // Keep the old cache on failure; the identity guard invalidates it next launch.
                return;
            }
            if (!temp.renameTo(file)) {
                // Best effort. Some OEM file systems dislike rename over an open fd.
                try (OutputStream out = new FileOutputStream(file)) {
                    out.write(root.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Throwable ignored) {
            // Cache is an optimization. Any persistence failure just forces a rescan.
        }
    }

    void clear() {
        try {
            if (file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        } catch (Throwable ignored) {
        }
    }

    private JSONObject readJson() {
        if (!file.isFile()) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) Math.min(file.length(), 1024 * 1024)];
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
}
