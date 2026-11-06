import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.FileOutputStream

buildscript {
    dependencies {
        classpath("org.apache.commons:commons-compress:1.28.0")
    }
}

plugins {
    kotlin("jvm")
    application
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(project.property("mainJavaToolchainVersion") as String)) }
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src")
            dependencies {
                api(project(":kotlinx-datetime"))
            }
        }

        test {
            kotlin.srcDir("test")
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

// Tzdata files that need to be passed to the compiler
val zicTzdbFiles = setOf(
    "africa", "antarctica", "asia", "australasia", "europe", "northamerica", "southamerica",
    "etcetera",
    "backward",
)

// Files in the `tzdata.tar.gz` distribution, but unused by the tzdata compiler
val otherTzdbFiles = setOf(
    /* Scripts */
    "Makefile",
    "checklinks.awk", "checknow.awk", "checktab.awk", "leapseconds.awk", "ziguard.awk", "zishrink.awk",
    /* Documentation */
    "CONTRIBUTING", "LICENSE", "NEWS", "README", "SECURITY",
    "calendars", "theory.html",
    /* Other data */
    "factory", // legacy "Factory" timezone, which we should not ship
    "backzone", // time zones that don't differ from other zones after 1970; replaces `backward`
    "iso3166.tab", // mapping of ISO country codes to English names
    "leapseconds", // compiled `leap-seconds.list`
    "leap-seconds.list", // list of leap seconds
    "version", // single line with the timezone database version (e.g., "2026e")
    "zone1970.tab", // geographic coordinate -> timezone mapping
    "zonenow.tab", // geographic coordinate -> timezone mapping, but only covering the modern political state
    "zone.tab", // outdated version of `zone1970.tab` for backward compatibility
)

val unpackedTzdbDir = project.layout.buildDirectory.dir("unpacked-tzdb")
val compiledTzdbDir = project.layout.buildDirectory.dir("compiled-tzdb")

val downloadLatestTzdb = tasks.register("downloadLatestTzdb") {
    description = "Download and unpack the latest raw timezone database files"
    outputs.dir(unpackedTzdbDir)
    outputs.upToDateWhen { false } // never up-to-date
    doLast {
        mkdir(unpackedTzdbDir)
        val latestDataUrl = URL("https://data.iana.org/time-zones/tzdata-latest.tar.gz")
        val connection = latestDataUrl.openConnection()
        TarArchiveInputStream(GzipCompressorInputStream(connection.inputStream)).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                check(entry.name in zicTzdbFiles || entry.name in otherTzdbFiles) {
                    "The format of the tzdata archive changed: unknown file $entry"
                }
                check(!entry.isDirectory()) {
                    "The format of the tzdata archive changed: $entry is a directory"
                }
                val outputPath = unpackedTzdbDir.get().file(entry.name).asFile
                FileOutputStream(outputPath).use { fileOut ->
                    tar.copyTo(fileOut)
                }
                entry = tar.nextEntry
            }
        }
    }
}

val compileLatestTzdb = tasks.register<JavaExec>("compileLatestTzdb") {
    description = "Build the up-to-date timezone database in the tzfile format"
    outputs.dir(compiledTzdbDir)
    dependsOn(downloadLatestTzdb)
    val inputDir = unpackedTzdbDir.get()
    val outputDir = compiledTzdbDir.get()
    val inputs = zicTzdbFiles.map { inputDir.file(it).asFile.toString() }
    mainClass.set("TzdataCompilerKt")
    args = listOf("zic", "-d", outputDir.asFile.toString()) + inputs
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register("checkTzdbVersion") {
    description = "Verify that the tzdb version in gradle.properties is up-to-date"
    dependsOn(downloadLatestTzdb)
    doLast {
        val current = project.property("tzdbVersion")
        val latest = unpackedTzdbDir.get().file("version").asFile.readLines().single()
        check(current == latest) { "Timezone database $latest is available. The current version $current is outdated." }
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        freeCompilerArgs.set(listOf("-XXLanguage:+ContextParameters"))
    }
}

application {
    mainClass.set("TzdataCompilerKt")
}

configurations {
    consumable("fullTzfileTzdb")
}

artifacts {
    add("fullTzfileTzdb", compileLatestTzdb)
}

