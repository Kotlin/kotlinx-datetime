/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

import org.testcontainers.containers.Container.ExecResult
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.file.Path

class TimezoneTestContainer(dockerfilePath: Path, val imageName: String) :
    GenericContainer<TimezoneTestContainer>(ImageFromDockerfile(imageName).withDockerfile(dockerfilePath)) {

    init {
        withCommand("tail -f /dev/null")
    }

    fun runTest(): ExecResult {
        return execInContainer("bash", "-c", "echo Inside container $imageName, system time zone: $(date -R)")
    }
}