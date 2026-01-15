// File: app/build.gradle.kts

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

private fun String.escapeForBuildConfigString(): String =
    buildString {
        for (c in this@escapeForBuildConfigString) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.zak.pressmark"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zak.pressmark"
        minSdk = 26
        targetSdk = 36

        // ✅ For “official” alpha cadence: bump this for every distributed build
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val discogsToken = (localProps["DISCOGS_TOKEN"] as String?)?.trim().orEmpty()
        val tokenLiteral = "\"${discogsToken.escapeForBuildConfigString()}\""
        buildConfigField("String", "DISCOGS_TOKEN", tokenLiteral)
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        /**
         * Alpha = installable, release-like behavior, debug-signed for easy side-load.
         */
        create("alpha") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")

            applicationIdSuffix = ".alpha"
            versionNameSuffix = "-alpha"

            isDebuggable = false
            matchingFallbacks += listOf("release")

            // ✅ Firebase App Distribution config for Alpha
            firebaseAppDistribution {
                // Option A: distribute to a Firebase tester GROUP
                groups = "internal"

                // Option B: distribute directly to emails (comma-separated)
                // testers = "wanderingbogeygrips@gmail.com"

                releaseNotes = "Pressmark 0.1.0-alpha01"
                // Or use a file:
                // releaseNotesFile = "release-notes-alpha.txt"
            }
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose (BOM once)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation + lifecycle compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room + KSP
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Images
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))

    // Instrumentation tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
