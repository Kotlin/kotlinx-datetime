plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-81"
    kotlin("multiplatform") apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.14.0"
}

infra {
    teamcity {
    }
    publishing {
        include(":kotlinx-datetime")
        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-datetime"
        sonatype {
            libraryStagingRepoDescription = project.name
        }
    }
}

val mainJavaToolchainVersion by ext(project.property("java.mainToolchainVersion"))
val modularJavaToolchainVersion by ext(project.property("java.modularToolchainVersion"))

allprojects {
    repositories {
        addTrainRepositories(project)
        mavenCentral()
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        // outputs the compiler version to logs so we can check whether the train configuration applied
        kotlinOptions.freeCompilerArgs += "-version"
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
        compilerOptions { freeCompilerArgs.add("-Xpartial-linkage-loglevel=ERROR") }
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
        compilerOptions { freeCompilerArgs.add("-Xpartial-linkage-loglevel=ERROR") }
    }
}

// Disable NPM to NodeJS nightly compatibility check.
// Drop this when NodeJs version that supports latest Wasm become stable
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}
