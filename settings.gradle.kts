pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")
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
