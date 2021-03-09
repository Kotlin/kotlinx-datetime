import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.*
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.*

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

version = "2020.1"

enum class Platform {
    Windows, Linux, MacOS;
}

val versionSuffixParameter = "versionSuffix"
val teamcitySuffixParameter = "teamcitySuffix"
val releaseVersionParameter = "releaseVersion"
val BUILD_ALL_ID = "Build_All"
val DEPLOY_PUBLISH_ID = "Deploy_Publish"
val DEPLOY_CONFIGURE_VERSION_ID = "Deploy_Configure"
val BUILD_CONFIGURE_VERSION_ID = "Build_Version"
val BUILD_CREATE_STAGING_REPO_ABSOLUTE_ID = AbsoluteId("KotlinTools_CreateSonatypeStagingRepository")
val libraryStagingRepoDescription = "Kotlin-DateTime"
val platforms = Platform.values()
val jdk = "JDK_18_x64"

fun Project.additionalConfiguration() { }

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

    features {
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:af36802a-ccd4-401b-86b9-0b08d2dfad17"
                }
            }
            param("github_oauth_user", "ilya-g")
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

fun BuildType.dependsOn(build: IdOwner, configure: Dependency.() -> Unit) =
    apply {
        dependencies.dependency(build, configure)
    }

fun BuildType.dependsOnSnapshot(build: IdOwner, onFailure: FailureAction = FailureAction.FAIL_TO_START, configure: SnapshotDependency.() -> Unit = {}) = apply {
    dependencies.dependency(build) {
        snapshot {
            configure()
            onDependencyFailure = onFailure
            onDependencyCancel = FailureAction.CANCEL
        }
    }
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
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter% -PbintrayApiKey=%bintray-key% -PbintrayUser=%bintray-user%"
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
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter% -PbintrayApiKey=%bintray-key% -PbintrayUser=%bintray-user%"
            tasks = "clean publish"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.dependsOnSnapshot(configureBuild)

fun Platform.buildTypeName(): String = when (this) {
    Platform.Windows, Platform.Linux -> name
    Platform.MacOS -> "Mac OS X"
}

fun Platform.buildTypeId(): String = buildTypeName().substringBefore(" ")

fun Platform.teamcityAgentName(): String = buildTypeName()

fun Project.buildType(name: String, platform: Platform, configure: BuildType.() -> Unit) = BuildType {
    // ID is prepended with Project ID, so don't repeat it here
    // ID should conform to identifier rules, so just letters, numbers and underscore
    id("${name}_${platform.buildTypeId()}")
    // Display name of the build configuration
    this.name = "$name (${platform.buildTypeName()})"

    requirements {
        contains("teamcity.agent.jvm.os.name", platform.teamcityAgentName())
    }

    params {
        // This parameter is needed for macOS agent to be compatible
        if (platform == Platform.MacOS) param("env.JDK_17", "")
    }

    commonConfigure()
    configure()
}.also { buildType(it) }


fun BuildType.commonConfigure() {
    requirements {
        noLessThan("teamcity.agent.hardware.memorySizeMb", "6144")
    }

    // Allow to fetch build status through API for badges
    allowExternalStatus = true

    // Configure VCS, by default use the same and only VCS root from which this configuration is fetched
    vcs {
        root(DslContext.settingsRoot)
        showDependenciesChanges = true
        checkoutMode = CheckoutMode.ON_AGENT
    }

    failureConditions {
        errorMessage = true
        nonZeroExitCode = true
        executionTimeoutMin = 120
    }

    features {
        feature {
            id = "perfmon"
            type = "perfmon"
        }
    }
}
