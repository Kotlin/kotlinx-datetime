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
