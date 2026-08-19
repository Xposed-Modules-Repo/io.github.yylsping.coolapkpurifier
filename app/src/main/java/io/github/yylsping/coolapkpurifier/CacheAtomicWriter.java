package io.github.yylsping.coolapkpurifier;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Atomic cache persistence. The destination is never opened directly: the
 * complete payload is written to a sibling temp file, fsynced, then replaced
 * with one rename. If rename fails the old destination remains untouched.
 */
final class CacheAtomicWriter {
    interface ReplaceOperation {
        boolean replace(File temp, File destination);
    }

    /**
     * Production replacement: one atomic rename. On Android/POSIX rename
     * replaces the destination atomically, so a failure always leaves the
     * previous cache file completely intact. Platforms that refuse to rename
     * onto an existing file simply report failure — a delete-then-rename
     * fallback is deliberately NOT attempted because its own failure would
     * destroy the old cache. JVM tests that must replace an existing file
     * inject a Files.move(REPLACE_EXISTING)-based operation instead.
     */
    static final ReplaceOperation RENAME_REPLACE =
            (temp, destination) -> temp.renameTo(destination);

    private CacheAtomicWriter() {
    }

    static boolean write(File destination, byte[] payload,
                         ReplaceOperation replaceOperation) {
        File parent = destination.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            return false;
        }
        File temp = new File(parent, destination.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(payload);
            out.flush();
            out.getFD().sync();
        } catch (Throwable ignored) {
            return false;
        }
        boolean replaced = false;
        try {
            replaced = replaceOperation.replace(temp, destination);
        } catch (Throwable ignored) {
            replaced = false;
        }
        return replaced;
    }
}
