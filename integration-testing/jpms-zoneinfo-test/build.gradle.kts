plugins {
    kotlin("jvm")
    application
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(11)) }
}

dependencies {
    implementation(project(":kotlinx-datetime"))
    implementation(project(":kotlinx-datetime-zoneinfo"))
}

application {
    mainClass.set("my.jpms.test.QueryTimeZone")
    mainModule.set("my.jpms.test")
}

tasks.named("check") {
    dependsOn(tasks.named("run"))
}
