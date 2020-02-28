group = "org.jetbrains.kotlinx"
version = "0.0.1-SNAPSHOT"

plugins {
    id("cpp-library")
}

library {
    linkage.set(setOf(Linkage.STATIC))
    dependencies {
        implementation(project(":date-cpp-library"))
    }
}

println("$buildDir")