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
    val optionalKotlinArtifactsRepo = providers.gradleProperty("kotlin_repo_url").orNull
    if (optionalKotlinArtifactsRepo != null) {
        maven(url = optionalKotlinArtifactsRepo)
        logger.info(
            "[ktDT-as-KUP] Registered '$optionalKotlinArtifactsRepo' as a dependency Maven repository for buildSrc"
        )
    }
}

// !! infrastructure for builds as a Kotlin user project
val Project.kotlinVersion: String by lazy {
        val optionalOverridingKotlinVersion = providers.gradleProperty("kotlin_version").orNull
        if (optionalOverridingKotlinVersion != null) {
            logger.info(
                "[ktDT-as-KUP] Overrode the Kotlin distribution version with $optionalOverridingKotlinVersion"
            )
            optionalOverridingKotlinVersion
        } else {
            // we don't have access to the properties defined in `gradle.properties` of the encompassing project,
            // so we have to get them manually
            val properties = Properties().apply {
                file("../gradle.properties").inputStream().use { load(it) }
            }
            properties.getProperty("defaultKotlinVersion")
        }
    }

dependencies {
    fun gradlePlugin(id: String, version: String): String = "$id:$id.gradle.plugin:$version"
    implementation(gradlePlugin("org.jetbrains.kotlin.multiplatform", kotlinVersion))
    implementation(gradlePlugin("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
}
