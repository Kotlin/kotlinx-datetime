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

val unpackedTzdbDir = project.layout.buildDirectory.dir("unpacked-tzdb")

val downloadLatestTzdb = tasks.register("downloadLatestTzdb") {
    outputs.dir(unpackedTzdbDir)
    doLast {
        mkdir(unpackedTzdbDir)
        val latestDataUrl = URL("https://data.iana.org/time-zones/tzdata-latest.tar.gz")
        val connection = latestDataUrl.openConnection()
        TarArchiveInputStream(GzipCompressorInputStream(connection.inputStream)).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                check(!entry.isDirectory()) { "The format of the tzdata archive changed" }
                val outputPath = unpackedTzdbDir.get().file(entry.name).asFile
                FileOutputStream(outputPath).use { fileOut ->
                    tar.copyTo(fileOut)
                }
                entry = tar.nextEntry
            }
        }
    }
}

tasks.register("checkTzdbVersion") {
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
