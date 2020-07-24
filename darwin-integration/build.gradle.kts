plugins {
    id("kotlin-multiplatform")
}

kotlin {
    infra {
        target("macosX64")
        target("iosX64")
        target("iosArm64")
        target("iosArm32")
        target("watchosArm32")
        target("watchosArm64")
        target("watchosX86")
        target("tvosArm64")
        target("tvosX64")
    }

    if (System.getProperty("idea.active")?.toBoolean() == true) {
        // workaround: add jvm target for non-Mac hosts during IDEA import to avoid 'no targets' error
        jvm()
    }

    sourceSets.all {
        kotlin.setSrcDirs(listOf("$name/src"))
    }

    sourceSets {
        if (any { it.name == "nativeMain" }) {
            val nativeMain by getting {
                dependencies {
                    implementation(project(":kotlinx-datetime"))
                }
            }
        }
    }

}
