/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("com.github.node-gradle.node") version "7.0.2"
    id("org.jetbrains.kotlinx.kover")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

node {
    download.set(true)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
}

val tzdbVersion = rootProject.property("tzdbVersion") as String
version = "$tzdbVersion-spi.$version"

val tzdbMetainformationDir =
    project.layout.buildDirectory.dir("convertedTimesZones-full/src/internal/tzdbMetainformation")
val tzdataAsKotlinFilesDir =
    project.layout.buildDirectory.dir("convertedTimesZones-full/src/internal/tzdataAsKotlinFiles")
val tzdbDirectory = File(project.projectDir, "tzdb")

val timeTzdbInstall = tasks.register<NpmTask>("timeTzdbInstall") {
    args.addAll(
        "install",
        "@tubular/time-tzdb",
    )
}

tasks.register<NpxTask>("tzdbDownloadAndCompile") {
    dependsOn(timeTzdbInstall)
    command.set("@tubular/time-tzdb")
    args.addAll("-b", "--large")
    if (tzdbVersion.isNotEmpty()) {
        args.addAll("-u", tzdbVersion)
    }
    args.add(tzdbDirectory.toString())
}

val generateTzdataAsKotlinFiles = tasks.register("generateTzdataAsKotlinFiles") {
    inputs.dir(tzdbDirectory)
    outputs.dir(tzdataAsKotlinFilesDir)
    doLast {
        generateZoneInfosResources(tzdbDirectory, tzdataAsKotlinFilesDir.get(), tzdbVersion)
    }
}

val generateTzdbMetainformation = tasks.register("generateTzdbMetainformation") {
    inputs.dir(tzdbDirectory)
    outputs.dir(tzdbMetainformationDir)
    doLast {
        generateTzdbMetainformation(tzdbDirectory, tzdbMetainformationDir.get(), tzdbVersion)
    }
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(project.property("mainJavaToolchainVersion") as String)) }
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
    macosArm64()
    iosSimulatorArm64()
    iosArm64()
    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()
    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    iosX64()
    mingwX64()
    watchosDeviceArm64()
    // Deprecated, preserved for KT-58864
    @Suppress("DEPRECATION") linuxArm32Hfp()
    // Deprecated for removal: KT-78660
    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    run {
        macosX64()
        watchosX64()
        tvosX64()
    }

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

        named("commonWithoutResourcesMain") {
            kotlin.srcDir(generateTzdataAsKotlinFiles)
        }

        commonTest {
            dependencies {
                runtimeOnly(project(":kotlinx-datetime"))
                implementation(kotlin("test"))
                implementation(project(":test-utils"))
            }
        }

        wasmWasiMain {
            languageSettings.optIn("kotlinx.datetime.internal.InternalDateTimeApi")
        }
    }
}

tasks {
    named<ProcessResources>("jvmProcessResources") {
        into("tzdb") {
            from(tzdbDirectory)
        }
    }

    // Copy-pasted from core/build.gradle.kts. TODO: unify in buildSrc/.

    val compileJavaModuleInfo = register<JavaCompile>("compileJavaModuleInfo") {
        val moduleName = "kotlinx.datetime.zoneinfo" // this module's name
        val compileKotlinJvm = getByName<KotlinCompile>("compileKotlinJvm")
        val sourceDir = file("jvm/java9/")
        val targetDir = compileKotlinJvm.destinationDirectory.map { it.dir("../java9/") }

        // Use a Java 11 compiler for the module info.
        javaCompiler.set(project.javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(project.property("modularJavaToolchainVersion") as String))
        })

        // Always compile kotlin classes before the module descriptor.
        dependsOn(compileKotlinJvm)

        // Add the module-info source file.
        source(sourceDir)

        // Also add the module-info.java source file to the Kotlin compile task.
        // The Kotlin compiler will parse and check module dependencies,
        // but it currently won't compile to a module-info.class file.
        // Note that module checking only works on JDK 9+,
        // because the JDK built-in base modules are not available in earlier versions.
        val javaVersion = compileKotlinJvm.kotlinJavaToolchain.javaVersion.getOrNull()
        if (javaVersion?.isJava9Compatible == true) {
            logger.info("Module-info checking is enabled; $compileKotlinJvm is compiled using Java $javaVersion")
            compileKotlinJvm.source(sourceDir)
        } else {
            logger.info("Module-info checking is disabled")
        }

        // Set the task outputs and destination dir
        outputs.dir(targetDir)
        destinationDirectory.set(targetDir)

        // Configure JVM compatibility
        sourceCompatibility = JavaVersion.VERSION_1_9.toString()
        targetCompatibility = JavaVersion.VERSION_1_9.toString()

        // Set the Java release version.
        options.release.set(9)

        // Ignore warnings about using 'requires transitive' on automatic modules.
        // not needed when compiling with recent JDKs, e.g. 17
        options.compilerArgs.add("-Xlint:-requires-transitive-automatic")

        // Patch the compileKotlinJvm output classes into the compilation so exporting packages works correctly.
        options.compilerArgs.addAll(listOf("--patch-module", "$moduleName=${compileKotlinJvm.destinationDirectory.get()}"))

        // Use the classpath of the compileKotlinJvm task.
        // Also ensure that the module path is used instead of classpath.
        classpath = compileKotlinJvm.libraries

        modularity.inferModulePath.set(true)
        options.javaModuleVersion.set(project.version.toString().takeUnless { it == Project.DEFAULT_VERSION })
    }

    named<Jar>("jvmJar") {
        manifest {
            attributes(
                "Multi-Release" to true,
                "Implementation-Vendor" to "JetBrains",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            )
        }
        from(compileJavaModuleInfo.map { it.destinationDirectory }) {
            into("META-INF/versions/9/")
        }
    }
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}
