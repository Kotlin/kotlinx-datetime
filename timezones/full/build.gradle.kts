/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("com.github.node-gradle.node") version "7.0.2"
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

node {
    download.set(true)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
}

val tzdbVersion: String by rootProject.properties
version = "$tzdbVersion-spi.$version"

val tzdbMetainformationDir =
    project.layout.buildDirectory.dir("convertedTimesZones-full/src/internal/tzdbMetainformation")
val tzdataAsKotlinFilesDir =
    project.layout.buildDirectory.dir("convertedTimesZones-full/src/internal/tzdataAsKotlinFiles")
val tzdbDirectory = File(project.projectDir, "tzdb")

val copiedTzdbDirectory = project.layout.buildDirectory.dir("jvmResources")

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

val tzdbCopyToJvmResources by tasks.creating(Copy::class) {
    val outputDir = copiedTzdbDirectory.map { it.dir("tzdb") }
    inputs.dir(tzdbDirectory)
    outputs.dir(outputDir)
    from(tzdbDirectory)
    into(outputDir)
}

val generateTzdataAsKotlinFiles by tasks.registering {
    inputs.dir(tzdbDirectory)
    outputs.dir(tzdataAsKotlinFilesDir)
    doLast {
        generateZoneInfosResources(tzdbDirectory, tzdataAsKotlinFilesDir.get(), tzdbVersion)
    }
}

val generateTzdbMetainformation by tasks.registering {
    inputs.dir(tzdbDirectory)
    outputs.dir(tzdbMetainformationDir)
    doLast {
        generateTzdbMetainformation(tzdbDirectory, tzdbMetainformationDir.get(), tzdbVersion)
    }
}

kotlin {
    explicitApi()
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            // The targets with no native notion of resources. Currently, that's everything except the JVM.
            group("commonWithoutResources") {
                withJs()
                withWasmJs()
                withWasmWasi()
                withLinux()
                withMacosX64()
                withMacosArm64()
                withWatchosX64()
                withWatchosArm32()
                withWatchosArm64()
                withTvosX64()
                withTvosArm64()
                withIosArm64()
                withWatchosDeviceArm64()
                withIosSimulatorArm64()
                withIosX64()
                withWatchosSimulatorArm64()
                withTvosSimulatorArm64()
                withAndroidNative()
                withMingw()
            }
        }
    }

    jvm {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.all {
            // Set compilation options for JVM target here
        }

    }

    js {
        nodejs {
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Tiers are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
    // Tier 1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()
    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()
    // Deprecated
    @Suppress("DEPRECATION") linuxArm32Hfp()

    sourceSets.all {
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).lowercase().takeIf { it != "main" }
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources" } ?: "resources"}")
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-datetime"))
            }
            kotlin.srcDir(generateTzdbMetainformation)
        }

        val commonWithoutResourcesMain by getting {
            kotlin.srcDir(generateTzdataAsKotlinFiles)
        }

        val commonTest by getting {
            dependencies {
                runtimeOnly(project(":kotlinx-datetime"))
                implementation(kotlin("test"))
            }
        }

        jvmMain {
            resources.srcDir(copiedTzdbDirectory)
        }

        val wasmWasiMain by getting {
            languageSettings.optIn("kotlinx.datetime.internal.InternalDateTimeApi")
        }
    }
}

tasks.named("jvmProcessResources") {
    dependsOn(tzdbCopyToJvmResources)
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}
