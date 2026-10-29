package io.github.yylsping.coolapkpurifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Bounded, atomic snapshots in the host's files directory, owned by its UID. */
final class HostPrivateBlobStore implements ConfigStore, ResolverCacheStore {
    static final String CACHE_FILE_NAME = "coolapk_purifier_resolver_cache.json";
    private final String key;
    private final HostDataMutationGuard guard;
    private final ModuleLog log;
    private File file;
    private int writes;

    HostPrivateBlobStore(String key, HostDataMutationGuard guard, ModuleLog log) {
        if (!ModuleStorageContract.isValidKey(key)) throw new IllegalArgumentException("key");
        this.key = key;
        this.guard = guard;
        this.log = log;
    }

    synchronized void attach(File filesDir) {
        if (file == null && filesDir != null) {
            file = new File(filesDir, ModuleStorageContract.CONFIG_KEY.equals(key)
                    ? PurifierConfig.FILE_NAME : CACHE_FILE_NAME);
        }
    }

    synchronized boolean hasSnapshot() {
        // An unreadable/corrupt existing snapshot must not resurrect stale framework values.
        return file != null && file.exists();
    }

    @Override public synchronized byte[] read() {
        if (file == null || !file.exists()) return null;
        if (!file.isFile()) return new byte[0];
        long size = file.length();
        if (size > ModuleStorageContract.maxBytesForKey(key)) return new byte[0];
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) size];
            int offset = 0;
            while (offset < bytes.length) {
                int count = input.read(bytes, offset, bytes.length - offset);
                if (count < 0) return new byte[0];
                offset += count;
            }
            return bytes;
        } catch (Exception failure) {
            if (log != null) log.error("host storage read failed key=" + key, failure);
            return new byte[0];
        }
    }

    @Override public synchronized boolean write(byte[] value, String reason) {
        if (file == null || value == null || value.length == 0
                || value.length > ModuleStorageContract.maxBytesForKey(key)
                || !ModuleStorageContract.isValidWriteReason(key, reason)) return false;
        File temporary = null;
        try {
            temporary = File.createTempFile(file.getName(), ".tmp", file.getParentFile());
            guard.recordHostMutation(HostDataMutationGuard.HostMutation.TEMP_CREATE);
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(value);
                output.getFD().sync();
            }
            // Both files are in the same private directory. Never delete the old
            // snapshot before the replacement is ready; failure preserves it.
            Files.move(temporary.toPath(), file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            guard.recordHostMutation(HostDataMutationGuard.HostMutation.RENAME);
            guard.recordHostMutation(HostDataMutationGuard.HostMutation.FILES_DIR);
            writes++;
            return true;
        } catch (Exception failure) {
            if (log != null) log.error("host storage write failed key=" + key + " reason=" + reason, failure);
            return false;
        } finally {
            if (temporary != null && temporary.exists() && temporary.delete()) {
                guard.recordHostMutation(HostDataMutationGuard.HostMutation.DELETE);
            }
        }
    }

    @Override public synchronized boolean isAvailable() { return file != null; }
    @Override public String backendName() { return "coolapkPrivateFiles"; }
    @Override public synchronized int persistentWriteCount() { return writes; }
}
