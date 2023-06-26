pluginManagement {
    repositories {
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        mavenCentral()
        gradlePluginPortal()
    }
    val dokkaVersion: String by settings
    plugins {
        id("org.jetbrains.dokka") version dokkaVersion
    }
}

rootProject.name = "Kotlin-DateTime-library"

include(":core")
project(":core").name = "kotlinx-datetime"
include(":serialization")
project(":serialization").name = "kotlinx-datetime-serialization"
