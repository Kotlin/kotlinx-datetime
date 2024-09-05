import kotlinx.team.infra.mavenPublicationsPom
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.Family
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    with(libs.plugins) {
        alias(kotlin.multiplatform)
        alias(kotlin.plugin.serialization)
        alias(kover)
        alias(dokka)
    }
    `maven-publish`
}

val mainJavaToolchainVersion: JavaLanguageVersion by project
val modularJavaToolchainVersion: JavaLanguageVersion by project

mavenPublicationsPom {
    description = "Kotlin Datetime Library"
}

java {
    toolchain.languageVersion = mainJavaToolchainVersion
}

kotlin {
    // all-targets configuration

    explicitApi()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xdont-warn-on-error-suppression")
    }

    // target-specific and compilation-specific configuration

    jvm {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, java.toolchain.languageVersion.get().asInt())
        }
    }

    // tiers of K/Native targets are in accordance with <https://kotlinlang.org/docs/native-target-support.html>
    infra {
        common("tzfile") {
            common("tzdbOnFilesystem") {
                common("linux") {
                    // Tier 2
                    target("linuxX64")
                    target("linuxArm64")
                    // Deprecated
                    target("linuxArm32Hfp")
                }
                common("darwin") {
                    common("darwinDevices") {
                        // Tier 1
                        target("macosX64")
                        target("macosArm64")
                        // Tier 2
                        target("watchosX64")
                        target("watchosArm32")
                        target("watchosArm64")
                        target("tvosX64")
                        target("tvosArm64")
                        target("iosArm64")
                        // Tier 3
                        target("watchosDeviceArm64")
                    }
                    common("darwinSimulator") {
                        // Tier 1
                        target("iosSimulatorArm64")
                        target("iosX64")
                        // Tier 2
                        target("watchosSimulatorArm64")
                        target("tvosSimulatorArm64")
                    }
                }
            }
            common("androidNative") {
                // Tier 3
                target("androidNativeArm32")
                target("androidNativeArm64")
                target("androidNativeX86")
                target("androidNativeX64")
            }
        }
        common("windows") {
            // Tier 3
            target("mingwX64")
        }
    }

    targets.withType<KotlinNativeTarget> {
        compilations["test"].compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.add("-trw")
        }
        if (konanTarget.family == Family.MINGW) {
            compilations["test"].cinterops {
                create("modern_api") {
                    defFile("$projectDir/windows/test_cinterop/modern_api.def")
                    headers("$projectDir/windows/test_cinterop/modern_api.h")
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

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap = true
            moduleKind = JsModuleKind.MODULE_UMD
        }
    }

    // configuration of source sets

    sourceSets.all {
        val suffixIndex = name.indexOfLast { it.isUpperCase() }
        val targetName = name.substring(0, suffixIndex)
        val suffix = name.substring(suffixIndex).lowercase().takeIf { it != "main" }
        kotlin.srcDir("$targetName/${suffix ?: "src"}")
        resources.srcDir("$targetName/${suffix?.let { it + "Resources" } ?: "resources"}")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                compileOnly(libs.kotlinx.serialization.core)
            }
        }
        val commonTest by getting {
            dependencies {
                api(libs.kotlin.test)
            }
        }

        val jvmMain by getting
        val jvmTest by getting

        val nativeMain by getting {
            dependencies {
                api(libs.kotlinx.serialization.core)
            }
        }
        val nativeTest by getting

        val commonJsMain by creating {
            dependsOn(commonMain)
            dependencies {
                api(libs.kotlinx.serialization.core)
                implementation(npm("@js-joda/core", libs.versions.jsJoda.core.get()))
            }
        }
        val commonJsTest by creating {
            dependsOn(commonTest)
            dependencies {
                implementation(npm("@js-joda/timezone", libs.versions.jsJoda.timezone.get()))
            }
        }

        val wasmJsMain by getting {
            dependsOn(commonJsMain)
        }
        val wasmJsTest by getting {
            dependsOn(commonJsTest)
        }

        val jsMain by getting {
            dependsOn(commonJsMain)
        }
        val jsTest by getting {
            dependsOn(commonJsTest)
        }
    }
}

val compileJavaModuleInfo by tasks.registering(JavaCompile::class) {
    val moduleName = "kotlinx.datetime" // this module's name
    val compileKotlinJvm by tasks.getting(KotlinCompile::class)
    val sourceDir = file("jvm/java9/")
    val targetDir = compileKotlinJvm.destinationDirectory.map { it.dir("../java9/") }

    // Always compile Kotlin classes first before compiling the module descriptor.
    dependsOn(compileKotlinJvm)

    // Add the module-info.java source file to the Kotlin compilation.
    // The Kotlin compiler currently won't compile a module descriptor itself,
    // but it will parse and check module dependencies.
    // (Note that module checking only works on JDK 9+ because built-in JDK base modules are not available in earlier versions.)
    val javaVersion = compileKotlinJvm.kotlinJavaToolchain.javaVersion.getOrNull()
    if (javaVersion?.isJava9Compatible == true) {
        logger.info("module-info.java checking is enabled; $compileKotlinJvm is compiled using Java $javaVersion")
        compileKotlinJvm.source(sourceDir)
    } else {
        logger.info("module-info.java checking is disabled")
    }

    // Add the module-info.java source file to this task.
    source(sourceDir)

    // Configure output destinations of this task.
    outputs.dir(targetDir)
    destinationDirectory = targetDir

    // Use a Java 11 compiler for the module descriptor compilation.
    javaCompiler = project.javaToolchains.compilerFor { languageVersion = modularJavaToolchainVersion }

    // Configure the minimum JVM compatibility options and Java release version that support JPMS.
    sourceCompatibility = JavaVersion.VERSION_1_9.toString()
    targetCompatibility = JavaVersion.VERSION_1_9.toString()
    options.release = 9

    // Use the same classpath as during the Kotlin compilation.
    classpath = compileKotlinJvm.libraries

    // Ensure that the classpath is interpreted as a module path when compiling the module descriptor.
    modularity.inferModulePath = true

    // Patch compiled Kotlin classes into the module descriptor compilation so that exporting packages works correctly.
    options.compilerArgs.addAll(listOf("--patch-module", "$moduleName=${compileKotlinJvm.destinationDirectory.get()}"))

    // Ignore warnings about using `requires transitive` on automatic modules.
    // (Not needed when compiling with recent JDKs, e.g., 17.)
    options.compilerArgs.add("-Xlint:-requires-transitive-automatic")
}

// Configure the JAR task so that it includes the compiled module descriptor.
val jvmJar by tasks.existing(Jar::class) {
    manifest.attributes("Multi-Release" to true)
    from(compileJavaModuleInfo.map { it.destinationDirectory }) {
        into("META-INF/versions/9/")
    }
}

tasks.withType<AbstractDokkaLeafTask>().configureEach {
    val dokkaBasePluginConfiguration = """{ "templatesDir" : "${projectDir.toString().replace('\\', '/')}/dokka-templates" }"""
    pluginsMapConfiguration = mapOf("org.jetbrains.dokka.base.DokkaBase" to dokkaBasePluginConfiguration)

    failOnWarning = true

    dokkaSourceSets.configureEach {
        kotlin.sourceSets.commonTest.get().kotlin.srcDirs.forEach {
            samples.from(it)
        }

        skipDeprecated = true
        // hide enum members and undocumented 'toString's
        suppressInheritedMembers = true
        // hide the `internal` package, which, on JS, has public members generated by Dukat that would otherwise get mentioned
        perPackageOption {
            matchingRegex = ".*\\.internal(\\..*)?"
            suppress = true
        }

        sourceLink {
            localDirectory = rootDir
            remoteUrl = URL("https://github.com/kotlin/kotlinx-datetime/tree/latest-release")
            remoteLineSuffix = "#L"
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
            throw GradleException(
                "The mappings between Windows and IANA timezone names changed. " +
                "The new mappings were written to the filesystem."
            )
        }
    }
}

// workaround from KT-61313
tasks.withType<Sign>().configureEach {
    val platform = name.removePrefix("sign").removeSuffix("Publication")

    // these tasks only exist for native targets,
    // hence 'findByName' to avoid trying to find them for other targets

    // "Task `:compileTestKotlin<platform>` uses [...] output of task `:sign<platform>Publication`
    //  without declaring an explicit or implicit dependency [...]"
    tasks.findByName("compileTestKotlin$platform")?.let {
        mustRunAfter(it)
    }
    // "Task `:linkDebugTest<platform>` uses [...] output of task `:sign<platform>Publication`
    //  without declaring an explicit or implicit dependency [...]"
    tasks.findByName("linkDebugTest$platform")?.let {
        mustRunAfter(it)
    }
}
