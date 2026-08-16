package io.github.yylsping.coolapkpurifier;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Identity of the target APK. A cache entry is only valid while every value
 * below still matches the APK currently installed for com.coolapk.market.
 */
final class TargetIdentity {
    final long versionCode;
    final String versionName;
    final String apkPath;
    final long apkSize;
    final long apkLastModified;
    final long packageLastUpdateTime;
    final String apkToken;

    private TargetIdentity(long versionCode, String versionName, String apkPath,
                           long apkSize, long apkLastModified, long packageLastUpdateTime,
                           String apkToken) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.apkPath = apkPath;
        this.apkSize = apkSize;
        this.apkLastModified = apkLastModified;
        this.packageLastUpdateTime = packageLastUpdateTime;
        this.apkToken = apkToken;
    }

    static TargetIdentity compute(Context appContext) {
        long versionCode = -1L;
        long packageLastUpdateTime = -1L;
        String versionName = "unknown";
        try {
            PackageInfo packageInfo = appContext.getPackageManager()
                    .getPackageInfo(CoolapkModule.TARGET_PACKAGE, 0);
            versionCode = packageInfo.getLongVersionCode();
            versionName = packageInfo.versionName;
            packageLastUpdateTime = packageInfo.lastUpdateTime;
        } catch (Throwable ignored) {
        }

        ApplicationInfo info = appContext.getApplicationInfo();
        String apkPath = info == null ? "" : String.valueOf(info.sourceDir);
        File apk = new File(apkPath);
        long size = apk.isFile() ? apk.length() : -1L;
        long modified = apk.isFile() ? apk.lastModified() : -1L;

        // Do NOT read the protected target APK contents here. Jiagu kills the
        // reader thread. Hash only PackageManager/file metadata, which still
        // changes on every APK upgrade and is used together with the DexKit
        // in-memory DEX count as the cache invalidation token.
        String tokenSource = versionCode + "|" + versionName + "|" + apkPath + "|"
                + size + "|" + modified + "|" + packageLastUpdateTime;
        return new TargetIdentity(versionCode, versionName, apkPath, size, modified,
                packageLastUpdateTime, sha256(tokenSource));
    }

    boolean sameTarget(TargetIdentity other) {
        return other != null
                && versionCode == other.versionCode
                && apkSize == other.apkSize
                && apkLastModified == other.apkLastModified
                && packageLastUpdateTime == other.packageLastUpdateTime
                && apkToken != null
                && apkToken.equals(other.apkToken)
                && apkPath != null
                && apkPath.equals(other.apkPath);
    }

    String describe() {
        return "versionCode=" + versionCode
                + " versionName=" + versionName
                + " apk=" + apkPath
                + " size=" + apkSize
                + " modified=" + apkLastModified
                + " packageLastUpdate=" + packageLastUpdateTime
                + " token=" + (apkToken == null ? "null" : apkToken.substring(0, Math.min(16, apkToken.length())));
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("versionCode", versionCode);
        json.put("versionName", String.valueOf(versionName));
        json.put("apkPath", String.valueOf(apkPath));
        json.put("apkSize", apkSize);
        json.put("apkLastModified", apkLastModified);
        json.put("apkToken", String.valueOf(apkToken));
        json.put("packageLastUpdateTime", packageLastUpdateTime);
        return json;
    }

    static TargetIdentity fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        return new TargetIdentity(
                json.optLong("versionCode", -1L),
                json.optString("versionName", "unknown"),
                json.optString("apkPath", ""),
                json.optLong("apkSize", -1L),
                json.optLong("apkLastModified", -1L),
                json.optLong("packageLastUpdateTime", -1L),
                json.optString("apkToken", null));
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
            return null;
        }
    }
}
