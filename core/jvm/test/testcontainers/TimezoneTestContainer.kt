/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.Container.ExecResult
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.file.Path
import java.nio.file.Paths

class TimezoneTestContainer(dockerfilePath: Path, binaryDir: String, imageName: String) :
    GenericContainer<TimezoneTestContainer>(ImageFromDockerfile(imageName).withDockerfile(dockerfilePath)) {

    init {
        withCommand("tail -f /dev/null")
        withFileSystemBind(binaryDir, "/app", BindMode.READ_WRITE)
    }

    fun runTest(): ExecResult {
        return execInContainer("bash", "-c", "chmod +x /app/test.kexe && /app/test.kexe")
    }
}

fun createTimezoneTestContainer(typeName: String): TimezoneTestContainer {
    return TimezoneTestContainer(
        Paths.get("./jvm/test/testcontainers/$typeName/Dockerfile"),
        "./build/bin/linuxArm64/debugTest/",
        "ubuntu-arctic-longyearbyen-$typeName"
    )
}