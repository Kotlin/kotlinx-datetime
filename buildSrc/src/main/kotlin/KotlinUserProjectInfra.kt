/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmName("KotlinUserProjectInfra")

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.maven
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

/**
 * Exposes a set of utilities for builds as a Kotlin user project.
 *
 * To be more specific, the utilities in question simplify building the project
 * with a custom Kotlin distribution and according to certain guidelines of the Kotlin team.
 *
 * This, in turn, allows the Kotlin team to continuously check
 * whether not-yet-released changes are compatible with the project
 * (a less generous wording would be "to perform integration testing that substitutes lack of unit testing").
 *
 * @see [KotlinUserProjectUtilities.kupArtifactsRepo]
 * @see [KotlinUserProjectUtilities.kupConfiguration]
 */
fun <T> kupInfra(action: KotlinUserProjectUtilities.() -> T) =
    with(KotlinUserProjectUtilities) { action() }

/**
 * @see [kupInfra]
 */
object KotlinUserProjectUtilities {
    private val Project.kupArtifactsRepoURL: String?
        get() = providers.gradleProperty("kotlin_repo_url").orNull

    /**
     * Adds a URL provided via the [kotlin_repo_url][kupArtifactsRepoURL] Gradle property (if any) to the given [RepositoryHandler].
     * Such a repository is expected to contain development distributions of Kotlin.
     */
    // TODO: turn `context` into a context parameter `project` whenever it becomes possible to do so
    fun RepositoryHandler.kupArtifactsRepo(context: Project) {
        val kupArtifactsRepoURL = context.kupArtifactsRepoURL ?: return
        maven(kupArtifactsRepoURL)
        kupLog("Added '$kupArtifactsRepoURL' as a Maven repo to '${context.path}'")
    }

    private val languageVersionByTask =
        mutableMapOf<KotlinCompilationTask<*>, KotlinVersion>()

    /**
     * A [Kotlin language version][KotlinCommonCompilerOptions.languageVersion] value for [kupConfiguration] to use
     * in absence of any value provided via the [kotlin_language_version][kupLanguageVersion] Gradle property.
     *
     * Use this property instead of [KotlinCommonCompilerOptions.languageVersion] when using [kupConfiguration]
     * to avoid order-dependent configuration conflicts.
     * Do _not_ use this property when not using [kupConfiguration]; the value will end up ignored.
     *
     * @see [kupConfiguration]
     */
    var KotlinCompilationTask<*>.defaultLanguageVersion: KotlinVersion?
        get() = languageVersionByTask[this]
        set(value) {
            when (value) {
                null -> languageVersionByTask -= this
                else -> languageVersionByTask[this] = value
            }
        }

    private val apiVersionByTask =
        mutableMapOf<KotlinCompilationTask<*>, KotlinVersion>()

    /**
     * A [Kotlin API version][KotlinCommonCompilerOptions.apiVersion] value for [kupConfiguration] to use
     * in absence of any value provided via the [kotlin_api_version][kupApiVersion] Gradle property.
     *
     * Use this property instead of [KotlinCommonCompilerOptions.apiVersion] when using [kupConfiguration]
     * to avoid order-dependent configuration conflicts.
     * Do _not_ use this property when not using [kupConfiguration]; the value will end up ignored.
     *
     * @see [kupConfiguration]
     */
    var KotlinCompilationTask<*>.defaultApiVersion: KotlinVersion?
        get() = apiVersionByTask[this]
        set(value) {
            when (value) {
                null -> apiVersionByTask -= this
                else -> apiVersionByTask[this] = value
            }
        }

    private val wErrorTasks =
        mutableSetOf<KotlinCompilationTask<*>>()

    /**
     * A value of [the setting for treating Kotlin warnings as errors][KotlinCommonCompilerOptions.allWarningsAsErrors]
     * for [kupConfiguration] to use in absence of any value
     * provided via the [kotlin_Werror_override][kupWError] Gradle property.
     *
     * Use this property instead of [KotlinCommonCompilerOptions.allWarningsAsErrors] when using [kupConfiguration]
     * to avoid order-dependent configuration conflicts.
     * Do _not_ use this property when not using [kupConfiguration]; the value will end up ignored.
     *
     * @see [kupConfiguration]
     */
    var KotlinCompilationTask<*>.wErrorDefault: Boolean
        get() = wErrorTasks.contains(this)
        set(value) {
            when (value) {
                false -> wErrorTasks -= this
                true -> wErrorTasks += this
            }
        }

    private val KotlinCompilationTask<*>.kupLanguageVersion: KotlinVersion?
        get() = project.providers.gradleProperty("kotlin_language_version")
            .map(KotlinVersion::fromVersion)
            .orNull

    private val KotlinCompilationTask<*>.kupApiVersion: KotlinVersion?
        get() = project.providers.gradleProperty("kotlin_api_version")
            .map(KotlinVersion::fromVersion)
            .orNull

    private val KotlinCompilationTask<*>.kupAdditionalCliOptions
        get() = project.providers.gradleProperty("kotlin_additional_cli_options")
            .map { kupAdditionalCliOptions ->
                kupAdditionalCliOptions.split(" ").filter(String::isNotBlank)
            }
            .map(List<String>::toTypedArray)
            .getOrElse(emptyArray())

    private val KotlinCompilationTask<*>.kupWError: Boolean?
        get() = project.providers.gradleProperty("kotlin_Werror_override")
            .map { kupWError ->
                when (kupWError) {
                    "enable" -> true
                    "disable" -> false
                    else -> error("invalid 'kotlin_Werror_override' value: $kupWError")
                }
            }
            .orNull

    /**
     * Configures the given [KotlinCompilationTask] according to Kotlin user project guidelines.
     *
     * To be more specific:
     * * the task's [Kotlin language version][KotlinCommonCompilerOptions.languageVersion]
     *   is set to the value provided via the [kotlin_language_version][kupLanguageVersion] Gradle property if any
     *   (otherwise, [defaultLanguageVersion] is used instead);
     * * the task's [Kotlin API version][KotlinCommonCompilerOptions.apiVersion]
     *   is set to the value provided via the [kotlin_api_version][kupApiVersion] Gradle property if any
     *   (otherwise, [defaultApiVersion] is used instead);
     * * the following CLI options are injected into the task's Kotlin compiler configuration:
     *     * `-Xreport-all-warnings`
     *     * `-Xrender-internal-diagnostic-names`
     *     * `-Xpartial-linkage-loglevel=error` (for klib-based targets)
     * * CLI options provided via the [kotlin_additional_cli_options][kupAdditionalCliOptions] Gradle property (if any)
     *   are injected into the task's Kotlin compiler configuration;
     * * the task's [setting for treating Kotlin warnings as errors][KotlinCommonCompilerOptions.allWarningsAsErrors]
     *   is configured according to the [kotlin_Werror_override][kupWError] Gradle property
     *   (`true` for `enable`, `false` for `disable`)
     *   if any (otherwise, [wErrorDefault] is used instead).
     *
     * **important!**
     *
     * Do not use the following [KotlinCommonCompilerOptions] properties directly together with this utility
     * to avoid order-dependent configuration conflicts:
     * * [languageVersion][KotlinCommonCompilerOptions.languageVersion] (use [defaultLanguageVersion] instead)
     * * [apiVersion][KotlinCommonCompilerOptions.apiVersion] (use [defaultApiVersion] instead)
     * * [allWarningsAsErrors][KotlinCommonCompilerOptions.allWarningsAsErrors] (use [wErrorDefault] instead)
     *
     */
    fun KotlinCompilationTask<*>.kupConfiguration() {
        // configure language version
        val languageVersion = kupLanguageVersion ?: defaultLanguageVersion
        compilerOptions.languageVersion.set(languageVersion)
        kupLog("Set 'languageVersion' to $languageVersion for '$path'")

        // configure API version
        val apiVersion = kupApiVersion ?: defaultApiVersion
        compilerOptions.apiVersion.set(apiVersion)
        kupLog("Set 'apiVersion' to $apiVersion for '$path'")

        fun KotlinCommonCompilerOptions.addAllCliOptions(vararg cliOptions: String) {
            for (cliOption in cliOptions) {
                freeCompilerArgs.add(cliOption)
                kupLog("Added '$cliOption' as a Kotlin CLI option to '$path'")
            }
        }

        // add mandatory KUP-relevant CLI options
        compilerOptions.addAllCliOptions(
            // output reported warnings even in the presence of reported errors
            "-Xreport-all-warnings",
            // output kotlin.git-searchable names of reported diagnostics
            "-Xrender-internal-diagnostic-names",
        )
        when (this) {
            // K/Native
            is KotlinNativeCompile -> {
                compilerOptions.addAllCliOptions(
                    "-Xpartial-linkage-loglevel=error",
                )
            }
            // K/JS **and** K/Wasm
            is Kotlin2JsCompile -> {
                compilerOptions.addAllCliOptions(
                    "-Xpartial-linkage-loglevel=error",
                )
            }
        }

        // add additional (i.e. user-defined) KUP-relevant CLI options
        compilerOptions.addAllCliOptions(*kupAdditionalCliOptions)

        // configure -Werror
        val wError = kupWError ?: wErrorDefault
        compilerOptions.allWarningsAsErrors.set(wError)
        kupLog("Set 'allWarningsAsErrors' to $wError for '$path'")
    }
}

private val kupLogger: Logger = Logging.getLogger("KotlinUserProjectInfraLogger")
private fun kupLog(message: String) { kupLogger.lifecycle("[KUP infra] $message") }
