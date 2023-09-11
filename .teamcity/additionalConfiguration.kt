/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher

fun Project.additionalConfiguration() {
    knownBuilds.buildAll.features {
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
}