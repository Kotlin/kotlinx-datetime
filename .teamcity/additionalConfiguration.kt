/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import jetbrains.buildServer.configs.kotlin.DslContext
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher

fun Project.additionalConfiguration() {
    knownBuilds.buildAll.features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = storedToken {
                    tokenId = "tc_token_id:CID_3c2e87590eb212c2b4b597f0b6696f72:34894:ebd4e9b7-fa1d-4e09-b44a-8604fc643a7c"
                }
            }
        }
    }
}