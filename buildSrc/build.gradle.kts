/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
 *
 * D4L owns all legal rights, title and interest in and to the Software Development Kit ("SDK"),
 * including any intellectual property rights that subsist in the SDK.
 *
 * The SDK and its documentation may be accessed and used for viewing/review purposes only.
 * Any usage of the SDK for other purposes, including usage for the development of
 * applications/third-party applications shall require the conclusion of a license agreement
 * between you and D4L.
 *
 * If you are interested in licensing the SDK for your own applications/third-party
 * applications and/or if you’d like to contribute to the development of the SDK, please
 * contact D4L by email to help@data4life.care.
 */
import care.data4life.gradle.core.dependency.d4l
import care.data4life.gradle.core.dependency.ensureKotlinVersion

plugins {
    `kotlin-dsl`

    id("care.data4life.gradle.core.dependency")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
    d4l()
}

dependencies {
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.kotlin)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.android)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.serialization)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.groovyAll)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.androidMaven)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.androidJacoco)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.dexcount)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.downloadTask)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.httpComponents)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.dokka)
    implementation(care.data4life.gradle.core.dependency.GradlePlugin.owasp)

    implementation("com.google.code.gson:gson:2.9.1")

    // dependency-updates.gradle.kts
    implementation("com.github.ben-manes:gradle-versions-plugin:0.42.0")
    // download-scripts.gradle.kts
    implementation("de.undercouch:gradle-download-task:5.1.1")
    // publishing.gradle.kts
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.2.0.202206071550-r")
    // quality-spotless.gradle.kts
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.9.0")
    implementation("com.pinterest:ktlint:0.46.1")
    // versioning.gradle.kts
    implementation("care.data4life.gradle.gitversion:gradle-git-version:0.12.4-d4l")
}
