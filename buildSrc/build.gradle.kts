/*
 * Copyright 2016-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import java.util.*

plugins {
    `kotlin-dsl`
}

val props = Properties().apply {
    file("../gradle.properties").inputStream().use { load(it) }
}

// copy-pasted from `CommunityProjectsBuild`, see the explanation there
fun RepositoryHandler.addTrainRepositories(project: Project) {
    if (project.rootProject.properties["build_snapshot_train"]?.toString()?.toBoolean() == true) {
        mavenLocal()
    }
    (project.rootProject.properties["kotlin_repo_url"] as? String)?.let(::maven)
}

// copy-pasted from `CommunityProjectsBuild`, but uses `props` to obtain the non-snapshot version, because
// we don't have access to the properties defined in `gradle.properties` of the encompassing project
val Project.kotlinVersion: String
    get() = if (rootProject.properties["build_snapshot_train"]?.toString()?.toBoolean() == true) {
        rootProject.properties["kotlin_snapshot_version"] as? String ?: error("kotlin_snapshot_version must be specified")
    } else {
        props.getProperty("defaultKotlinVersion")
    }

repositories {
    mavenCentral()
    gradlePluginPortal()
    addTrainRepositories(project)
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

dependencies {
    fun gradlePlugin(id: String, version: String): String = "$id:$id.gradle.plugin:$version"
    implementation(gradlePlugin("org.jetbrains.kotlin.multiplatform", kotlinVersion))
    implementation(gradlePlugin("org.jetbrains.kotlin.plugin.serialization", kotlinVersion))
}
