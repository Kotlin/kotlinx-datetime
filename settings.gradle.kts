pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlinx/maven")

        // !! infrastructure for builds as a Kotlin user project
        val optionalKotlinArtifactsRepo = providers.gradleProperty("kotlin_repo_url").orNull
        if (optionalKotlinArtifactsRepo != null) {
            maven(url = optionalKotlinArtifactsRepo)
            logger.info(
                "[ktDT-as-KUP] Registered '$optionalKotlinArtifactsRepo' as a plugin Maven repository"
            )
        }
    }
}

gradle.projectsLoaded {
    allprojects {
        repositories {
            mavenCentral()

            // !! infrastructure for builds as a Kotlin user project
            val optionalKotlinArtifactsRepo = providers.gradleProperty("kotlin_repo_url").orNull
            if (optionalKotlinArtifactsRepo != null) {
                maven(url = optionalKotlinArtifactsRepo)
                logger.info(
                    "[ktDT-as-KUP] Registered '$optionalKotlinArtifactsRepo' as a dependency Maven repository for '${path}'"
                )
            }
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // !! infrastructure for builds as a Kotlin user project
            val optionalOverridingKotlinVersion = providers.gradleProperty("kotlin_version").orNull
            if (optionalOverridingKotlinVersion != null) {
                version("kotlin", optionalOverridingKotlinVersion)
                logger.info(
                    "[ktDT-as-KUP] Overrode the Kotlin distribution version with $optionalOverridingKotlinVersion"
                )
            }
        }
    }
}

rootProject.name = "Kotlin-DateTime-library"

include(":core")
project(":core").name = "kotlinx-datetime"

include(":serialization")
project(":serialization").name = "kotlinx-datetime-serialization"

include(":benchmarks")
