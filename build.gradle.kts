buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0-rc")
    }
}

plugins {
    id("kotlinx.team.infra") version "0.3.0-dev-64"
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

fun jdkPath(version: Int): String {
    fun envOrProperty(name: String): String? = System.getenv(name) ?: findProperty(name) as String?

    return envOrProperty("JDK_$version") ?:
            version.takeIf { it < 9 }?.let { envOrProperty("JDK_1$version") } ?:
            error("Specify path to JDK $version in JDK_$version environment variable or Gradle property")
}
//val JDK_6 by ext(jdkPath(6))
val JDK_8 by ext(jdkPath(8))

allprojects {
    repositories {
        mavenCentral()
    }
}

