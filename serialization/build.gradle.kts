import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.kover")
}

val mainJavaToolchainVersion: JavaLanguageVersion by project
val modularJavaToolchainVersion: JavaLanguageVersion by project

val serializationVersion: String by project
val jsJodaCoreVersion: String by project
val jsJodaTimezoneVersion: String by project

java {
    toolchain.languageVersion = mainJavaToolchainVersion
}

kotlin {
    // all-targets configuration

    // [^ none that would be specific to this Gradle module]

    // target-specific and compilation-specific configuration

    jvm {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, java.toolchain.languageVersion.get().asInt())
        }
    }

    // tiers of K/Native targets are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
    infra {
        // Tier 1
        target("macosX64")
        target("macosArm64")
        target("iosSimulatorArm64")
        target("iosX64")
        // Tier 2
        target("linuxX64")
        target("linuxArm64")
        target("watchosSimulatorArm64")
        target("watchosX64")
        target("watchosArm32")
        target("watchosArm64")
        target("tvosSimulatorArm64")
        target("tvosX64")
        target("tvosArm64")
        target("iosArm64")
        // Tier 3
        target("mingwX64")
        target("watchosDeviceArm64")
        // Deprecated
        target("linuxArm32Hfp")
    }

    targets.withType<KotlinNativeTarget> {
        compilations["test"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-trw")
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    js {
        nodejs()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap = true
            moduleKind = JsModuleKind.MODULE_UMD
        }
    }

    // configuration of source sets

    sourceSets.all {
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).lowercase().takeIf { it != "main" }
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources" } ?: "resources"}")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlinx-datetime"))
            }
        }
        val commonTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }

        val jvmMain by getting
        val jvmTest by getting

        val nativeMain by getting
        val nativeTest by getting

        val commonJsMain by creating {
            dependsOn(commonMain)
        }
        val commonJsTest by creating {
            dependsOn(commonTest)
            dependencies {
                implementation(npm("@js-joda/timezone", jsJodaTimezoneVersion))
            }
        }

        val wasmJsMain by getting {
            dependsOn(commonJsMain)
        }
        val wasmJsTest by getting {
            dependsOn(commonJsTest)
        }

        val jsMain by getting {
            dependsOn(commonJsMain)
        }
        val jsTest by getting {
            dependsOn(commonJsTest)
        }
    }
}
