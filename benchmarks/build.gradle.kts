/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

plugins {
    id("kotlin")
    id("me.champeau.jmh")
}


val mainJavaToolchainVersion by ext(project.property("java.mainToolchainVersion"))
val modularJavaToolchainVersion by ext(project.property("java.modularToolchainVersion"))

sourceSets {
    dependencies {
        implementation(project(":kotlinx-datetime"))
        implementation("org.openjdk.jmh:jmh-core:1.35")
    }
}

// Publish benchmarks to the root for the easier 'java -jar benchmarks.jar`
tasks.named<Jar>("jmhJar") {
    archiveBaseName = "benchmarks"
    archiveClassifier = null
    archiveVersion.convention(null as String?)
    archiveVersion = null
    destinationDirectory = file("$rootDir")
}

repositories {
    mavenCentral()
}
