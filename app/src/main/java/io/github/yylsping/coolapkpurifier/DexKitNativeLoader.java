package io.github.yylsping.coolapkpurifier;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Process;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Framework-owned module location; never queries the host PackageManager. */
final class DexKitNativeLoader {
    static final String FAILURE_REASON = "DEXKIT_NATIVE_LOAD_FAILED";
    interface LocationProvider { Location get() throws Exception; }
    interface LibraryLoader { void load(String path); }

    static final class LoadFailure extends IllegalStateException {
        final String stage;
        LoadFailure(String stage, Throwable cause) {
            super(FAILURE_REASON + " stage=" + stage + " " + describeFailure(cause), cause);
            this.stage = stage;
        }
    }

    static final class Location {
        final List<File> apks;
        final File nativeDirectory;
        final long versionCode;
        Location(List<File> apks, File nativeDirectory, long versionCode) {
            this.apks = new ArrayList<>(apks);
            this.nativeDirectory = nativeDirectory;
            this.versionCode = versionCode;
        }
        static Location fromFramework(ApplicationInfo info, long versionCode) throws IOException {
            if (info == null || info.sourceDir == null || info.sourceDir.isEmpty()) {
                throw new IOException("framework module APK path unavailable");
            }
            List<File> apks = new ArrayList<>();
            apks.add(new File(info.sourceDir));
            if (info.splitSourceDirs != null) {
                for (String path : info.splitSourceDirs) {
                    if (path != null && !path.isEmpty()) apks.add(new File(path));
                }
            }
            return new Location(apks, info.nativeLibraryDir == null
                    ? null : new File(info.nativeLibraryDir), versionCode);
        }
    }

    private static final Engine PROCESS_LOADER = new Engine(System::load);
    private static final OnceFlag legacyCleanup = new OnceFlag();
    private static LocationProvider moduleLocation;

    static synchronized void configure(LocationProvider provider) {
        if (moduleLocation == null) moduleLocation = provider;
    }

    static void ensureLoaded(Context context, ModuleLog log, BootstrapTrace trace) {
        final LocationProvider captured;
        synchronized (DexKitNativeLoader.class) { captured = moduleLocation; }
        boolean process64 = Process.is64Bit();
        PROCESS_LOADER.ensureLoaded(() -> {
            if (captured == null) throw new IOException("framework module location provider unavailable");
            return captured.get();
        }, process64 ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS,
                process64, () -> new File(context.getCodeCacheDir(), "coolapk-purifier-native"),
                detail -> {
                    log.info("dexkitNative " + detail);
                    if (trace != null) trace.mark("dexkitNative", detail);
                });
        // Only the old loader's own exact filenames, after a successful load.
        if (legacyCleanup.tryOnce()) {
            try {
                File[] legacy = context.getFilesDir().listFiles(
                        (dir, name) -> name.matches("libdexkit-[0-9]+\\.so(?:\\.tmp)?"));
                if (legacy != null) {
                    for (File file : legacy) {
                        if (file.isFile() && !file.delete()) {
                            log.info("dexkitNative stage=cleanup legacyDelete=false file=" + file.getName());
                        }
                    }
                }
            } catch (RuntimeException error) {
                // Cleanup cannot turn a successful native load into bootstrap failure.
                log.info("dexkitNative stage=cleanup success=false " + describeFailure(error));
            }
        }
    }

    /** One success or terminal failure per process; testable without Android JNI. */
    static final class Engine {
        private final LibraryLoader loader;
        private boolean loaded;
        private LoadFailure failure;
        private String stage = "moduleLocation";
        Engine(LibraryLoader loader) { this.loader = loader; }

        synchronized void ensureLoaded(LocationProvider provider, String[] supported,
                                       boolean process64, Supplier<File> directory,
                                       Consumer<String> log) {
            if (loaded) return;
            if (failure != null) throw failure;
            Throwable sharedFailure = null;
            try {
                mark(log, "moduleLocation", "source=framework");
                Location location = provider.get();
                if (location == null || location.apks.isEmpty()) {
                    throw new IOException("framework module APK path unavailable");
                }
                log.accept("moduleApk=available count=" + location.apks.size()
                        + " versionCode=" + location.versionCode);
                Candidate candidate = select(location, supported, process64, log);
                if (location.nativeDirectory != null) {
                    File nativeFile = new File(location.nativeDirectory, "libdexkit.so");
                    if (nativeFile.isFile()) {
                        try {
                            mark(log, "loadExisting", "source=frameworkNative abi=" + candidate.abi);
                            candidate.verify(nativeFile);
                            mark(log, "systemLoad", "source=frameworkNative path=" + nativeFile.getName());
                            loader.load(nativeFile.getAbsolutePath());
                            loaded = true;
                            log.accept("stage=systemLoad result=success source=frameworkNative");
                            return;
                        } catch (Exception | LinkageError first) {
                            sharedFailure = first;
                            log.accept("stage=" + stage + " source=frameworkNative success=false "
                                    + describeFailure(first));
                            // Never change/delete framework or package-manager-owned files.
                        }
                    } else log.accept("stage=loadExisting source=frameworkNative available=false");
                }
                loadExtracted(candidate, location.versionCode, directory.get(), log);
                loaded = true;
            } catch (Exception | LinkageError error) {
                if (sharedFailure != null && sharedFailure != error) {
                    error.addSuppressed(sharedFailure);
                }
                failure = new LoadFailure(stage, error);
                log.accept("stage=" + stage + " result=failure classification=NATIVE_BOOTSTRAP_FAILED "
                        + describeFailure(error));
                throw failure;
            }
        }

        private Candidate select(Location location, String[] supported, boolean process64,
                                 Consumer<String> log) throws Exception {
            mark(log, "abiSelect", "supportedAbis=" + Arrays.toString(supported) + " process64=" + process64);
            Set<String> available = new LinkedHashSet<>();
            for (File apk : location.apks) {
                try (ZipFile zip = new ZipFile(apk)) {
                    zip.stream().map(ZipEntry::getName)
                            .filter(n -> n.startsWith("lib/") && n.endsWith("/libdexkit.so"))
                            .forEach(n -> available.add(n.substring(4, n.length() - "/libdexkit.so".length())));
                }
            }
            log.accept("stage=abiSelect availableAbis=" + available);
            for (String abi : supported == null ? new String[0] : supported) {
                if (!compatible(abi, process64) || !available.contains(abi)) continue;
                for (File apk : location.apks) {
                    try (ZipFile zip = new ZipFile(apk)) {
                        ZipEntry entry = zip.getEntry("lib/" + abi + "/libdexkit.so");
                        if (entry != null) {
                            Candidate result = new Candidate(apk, abi, entry, zip);
                            log.accept("stage=abiSelect selectedAbi=" + abi + " apkSha256=" + result.apkHash
                                    + " entry=" + entry.getName() + " size=" + result.size + " crc=" + result.crc);
                            return result;
                        }
                    }
                }
            }
            throw new IOException("missing compatible DexKit zip entry; supportedAbis="
                    + Arrays.toString(supported) + " availableAbis=" + available + " process64=" + process64);
        }

        private void loadExtracted(Candidate candidate, long version, File directory,
                                   Consumer<String> log) throws Exception {
            mark(log, "extract", "source=privateCodeCache");
            if ((!directory.isDirectory() && !directory.mkdirs()) || !directory.isDirectory()) {
                throw new IOException("cannot create native extraction directory");
            }
            File library = new File(directory, candidate.fileName(version));
            Throwable first = null;
            try {
                for (int attempt = 0; attempt < 2; attempt++) {
                    try {
                        if (attempt == 0 && library.isFile()) {
                            mark(log, "loadExisting", "source=privateCodeCache file=" + library.getName());
                            candidate.verify(library);
                        } else {
                            mark(log, attempt == 0 ? "extract" : "reextract",
                                    "entry=" + candidate.entryName + " attempt=" + (attempt + 1));
                            extract(candidate, library);
                            log.accept("stage=extract extractedSize=" + library.length()
                                    + " sha256=" + candidate.libraryHash);
                        }
                        mark(log, "systemLoad", "source=privateCodeCache path=" + library.getName()
                                + " attempt=" + (attempt + 1));
                        loader.load(library.getAbsolutePath());
                        log.accept("stage=systemLoad result=success source=privateCodeCache");
                        return;
                    } catch (Exception | LinkageError error) {
                        log.accept("stage=" + stage + " success=false attempt=" + (attempt + 1)
                                + " " + describeFailure(error));
                        if (attempt == 1) {
                            if (first != null && first != error) error.addSuppressed(first);
                            throw error;
                        }
                        first = error;
                        mark(log, "reextract", "reason=" + describeFailure(error));
                        Files.deleteIfExists(library.toPath());
                    }
                }
            } finally {
                // Loaded pages remain mapped after unlink; no persistent extracted .so needed.
                try {
                    Files.deleteIfExists(library.toPath());
                    File[] stale = directory.listFiles((dir, name) -> name.matches(
                            "libdexkit-[0-9]+-(?:arm64-v8a|armeabi-v7a|armeabi|x86_64|x86)-"
                                    + "[0-9a-f]{64}-[0-9]+\\.so(?:\\.[A-Za-z0-9_-]+\\.tmp)?"));
                    if (stale != null) {
                        for (File file : stale) {
                            if (file.isFile()) Files.deleteIfExists(file.toPath());
                        }
                    }
                    if (!directory.delete() && directory.exists()) log.accept("stage=cleanup directoryRetained=true");
                } catch (IOException cleanup) {
                    log.accept("stage=cleanup success=false " + describeFailure(cleanup));
                }
            }
        }
        private void mark(Consumer<String> log, String next, String detail) {
            stage = next;
            log.accept("stage=" + next + " " + detail);
        }
    }

    static boolean compatible(String abi, boolean process64) {
        return process64 ? ("arm64-v8a".equals(abi) || "x86_64".equals(abi))
                : ("armeabi-v7a".equals(abi) || "armeabi".equals(abi) || "x86".equals(abi));
    }

    static final class Candidate {
        final File apk;
        final String abi, entryName, apkHash, libraryHash;
        final long size, crc;
        Candidate(File apk, String abi, ZipEntry entry, ZipFile zip) throws Exception {
            this.apk = apk;
            this.abi = abi;
            entryName = entry.getName();
            size = entry.getSize();
            crc = entry.getCrc();
            try (InputStream in = new FileInputStream(apk)) { apkHash = sha256(in); }
            try (InputStream in = zip.getInputStream(entry)) { libraryHash = sha256(in); }
            try (InputStream in = zip.getInputStream(entry)) { verifyElf(in, abi); }
            if (size < 20) throw new IOException("invalid packaged native size=" + size);
        }
        String fileName(long version) {
            return "libdexkit-" + version + "-" + abi + "-" + apkHash + "-" + crc + ".so";
        }
        void verify(File file) throws Exception {
            if (!file.isFile() || file.length() != size) {
                throw new IOException("native file size mismatch expected=" + size + " actual=" + file.length());
            }
            try (InputStream in = new FileInputStream(file)) {
                if (!libraryHash.equals(sha256(in))) throw new IOException("native file SHA-256 mismatch");
            }
            try (InputStream in = new FileInputStream(file)) { verifyElf(in, abi); }
        }
    }

    private static void extract(Candidate candidate, File output) throws Exception {
        File temporary = File.createTempFile(output.getName() + ".", ".tmp", output.getParentFile());
        try {
            try (ZipFile zip = new ZipFile(candidate.apk)) {
                ZipEntry entry = zip.getEntry(candidate.entryName);
                if (entry == null || entry.getCrc() != candidate.crc || entry.getSize() != candidate.size) {
                    throw new IOException("module APK changed during native extraction");
                }
                try (InputStream in = zip.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                    out.getFD().sync();
                }
            }
            candidate.verify(temporary);
            if (!temporary.setReadOnly()) throw new IOException("cannot make extracted native library read-only");
            // Same filesystem; do not copy into a possibly mapped .so.
            Files.move(temporary.toPath(), output.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }

    static void verifyElf(InputStream in, String abi) throws IOException {
        byte[] header = new byte[20];
        int count = 0;
        while (count < header.length) {
            int read = in.read(header, count, header.length - count);
            if (read < 0) throw new IOException("truncated ELF header");
            count += read;
        }
        int expectedClass = compatible(abi, true) ? 2 : 1;
        int expectedMachine = "arm64-v8a".equals(abi) ? 183
                : ("x86_64".equals(abi) ? 62 : ("x86".equals(abi) ? 3 : 40));
        int machine = (header[18] & 255) | ((header[19] & 255) << 8);
        if (header[0] != 0x7f || header[1] != 'E' || header[2] != 'L' || header[3] != 'F'
                || header[4] != expectedClass || header[5] != 1 || machine != expectedMachine) {
            throw new IOException("wrong ELF class/machine for abi=" + abi
                    + " class=" + header[4] + " machine=" + machine);
        }
    }

    static String sha256(InputStream in) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }

    static String describeFailure(Throwable error) {
        Throwable root = error;
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (root.getCause() != null && seen.add(root) && !seen.contains(root.getCause())) root = root.getCause();
        return "failureType=" + error.getClass().getName() + " failureMessage=" + oneLine(error.getMessage())
                + " rootCauseType=" + root.getClass().getName() + " rootCauseMessage=" + oneLine(root.getMessage());
    }
    private static String oneLine(String value) {
        return value == null ? "<null>" : value.replace('\n', ' ').replace('\r', ' ');
    }
    private DexKitNativeLoader() {}
}
