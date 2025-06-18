/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger

fun deploymentProject() = Project {
    this.id("Deployment")
    this.name = "Deployment"

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val startDeploymentTask = startDeployment()

    val copyToCentralTask = copyToCentral(startDeploymentTask)
    val copyZoneInfoTask = copyZoneInfoToCentral(startDeploymentTask)

    val deployTasks = buildList {
        Platform.entries.forEach {
            add(deployToCentral(it, startDeploymentTask))
        }
    }

    deployTasks.forEach { deploy ->
        copyToCentralTask.dependsOnSnapshot(deploy, onFailure = FailureAction.CANCEL)
        copyZoneInfoTask.dependsOnSnapshot(deploy, onFailure = FailureAction.CANCEL)
    }

    buildTypesOrder = listOf(startDeploymentTask, *deployTasks.toTypedArray()) + listOf(copyToCentralTask, copyZoneInfoTask)
}

fun Project.startDeployment() = BuildType {
    id("StartDeployment")
    this.name = "Start Deployment [RUN THIS ONE]"
    type = BuildTypeSettings.Type.DEPLOYMENT

    params {
        text(
            "Version",
            "",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false
        )
        text(
            "VersionSuffix",
            "",
            display = ParameterDisplay.PROMPT
        )
        select(
            "Repository",
            "central",
            options = listOf("central", "sonatype"),
            display = ParameterDisplay.PROMPT
        )
        text(
            "ZoneInfoVersion",
            "",
            display = ParameterDisplay.HIDDEN
        )
    }

    steps {
        gradle {
            name = "Get ZoneInfoVersion"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            gradleParams =
                "--info --stacktrace -P$versionSuffixParameter=%VersionSuffix% -P$releaseVersionParameter=%Version% -Pzoneinfo.version.tc.parameter=ZoneInfoVersion"
            tasks = ":kotlinx-datetime-zoneinfo:setZoneInfoVersionToTeamcity"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }

    commonConfigure()
}.also { buildType(it) }

fun Project.deployToCentral(platform: Platform, startDeployment: BuildType) = buildType("DeployCentral", platform) {
    type = BuildTypeSettings.Type.DEPLOYMENT
    enablePersonalBuilds = false
    maxRunningBuilds = 1
    params {
        param(versionSuffixParameter, "${startDeployment.depParamRefs["VersionSuffix"]}")
        param(releaseVersionParameter, "${startDeployment.depParamRefs["Version"]}")
        param("system.publication_repository", "${startDeployment.depParamRefs["Repository"]}")
    }

    vcs {
        cleanCheckout = true
    }

    val taskNames = buildList {
        add("clean")
        when (platform) {
            Platform.Linux -> {
                addAll(
                    listOf(
                        "publishAndroidNativeArm32PublicationToCentralRepository",
                        "publishAndroidNativeArm64PublicationToCentralRepository",
                        "publishAndroidNativeX64PublicationToCentralRepository",
                        "publishAndroidNativeX86PublicationToCentralRepository",
                        "publishLinuxArm64PublicationToCentralRepository",
                        "publishLinuxX64PublicationToCentralRepository"
                    )
                )
            }

            Platform.Windows -> {
                add("publishMingwX64PublicationToCentralRepository")
            }

            Platform.MacOS -> {
                addAll(
                    listOf(
                        // metadata
                        "publishKotlinMultiplatformPublicationToCentralRepository",
                        // web
                        "publishJsPublicationToCentralRepository",
                        "publishWasmJsPublicationToCentralRepository",
                        "publishWasmWasiPublicationToCentralRepository",
                        // jvm
                        "publishJvmPublicationToCentralRepository",
                        // native
                        "publishIosArm64PublicationToCentralRepository",
                        "publishIosSimulatorArm64PublicationToCentralRepository",
                        "publishIosX64PublicationToCentralRepository",
                        "publishMacosArm64PublicationToCentralRepository",
                        "publishMacosX64PublicationToCentralRepository",
                        "publishTvosArm64PublicationToCentralRepository",
                        "publishTvosSimulatorArm64PublicationToCentralRepository",
                        "publishTvosX64PublicationToCentralRepository",
                        "publishWatchosArm32PublicationToCentralRepository",
                        "publishWatchosArm64PublicationToCentralRepository",
                        "publishWatchosDeviceArm64PublicationToCentralRepository",
                        "publishWatchosSimulatorArm64PublicationToCentralRepository",
                        "publishWatchosX64PublicationToCentralRepository"
                    )
                )
            }
        }
    }

    steps {
        gradle {
            name = "Deploy ${platform.buildTypeName()} Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            gradleParams =
                "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter%"
            tasks = taskNames.joinToString(" ")
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.dependsOnSnapshot(startDeployment)

fun Project.copyToCentral(startDeployment: BuildType) = BuildType {
    id("CopyToCentral")
    this.name = "Deploy To Central"
    type = BuildTypeSettings.Type.DEPLOYMENT

    templates(AbsoluteId("KotlinTools_DeployToCentral"))

    params {
        param("DeployVersion", startDeployment.depParamRefs["Version"].ref)
        param("ArtifactPrefixes", "[kotlinx-datetime]")
    }

    requirements {
        doesNotMatch("teamcity.agent.jvm.os.name", "Windows")
    }

    dependsOnSnapshot(startDeployment)

    triggers {
        finishBuildTrigger {
            buildType = "${startDeployment.id}"
            successfulOnly = true
            branchFilter = "+:*"
        }
    }
}.also { buildType(it) }

fun Project.copyZoneInfoToCentral(startDeployment: BuildType) = BuildType {
    id("CopyZoneInfoToCentral")
    this.name = "Deploy ZoneInfo To Central"
    type = BuildTypeSettings.Type.DEPLOYMENT

    templates(AbsoluteId("KotlinTools_DeployToCentral"))

    params {
        param("DeployVersion", startDeployment.depParamRefs["ZoneInfoVersion"].ref)
        param("ArtifactPrefixes", "[kotlinx-datetime-zoneinfo]")
    }

    requirements {
        doesNotMatch("teamcity.agent.jvm.os.name", "Windows")
    }

    dependsOnSnapshot(startDeployment)

    triggers {
        finishBuildTrigger {
            buildType = "${startDeployment.id}"
            successfulOnly = true
            branchFilter = "+:*"
        }
    }
}.also { buildType(it) }
