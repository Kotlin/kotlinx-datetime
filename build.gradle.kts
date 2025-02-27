import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-85"
    kotlin("multiplatform") apply false
    id("org.jetbrains.kotlinx.kover") version "0.8.0-Beta2"
}

infra {
    teamcity {
    }
    publishing {
        include(":kotlinx-datetime")
        include(":kotlinx-datetime-zoneinfo")
        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-datetime"
        sonatype {
            libraryStagingRepoDescription = project.name
        }
    }
}

val mainJavaToolchainVersion by ext(project.property("java.mainToolchainVersion"))
val modularJavaToolchainVersion by ext(project.property("java.modularToolchainVersion"))

// !! infrastructure for builds as a Kotlin user project
val shouldWarningsBeErrors = providers.gradleProperty("kotlin_Werror_override")
    .map { optionalWErrorOverride ->
        when (optionalWErrorOverride) {
            "enable" -> true
            "disable" -> false
            else -> error("Unknown value for 'kotlin_Werror_override': $optionalWErrorOverride")
        }
    }
    .getOrElse(false)
    .also { logger.info("shouldWarningsBeErrors: $it") }

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

subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            allWarningsAsErrors.set(shouldWarningsBeErrors)
        }
    }

    // drop this after migration to 2.2.0
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=disable")
        }
    }
}

kover {
    reports {
        verify {
            rule {
                // requirement for a minimum lines coverage of 80%
                minBound(80)
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
            // output reported warnings even in the presence of reported errors
            freeCompilerArgs.add("-Xreport-all-warnings")
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
                gradleProperty("kotlin_additional_cli_options").orNull?.let { optionalAdditionalCLIOptions ->
                    if (optionalAdditionalCLIOptions.isNotBlank()) {
                        freeCompilerArgs.addAll(optionalAdditionalCLIOptions.split(" "))
                        logger.info(
                            "[ktDT-as-KUP] Added the following Kotlin CLI options to '$path': $optionalAdditionalCLIOptions"
                        )
                    }
                }
            }
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
