// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM module: all sound synthesis lives here so it can be unit-tested and
// rendered to WAV on any computer, without an Android device.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
}

/** ./gradlew :engine-core:renderPreviews → engine-core/build/previews/*.wav */
tasks.register<JavaExec>("renderPreviews") {
    group = "omoz"
    description = "Render every engine sound to a WAV file for listening on a computer."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.logicsekai.omoz.engine.tools.PreviewRenderer")
    args(layout.buildDirectory.dir("previews").get().asFile.absolutePath)
}
