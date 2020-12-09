import java.util.Locale

plugins {
    id("kotlin-multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

val JDK_8: String by project

kotlin {
    infra {
        target("linuxX64")
        target("mingwX64")
        target("macosX64")
        target("iosX64")
        target("iosArm64")
        target("iosArm32")
        target("watchosArm32")
        target("watchosArm64")
        target("watchosX86")
        target("tvosArm64")
        target("tvosX64")
    }

    jvm {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                jdkHome = JDK_8
            }
        }

    }

    js {
        nodejs {
        }
        compilations.all {
            kotlinOptions {
                sourceMap = true
                moduleKind = "umd"
                metaInfo = true
            }
        }
    }

    sourceSets.all {
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).toLowerCase(Locale.ROOT).takeIf { it != "main" }
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources "} ?: "resources"}")
        languageSettings.apply {
            useExperimentalAnnotation("kotlin.Experimental")
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["test"].kotlinOptions {
            freeCompilerArgs += listOf("-trw")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-datetime"))
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-common")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
                api("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        val jsMain by getting
        val jsTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-js")
                implementation(npm("@js-joda/timezone", "2.3.0"))
            }
        }

        val nativeMain by getting
        val nativeTest by getting
    }
}
