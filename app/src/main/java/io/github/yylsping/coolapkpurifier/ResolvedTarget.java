package io.github.yylsping.coolapkpurifier;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * One persisted resolution record. Descriptors are DexKit descriptors:
 * class: Lcom/example/Foo;  method: Lcom/example/Foo;->bar()V
 */
final class ResolvedTarget {
    final String key;
    final String source;
    final String classDescriptor;
    final String methodDescriptor;
    final long resolvedAt;

    ResolvedTarget(String key, String source, String classDescriptor, String methodDescriptor) {
        this(key, source, classDescriptor, methodDescriptor, System.currentTimeMillis());
    }

    private ResolvedTarget(String key, String source, String classDescriptor,
                           String methodDescriptor, long resolvedAt) {
        this.key = key;
        this.source = source;
        this.classDescriptor = classDescriptor;
        this.methodDescriptor = methodDescriptor;
        this.resolvedAt = resolvedAt;
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("key", key);
        json.put("source", source);
        json.put("class", String.valueOf(classDescriptor));
        json.put("method", String.valueOf(methodDescriptor));
        json.put("at", resolvedAt);
        return json;
    }

    static ResolvedTarget fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        return new ResolvedTarget(
                json.optString("key", ""),
                json.optString("source", "cache"),
                json.optString("class", ""),
                json.optString("method", ""),
                json.optLong("at", 0L));
    }

    String describe() {
        return key + " class=" + classDescriptor + " method=" + methodDescriptor + " source=" + source;
    }
}
