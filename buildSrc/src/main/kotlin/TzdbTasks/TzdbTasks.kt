/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package TzdbTasks

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

abstract class InstallTimeTzdb : Exec() {
    @get:OutputDirectory
    @get:Optional
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    init {
        outputDirectory.convention(project.layout.buildDirectory.dir("time-tzdb"))
        executable = "npm"
    }

    override fun exec() {
        val installVersion = version.orNull?.let { "@$it" } ?: ""
        this.setArgs(listOf("install", "--prefix", outputDirectory.get().asFile, "@tubular/time-tzdb$installVersion"))
        super.exec()
    }
}

enum class ConvertType {
    LARGE, SMALL
}

abstract class TzdbDownloadAndCompile : Exec() {
    @get:Input
    @get:Optional
    abstract val ianaVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val convertType: Property<ConvertType>

    @get:OutputDirectory
    @get:Optional
    abstract val outputDirectory: DirectoryProperty

    @get:InputDirectory
    abstract val timeTzdbDirectory: DirectoryProperty

    init {
        val typePostfix = convertType.map {
            when(it) {
                ConvertType.LARGE -> "-large"
                ConvertType.SMALL -> "-small"
            }
        }

        val outputDir = project.layout.buildDirectory.zip(typePostfix) { build, type -> build.dir("tzdbCompiled$type") }

        outputDirectory.convention(outputDir)
        convertType.convention(ConvertType.LARGE)
        executable = ""
    }

    override fun exec() {
        executable = File(timeTzdbDirectory.get().asFile, "node_modules/.bin/tzc").path
        val installVersion = ianaVersion.orNull
        val convertTypeArg = when(convertType.get()) {
            ConvertType.LARGE -> "--large"
            ConvertType.SMALL -> "--small"
        }
        if (installVersion.isNullOrEmpty()) {
            this.setArgs(listOf(outputDirectory.get().asFile, "-b", "-o", convertTypeArg))
        } else {
            this.setArgs(listOf(outputDirectory.get().asFile, "-b", "-o", convertTypeArg, "-u", installVersion))
        }
        super.exec()
    }
}
