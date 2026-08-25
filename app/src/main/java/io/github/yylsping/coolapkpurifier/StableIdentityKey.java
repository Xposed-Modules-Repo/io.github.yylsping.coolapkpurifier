package io.github.yylsping.coolapkpurifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stable target-code identity independent of the random APK install path.
 * The same target build reinstalled at a different path gets the same key.
 */
final class StableIdentityKey {
    private StableIdentityKey() {
    }

    static String compute(String packageName, long versionCode, long baseApkSize,
                           long[] splitSizes, String signingHash) {
        List<String> splits = new ArrayList<>();
        if (splitSizes != null) {
            for (long size : splitSizes) {
                splits.add("size:" + size);
            }
        }
        return compute(packageName, versionCode, "base.apk#" + baseApkSize,
                splits, signingHash);
    }

    static String compute(String packageName, long versionCode, String baseIdentity,
                          List<String> splitIdentities, String signingHash) {
        StringBuilder source = new StringBuilder();
        source.append("pkg=").append(packageName);
        // Cache isolation only. Resolver behavior must remain version-agnostic.
        source.append("|versionCode=").append(versionCode);
        source.append("|base=").append(baseIdentity == null ? "" : baseIdentity);
        if (splitIdentities != null) {
            List<String> sorted = new ArrayList<>(splitIdentities);
            Collections.sort(sorted);
            for (String identity : sorted) {
                source.append("|split=").append(identity == null ? "" : identity);
            }
        }
        source.append("|cert=").append(signingHash == null ? "" : signingHash);
        return sha256(source.toString());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xf, 16));
                sb.append(Character.forDigit(b & 0xf, 16));
            }
            return sb.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }
}
