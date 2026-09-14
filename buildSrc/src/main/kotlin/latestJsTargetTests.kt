/*
 * Copyright 2016-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

/**
 * Introduces an additional compilation and a Node.js test run that check the library
 * against the latest JS target supported by the Kotlin/JS compiler.
 *
 * This is meant to be called from inside the `js { }` block of the `kotlin { }` block, e.g.:
 * ```
 * kotlin {
 *     js {
 *         // regular `js` configuration
 *         configureTestWithTheLatestJsTarget()
 *     }
 * }
 * ```
 * so the setup does not have to be copy-pasted across submodules.
 */
fun KotlinJsTargetDsl.configureTestWithTheLatestJsTarget() {
    // The part for testing with the latest JS target supported
    val mainCompilation = compilations.getByName("main")
    val testCompilation = compilations.getByName("test")

    val latestJsCompilation = compilations.create("latestJsTest") {
        associateWith(mainCompilation)
        defaultSourceSet.dependsOn(testCompilation.defaultSourceSet)
        binaries.executable(this)
        binaries.configureEach {
            linkTask.configure {
                compilerOptions {
                    target.set("es2015")
                    moduleKind.set(JsModuleKind.MODULE_COMMONJS) // Mocha adapter doesn't support ES modules yet
                    freeCompilerArgs.add("-Xes-long-as-bigint")
                }
            }
        }
    }

    nodejs {
        val latestTargetRun = testRuns.create("latestTarget") {
            setExecutionSourceFrom(latestJsCompilation)
            executionTask.configure {
                val devBinary = latestJsCompilation.binaries
                    .matching { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
                    .single()

                inputFileProperty.set(devBinary.mainFileSyncPath)
            }
        }

        testTask {
            dependsOn(latestTargetRun.executionTask)
        }
    }
}
