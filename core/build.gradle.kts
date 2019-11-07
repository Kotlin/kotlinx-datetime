/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

plugins {
    id("kotlin-multiplatform")
    `maven-publish`
}

base {
    archivesBaseName = "kotlinx-datetime" // doesn't work
}

//val JDK_6: String by project
val JDK_8: String by project

//publishing {
//    repositories {
//        maven(url = "${rootProject.buildDir}/maven") {
//            this.name = "buildLocal2"
//        }
//    }
//}

kotlin {
    infra {
//        target("macosX64")
//        target("linuxX64")
//        target("mingwX64")
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

    /*
    jvm("jvm6") {
        this.withJava()
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 6)
        }
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.6"
                jdkHome = JDK_6
            }
        }
    }
     */

    js {
        nodejs {
//            testTask { }
        }
        compilations.all {
            kotlinOptions {
                sourceMap = true
                moduleKind = "umd"
                metaInfo = true
            }
        }
//        compilations["main"].apply {
//            kotlinOptions {
//                outputFile = "kotlinx-datetime-tmp.js"
//            }
//        }
    }

    sourceSets.all {
        kotlin.setSrcDirs(listOf("$name/src"))
        resources.setSrcDirs(listOf("$name/resources"))
        languageSettings.apply {
            //            progressiveMode = true
            useExperimentalAnnotation("kotlin.Experimental")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-common")
                api("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }

        /*
        val jvm6Main by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib")
                api("org.threeten:threetenbp:1.4.0")

            }
        }
        val jvm6Test by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }
        */

        val jvmMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }
        val jvmTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }

        val jsMain by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-js")
                implementation(npm("js-joda", "core",  "1.12.0"))
            }
        }

        val jsTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-js")
            }
        }
    }
}

tasks {
    named("jvmTest", Test::class) {
        // maxHeapSize = "1024m"
//        executable = "$JDK_6/bin/java"
    }
}