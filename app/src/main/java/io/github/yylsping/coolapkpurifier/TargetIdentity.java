package io.github.yylsping.coolapkpurifier;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stable target-code identity.
 *
 * <p>versionCode participates only in cache isolation, never in resolver or
 * hook behavior. The stable token is derived from package name, versionCode,
 * installed APK set sizes and the signing certificate DER; it survives a
 * same-build reinstall at a different random path while different releases
 * cannot collide merely because their APK sizes match. Reading protected APK
 * contents from inside Coolapk is deliberately avoided because the packer
 * kills such reader threads.
 */
final class TargetIdentity {
    interface PackageInfoReader {
        PackageInfo get(int flags) throws Exception;
    }

    static final class PackageFacts {
        long versionCode = -1L;
        String versionName = "unknown";
        String signingHash = SIGNING_UNAVAILABLE;
        ApplicationInfo applicationInfo;
    }

    static final String SIGNING_UNAVAILABLE = "unavailable";
    final String packageName;
    final String apkPath;
    final long apkSize;
    final String signingHash;
    final boolean signingAvailable;
    final String baseIdentity;
    final List<String> splitIdentities;
    final String token;

    final long versionCode;
    // Logging/diagnostics only.
    final String versionName;

    private TargetIdentity(String packageName, String apkPath, long apkSize,
                           String signingHash, String token,
                           long versionCode, String versionName,
                           boolean signingAvailable, String baseIdentity,
                           List<String> splitIdentities) {
        this.packageName = packageName;
        this.apkPath = apkPath;
        this.apkSize = apkSize;
        this.signingHash = signingHash;
        this.signingAvailable = signingAvailable;
        this.token = token;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.baseIdentity = baseIdentity;
        this.splitIdentities = Collections.unmodifiableList(
                new ArrayList<>(splitIdentities == null
                        ? Collections.emptyList() : splitIdentities));
    }

    static TargetIdentity compute(Context appContext) {
        PackageManager packageManager = appContext.getPackageManager();
        PackageFacts facts = readPackageFacts(flags -> packageManager.getPackageInfo(
                CoolapkModule.TARGET_PACKAGE, flags));
        long versionCode = facts.versionCode;
        String versionName = facts.versionName;
        String signingHash = facts.signingHash;
        ApplicationInfo info = facts.applicationInfo;
        if (info == null) {
            info = appContext.getApplicationInfo();
        }
        String apkPath = info == null ? "" : String.valueOf(info.sourceDir);
        File apk = new File(apkPath);
        long size = apk.isFile() ? apk.length() : -1L;
        String baseIdentity = apk.getName() + "#" + size;

        List<String> splitIdentities = new ArrayList<>();
        if (info != null && info.splitSourceDirs != null) {
            for (String splitSourceDir : info.splitSourceDirs) {
                File splitFile = new File(splitSourceDir);
                long splitSize = splitFile.isFile() ? splitFile.length() : -1L;
                splitIdentities.add(splitFile.getName() + "#" + splitSize);
            }
        }
        Collections.sort(splitIdentities);

        // The random /data/app/... install path is intentionally excluded.
        // versionName stays diagnostic-only. versionCode isolates cache
        // entries but is never used to choose resolver or hook behavior.
        return new TargetIdentity(
                CoolapkModule.TARGET_PACKAGE,
                apkPath,
                size,
                signingHash,
                StableIdentityKey.compute(CoolapkModule.TARGET_PACKAGE, versionCode,
                        baseIdentity, splitIdentities, signingHash),
                versionCode,
                versionName,
                !SIGNING_UNAVAILABLE.equals(signingHash),
                baseIdentity,
                splitIdentities);
    }

    boolean sameTarget(TargetIdentity other) {
        return other != null
                && token != null && token.equals(other.token)
                && packageName.equals(other.packageName)
                && apkSize == other.apkSize
                && versionCode == other.versionCode;
    }

    String describe() {
        return "identity=" + shortToken()
                + " pkg=" + packageName
                + " apk=" + apkPath
                + " size=" + apkSize
                + " baseIdentity=" + baseIdentity
                + " splitIdentity=" + splitIdentities
                + " cert=" + (signingHash == null ? "null"
                : signingHash.substring(0, Math.min(12, signingHash.length())))
                + " signingAvailable=" + signingAvailable
                + " [log]version=" + versionName + "(" + versionCode + ")";
    }

    String shortToken() {
        return token == null ? "null" : token.substring(0, Math.min(16, token.length()));
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("package", packageName);
        json.put("apkPath", apkPath);
        json.put("apkSize", apkSize);
        json.put("signingHash", String.valueOf(signingHash));
        json.put("signingAvailable", signingAvailable);
        json.put("baseIdentity", baseIdentity);
        json.put("splitIdentity", new org.json.JSONArray(splitIdentities));
        json.put("token", String.valueOf(token));
        json.put("versionCode", versionCode);
        json.put("versionName", String.valueOf(versionName));
        return json;
    }

    static TargetIdentity fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        String signingHash = json.optString("signingHash", SIGNING_UNAVAILABLE);
        List<String> splitIdentities = new ArrayList<>();
        org.json.JSONArray splits = json.optJSONArray("splitIdentity");
        if (splits != null) {
            for (int index = 0; index < splits.length(); index++) {
                splitIdentities.add(splits.optString(index, ""));
            }
        }
        return new TargetIdentity(
                json.optString("package", ""),
                json.optString("apkPath", ""),
                json.optLong("apkSize", -1L),
                signingHash,
                json.optString("token", ""),
                json.optLong("versionCode", -1L),
                json.optString("versionName", "unknown"),
                json.optBoolean("signingAvailable",
                        !signingHash.isEmpty()
                                && !SIGNING_UNAVAILABLE.equals(signingHash)),
                json.optString("baseIdentity",
                        "base.apk#" + json.optLong("apkSize", -1L)),
                splitIdentities);
    }

    static PackageFacts readPackageFacts(PackageInfoReader reader) {
        PackageFacts facts = new PackageFacts();
        try {
            PackageInfo basicInfo = reader.get(0);
            if (basicInfo != null) {
                long reportedVersion = basicInfo.getLongVersionCode();
                // android.jar's local-unit-test stub returns zero from the
                // method even when the legacy field is populated.
                facts.versionCode = reportedVersion != 0L || basicInfo.versionCode == 0
                        ? reportedVersion : basicInfo.versionCode & 0xffffffffL;
                facts.versionName = basicInfo.versionName;
                facts.applicationInfo = basicInfo.applicationInfo;
            }
        } catch (Throwable ignored) {
        }
        try {
            PackageInfo signingInfo = reader.get(PackageManager.GET_SIGNING_CERTIFICATES);
            facts.signingHash = signerDigest(signerCertificateBytes(signingInfo));
        } catch (Throwable ignored) {
            // Base version facts intentionally survive signer query failures.
        }
        return facts;
    }

    private static byte[][] signerCertificateBytes(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return new byte[0][];
        }
        try {
            SigningInfo signingInfo = packageInfo.signingInfo;
            if (signingInfo != null && signingInfo.getApkContentsSigners() != null
                    && signingInfo.getApkContentsSigners().length > 0) {
                Signature[] signers = signingInfo.getApkContentsSigners();
                byte[][] certificates = new byte[signers.length][];
                for (int index = 0; index < signers.length; index++) {
                    certificates[index] = signers[index] == null
                            ? new byte[0] : signers[index].toByteArray();
                }
                return certificates;
            }
        } catch (Throwable ignored) {
        }
        return new byte[0][];
    }

    /** Stable, order-independent digest over the current signer certificates. */
    static String signerDigest(byte[][] certificates) {
        try {
            List<byte[]> usable = new ArrayList<>();
            if (certificates != null) {
                for (byte[] certificate : certificates) {
                    if (certificate != null && certificate.length > 0) {
                        usable.add(certificate.clone());
                    }
                }
            }
            if (usable.isEmpty()) {
                return SIGNING_UNAVAILABLE;
            }
            usable.sort(TargetIdentity::compareUnsigned);
            if (usable.size() == 1) {
                return sha256(usable.get(0));
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (byte[] certificate : usable) {
                int length = certificate.length;
                digest.update((byte) (length >>> 24));
                digest.update((byte) (length >>> 16));
                digest.update((byte) (length >>> 8));
                digest.update((byte) length);
                digest.update(certificate);
            }
            return hex(digest.digest());
        } catch (Throwable ignored) {
            return SIGNING_UNAVAILABLE;
        }
    }

    private static String sha256(byte[] value) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static int compareUnsigned(byte[] left, byte[] right) {
        int shared = Math.min(left.length, right.length);
        for (int index = 0; index < shared; index++) {
            int compared = Integer.compare(left[index] & 0xff, right[index] & 0xff);
            if (compared != 0) {
                return compared;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    private static String hex(byte[] hash) {
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }
}
