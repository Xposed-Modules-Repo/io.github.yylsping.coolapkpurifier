package io.github.yylsping.coolapkpurifier;

import android.content.Context;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Monotonic-clock startup trace, persisted in the target app files dir. */
final class BootstrapTrace {
    private static final long MAX_BYTES = 256L * 1024L;

    private final File file;
    private final long startRealtime = SystemClock.elapsedRealtime();

    BootstrapTrace(Context appContext) {
        this.file = new File(appContext.getFilesDir(), "coolapk_purifier_bootstrap.log");
    }

    synchronized void mark(String event, String detail) {
        long now = SystemClock.elapsedRealtime();
        String line = String.format(Locale.US,
                "rel=%6dms evt=%-22s %s%n", now - startRealtime, event, detail);
        try {
            if (file.isFile() && file.length() > MAX_BYTES) {
                // Keep the most recent startup trace only. Diagnostics must
                // never grow without bound.
                File old = new File(file.getParentFile(), file.getName() + ".old");
                if (old.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    old.delete();
                }
                //noinspection ResultOfMethodCallIgnored
                file.renameTo(old);
            }
            try (OutputStream out = new FileOutputStream(file, true)) {
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {
        }
    }

    void mark(String event) {
        mark(event, "");
    }
}
