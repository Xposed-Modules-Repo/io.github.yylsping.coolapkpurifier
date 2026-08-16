package io.github.yylsping.coolapkpurifier;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Resolver-only log file owned by the target app. LSPosed log delivery can be
 * lossy while DexKit is scanning, so resolver decisions are always mirrored
 * here: /data/user/0/com.coolapk.market/files/coolapk_purifier_resolver.log
 */
final class ResolverTrace {
    private final File file;

    ResolverTrace(Context appContext) {
        this.file = new File(appContext.getFilesDir(), "coolapk_purifier_resolver.log");
    }

    synchronized void info(String message) {
        try (OutputStream out = new FileOutputStream(file, true)) {
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            out.write((time + " " + message + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    File getFile() {
        return file;
    }
}
