@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import java.util.Locale

plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-81"
    kotlin("multiplatform") version "1.9.21"
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js {
        nodejs {
        }
    }

    wasmJs {
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

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-datetime:$version")
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

// Disable NPM to NodeJS nightly compatibility check.
// Drop this when NodeJs version that supports latest Wasm become stable
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}

// Drop this configuration when the Node.JS version in KGP will support wasm gc milestone 4
// check it here:
// https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/js/nodejs/NodeJsRootExtension.kt
with(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(rootProject)) {
    nodeVersion = "21.0.0-v8-canary202309167e82ab1fa2"
    nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary" 
}
