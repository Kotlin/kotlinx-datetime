import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

tasks.named<KotlinCompile>("compileKotlin") {
    compilerOptions {
        freeCompilerArgs.set(listOf("-XXLanguage:+ContextParameters"))
    }
}

application {
    mainClass.set("TzdataCompilerKt")
}
