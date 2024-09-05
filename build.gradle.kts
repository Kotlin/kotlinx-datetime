import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    with(libs.plugins) {
        alias(kotlinx.infra)
        alias(kover)

        alias(kotlin.multiplatform) apply false
    }
}

fun javaLanguageVersionProperty(propertyName: String) = JavaLanguageVersion.of(property(propertyName) as String)
val mainJavaToolchainVersion by extra(javaLanguageVersionProperty("java.mainToolchainVersion"))
val modularJavaToolchainVersion by extra(javaLanguageVersionProperty("java.modularToolchainVersion"))

infra {
    publishing {
        include(":kotlinx-datetime")
        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-datetime"
        sonatype {
            libraryStagingRepoDescription = project.name
        }
    }
}

kover {
    reports {
        verify {
            rule {
                // requirement for a minimum line coverage of 85%
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
// (although some of these settings are just enabled by default because they're not very invasive anyway)
subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            // output kotlin.git-searchable names of reported diagnostics
            freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
            // output reported warnings even in the presence of reported errors
            freeCompilerArgs.add("-Xreport-all-warnings")

            with(providers) {
                gradleProperty("kotlin_language_version").orNull?.let { optionalOverridingKotlinLanguageVersion ->
                    languageVersion = KotlinVersion.fromVersion(optionalOverridingKotlinLanguageVersion)
                    logger.info(
                        "[ktDT-as-KUP] Overrode the Kotlin language version with $optionalOverridingKotlinLanguageVersion for '$path'"
                    )
                }
                gradleProperty("kotlin_api_version").orNull?.let { optionalOverridingKotlinApiVersion ->
                    apiVersion = KotlinVersion.fromVersion(optionalOverridingKotlinApiVersion)
                    logger.info(
                        "[ktDT-as-KUP] Overrode the Kotlin API version with $optionalOverridingKotlinApiVersion for '$path'"
                    )
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
