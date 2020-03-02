group = "org.jetbrains.kotlinx"
version = "0.0.1-SNAPSHOT"

plugins {
    id("cpp-library")
}

extensions.configure<CppLibrary> {
    linkage.set(setOf(Linkage.STATIC))
    source.from(file("date/src"))
    publicHeaders.from(file("date/include"))
}

tasks.withType(CppCompile::class.java).configureEach {
    macros.put("AUTO_DOWNLOAD", "0")
    macros.put("HAS_REMOTE_API", "0")
    source("date/src/ios.mm")
}
