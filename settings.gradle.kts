pluginManagement {
    repositories {
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        mavenCentral()
        gradlePluginPortal()
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
include(":js-with-timezones")
project(":js-with-timezones").name = "kotlinx-datetime-js-test-with-timezones"
include(":jpms-test")
project(":jpms-test").name = "kotlinx-datetime-test-with-jpms"
include(":benchmarks")
