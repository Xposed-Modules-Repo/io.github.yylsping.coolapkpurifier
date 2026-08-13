import java.util.Properties

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
    namespace = "io.github.coolapk.adblock"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.coolapk.adblock"
        minSdk = 23
        targetSdk = 35
        versionCode = 5
        versionName = "2.0.0"

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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    testImplementation("junit:junit:4.13.2")
}
