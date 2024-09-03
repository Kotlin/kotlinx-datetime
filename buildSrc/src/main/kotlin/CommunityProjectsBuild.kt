/*
 * Copyright 2016-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:JvmName("CommunityProjectsBuild")

import org.gradle.api.*
import org.gradle.api.artifacts.dsl.*
import org.gradle.api.artifacts.repositories.*
import org.gradle.kotlin.dsl.*
import java.util.logging.*

private val LOGGER: Logger = Logger.getLogger("Kotlin settings logger")

/**
 * Functions in this file are responsible for configuring kotlinx-datetime build against a custom dev version
 * of Kotlin compiler.
 * Such configuration is used in a composite community build of Kotlin in order to check whether not-yet-released changes
 * are compatible with our libraries (aka "integration testing that substitues lack of unit testing").
 *
 * Three parameters are used to configure the build:
 * 1. `build_snapshot_train` - if true, the build is run against a snapshot train of Kotlin compiler
 * 2. `kotlin_snapshot_version` - if specified, the build uses a custom Kotlin compiler version
 * 3. `kotlin_repo_url` - if specified, the build uses a custom Kotlin compiler repository
 *
 * If `build_snapshot_train` is true, `kotlin_snapshot_version` must be present.
 */

/**
 * Adds all the repositories that are needed for the Kotlin aggregate build.
 */
fun RepositoryHandler.addTrainRepositories(project: Project) {
    if (project.rootProject.properties["build_snapshot_train"]?.toString()?.toBoolean() == true) {
        mavenLocal()
    }
    // A kotlin-dev space repository with dev versions of Kotlin.
    val devRepoUrl = project.rootProject.properties["kotlin_repo_url"] as? String ?: return
    LOGGER.info("""Configured Kotlin Compiler repository url: '$devRepoUrl' for project ${project.name}""")
    maven(devRepoUrl)
}

/**
 * The version of Kotlin compiler to use in the Kotlin aggregate build.
 */
// unused, but may be useful in the future
val Project.kotlinVersion: String
    get() = if (rootProject.properties["build_snapshot_train"]?.toString()?.toBoolean() == true) {
        rootProject.properties["kotlin_snapshot_version"] as? String ?: error("kotlin_snapshot_version must be specified")
    } else {
        rootProject.properties["defaultKotlinVersion"] as String
    }
