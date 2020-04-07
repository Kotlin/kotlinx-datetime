/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.CInteropProcess> {
    // dependsOn(":date-cpp-c-wrapper:assembleRelease")
    // dependsOn(":date-cpp-library:assembleRelease")
}

kotlin {
    infra {
        target("macosX64")
        target("iosX64")
        target("iosArm64")
        target("iosArm32")
        target("linuxX64")
        target("mingwX64")
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

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].cinterops {
            create("date") {
                val cinteropDir = "$projectDir/nativeMain/cinterop"
                val dateLibDir = "${project(":").projectDir}/thirdparty/date"
                headers("$cinteropDir/public/cdate.h")
                defFile("nativeMain/cinterop/date.def")
                // common options
                extraOpts("-Xsource-compiler-option", "-std=c++11")
                extraOpts("-Xsource-compiler-option", "-I$cinteropDir/public")
                extraOpts("-Xsource-compiler-option", "-include$cinteropDir/cpp/defines.hpp")
                // *nix support
                extraOpts("-Xcompile-source", "$dateLibDir/src/tz.cpp")
                extraOpts("-Xcompile-source", "$dateLibDir/src/ios.mm")
                extraOpts("-Xsource-compiler-option", "-I$dateLibDir/include")
                extraOpts("-Xcompile-source", "$cinteropDir/cpp/cdate.cpp")
                // iOS support
                extraOpts("-Xcompile-source", "$cinteropDir/cpp/apple.mm")
                // Windows support
                extraOpts("-Xcompile-source", "$cinteropDir/cpp/windows.cpp")
            }
        }
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
                implementation(npm("js-joda", "timezone", "2.2.0"))
            }
        }

        val nativeMain by getting {
        }

        val nativeTest by getting {
        }
    }
}

tasks {
    named("jvmTest", Test::class) {
        // maxHeapSize = "1024m"
//        executable = "$JDK_6/bin/java"
    }
}

task("downloadWindowsZonesMapping") {
    description = "Updates the mapping between Windows-specific and usual names for timezones"
    val output = "$projectDir/nativeMain/cinterop/public/windows_zones.hpp"
    outputs.file(output)
    doLast {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        // otherwise, parsing fails since it can't find the dtd
        documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        val builder = documentBuilderFactory.newDocumentBuilder()
        val url = URL("https://raw.githubusercontent.com/unicode-org/cldr/master/common/supplemental/windowsZones.xml")
        val xmlDoc = with(url.openConnection() as java.net.HttpURLConnection) {
            builder.parse(this.inputStream)
        }
        xmlDoc.documentElement.normalize()
        val mapZones = xmlDoc.getElementsByTagName("mapZone")
        val mapping = linkedMapOf<String, String>()
        for (i in 0 until mapZones.length) {
            val mapZone = mapZones.item(i)
            val windowsName = mapZone.attributes.getNamedItem("other").nodeValue
            val usualNames = mapZone.attributes.getNamedItem("type").nodeValue
            for (usualName in usualNames.split(' ')) {
                val oldWindowsName = mapping[usualName] // don't do it in `put` to preserve the order in the map
                if (oldWindowsName == null) {
                    mapping[usualName] = windowsName
                } else if (oldWindowsName != windowsName) {
                    throw Error("Ambiguous mapping: '$usualName' to '$oldWindowsName' and '$windowsName'")
                }
            }
        }
        File(output).printWriter().use { out ->
            out.println("""// generated with gradle task `$name`""")
            out.println("""#include <unordered_map>""")
            out.println("""#include <string>""")
            out.println("""static const std::unordered_map<std::string, std::string> standard_to_windows = {""")
            for ((usualName, windowsName) in mapping) {
                out.println("\t{ \"$usualName\", \"$windowsName\" },")
            }
            out.println("};")
            out.println("""static const std::unordered_map<std::string, std::string> windows_to_standard = {""")
            val reverseMap = mutableMapOf<String, String>()
            for ((usualName, windowsName) in mapping) {
                if (reverseMap[windowsName] == null) {
                    reverseMap[windowsName] = usualName
                }
            }
            for ((windowsName, usualName) in reverseMap) {
                out.println("\t{ \"$windowsName\", \"$usualName\" },")
            }
            out.println("};")
        }
    }
}
