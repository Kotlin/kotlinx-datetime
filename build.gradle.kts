plugins {
    id("kotlinx.team.infra") version "0.4.0-dev-81"
    kotlin("multiplatform") apply false
    id("org.jetbrains.kotlinx.kover") version "0.8.0-Beta2"
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
        mavenCentral()
    }
}

kover {
    reports {
        verify {
            rule {
                // requirement for a minimum lines coverage of 85%
                minBound(85)
            }
        }
    }
}

dependencies {
    kover(project(":kotlinx-datetime"))
    kover(project(":kotlinx-datetime-serialization"))
}
