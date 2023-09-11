plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-80"
    kotlin("multiplatform") apply false
}

infra {
    teamcity {
    }
    publishing {
        include(":kotlinx-datetime")
        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-datetime"
        sonatype {
            libraryStagingRepoDescription = project.name
        }
    }
}

val mainJavaToolchainVersion by ext(project.property("java.mainToolchainVersion"))
val modularJavaToolchainVersion by ext(project.property("java.modularToolchainVersion"))

allprojects {
    repositories {
        addTrainRepositories(project)
        mavenCentral()
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        // outputs the compiler version to logs so we can check whether the train configuration applied
        kotlinOptions.freeCompilerArgs += "-version"
    }
}
