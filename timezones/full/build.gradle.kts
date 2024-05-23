/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import TzdbTasks.ConvertType
import TzdbTasks.InstallTimeTzdb
import TzdbTasks.TzdbDownloadAndCompile
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin
import java.util.*

plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

val tzdbVersion: String by rootProject.properties
version = "$tzdbVersion-spi.$version"

val convertedKtFilesDir = File(project.buildDir, "wasmWasi-full/src/internal/tzData")
val tzdbDirectory = File(project.projectDir, "tzdb")

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
        NpmResolverPlugin.apply(project) //Workaround KT-66373
    }

    sourceSets.all {
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).toLowerCase(Locale.ROOT).takeIf { it != "main" }
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources" } ?: "resources"}")
    }

    sourceSets {
        commonMain {
            dependencies {
                compileOnly(project(":kotlinx-datetime"))
            }
        }

        val wasmWasiMain by getting {
            kotlin.srcDir(convertedKtFilesDir)
            languageSettings.optIn("kotlinx.datetime.internal.InternalDateTimeApi")
        }

        val wasmWasiTest by getting {
            dependencies {
                runtimeOnly(project(":kotlinx-datetime"))
                implementation(kotlin("test"))
            }
        }
    }
}

val timeTzdbInstall by tasks.creating(InstallTimeTzdb::class) { }

val tzdbDownloadAndCompile by tasks.creating(TzdbDownloadAndCompile::class) {
    dependsOn(timeTzdbInstall)
    timeTzdbDirectory.set(timeTzdbInstall.outputDirectory)
    outputDirectory.set(tzdbDirectory)
    ianaVersion.set(tzdbVersion)
    convertType.set(ConvertType.LARGE)
}

val generateWasmWasiZoneInfo by tasks.registering {
    inputs.dir(tzdbDirectory)
    outputs.dir(convertedKtFilesDir)
    doLast {
        generateZoneInfosResources(tzdbDirectory, convertedKtFilesDir, tzdbVersion)
    }
}

tasks.getByName("compileKotlinWasmWasi") {
    dependsOn(generateWasmWasiZoneInfo)
}

tasks.getByName("wasmWasiSourcesJar") {
    dependsOn(generateWasmWasiZoneInfo)
}