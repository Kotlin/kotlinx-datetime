import kotlinx.validation.ArtifactsValidatorPluginSettingsExtension

pluginManagement {
    repositories {
        maven(url = "https://packages.jetbrains.team/maven/p/kotlinx-team-infra/maven")
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
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

plugins {
    id("org.jetbrains.kotlinx.artifacts-validator-plugin") version "0.0.1-SNAPSHOT"
}

rootProject.name = "Kotlin-DateTime-library"

include(":core")
project(":core").name = "kotlinx-datetime"
include(":timezones/full")
project(":timezones/full").name = "kotlinx-datetime-zoneinfo"
include(":integration-testing/serialization")
project(":integration-testing/serialization").name = "kotlinx-datetime-serialization"
include(":integration-testing/js-without-timezones")
project(":integration-testing/js-without-timezones").name = "kotlinx-datetime-js-test-without-timezones"
include(":integration-testing/js-with-timezones")
project(":integration-testing/js-with-timezones").name = "kotlinx-datetime-js-test-with-timezones"
include(":integration-testing/jpms-test")
project(":integration-testing/jpms-test").name = "kotlinx-datetime-test-with-jpms"
include(":benchmarks")

extensions.getByType<ArtifactsValidatorPluginSettingsExtension>().apply {
    dumpFileForProjects(rootDir.resolve("gradle/artifacts.zoneinfo.txt"), project(":kotlinx-datetime-zoneinfo"))
}
