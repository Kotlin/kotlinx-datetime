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

    val deployTask = deployToCentral(startDeploymentTask)

    copyToCentralTask.dependsOnSnapshot(deployTask, onFailure = FailureAction.CANCEL)
    copyZoneInfoTask.dependsOnSnapshot(deployTask, onFailure = FailureAction.CANCEL)

    buildTypesOrder = listOf(startDeploymentTask, deployTask, copyToCentralTask, copyZoneInfoTask)
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
        text(
            "ZoneInfoVersion",
            "",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false
        )
    }

    steps {
    }

    commonConfigure()
}.also { buildType(it) }

fun Project.deployToCentral(startDeployment: BuildType) = buildType("DeployCentral", Platform.MacOS) {
    type = BuildTypeSettings.Type.DEPLOYMENT
    enablePersonalBuilds = false
    maxRunningBuilds = 1
    params {
        param(versionSuffixParameter, "${startDeployment.depParamRefs["VersionSuffix"]}")
        param(releaseVersionParameter, "${startDeployment.depParamRefs["Version"]}")
    }

    vcs {
        cleanCheckout = true
    }

    val taskNames = listOf("clean", "publish")

    steps {
        gradle {
            name = "Deploy All Binaries"
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
