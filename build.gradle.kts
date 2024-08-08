plugins {
    id("kotlinx.team.infra")
    id("org.jetbrains.kotlinx.kover")

    kotlin("multiplatform") apply false
}

fun javaLanguageVersionProperty(propertyName: String) = JavaLanguageVersion.of(property(propertyName) as String)
val mainJavaToolchainVersion by extra(javaLanguageVersionProperty("java.mainToolchainVersion"))
val modularJavaToolchainVersion by extra(javaLanguageVersionProperty("java.modularToolchainVersion"))

infra {
    publishing {
        include(":kotlinx-datetime")
        libraryRepoUrl = "https://github.com/Kotlin/kotlinx-datetime"
        sonatype {
            libraryStagingRepoDescription = project.name
        }
    }
}

kover {
    reports {
        verify {
            rule {
                // requirement for a minimum line coverage of 85%
                minBound(85)
            }
        }
    }
}

dependencies {
    kover(project(":kotlinx-datetime"))
    kover(project(":kotlinx-datetime-serialization"))
}
