import java.util.Properties
import java.security.MessageDigest

plugins {
    id("com.android.application")
}

val signingPropertiesFile = rootProject.file("signing-private/signing.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.isFile) {
        signingPropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "io.github.yylsping.coolapkpurifier"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.yylsping.coolapkpurifier"
        minSdk = 28
        targetSdk = 35
        versionCode = 12
        versionName = "2.2.2"
        buildConfigField("boolean", "SPLASH_DIAGNOSTICS", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (signingPropertiesFile.isFile) {
                storeFile = rootProject.file(requireNotNull(signingProperties.getProperty("storeFile")))
                storePassword = requireNotNull(signingProperties.getProperty("storePassword"))
                keyAlias = requireNotNull(signingProperties.getProperty("keyAlias"))
                keyPassword = requireNotNull(signingProperties.getProperty("keyPassword"))
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        create("compatible") {
            initWith(getByName("release"))
            isDebuggable = false
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = if (signingPropertiesFile.isFile) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("splashDiagnostic") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            buildConfigField("boolean", "SPLASH_DIAGNOSTICS", "true")
            proguardFiles("splash-diagnostic.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    testImplementation("io.github.libxposed:api:102.0.0")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.5.0")
    implementation("org.luckypray:dexkit:2.0.6")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.0")
    // Real org.json for unit tests: the android.jar stub returns defaults
    // (null) from JSONObject methods, which would silently break every
    // cache serialization test.
    testImplementation("org.json:json:20240303")
}
// Public trust root, intentionally source-pinned. A normal Gradle property
// must never be able to make a wrong release keystore pass staging.
val expectedReleaseSignerSha256 =
    "12B482270B217A377CF3881382C86EE9F1C3E2B8E4BE25EE5118F327F2858875"
val expectedReleaseSignerSet = setOf(expectedReleaseSignerSha256)
val deprecatedSignerOverride = providers.gradleProperty("releaseSignerSha256").orNull

val signerDigestPattern = Regex(
    """Signer #\d+ certificate SHA-256 digest:\s*([0-9A-Fa-f:]+)""")

fun signerDigests(verifyOutput: String): List<String> =
    signerDigestPattern.findAll(verifyOutput).map {
        it.groupValues[1].replace(":", "").uppercase()
    }.toList()

fun hasExactSignerSet(actual: List<String>, expected: Set<String>): Boolean =
    actual.isNotEmpty() && actual.toSet().size == actual.size && actual.toSet() == expected

tasks.register("verifyReleaseSignerPolicy") {
    group = "verification"
    description = "Regression checks for exact release signer-set matching"
    doLast {
        check(deprecatedSignerOverride == null
                || deprecatedSignerOverride.replace(":", "").trim()
                    .equals(expectedReleaseSignerSha256, ignoreCase = true)) {
            "releaseSignerSha256 cannot override the source-pinned release trust root; " +
                    "review and change expectedReleaseSignerSha256 in source for an " +
                    "explicit signer rotation"
        }
        check(expectedReleaseSignerSet == setOf(expectedReleaseSignerSha256))
        val expected = setOf("AA")
        check(hasExactSignerSet(signerDigests(
            "Signer #1 certificate SHA-256 digest: AA"), expected))
        check(!hasExactSignerSet(signerDigests(
            "Signer #1 certificate SHA-256 digest: BB"), expected))
        check(!hasExactSignerSet(signerDigests(
            "Signer #1 certificate SHA-256 digest: AA\n" +
                    "Signer #2 certificate SHA-256 digest: BB"), expected))
        check(!hasExactSignerSet(signerDigests("unsigned"), expected))
    }
}

/**
 * Stages the exact assembled release APK and derives its checksum from bytes.
 * This task intentionally performs no publishing, tagging, or network access.
 */
tasks.register("stageReleaseCandidate") {
    group = "build"
    description = "Copy the release APK to dist and generate/verify SHA-256"
    dependsOn("assembleRelease", "verifyReleaseSignerPolicy", "stageReleaseNotes")

    doLast {
        check(signingPropertiesFile.isFile) {
            "Release signing properties are required before staging a candidate"
        }
        val source = layout.buildDirectory
            .file("outputs/apk/release/app-release.apk").get().asFile
        check(source.isFile) { "Release APK missing: $source" }

        val apksignerName = if (System.getProperty("os.name")
                .startsWith("Windows", ignoreCase = true)) {
            "apksigner.bat"
        } else {
            "apksigner"
        }
        val apksigner = android.sdkDirectory.resolve(
            "build-tools/${android.buildToolsVersion}/$apksignerName")
        check(apksigner.isFile) { "apksigner missing: $apksigner" }
        val verifyProcess = ProcessBuilder(
            apksigner.absolutePath,
            "verify",
            "--verbose",
            "--print-certs",
            source.absolutePath,
        ).redirectErrorStream(true).start()
        val verifyOutput = verifyProcess.inputStream.bufferedReader().use { it.readText() }
        val verifyExit = verifyProcess.waitFor()
        check(verifyExit == 0 && verifyOutput.lineSequence().any {
            (it.contains("Verified using v2 scheme")
                    || it.contains("Verified using v3 scheme"))
                    && it.trim().endsWith("true")
        }) {
            "Release APK is unsigned or has no verified v2/v3 signature:\n$verifyOutput"
        }
        val signerMatches = signerDigests(verifyOutput)
        val actualSignerSet = signerMatches.toSet()
        check(hasExactSignerSet(signerMatches, expectedReleaseSignerSet)) {
            "Release signer set mismatch: expected=$expectedReleaseSignerSet " +
                    "actual=$actualSignerSet count=${signerMatches.size}"
        }

        val version = android.defaultConfig.versionName
            ?: error("versionName is required for candidate staging")
        val distDir = rootProject.layout.projectDirectory.dir("dist").asFile
        check(distDir.isDirectory || distDir.mkdirs()) {
            "Unable to create candidate directory: $distDir"
        }
        val candidate = distDir.resolve("coolapk-purifier-v$version.apk")
        source.copyTo(candidate, overwrite = true)

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02X".format(it) }
        }

        val sourceHash = sha256(source)
        val candidateHash = sha256(candidate)
        check(sourceHash == candidateHash) {
            "Staged APK differs from assembled release"
        }
        val checksum = distDir.resolve(candidate.name + ".sha256")
        checksum.writeText("$candidateHash *${candidate.name}\n", Charsets.UTF_8)
        val recorded = checksum.readText(Charsets.UTF_8)
            .substringBefore(' ').trim()
        check(recorded.equals(sha256(candidate), ignoreCase = true)) {
            "Generated checksum does not match staged APK"
        }
        logger.lifecycle("Staged ${candidate.name} SHA-256=$candidateHash "
                + "signerCount=${actualSignerSet.size} signerSHA256=$actualSignerSet")
    }
}

/**
 * Run after versioned release notes are synchronized with the staged candidate.
 * Kept separate from staging so a new candidate can be built before its final
 * hash exists, while the final preflight still fails on stale release notes.
 */
// The repository copy resolves from the root; its staged copy resolves from dist.
// Transform only relative Markdown destinations, never the document's content/hash.
val markdownDestination = Regex("""\]\(([^)]+)\)""")
fun relativeMarkdownTarget(raw: String): String? {
    val target = raw.trim().removeSurrounding("<", ">").substringBefore('#')
    return target.takeIf {
        it.endsWith(".md") && !it.startsWith("/")
                && !Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(it)
    }
}
fun stagedReleaseNotes(text: String): String = markdownDestination.replace(text) { match ->
    val raw = match.groupValues[1].trim()
    if (relativeMarkdownTarget(raw) == null) match.value
    else if (raw.startsWith("<") && raw.endsWith(">")) "](<../" + raw.drop(1)
            .dropLast(1) + ">)"
    else "](../$raw)"
}

tasks.register("stageReleaseNotes") {
    group = "documentation"
    val version = android.defaultConfig.versionName
    val source = rootProject.file("release-notes-$version.md")
    val destination = rootProject.file("dist/release-notes-$version.md")
    inputs.file(source)
    outputs.file(destination)
    doLast {
        destination.parentFile.mkdirs()
        destination.writeText(stagedReleaseNotes(source.readText(Charsets.UTF_8)), Charsets.UTF_8)
    }
}

tasks.register("verifyDocumentationLinks") {
    group = "verification"
    dependsOn("stageReleaseNotes")
    doLast {
        // Guard the relocation operation as well as the actual current documents.
        check(stagedReleaseNotes("[report](report.md#result) [web](https://example.org/a.md)")
                == "[report](../report.md#result) [web](https://example.org/a.md)")
        check(stagedReleaseNotes("[report](<some report.md#result>)")
                == "[report](<../some report.md#result>)")
        val files = rootProject.fileTree(rootProject.projectDir) {
            include("README.md", "release-notes-2.2.2.md", "*report.md", "dist/release-notes-2.2.2.md")
        }.files
        var checked = 0
        files.forEach { document ->
            markdownDestination.findAll(document.readText(Charsets.UTF_8)).forEach { match ->
                relativeMarkdownTarget(match.groupValues[1])?.let { relative ->
                    val target = document.parentFile.resolve(relative).normalize()
                    check(target.isFile) { "Broken Markdown link: $document -> $relative" }
                    checked++
                }
            }
        }
        logger.lifecycle("Documentation links passed: $checked relative Markdown targets")
    }
}

tasks.register("verifySettingsReflectionNames") {
    group = "verification"
    dependsOn("minifyCompatibleWithR8", "minifyReleaseWithR8")
    doLast {
        listOf("compatible", "release").forEach { variant ->
            val mapping = layout.buildDirectory.file("outputs/mapping/$variant/mapping.txt")
                .get().asFile.readText()
            listOf("kotlin.Unit", "kotlin.jvm.functions.Function1").forEach { name ->
                check(mapping.lineSequence().any { it == "$name -> $name:" }) {
                    "Host reflection name was obfuscated in $variant: $name"
                }
            }
        }
        logger.lifecycle("Host settings reflection names retained in Compatible and Release")
    }
}

tasks.register("verifySplashDiagnosticIsolation") {
    group = "verification"
    dependsOn("generateDebugBuildConfig", "generateCompatibleBuildConfig", "generateReleaseBuildConfig",
        "generateSplashDiagnosticBuildConfig", "minifySplashDiagnosticWithR8")
    doLast {
        listOf("debug", "compatible", "release", "splashDiagnostic").forEach { variant ->
            val config = layout.buildDirectory.file("generated/source/buildConfig/$variant/" +
                "io/github/yylsping/coolapkpurifier/BuildConfig.java").get().asFile.readText()
            val expected = variant == "splashDiagnostic"
            check(config.contains("SPLASH_DIAGNOSTICS = $expected;")) {
                "Splash observation must be enabled only in splashDiagnostic: $variant"
            }
        }
        val mapping = layout.buildDirectory.file("outputs/mapping/splashDiagnostic/mapping.txt")
            .get().asFile.readText()
        check(mapping.lineSequence().any {
            it == "kotlin.jvm.functions.Function0 -> kotlin.jvm.functions.Function0:"
        }) { "Diagnostic host Function0 parameter name was rewritten" }
        logger.lifecycle("Splash diagnostic build isolation and host parameter name verified")
    }
}

tasks.register("verifyReleaseNotesPreflight") {
    group = "verification"
    description = "Verify release-notes hash and signer set against staged APK"
    dependsOn("stageReleaseCandidate", "verifyDocumentationLinks", "verifySettingsReflectionNames")

    doLast {
        val version = android.defaultConfig.versionName
            ?: error("versionName is required for release-notes preflight")
        val distDir = rootProject.layout.projectDirectory.dir("dist").asFile
        val candidate = distDir.resolve("coolapk-purifier-v$version.apk")
        val notes = distDir.resolve("release-notes-$version.md")
        check(candidate.isFile) { "Staged candidate missing: $candidate" }
        check(notes.isFile) { "Release notes missing: $notes" }

        val digest = MessageDigest.getInstance("SHA-256")
        candidate.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val candidateHash = digest.digest().joinToString("") { "%02X".format(it) }
        val text = notes.readText(Charsets.UTF_8)
        val recordedHash = Regex(
            """APK SHA-256:\s*`?([0-9A-Fa-f]{64})`?""")
            .find(text)?.groupValues?.get(1)?.uppercase()
        check(recordedHash == candidateHash) {
            "Release notes APK hash mismatch: recorded=$recordedHash actual=$candidateHash"
        }
        val upperNotes = text.uppercase()
        check(expectedReleaseSignerSet.all { upperNotes.contains(it) }) {
            "Release notes missing expected signer set: $expectedReleaseSignerSet"
        }
        logger.lifecycle("Release notes preflight passed APK SHA-256=$candidateHash " +
                "signerSHA256=$expectedReleaseSignerSet")
    }
}
