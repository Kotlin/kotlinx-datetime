/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin
import java.util.*

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("com.github.node-gradle.node") version "7.0.2"
}

node {
    download.set(true)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
}

val tzdbVersion: String by rootProject.properties
version = "$tzdbVersion-spi.$version"

val convertedKtFilesDir = File(project.buildDir, "convertedTimesZones-full/src/internal/tzData")
val tzdbDirectory = File(project.projectDir, "tzdb")

val timeTzdbInstall by tasks.creating(NpmTask::class) {
    args.addAll(
        "install",
        "@tubular/time-tzdb",
    )
}

val tzdbDownloadAndCompile by tasks.creating(NpxTask::class) {
    doFirst {
        tzdbDirectory.mkdirs()
    }
    dependsOn(timeTzdbInstall)
    command.set("@tubular/time-tzdb")
    args.addAll("-b", "-o", "--large")
    if (tzdbVersion.isNotEmpty()) {
        args.addAll("-u", tzdbVersion)
    }
    args.add(tzdbDirectory.toString())
}

val generateZoneInfo by tasks.registering {
    inputs.dir(tzdbDirectory)
    outputs.dir(convertedKtFilesDir)
    doLast {
        generateZoneInfosResources(tzdbDirectory, convertedKtFilesDir, tzdbVersion)
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
        NpmResolverPlugin.apply(project) //Workaround KT-66373
    }

    sourceSets.all {
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).lowercase(Locale.ROOT).takeIf { it != "main" }
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources" } ?: "resources"}")
    }

    sourceSets {
        commonMain {
            dependencies {
                compileOnly(project(":kotlinx-datetime"))
                kotlin.srcDir(generateZoneInfo)
            }
        }

        val commonTest by getting {
            dependencies {
                runtimeOnly(project(":kotlinx-datetime"))
                implementation(kotlin("test"))
            }
        }

        val wasmWasiMain by getting {
            languageSettings.optIn("kotlinx.datetime.internal.InternalDateTimeApi")
        }
    }
}
