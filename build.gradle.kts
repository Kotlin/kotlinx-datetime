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


allprojects {
    repositories {
        mavenCentral()
    }
}

