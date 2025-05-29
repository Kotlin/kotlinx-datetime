/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.notifications
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.toId
import jetbrains.buildServer.configs.kotlin.triggers.schedule
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

fun Project.additionalConfiguration() {
    knownBuilds.buildAll.features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = storedToken {
                    tokenId = "tc_token_id:CID_7db3007c46f7e30124f81ef54591b223:-1:4b5743ff-b95e-41b0-89d6-e9a50d3048db"
                }
            }
        }
    }

    BuildType {
        id("Check_Updates")
        name = "Check for timezone database updates"
        description = "Queries the Internet to see if the timezone data was updated"

        vcs {
            root(DslContext.settingsRoot)
            cleanCheckout = true
        }

        steps {
            script {
                name = "Check if a new version of the timezone database is present"
                id = "Check_if_a_new_version_of_the_timezone_database_is_present"
                scriptContent = """
                    set -efu
                    git clone https://github.com/eggert/tz
                    latest_tag=$(git -C tz/ describe --abbrev=0 --tags)
                    current_tag=$(grep tzdbVersion gradle.properties | cut -d= -f2)
                    if [ "${'$'}latest_tag" != "${'$'}current_tag" ]; then
                      printf "A new version of the timezone database is available: %s\n" "${'$'}latest_tag" >&2
                      exit 1
                    fi
                """.trimIndent()
            }
            gradle {
                name = "Check if the Windows names need updating"
                id = "Check_if_the_Windows_names_need_updating"
                tasks = "downloadWindowsZonesMapping"
                jdkHome = "%env.JDK_18_x64%"
            }
        }

        triggers {
            schedule {
                schedulingPolicy = daily {
                    hour = 1
                    minute = 50
                }
                triggerBuild = always()
                withPendingChangesOnly = false
                branchFilter = "+:<default>"
            }
        }

        features {
            notifications {
                notifierSettings = slackNotifier {
                    connection = "PROJECT_EXT_51"
                    sendTo = "#kotlin-lib-team"
                    messageFormat = simpleMessageFormat()
                }
                buildFailed = true
                firstFailureAfterSuccess = true
            }
        }

        requirements {
            doesNotContain("teamcity.agent.jvm.os.name", "Windows")
        }
    }.also { buildType(it) }
}
