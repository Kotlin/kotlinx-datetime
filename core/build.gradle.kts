import kotlinx.team.infra.mavenPublicationsPom
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.Family
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
    id("org.jetbrains.kotlinx.kover")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

mavenPublicationsPom {
    description.set("Kotlin Datetime Library")
}

val mainJavaToolchainVersion: String by project
val modularJavaToolchainVersion: String by project
val serializationVersion: String by project

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(mainJavaToolchainVersion)) }
}

kotlin {
    explicitApi()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("commonKotlin") {
                group("commonJs") {
                    withJs()
                    withWasmJs()
                }
                withWasmWasi()
                group("native") {
                    group("tzfile") {
                        group("tzdbOnFilesystem") {
                            group("linux") {
                                withLinux()
                            }
                            group("darwin") {
                                group("darwinDevices") {
                                    withMacosX64()
                                    withMacosArm64()
                                    withWatchosX64()
                                    withWatchosArm32()
                                    withWatchosArm64()
                                    withTvosX64()
                                    withTvosArm64()
                                    withIosArm64()
                                    withWatchosDeviceArm64()
                                }
                                group("darwinSimulator") {
                                    withIosSimulatorArm64()
                                    withIosX64()
                                    withWatchosSimulatorArm64()
                                    withTvosSimulatorArm64()
                                }
                            }
                        }
                        group("androidNative") {
                            withAndroidNative()
                        }
                    }
                    group("windows") {
                        withMingw()
                    }
                }
            }
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

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
        }
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    sourceMap = true
                    moduleKind = JsModuleKind.MODULE_UMD
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Tiers are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
    // Tier 1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()
    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()
    // Deprecated
    @Suppress("DEPRECATION") linuxArm32Hfp()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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
        if (konanTarget.family == Family.MINGW) {
            compilations["test"].cinterops {
                create("modern_api") {
                    defFile("$projectDir/windows/test_cinterop/modern_api.def")
                    headers("$projectDir/windows/test_cinterop/modern_api.h")
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }

        commonTest {
            dependencies {
                api("org.jetbrains.kotlin:kotlin-test")
            }
        }

        val commonJsMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation(npm("@js-joda/core", "3.2.0"))
            }
        }

        val commonJsTest by getting {
            dependencies {
                implementation(npm("@js-joda/timezone", "2.3.0"))
            }
        }

        val commonKotlinMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }

        val wasmWasiTest by getting {
            dependencies {
                runtimeOnly(project(":kotlinx-datetime-zoneinfo"))
            }
        }
    }

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
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
        from(compileJavaModuleInfo.map { it.destinationDirectory }) {
            into("META-INF/versions/9/")
        }
    }

    // Workaround for https://youtrack.jetbrains.com/issue/KT-58303:
    // the `clean` task can't delete the expanded.lock file on Windows as it's still held by Gradle, failing the build
    val clean by existing(Delete::class) {
        setDelete(fileTree(layout.buildDirectory) {
            exclude("tmp/.cache/expanded/expanded.lock")
        })
    }

    // workaround from KT-61313
    withType<Sign>().configureEach {
        val pubName = name.removePrefix("sign").removeSuffix("Publication")

        // These tasks only exist for native targets, hence findByName() to avoid trying to find them for other targets

        // Task ':linkDebugTest<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
        findByName("linkDebugTest$pubName")?.let {
            mustRunAfter(it)
        }
        // Task ':compileTestKotlin<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
        findByName("compileTestKotlin$pubName")?.let {
            mustRunAfter(it)
        }
    }
}

val downloadWindowsZonesMapping by tasks.registering {
    description = "Updates the mapping between Windows-specific and usual names for timezones"
    val output = "$projectDir/windows/src/internal/WindowsZoneNames.kt"
    outputs.file(output)
    doLast {
        val initialFileContents = try { File(output).readBytes() } catch(_: Throwable) { ByteArray(0) }
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
        mapping["UTC"] = "UTC"
        for (i in 0 until mapZones.length) {
            val mapZone = mapZones.item(i)
            val windowsName = mapZone.attributes.getNamedItem("other").nodeValue
            val usualNames = mapZone.attributes.getNamedItem("type").nodeValue
            for (usualName in usualNames.split(' ')) {
                if (usualName == "") continue
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
            out.println("""package kotlinx.datetime.internal""")
            out.println("""internal val standardToWindows: Map<String, String> = mutableMapOf(""")
            for ((usualName, windowsName) in sortedMapping) {
                out.println("  \"$usualName\" to \"$windowsName\",")
            }
            out.println(")")
            out.println("""internal val windowsToStandard: Map<String, String> = mutableMapOf(""")
            val reverseMap = sortedMapOf<String, String>()
            for ((usualName, windowsName) in mapping) {
                if (reverseMap[windowsName] == null) {
                    reverseMap[windowsName] = usualName
                }
            }
            for ((windowsName, usualName) in reverseMap) {
                out.println("  \"$windowsName\" to \"$usualName\",")
            }
            out.println(")")
        }
        val newFileContents = bos.toByteArray()
        if (!(initialFileContents contentEquals newFileContents)) {
            File(output).writeBytes(newFileContents)
            throw GradleException("The mappings between Windows and IANA timezone names changed. " +
                "The new mappings were written to the filesystem.")
        }
    }
}

dokka {
    pluginsConfiguration.html {
        templatesDir.set(projectDir.resolve("dokka-templates"))
    }

    dokkaPublications.html {
        failOnWarning.set(true)
        // Enum members and undocumented toString()
        suppressInheritedMembers.set(true)
    }

    dokkaSourceSets.configureEach {
        val platform = name.dropLast(4)
        samples.from("$platform/test")

        // reportUndocumented.set(true) // much noisy output about `hashCode` and serializer encoders, decoders etc
        skipDeprecated.set(true)
        // hide the `internal` package, which, on JS, has public members generated by Dukat that would get mentioned
        perPackageOption {
            matchingRegex.set(".*\\.internal(\\..*)?")
            suppress.set(true)
        }
        sourceLink {
            localDirectory.set(rootDir)
            remoteUrl("https://github.com/kotlin/kotlinx-datetime/tree/latest-release")
            remoteLineSuffix.set("#L")
        }
    }
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

dependencies {
    // make samples runnable via Kotlin playground
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-playground-samples-plugin")

    dokka(project)
}
