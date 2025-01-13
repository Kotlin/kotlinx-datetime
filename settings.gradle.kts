pluginManagement {
    repositories {
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        mavenCentral()
        gradlePluginPortal()

        // !! infrastructure for builds as a Kotlin user project
        val optionalKotlinArtifactsRepo = providers.gradleProperty("kotlin_repo_url").orNull
        if (optionalKotlinArtifactsRepo != null) {
            maven(url = optionalKotlinArtifactsRepo)
            logger.info(
                "[ktDT-as-KUP] Registered '$optionalKotlinArtifactsRepo' as a plugin Maven repository"
            )
        }
    }
    val dokkaVersion: String by settings
    val benchmarksVersion: String by settings
    val bcvVersion: String by settings
    plugins {
        id("org.jetbrains.dokka") version dokkaVersion
        id("me.champeau.jmh") version benchmarksVersion
        id("org.jetbrains.kotlinx.binary-compatibility-validator") version bcvVersion
    }
}

rootProject.name = "Kotlin-DateTime-library"

include(":core")
project(":core").name = "kotlinx-datetime"
include(":timezones/full")
project(":timezones/full").name = "kotlinx-datetime-zoneinfo"
include(":serialization")
project(":serialization").name = "kotlinx-datetime-serialization"
include(":js-without-timezones")
project(":js-without-timezones").name = "kotlinx-datetime-js-test-without-timezones"
include(":benchmarks")
