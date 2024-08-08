pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
    }
    val kotlinxInfraVersion: String by settings
    val kotlinVersion: String by settings
    val koverVersion: String by settings
    val dokkaVersion: String by settings
    val jmhPluginVersion: String by settings
    plugins {
        id("kotlinx.team.infra") version kotlinxInfraVersion
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jetbrains.kotlinx.kover") version koverVersion
        id("org.jetbrains.dokka") version dokkaVersion
        id("me.champeau.jmh") version jmhPluginVersion
    }
}

gradle.projectsLoaded {
    allprojects {
        repositories {
            mavenCentral()
        }
    }
}

rootProject.name = "Kotlin-DateTime-library"

include(":core")
project(":core").name = "kotlinx-datetime"

include(":serialization")
project(":serialization").name = "kotlinx-datetime-serialization"

include(":benchmarks")
