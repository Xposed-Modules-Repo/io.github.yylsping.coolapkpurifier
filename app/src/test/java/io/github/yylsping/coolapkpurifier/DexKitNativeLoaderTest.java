package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.*;
import android.content.pm.ApplicationInfo;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class DexKitNativeLoaderTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final List<String> events = new ArrayList<>();

    @Test public void frameworkNativeLoadsWithoutCreatingPrivateFilesAndMemoizes() throws Exception {
        File apk = apk("arm64-v8a", elf("arm64-v8a", 1));
        File nativeDir = temporary.newFolder();
        File library = new File(nativeDir, "libdexkit.so");
        Files.write(library.toPath(), elf("arm64-v8a", 1));
        AtomicInteger calls = new AtomicInteger();
        DexKitNativeLoader.Engine engine = new DexKitNativeLoader.Engine(path -> {
            assertEquals(library.getAbsolutePath(), path);
            calls.incrementAndGet();
        });
        DexKitNativeLoader.Location location = location(apk, nativeDir);
        for (int i = 0; i < 3; i++) {
            engine.ensureLoaded(() -> location, new String[]{"arm64-v8a"}, true,
                    () -> { throw new AssertionError("private storage must not be needed"); }, events::add);
        }
        assertEquals(1, calls.get());
        assertTrue(library.isFile());
        assertEvent("result=success source=frameworkNative");
    }

    @Test public void frameworkNativeFailureFallsBackWithoutChangingSharedFile() throws Exception {
        byte[] bytes = elf("arm64-v8a", 1);
        File apk = apk("arm64-v8a", bytes);
        File nativeDir = temporary.newFolder();
        File shared = new File(nativeDir, "libdexkit.so");
        Files.write(shared.toPath(), bytes);
        AtomicInteger calls = new AtomicInteger();
        File dir = output();
        DexKitNativeLoader.Engine engine = engine(path -> {
            calls.incrementAndGet();
            if (path.equals(shared.getAbsolutePath())) throw new UnsatisfiedLinkError("namespace denied");
            assertArrayEqualsUnchecked(bytes, path);
        });
        load(engine, location(apk, nativeDir), dir, "arm64-v8a");
        assertEquals(2, calls.get());
        assertArrayEquals(bytes, Files.readAllBytes(shared.toPath()));
        assertFalse(dir.exists());
        assertEvent("namespace denied");
    }

    @Test public void firstExtractionLoadsOnceAndUnlinks() throws Exception {
        byte[] bytes = elf("arm64-v8a", 2);
        File apk = apk("arm64-v8a", bytes);
        File dir = output();
        AtomicInteger calls = new AtomicInteger();
        load(engine(path -> {
            assertArrayEqualsUnchecked(bytes, path);
            calls.incrementAndGet();
        }), location(apk, null), dir, "arm64-v8a");
        assertEquals(1, calls.get());
        assertFalse(dir.exists());
        assertEvent("stage=extract extractedSize=64");
    }

    @Test public void verifiedExistingFileIsReused() throws Exception {
        byte[] bytes = elf("arm64-v8a", 3);
        File apk = apk("arm64-v8a", bytes);
        File dir = output();
        File existing = seed(apk, dir, bytes);
        load(engine(path -> assertEquals(existing.getAbsolutePath(), path)),
                location(apk, null), dir, "arm64-v8a");
        assertEvent("stage=loadExisting source=privateCodeCache");
        assertFalse(events.stream().anyMatch(e -> e.contains("extractedSize=")));
        assertFalse(existing.exists());
    }

    @Test public void truncatedExistingFileIsRejectedBeforeLoadAndReextracted() throws Exception {
        recoverCorrupt(new byte[]{1, 2, 3});
    }

    @Test public void sameLengthCorruptExistingFileRequiresHashMatch() throws Exception {
        recoverCorrupt(elf("arm64-v8a", 99));
        assertEvent("SHA-256 mismatch");
    }

    private void recoverCorrupt(byte[] corrupt) throws Exception {
        byte[] correct = elf("arm64-v8a", 4);
        File apk = apk("arm64-v8a", correct);
        File dir = output();
        seed(apk, dir, corrupt);
        AtomicInteger calls = new AtomicInteger();
        load(engine(path -> {
            assertArrayEqualsUnchecked(correct, path);
            calls.incrementAndGet();
        }), location(apk, null), dir, "arm64-v8a");
        assertEquals(1, calls.get()); // Never call dlopen on known corrupt bytes.
        assertEvent("stage=reextract");
        assertFalse(dir.exists());
    }

    @Test public void validExistingLoadFailureGetsExactlyOneForcedExtraction() throws Exception {
        byte[] bytes = elf("arm64-v8a", 5);
        File apk = apk("arm64-v8a", bytes);
        File dir = output();
        seed(apk, dir, bytes);
        AtomicInteger calls = new AtomicInteger();
        load(engine(path -> {
            if (calls.incrementAndGet() == 1) throw new UnsatisfiedLinkError("stale linker state");
            assertArrayEqualsUnchecked(bytes, path);
        }), location(apk, null), dir, "arm64-v8a");
        assertEquals(2, calls.get());
        assertEvent("stage=reextract");
        assertFalse(dir.exists());
    }

    @Test public void permanentFailureStopsAfterTwoLoadsAndPreservesBothCauses() throws Exception {
        File apk = apk("arm64-v8a", elf("arm64-v8a", 6));
        File dir = output();
        AtomicInteger calls = new AtomicInteger();
        DexKitNativeLoader.Engine engine = engine(path -> {
            throw new UnsatisfiedLinkError("dlopen permission denied #" + calls.incrementAndGet());
        });
        DexKitNativeLoader.LoadFailure first = assertThrows(DexKitNativeLoader.LoadFailure.class,
                () -> load(engine, location(apk, null), dir, "arm64-v8a"));
        DexKitNativeLoader.LoadFailure second = assertThrows(DexKitNativeLoader.LoadFailure.class,
                () -> load(engine, location(apk, null), dir, "arm64-v8a"));
        assertSame(first, second);
        assertEquals(2, calls.get());
        assertEquals(1, first.getCause().getSuppressed().length);
        assertTrue(first.getMessage().contains("rootCauseType=java.lang.UnsatisfiedLinkError"));
        assertTrue(first.getMessage().contains("permission denied #2"));
        assertFalse(dir.exists());
    }

    @Test public void missingPrimaryUsesActualPackagedSecondaryOfSameProcessBitness() throws Exception {
        File apk = apk("arm64-v8a", elf("arm64-v8a", 1));
        load(engine(path -> assertTrue(path.contains("arm64-v8a"))),
                location(apk, null), output(), "x86_64", "arm64-v8a");
        assertEvent("selectedAbi=arm64-v8a");
    }

    @Test public void only32BitEntryCannotBeFallbackFor64BitProcess() throws Exception {
        File apk = apk("armeabi-v7a", elf("armeabi-v7a", 1));
        DexKitNativeLoader.LoadFailure failure = assertThrows(DexKitNativeLoader.LoadFailure.class,
                () -> load(engine(path -> fail("must not load wrong bitness")),
                        location(apk, null), output(), "arm64-v8a", "armeabi-v7a"));
        assertEquals("abiSelect", failure.stage);
        assertTrue(failure.getMessage().contains("missing compatible"));
        assertEvent("availableAbis=[armeabi-v7a]");
    }

    @Test public void process32BitSelects32BitEntryEvenIfInputContains64BitFirst() throws Exception {
        File apk = apk("armeabi-v7a", elf("armeabi-v7a", 7));
        File dir = output();
        engine(path -> assertTrue(path.contains("armeabi-v7a"))).ensureLoaded(
                () -> location(apk, null), new String[]{"arm64-v8a", "armeabi-v7a"},
                false, () -> dir, events::add);
        assertEvent("process64=false");
        assertEvent("selectedAbi=armeabi-v7a");
    }

    @Test public void mislabeledPackagedElfFailsBeforeSystemLoad() throws Exception {
        File apk = apk("arm64-v8a", elf("armeabi-v7a", 1));
        DexKitNativeLoader.LoadFailure failure = assertThrows(DexKitNativeLoader.LoadFailure.class,
                () -> load(engine(path -> fail("wrong ELF must not reach system load")),
                        location(apk, null), output(), "arm64-v8a"));
        assertTrue(failure.getMessage().contains("wrong ELF class/machine"));
    }

    @Test public void sameVersionDifferentApkNeverSharesExtractionIdentity() throws Exception {
        File a = apk("arm64-v8a", elf("arm64-v8a", 1));
        File b = apk("arm64-v8a", elf("arm64-v8a", 2));
        String first = candidate(a).fileName(11);
        assertNotEquals(first, candidate(b).fileName(11));
        assertNotEquals(first, candidate(a).fileName(12));
        assertTrue(first.startsWith("libdexkit-11-arm64-v8a-"));
    }

    @Test public void frameworkInfoSupportsSplitApkWithoutHostPackageManager() throws Exception {
        File empty = temporary.newFile("base.apk");
        try (ZipOutputStream ignored = new ZipOutputStream(new FileOutputStream(empty))) {}
        File split = apk("arm64-v8a", elf("arm64-v8a", 1));
        ApplicationInfo info = new ApplicationInfo();
        info.sourceDir = empty.getAbsolutePath();
        info.splitSourceDirs = new String[]{split.getAbsolutePath()};
        // No host Context or PackageManager is part of location or engine contracts.
        load(engine(path -> {}), DexKitNativeLoader.Location.fromFramework(info, 11),
                output(), "arm64-v8a");
        assertEvent("moduleApk=available count=2");
    }

    @Test public void oldBuildAndInterruptedExtractionAreRemovedButUnrelatedFilesSurvive() throws Exception {
        File old = apk("arm64-v8a", elf("arm64-v8a", 1));
        File current = apk("arm64-v8a", elf("arm64-v8a", 2));
        File dir = output();
        File stale = seed(old, dir, elf("arm64-v8a", 1));
        File partial = new File(dir, stale.getName() + ".abandoned.tmp");
        File unrelated = new File(dir, "unrelated.txt");
        Files.write(partial.toPath(), new byte[]{1});
        Files.write(unrelated.toPath(), new byte[]{2});
        load(engine(path -> assertArrayEqualsUnchecked(elf("arm64-v8a", 2), path)),
                location(current, null), dir, "arm64-v8a");
        assertFalse(stale.exists());
        assertFalse(partial.exists());
        assertTrue(unrelated.isFile());
        assertEquals(1, Objects.requireNonNull(dir.list()).length);
    }

    @Test public void unavailableFrameworkLocationFailsOnceWithRootCause() throws Exception {
        AtomicInteger queries = new AtomicInteger();
        DexKitNativeLoader.Engine engine = engine(path -> fail());
        DexKitNativeLoader.LocationProvider provider = () -> {
            queries.incrementAndGet();
            throw new IOException("module lookup failed", new SecurityException("access denied"));
        };
        for (int i = 0; i < 3; i++) {
            DexKitNativeLoader.LoadFailure failure = assertThrows(DexKitNativeLoader.LoadFailure.class,
                    () -> engine.ensureLoaded(provider, new String[]{"arm64-v8a"},
                            true, this::output, events::add));
            assertEquals("moduleLocation", failure.stage);
            assertTrue(failure.getMessage().contains("rootCauseType=java.lang.SecurityException"));
        }
        assertEquals(1, queries.get());
    }

    @Test public void extractionDirectoryFailureHasSpecificStage() throws Exception {
        File apk = apk("arm64-v8a", elf("arm64-v8a", 1));
        File notDirectory = temporary.newFile();
        DexKitNativeLoader.LoadFailure failure = assertThrows(DexKitNativeLoader.LoadFailure.class,
                () -> load(engine(path -> fail()), location(apk, null), notDirectory, "arm64-v8a"));
        assertEquals("extract", failure.stage);
        assertTrue(failure.getMessage().contains("cannot create native extraction directory"));
    }

    private DexKitNativeLoader.Engine engine(Consumer<String> load) {
        return new DexKitNativeLoader.Engine(path -> {
            // Windows test filesystem's read-only attribute prevents unlink.
            // Android unlink depends on directory permissions, not file mode.
            new File(path).setWritable(true);
            load.accept(path);
        });
    }
    private void load(DexKitNativeLoader.Engine engine, DexKitNativeLoader.Location location,
                      File directory, String... abis) {
        engine.ensureLoaded(() -> location, abis, true, () -> directory, events::add);
    }
    private DexKitNativeLoader.Location location(File apk, File nativeDir) {
        return new DexKitNativeLoader.Location(Collections.singletonList(apk), nativeDir, 11);
    }
    private File output() { return new File(temporary.getRoot(), "extracted"); }
    private File apk(String abi, byte[] bytes) throws Exception {
        File result = temporary.newFile();
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(result))) {
            zip.putNextEntry(new ZipEntry("lib/" + abi + "/libdexkit.so"));
            zip.write(bytes);
            zip.closeEntry();
        }
        return result;
    }
    private DexKitNativeLoader.Candidate candidate(File apk) throws Exception {
        try (ZipFile zip = new ZipFile(apk)) {
            return new DexKitNativeLoader.Candidate(apk, "arm64-v8a",
                    zip.getEntry("lib/arm64-v8a/libdexkit.so"), zip);
        }
    }
    private File seed(File apk, File dir, byte[] bytes) throws Exception {
        assertTrue(dir.mkdirs());
        File file = new File(dir, candidate(apk).fileName(11));
        Files.write(file.toPath(), bytes);
        return file;
    }
    private static byte[] elf(String abi, int payload) {
        byte[] data = new byte[64];
        data[0] = 0x7f; data[1] = 'E'; data[2] = 'L'; data[3] = 'F';
        data[4] = (byte) (DexKitNativeLoader.compatible(abi, true) ? 2 : 1);
        data[5] = 1;
        data[18] = (byte) ("arm64-v8a".equals(abi) ? 183
                : "x86_64".equals(abi) ? 62 : "x86".equals(abi) ? 3 : 40);
        data[63] = (byte) payload;
        return data;
    }
    private static void assertArrayEqualsUnchecked(byte[] expected, String path) {
        try { assertArrayEquals(expected, Files.readAllBytes(new File(path).toPath())); }
        catch (IOException e) { throw new AssertionError(e); }
    }
    private void assertEvent(String fragment) {
        assertTrue("missing " + fragment + " in " + events, events.stream().anyMatch(e -> e.contains(fragment)));
    }
}
