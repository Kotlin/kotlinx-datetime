import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.*
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.*

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

version = "2018.2"
val versionSuffixParameter = "versionSuffix"
val teamcitySuffixParameter = "teamcitySuffix"
val releaseVersionParameter = "releaseVersion"

val bintrayUserName = "%env.BINTRAY_USER%"
val bintrayToken = "%env.BINTRAY_API_KEY%"

val platforms = listOf("Windows", "Linux", "Mac OS X")
val jdk = "JDK_18_x64"

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

    val deployConfigure = deployConfigure().apply {
        dependsOnSnapshot(buildAll, onFailure = FailureAction.IGNORE)
    }
    val deploys = platforms.map { deploy(it, deployConfigure) }
    val deployPublish = deployPublish(deployConfigure).apply {
        dependsOnSnapshot(buildAll, onFailure = FailureAction.IGNORE)
        deploys.forEach {
            dependsOnSnapshot(it)
        }
    }

    buildTypesOrder = listOf(buildAll, buildVersion, *builds.toTypedArray(), deployPublish, deployConfigure, *deploys.toTypedArray())
}

fun Project.buildVersion() = BuildType {
    id("Build_Version")
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
    id("Build_All")
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

fun Project.build(platform: String, versionBuild: BuildType) = platform(platform, "Build") {

    dependsOnSnapshot(versionBuild)

    params {
        param(versionSuffixParameter, versionBuild.depParamRefs[versionSuffixParameter].ref)
        param(teamcitySuffixParameter, versionBuild.depParamRefs[teamcitySuffixParameter].ref)
    }

    steps {
        gradle {
            name = "Build and Test $platform Binaries"
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

fun BuildType.dependsOn(build: BuildType, configure: Dependency.() -> Unit) =
    apply {
        dependencies.dependency(build, configure)
    }

fun BuildType.dependsOnSnapshot(build: BuildType, onFailure: FailureAction = FailureAction.FAIL_TO_START, configure: SnapshotDependency.() -> Unit = {}) = apply {
    dependencies.dependency(build) {
        snapshot {
            configure()
            onDependencyFailure = onFailure
            onDependencyCancel = FailureAction.CANCEL
        }
    }
}

fun Project.deployConfigure() = BuildType {
    id("Deploy_Configure")
    this.name = "Deploy (Configure Version)"
    commonConfigure()

    params {
        // enable editing of this configuration to set up things
        param("teamcity.ui.settings.readOnly", "false")
        param("bintray-user", bintrayUserName)
        password("bintray-key", bintrayToken)
        param(versionSuffixParameter, "dev-%build.counter%")
    }

    requirements {
        // Require Linux for configuration build
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    steps {
        gradle {
            name = "Verify Gradle Configuration"
            tasks = "clean publishBintrayCreateVersion"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter% -PbintrayApiKey=%bintray-key% -PbintrayUser=%bintray-user%"
            buildFile = ""
            jdkHome = "%env.$jdk%"
        }
    }
}.also { buildType(it) }

fun Project.deployPublish(configureBuild: BuildType) = BuildType {
    id("Deploy_Publish")
    this.name = "Deploy (Publish)"
    type = BuildTypeSettings.Type.COMPOSITE
    dependsOnSnapshot(configureBuild)
    buildNumberPattern = configureBuild.depParamRefs.buildNumber.ref
    params {
        // Tell configuration build how to get release version parameter from this build
        // "dev" is the default and means publishing is not releasing to public
        text(configureBuild.reverseDepParamRefs[releaseVersionParameter].name, "dev", display = ParameterDisplay.PROMPT, label = "Release Version")
    }
    commonConfigure()
}.also { buildType(it) }


fun Project.deploy(platform: String, configureBuild: BuildType) = platform(platform, "Deploy") {
    type = BuildTypeSettings.Type.DEPLOYMENT
    enablePersonalBuilds = false
    maxRunningBuilds = 1
    params {
        param(versionSuffixParameter, "${configureBuild.depParamRefs[versionSuffixParameter]}")
        param(releaseVersionParameter, "${configureBuild.depParamRefs[releaseVersionParameter]}")
        param("bintray-user", bintrayUserName)
        password("bintray-key", bintrayToken)
    }

    vcs {
        cleanCheckout = true
    }

    steps {
        gradle {
            name = "Deploy $platform Binaries"
            jdkHome = "%env.$jdk%"
            jvmArgs = "-Xmx1g"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter% -PbintrayApiKey=%bintray-key% -PbintrayUser=%bintray-user%"
            tasks = "clean publish"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.dependsOnSnapshot(configureBuild)

fun Project.platform(platform: String, name: String, configure: BuildType.() -> Unit) = BuildType {
    // ID is prepended with Project ID, so don't repeat it here
    // ID should conform to identifier rules, so just letters, numbers and underscore
    id("${name}_${platform.substringBefore(" ")}")
    // Display name of the build configuration
    this.name = "$name ($platform)"

    requirements {
        contains("teamcity.agent.jvm.os.name", platform)
    }

    params {
        // This parameter is needed for macOS agent to be compatible
        if (platform.startsWith("Mac")) param("env.JDK_17", "")
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
