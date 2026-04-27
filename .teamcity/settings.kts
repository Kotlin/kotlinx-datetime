import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.*
import jetbrains.buildServer.configs.kotlin.triggers.*

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2023.05"

project {
    // Disable editing of project and build settings from the UI to avoid issues with TeamCity
    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val buildVersion = buildVersion()
    val buildAll = buildAll(buildVersion)
    val builds = platforms.map { build(it, buildVersion) }
    builds.forEach { build ->
        buildAll.dependsOnSnapshot(build, onFailure = FailureAction.ADD_PROBLEM)
        buildAll.dependsOn(build) {
            artifacts {
                artifactRules = "+:maven=>maven\n+:api=>api"
            }
        }
    }

    buildTypesOrder = listOf(buildAll, buildVersion, *builds.toTypedArray())

    subProject {
        id("Deployment")
        this.name = "Deployment"

        params {
            param("teamcity.ui.settings.readOnly", "true")
        }

        val deployVersion = deployVersion()
        val deployAll = deployAll(deployVersion)
        val deploys = platforms.map { buildArtifacts(deployVersion, it) }
        val deployUpload = deployUpload(deployVersion).apply {
            dependencies {
                deploys.forEach { dep ->
                    dependency(dep) {
                        snapshot {
                            onDependencyFailure = FailureAction.FAIL_TO_START
                            onDependencyCancel = FailureAction.CANCEL
                        }
                        artifacts {
                            artifactRules = "buildRepo.zip!** => buildRepo"
                        }
                    }
                }
            }
        }
        val deployPublish = deployPublish(deployVersion).apply {
            dependsOnSnapshot(deployUpload)
        }

        deploys.forEach { deployAll.dependsOnSnapshot(it) }
        deployAll.dependsOnSnapshot(deployUpload) {
            reuseBuilds = ReuseBuilds.NO
        }
        deployAll.dependsOnSnapshot(deployPublish) {
            reuseBuilds = ReuseBuilds.NO
        }

        buildTypesOrder = listOf(deployAll, deployVersion, *deploys.toTypedArray(), deployUpload, deployPublish)
    }

    additionalConfiguration()
}

fun Project.buildVersion() = BuildType {
    id(BUILD_CONFIGURE_VERSION_ID)
    this.name = "Build (Configure Version)"
    commonConfigure()

    params {
        param(versionSuffixParameter, "SNAPSHOT")
        param(teamcitySuffixParameter, "%build.counter%")
    }

    steps {
        gradle {
            name = "Generate build chain version"
            jdkHome = "%env.$jdk%"
            tasks = ""
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$teamcitySuffixParameter=%$teamcitySuffixParameter%"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.also { buildType(it) }

fun Project.buildAll(versionBuild: BuildType) = BuildType {
    id(BUILD_ALL_ID)
    this.name = "Build (All)"
    type = BuildTypeSettings.Type.COMPOSITE

    dependsOnSnapshot(versionBuild)
    buildNumberPattern = versionBuild.depParamRefs.buildNumber.ref

    triggers {
        vcs {
            triggerRules = """
                    -:*.md
                    -:.gitignore
                """.trimIndent()
        }
    }

    commonConfigure()
}.also { buildType(it) }

fun Project.build(platform: Platform, versionBuild: BuildType) = buildType("Build", platform) {

    dependsOnSnapshot(versionBuild)

    params {
        param(versionSuffixParameter, versionBuild.depParamRefs[versionSuffixParameter].ref)
        param(teamcitySuffixParameter, versionBuild.depParamRefs[teamcitySuffixParameter].ref)
    }

    steps {
        gradle {
            name = "Build and Test ${platform.buildTypeName()} Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            tasks = "clean publishToBuildLocal check"
            // --continue is needed to run tests for all targets even if one target fails
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$teamcitySuffixParameter=%$teamcitySuffixParameter% --continue"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }

    // What files to publish as build artifacts
    artifactRules = "+:build/maven=>maven\n+:build/api=>api"
}

fun Project.deployAll(deployVersion: BuildType) = BuildType {
    id(DEPLOY_ALL_ID)
    name = "Deploy [RUN THIS ONE]"
    description = "Start deployment pipeline"
    type = BuildTypeSettings.Type.COMPOSITE
    commonConfigure()

    failureConditions {
        // For publication a day is given to receive the approval,
        // so this job should not fail earlier.
        executionTimeoutMin = 1440
    }

    buildNumberPattern = deployVersion.depParamRefs.buildNumber.ref
    dependsOnSnapshot(deployVersion)

    params {
        text("reverse.dep.*.$releaseVersionParameter", "",
            label = "Version",
            description = "Version of artifacts to deploy",
            display = ParameterDisplay.PROMPT,
            regex = "[0-9]+\\.[0-9]+\\.[0-9]+(-.+)?",
            validationMessage = "It does not look like a proper version"
        )
    }
}.also { buildType(it) }

fun Project.deployVersion() = BuildType {
    id(DEPLOY_CONFIGURE_VERSION_ID)
    name = "Generate build version"
    description = "Generates build number used by all tasks in the pipeline and validates version numbers"
    type = BuildTypeSettings.Type.REGULAR
    commonConfigure()

    buildNumberPattern = "%$releaseVersionParameter% %build.counter%"

    params {
        param(versionSuffixParameter, "dev-%build.counter%")
    }

    requirements {
        // Require Linux for configuration build
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    steps {
        gradle {
            name = "Verify Gradle Configuration"
            tasks = "clean publishPrepareVersion"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter%"
            buildFile = ""
            jdkHome = "%env.$jdk%"
        }
    }
}.also { buildType(it) }

fun Project.deployUpload(deployVersion: BuildType) = BuildType {
    templates(UPLOAD_DEPLOYMENT_TEMPLATE_ID)
    id(DEPLOY_UPLOAD_ID)
    name = "Upload deployment to central portal"
    description = "Verifies artifacts, uploads it to the Central portal, and waits for verification results."
    type = BuildTypeSettings.Type.DEPLOYMENT
    commonConfigure()

    buildNumberPattern = deployVersion.depParamRefs.buildNumber.ref
    dependsOnSnapshot(deployVersion)

    artifactRules = """
        %LocalDeploymentPaths%
        buildRepo => buildRepo.zip
    """.trimIndent()

    params {
        param("DeployVersion", "%$releaseVersionParameter%")
    }
}.also { buildType(it) }

fun Project.deployPublish(deployVersion: BuildType) = BuildType {
    templates(PUBLISH_DEPLOYMENT_TEMPLATE_ID)
    id(DEPLOY_PUBLISH_ID)
    name = "Publish deployment"
    description = "Published previously uploaded deployment"
    type = BuildTypeSettings.Type.DEPLOYMENT
    commonConfigure()

    failureConditions {
        // Wait for a day for the approval
        executionTimeoutMin = 1440
    }

    buildNumberPattern = deployVersion.depParamRefs.buildNumber.ref
    dependsOnSnapshot(deployVersion)

    params {
        param("DeployVersion", "%$releaseVersionParameter%")
        // Override parameter from the template
        param("Approvers", DslContext.getParameter("Approvers", "<nobody>"))
    }
}.also { buildType(it) }


fun Project.buildArtifacts(deployVersion: BuildType, platform: Platform) = buildType("Binaries", platform) {
    type = BuildTypeSettings.Type.DEPLOYMENT
    enablePersonalBuilds = false
    maxRunningBuilds = 1

    buildNumberPattern = deployVersion.depParamRefs.buildNumber.ref
    dependsOnSnapshot(deployVersion)

    vcs {
        cleanCheckout = true
    }

    artifactRules = """
        build/maven/** => buildRepo.zip
    """.trimIndent()

    params {
        param(versionSuffixParameter, "${deployVersion.depParamRefs[versionSuffixParameter]}")
        param(publicationCommandParameter, "publishAllPublicationsToBuildLocalRepository")
    }

    steps {
        gradle {
            name = "Build ${platform.buildTypeName()} Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter%"
            tasks = "clean %publicationCommand%"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}
