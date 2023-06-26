import kotlinx.team.infra.mavenPublicationsPom
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
}

mavenPublicationsPom {
    description.set("Kotlin Datetime Library")
}

base {
    archivesBaseName = "kotlinx-datetime" // doesn't work
}

val mainJavaToolchainVersion: String by project
val modularJavaToolchainVersion: String by project
val serializationVersion: String by project

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(mainJavaToolchainVersion)) }
    with(javaToolchains.launcherFor(toolchain).get().metadata) {
        logger.info("Using JDK $languageVersion toolchain installed in $installationPath")
    }
}

kotlin {
    explicitApi()

    infra {
        // Tiers are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
        // Tier 1
        target("linuxX64")
        // Tier 2
        target("linuxArm64")
        // Tier 3
        target("mingwX64")
        // the following targets are not supported by kotlinx.serialization:
        /*
        target("androidNativeArm32")
        target("androidNativeArm64")
        target("androidNativeX86")
        target("androidNativeX64")
         */
        // Darwin targets are listed separately
        common("darwin") {
            // Tier 1
            target("macosX64")
            target("macosArm64")
            target("iosSimulatorArm64")
            target("iosX64")
            // Tier 2
            target("watchosSimulatorArm64")
            target("watchosX64")
            target("watchosArm32")
            target("watchosArm64")
            target("tvosSimulatorArm64")
            target("tvosX64")
            target("tvosArm64")
            target("iosArm64")
            // Tier 3
            // target("watchosDeviceArm64") // not supported by kotlinx.serialization
        }
    }

    jvm {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        }
        compilations.all {
            // Set compilation options for JVM target here
        }

    }

    js(IR) {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
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
        val suffix = name.substring(suffixIndex).lowercase().takeIf { it != "main" }
//        println("SOURCE_SET: $name")
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources" } ?: "resources"}")
        languageSettings {
            //            progressiveMode = true
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
                api("org.jetbrains.kotlin:kotlin-test")
            }
        }

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
                implementation(npm("@js-joda/core", "3.2.0"))
            }
        }

        val jsTest by getting {
            dependencies {
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
    val jvmTest by existing(Test::class) {
        // maxHeapSize = "1024m"
    }

    val compileJavaModuleInfo by registering(JavaCompile::class) {
        val moduleName = "kotlinx.datetime" // this module's name
        val compileKotlinJvm by getting(KotlinCompile::class)
        val sourceDir = file("jvm/java9/")
        val targetDir = compileKotlinJvm.destinationDirectory.map { it.dir("../java9/") }

        // Use a Java 11 compiler for the module info.
        javaCompiler.set(project.javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(modularJavaToolchainVersion)) })

        // Always compile kotlin classes before the module descriptor.
        dependsOn(compileKotlinJvm)

        // Add the module-info source file.
        source(sourceDir)

        // Also add the module-info.java source file to the Kotlin compile task.
        // The Kotlin compiler will parse and check module dependencies,
        // but it currently won't compile to a module-info.class file.
        // Note that module checking only works on JDK 9+,
        // because the JDK built-in base modules are not available in earlier versions.
        val javaVersion = compileKotlinJvm.kotlinJavaToolchain.javaVersion.getOrNull()
        if (javaVersion?.isJava9Compatible == true) {
            logger.info("Module-info checking is enabled; $compileKotlinJvm is compiled using Java $javaVersion")
            compileKotlinJvm.source(sourceDir)
        } else {
            logger.info("Module-info checking is disabled")
        }

        // Set the task outputs and destination dir
        outputs.dir(targetDir)
        destinationDirectory.set(targetDir)

        // Configure JVM compatibility
        sourceCompatibility = JavaVersion.VERSION_1_9.toString()
        targetCompatibility = JavaVersion.VERSION_1_9.toString()

        // Set the Java release version.
        options.release.set(9)

        // Ignore warnings about using 'requires transitive' on automatic modules.
        // not needed when compiling with recent JDKs, e.g. 17
        options.compilerArgs.add("-Xlint:-requires-transitive-automatic")

        // Patch the compileKotlinJvm output classes into the compilation so exporting packages works correctly.
        options.compilerArgs.addAll(listOf("--patch-module", "$moduleName=${compileKotlinJvm.destinationDirectory.get()}"))

        // Use the classpath of the compileKotlinJvm task.
        // Also ensure that the module path is used instead of classpath.
        classpath = compileKotlinJvm.libraries
        modularity.inferModulePath.set(true)
    }

    // Configure the JAR task so that it will include the compiled module-info class file.
    val jvmJar by existing(Jar::class) {
        manifest {
            attributes("Multi-Release" to true)
        }
        from(compileJavaModuleInfo) {
            into("META-INF/versions/9/")
        }
    }

    // Workaround for https://youtrack.jetbrains.com/issue/KT-58303:
    // the `clean` task can't delete the expanded.lock file on Windows as it's still held by Gradle, failing the build
    val clean by existing(Delete::class) {
        setDelete(fileTree(buildDir) {
            exclude("tmp/.cache/expanded/expanded.lock")
        })
    }
}

val downloadWindowsZonesMapping by tasks.registering {
    description = "Updates the mapping between Windows-specific and usual names for timezones"
    val output = "$projectDir/native/cinterop/public/windows_zones.hpp"
    val initialFileContents = File(output).readBytes()
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
        val sortedMapping = mapping.toSortedMap()
        val bos = ByteArrayOutputStream()
        PrintWriter(bos).use { out ->
            out.println("""// generated with gradle task `$name`""")
            out.println("""#include <unordered_map>""")
            out.println("""#include <string>""")
            out.println("""static const std::unordered_map<std::string, std::string> standard_to_windows = {""")
            for ((usualName, windowsName) in sortedMapping) {
                out.println("\t{ \"$usualName\", \"$windowsName\" },")
            }
            out.println("};")
            out.println("""static const std::unordered_map<std::string, std::string> windows_to_standard = {""")
            val reverseMap = sortedMapOf<String, String>()
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
            for ((usualName, windowsName) in sortedMapping) {
                out.println("\t{ \"$usualName\", $i },")
                ++i
            }
            out.println("};")
        }
        val newFileContents = bos.toByteArray()
        if (!(initialFileContents contentEquals newFileContents)) {
            File(output).writeBytes(newFileContents)
            throw GradleException("The mappings between Windows and IANA timezone names changed. " +
                "The new mappings were written to the filesystem.")
        }
    }
}

tasks.withType<AbstractDokkaLeafTask>().configureEach {
    pluginsMapConfiguration.set(mapOf("org.jetbrains.dokka.base.DokkaBase" to """{ "templatesDir" : "${projectDir.toString().replace('\\', '/')}/dokka-templates" }"""))

    dokkaSourceSets.configureEach {
        // reportUndocumented.set(true) // much noisy output about `hashCode` and serializer encoders, decoders etc
        skipDeprecated.set(true)
        // hide the `internal` package, which, on JS, has public members generated by Dukat that would get mentioned
        perPackageOption {
            matchingRegex.set(".*\\.internal\\..*")
            suppress.set(true)
        }
    }
}
