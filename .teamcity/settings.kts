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

    val deployVersion = deployVersion().apply {
        dependsOnSnapshot(buildAll, onFailure = FailureAction.IGNORE)
        dependsOnSnapshot(BUILD_CREATE_STAGING_REPO_ABSOLUTE_ID) {
            reuseBuilds = ReuseBuilds.NO
        }
    }
    val deploys = platforms.map { deploy(it, deployVersion) }
    val deployPublish = deployPublish(deployVersion).apply {
        dependsOnSnapshot(buildAll, onFailure = FailureAction.IGNORE)
        dependsOnSnapshot(BUILD_CREATE_STAGING_REPO_ABSOLUTE_ID) {
            reuseBuilds = ReuseBuilds.NO
        }
        deploys.forEach {
            dependsOnSnapshot(it)
        }
    }

    buildTypesOrder = listOf(buildAll, buildVersion, *builds.toTypedArray(), deployPublish, deployVersion, *deploys.toTypedArray())

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

fun Project.deployVersion() = BuildType {
    id(DEPLOY_CONFIGURE_VERSION_ID)
    this.name = "Deploy (Configure Version)"
    commonConfigure()

    params {
        // enable editing of this configuration to set up things
        param("teamcity.ui.settings.readOnly", "false")
        param(versionSuffixParameter, "dev-%build.counter%")
        param("reverse.dep.$BUILD_CREATE_STAGING_REPO_ABSOLUTE_ID.system.libs.repo.description", libraryStagingRepoDescription)
        param("env.libs.repository.id", "%dep.$BUILD_CREATE_STAGING_REPO_ABSOLUTE_ID.env.libs.repository.id%")
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

fun Project.deployPublish(configureBuild: BuildType) = BuildType {
    id(DEPLOY_PUBLISH_ID)
    this.name = "Deploy (Publish)"
    type = BuildTypeSettings.Type.COMPOSITE
    dependsOnSnapshot(configureBuild)
    buildNumberPattern = configureBuild.depParamRefs.buildNumber.ref
    params {
        // Tell configuration build how to get release version parameter from this build
        // "dev" is the default and means publishing is not releasing to public
        text(configureBuild.reverseDepParamRefs[releaseVersionParameter].name, "dev", display = ParameterDisplay.PROMPT, label = "Release Version")
        param("env.libs.repository.id", "%dep.$BUILD_CREATE_STAGING_REPO_ABSOLUTE_ID.env.libs.repository.id%")
    }
    commonConfigure()
}.also { buildType(it) }


fun Project.deploy(platform: Platform, configureBuild: BuildType) = buildType("Deploy", platform) {
    type = BuildTypeSettings.Type.DEPLOYMENT
    enablePersonalBuilds = false
    maxRunningBuilds = 1
    params {
        param(versionSuffixParameter, "${configureBuild.depParamRefs[versionSuffixParameter]}")
        param(releaseVersionParameter, "${configureBuild.depParamRefs[releaseVersionParameter]}")
        param("env.libs.repository.id", "%dep.$BUILD_CREATE_STAGING_REPO_ABSOLUTE_ID.env.libs.repository.id%")
    }

    vcs {
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Deploy ${platform.buildTypeName()} Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter%"
            tasks = "clean publish"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.dependsOnSnapshot(configureBuild)
