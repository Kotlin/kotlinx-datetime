import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-81"
    kotlin("multiplatform") apply false
    id("org.jetbrains.kotlinx.kover") version "0.8.0-Beta2"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.17.0"
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
        mavenCentral()

        // !! infrastructure for builds as a Kotlin user project
        val optionalKotlinArtifactsRepo = providers.gradleProperty("kotlin_repo_url").orNull
        if (optionalKotlinArtifactsRepo != null) {
            maven(url = optionalKotlinArtifactsRepo)
            logger.info(
                "[ktDT-as-KUP] Registered '$optionalKotlinArtifactsRepo' as a dependency Maven repository for '${path}'"
            )
        }
    }
}

// Disable NPM to NodeJS nightly compatibility check.
// Drop this when NodeJs version that supports latest Wasm become stable
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}

kover {
    reports {
        verify {
            rule {
                // requirement for a minimum lines coverage of 85%
                minBound(85)
            }
        }
    }
}

dependencies {
    kover(project(":kotlinx-datetime"))
    kover(project(":kotlinx-datetime-serialization"))
}

// !! infrastructure for builds as a Kotlin user project
subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            // output kotlin.git-searchable names of reported diagnostics
            freeCompilerArgs.add("-Xrender-internal-diagnostic-names")

            with(providers) {
                gradleProperty("kotlin_language_version").orNull?.let { optionalOverridingKotlinLV ->
                    languageVersion.set(KotlinVersion.fromVersion(optionalOverridingKotlinLV))
                    logger.info(
                        "[ktDT-as-KUP] Overrode the Kotlin language version with $optionalOverridingKotlinLV for '$path'"
                    )
                }
                gradleProperty("kotlin_api_version").orNull?.let { optionalOverridingKotlinAPIV ->
                    apiVersion.set(KotlinVersion.fromVersion(optionalOverridingKotlinAPIV))
                    logger.info(
                        "[ktDT-as-KUP] Overrode the Kotlin API version with $optionalOverridingKotlinAPIV for '$path'"
                    )
                }
            }
        }
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-jvm-default=disable")
        }
    }
    tasks.withType<KotlinNativeCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xpartial-linkage-loglevel=error")
        }
    }
    tasks.withType<Kotlin2JsCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xpartial-linkage-loglevel=error")
        }
    }
}
