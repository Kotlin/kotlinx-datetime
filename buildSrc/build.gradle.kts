import java.util.Properties

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

val kotlinVersion = file("../gradle.properties").inputStream().use {
    Properties().apply { load(it) }
}.getProperty("kotlinVersion") ?: throw IllegalStateException("Property 'kotlinVersion' must be defined in ../gradle.properties")

dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
}
