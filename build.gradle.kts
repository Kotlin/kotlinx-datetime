import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-88"
    kotlin("multiplatform") apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
}

infra {
    teamcity {
    }
    publishing {
        include(":kotlinx-datetime")
        include(":kotlinx-datetime-zoneinfo")
        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-datetime"
    }
}

val mainJavaToolchainVersion by ext(project.property("java.mainToolchainVersion"))
val modularJavaToolchainVersion by ext(project.property("java.modularToolchainVersion"))

allprojects {
    repositories {
        mavenCentral()
        kupInfra {
            kupArtifactsRepo(context = project)
        }
    }
}

subprojects {
    kupInfra {
        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            kupConfiguration()
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
