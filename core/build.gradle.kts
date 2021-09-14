import kotlinx.team.infra.mavenPublicationsPom
import java.net.URL
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

mavenPublicationsPom {
    description.set("Kotlin Datetime Library")
}

base {
    archivesBaseName = "kotlinx-datetime" // doesn't work
}

//val JDK_6: String by project
val JDK_8: String by project
val serializationVersion: String by project

kotlin {
    explicitApi()

    infra {
        target("linuxX64")
        target("mingwX64")

        common("darwin") {
            target("macosX64")
            target("macosArm64")
            target("iosX64")
            target("iosArm64")
            target("iosArm32")
            target("iosSimulatorArm64")
            target("watchosArm32")
            target("watchosArm64")
            target("watchosX86")
            target("watchosX64")
            target("watchosSimulatorArm64")
            target("tvosArm64")
            target("tvosX64")
            target("tvosSimulatorArm64")
        }
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
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).toLowerCase(Locale.ROOT).takeIf { it != "main" }
//        println("SOURCE_SET: $name")
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources "} ?: "resources"}")
        languageSettings.apply {
            //            progressiveMode = true
            useExperimentalAnnotation("kotlin.Experimental")
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["test"].kotlinOptions {
            freeCompilerArgs += listOf("-trw")
        }
        if (konanTarget.family.isAppleFamily) {
            return@withType
        }
        compilations["main"].cinterops {
            create("date") {
                val cinteropDir = "$projectDir/native/cinterop"
                val dateLibDir = "${project(":").projectDir}/thirdparty/date"
                headers("$cinteropDir/public/cdate.h")
                defFile("native/cinterop/date.def")
                extraOpts("-Xsource-compiler-option", "-I$cinteropDir/public")
                extraOpts("-Xsource-compiler-option", "-DONLY_C_LOCALE=1")
                when {
                    konanTarget.family == org.jetbrains.kotlin.konan.target.Family.LINUX -> {
                        // needed for the date library so that it does not try to download the timezone database
                        extraOpts("-Xsource-compiler-option", "-DUSE_OS_TZDB=1")
                        /* using a more modern C++ version causes the date library to use features that are not
                    * present in the currently outdated GCC root shipped with Kotlin/Native for Linux. */
                        extraOpts("-Xsource-compiler-option", "-std=c++11")
                        // the date library and its headers
                        extraOpts("-Xcompile-source", "$dateLibDir/src/tz.cpp")
                        extraOpts("-Xsource-compiler-option", "-I$dateLibDir/include")
                        // the main source for the platform bindings.
                        extraOpts("-Xcompile-source", "$cinteropDir/cpp/cdate.cpp")
                    }
                    konanTarget.family == org.jetbrains.kotlin.konan.target.Family.MINGW -> {
                        // needed to be able to use std::shared_mutex to implement caching.
                        extraOpts("-Xsource-compiler-option", "-std=c++17")
                        // the date library headers, needed for some pure calculations.
                        extraOpts("-Xsource-compiler-option", "-I$dateLibDir/include")
                        // the main source for the platform bindings.
                        extraOpts("-Xcompile-source", "$cinteropDir/cpp/windows.cpp")
                    }
                    else -> {
                        throw IllegalArgumentException("Unknown native target ${this@withType}")
                    }
                }
            }
        }
        compilations["main"].defaultSourceSet {
            kotlin.srcDir("native/cinterop_actuals")
        }
    }


    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-stdlib-common")
                compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
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
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation(npm("@js-joda/core",  "3.2.0"))
            }
        }

        val jsTest by getting {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test-js")
                implementation(npm("@js-joda/timezone", "2.3.0"))
            }
        }

        val nativeMain by getting {
            dependsOn(commonMain.get())
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }

        val nativeTest by getting {
        }

        val darwinMain by getting {
        }

        val darwinTest by getting {
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
            out.println("""static const std::unordered_map<std::string, size_t> zone_ids = {""")
            var i = 0
            for ((usualName, windowsName) in mapping) {
                out.println("\t{ \"$usualName\", $i },")
                ++i
            }
            out.println("};")
        }
    }
}
