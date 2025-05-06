plugins {
    kotlin("jvm")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(11)) }
}

dependencies {
    api(project(":kotlinx-datetime"))
}
