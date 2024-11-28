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
    infra {
        target("linuxX64")
        target("linuxArm64")
        target("linuxArm32Hfp")
        target("mingwX64")
        target("macosX64")
        target("macosArm64")
        target("iosX64")
        target("iosArm64")
        target("iosSimulatorArm64")
        target("watchosArm32")
        target("watchosArm64")
        target("watchosX64")
        target("watchosSimulatorArm64")
        target("watchosDeviceArm64")
        target("tvosArm64")
        target("tvosX64")
        target("tvosSimulatorArm64")
        target("androidNativeArm32")
        target("androidNativeArm64")
        target("androidNativeX86")
        target("androidNativeX64")
    }

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
                metaInfo = true
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

        val jvmMain by getting
        val jvmTest by getting

        val jsMain by getting
        val jsTest by getting

        val wasmJsMain by getting
        val wasmJsTest by getting

        val wasmWasiMain by getting
        val wasmWasiTest by getting

        val nativeMain by getting
        val nativeTest by getting
    }
}
