plugins {
    id("kotlinx.team.infra") version "0.3.0-dev-64"
    kotlin("multiplatform") apply false
}

infra {
    teamcity {
        libraryStagingRepoDescription = project.name
    }
    publishing {
        include(":kotlinx-datetime")
        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-datetime"
        sonatype { }
    }
}

val mainJavaToolchainVersion by ext(project.property("java.mainToolchainVersion"))
val modularJavaToolchainVersion by ext(project.property("java.modularToolchainVersion"))

allprojects {
    repositories {
        addTrainRepositories(project)
        mavenCentral()
    }
}

logger.info("Using Kotlin compiler version: ${org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION}")
