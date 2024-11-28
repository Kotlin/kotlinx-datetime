import java.util.Locale

plugins {
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.kover")
}

val mainJavaToolchainVersion: String by project
val serializationVersion: String by project

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(mainJavaToolchainVersion)) }
}

kotlin {
    explicitApi()

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

    applyDefaultHierarchyTemplate()

    jvm {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
    }

    js {
        nodejs {
        }
        compilations.all {
            kotlinOptions {
                sourceMap = true
                moduleKind = "umd"
            }
        }
    }


    wasmJs {
        nodejs {
        }
    }

    wasmWasi {
        nodejs {
        }
    }

    sourceSets.all {
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).toLowerCase(Locale.ROOT).takeIf { it != "main" }
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources" } ?: "resources"}")
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["test"].kotlinOptions {
            freeCompilerArgs += listOf("-trw")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
    }
}
