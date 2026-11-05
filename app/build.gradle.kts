// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing: scripts\signing-setup.bat creates keystore\omoz-release.jks and
// keystore.properties in the project root (both ignored by git). Without them the release build
// is signed with the debug key: fine for testing, not for publishing.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val omoVersionCode = (findProperty("omo.versionCode") as String?)?.toInt() ?: 1
val omoVersionName = (findProperty("omo.versionName") as String?) ?: "0.0.0"

// ---- Distribution ----------------------------------------------------------------------------
// "github" (default): APKs published on GitHub Releases. They check for updates, can download and
// install new versions, and show the donation links.
// "play": builds for Google Play (-Pomo.distribution=play). The store handles updates, so
// self-update and its install permission are removed, and donation links are hidden because
// Play's payments policy does not allow them outside Play billing.
val playBuild = (findProperty("omo.distribution") as String?) == "play"
fun quoted(value: String) = "\"$value\""

android {
    namespace = "com.logicsekai.omoz.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.logicsekai.omoz"
        minSdk = 26
        targetSdk = 35
        versionCode = omoVersionCode
        versionName = omoVersionName
        vectorDrawables { useSupportLibrary = true }

        // Links shown in the app. Leave a value empty to hide it.
        buildConfigField("String", "UPDATE_REPO", quoted(if (playBuild) "" else "LogicSekai/OmoZ"))
        buildConfigField("String", "SOURCE_URL", quoted("https://github.com/LogicSekai/OmoZ"))
        buildConfigField("String", "COMMUNITY_URL", quoted("https://logicsekai.com/omo/omoz"))
        buildConfigField("String", "SPONSOR_URL", quoted(if (playBuild) "" else "https://github.com/sponsors/LogicSekai"))
        buildConfigField("String", "KOFI_URL", quoted(if (playBuild) "" else "https://ko-fi.com/logicsekai"))
        buildConfigField("String", "TRAKTEER_URL", quoted(if (playBuild) "" else "https://trakteer.id/kwaii"))
        // Release builds from GitHub download and install updates themselves; debug builds
        // (different package) only link to the release page.
        buildConfigField("boolean", "SELF_UPDATE", "false")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (keystoreProps.isNotEmpty()) "release" else "debug")
            buildConfigField("boolean", "SELF_UPDATE", (!playBuild).toString())
        }
    }

    // Play builds: drop the self-update install permission (see src/play/AndroidManifest.xml).
    if (playBuild) {
        sourceSets.getByName("release").manifest.srcFile("src/play/AndroidManifest.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Lint must never block a community build; run ./gradlew lint to see the report.
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":engine-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
