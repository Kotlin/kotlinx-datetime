@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    js {
        nodejs {
        }
        binaries.executable()
    }
    wasmJs {
        nodejs {
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-datetime"))
            }
        }
        jsMain {
            dependencies {
                implementation(npm("@js-joda/timezone", "2.3.0"))
            }
        }
        wasmJsMain {
            dependencies {
                implementation(npm("@js-joda/timezone", "2.3.0"))
            }
        }
    }
}

// Ensure that loading the timezone code works even after minification
tasks.named("check") {
    dependsOn(tasks.named("jsNodeProductionRun"), tasks.named("wasmJsNodeProductionRun"))
}