@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import java.util.Locale

plugins {
    kotlin("multiplatform") version "2.1.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

val datetimeVersion: String = property("datetimeVersion").toString()

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
                api("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}
