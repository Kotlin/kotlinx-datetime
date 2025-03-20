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

    fun runTimeZoneTests(): ExecResult {
        return executeTest("--ktest_filter=kotlinx.datetime.test.TimeZoneLinuxNativeTest.*")
    }

    fun runAllTests(): ExecResult {
        return executeTest()
    }

    private fun executeTest(testFilter: String? = null): ExecResult {
        val command = buildString {
            append("chmod +x /app/test.kexe && /app/test.kexe")
            testFilter?.also {
                append(" ")
                append(it)
            }
        }

        return execInContainer("bash", "-c", command)
    }
}

fun createTimezoneTestContainer(configType: String): TimezoneTestContainer {
    return TimezoneTestContainer(
        Paths.get("./jvm/test/testcontainers/$configType/Dockerfile"),
        "./build/bin/linuxArm64/debugTest/",
        "ubuntu-arctic-longyearbyen-$configType"
    )
}