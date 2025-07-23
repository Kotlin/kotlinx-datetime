/*
 * Copyright 2016-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import java.util.*

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()

    // !! infrastructure for builds as a Kotlin user project
    // this is an inlined version of `KotlinUserProjectUtilities.kupArtifactsRepo`
    // (because `buildSrc/.../KotlinUserProjectInfra.kt` is not available here)
    val kupArtifactsRepoURL = providers.gradleProperty("kotlin_repo_url").orNull
    if (kupArtifactsRepoURL != null) {
        maven(kupArtifactsRepoURL)
        logger.lifecycle("[KUP infra] Added '$kupArtifactsRepoURL' as a Maven repo to ':buildSrc'")
    }
}

// !! infrastructure for builds as a Kotlin user project
/**
 * the value provided via the `kotlin_version` Gradle property if any
 * (otherwise, `defaultKotlinVersion` defined in `gradle.properties` of the root project is used instead);
 * note that there is no direct `buildSrc/.../KotlinUserProjectInfra.kt` analogue for this utility
 * because `kotlin_version` is commonly used where `buildSrc/.../KotlinUserProjectInfra.kt` is not available
 * (`settings.gradle.kts`, `buildSrc`, etc.)
 */
val Project.kotlinVersion: String by lazy {
    val kotlinVersion: String = providers.gradleProperty("kotlin_version").orNull
        ?: run {
            // we don't have access to the properties defined in `gradle.properties` of the root project,
            // so we have to get them manually
            val properties = Properties().apply {
                file("../gradle.properties").inputStream().use { load(it) }
            }
            properties.getProperty("defaultKotlinVersion")
        }
    logger.lifecycle("[KUP infra] Set Kotlin distribution version to '$kotlinVersion'")
    kotlinVersion
}

dependencies {
    fun gradlePlugin(id: String, version: String): String = "$id:$id.gradle.plugin:$version"
    implementation(gradlePlugin("org.jetbrains.kotlin.multiplatform", kotlinVersion))
    implementation(gradlePlugin("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
}
