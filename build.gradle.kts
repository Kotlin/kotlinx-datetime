buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")
    }
}

plugins {
    id("kotlinx.team.infra") version "0.1.0-dev-51"
}

project(":kotlinx-datetime") {
    pluginManager.apply("kotlin-multiplatform")
//    pluginManager.apply("maven-publish")
}

infra {
    teamcity {
        bintrayUser = "%env.BINTRAY_USER%"
        bintrayToken = "%env.BINTRAY_API_KEY%"
    }
    publishing {
        include(":kotlinx-datetime")

        bintray {
            organization = "kotlin"
            repository = "kotlinx"
            library = "kotlinx.datetime"
            username = findProperty("bintrayUser") as String?
            password = findProperty("bintrayApiKey") as String?
        }

        bintrayDev {
            organization = "kotlin"
            repository = "kotlin-dev"
            library = "kotlinx.datetime"
            username = findProperty("bintrayUser") as String?
            password = findProperty("bintrayApiKey") as String?
        }
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
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

