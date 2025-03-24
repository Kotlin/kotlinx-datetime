/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

import org.testcontainers.containers.BindMode
import org.testcontainers.containers.Container.ExecResult
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.nio.file.Paths

enum class ContainerType(val dockerfilePath: String, val imageName: String) {
    // Standard Jessie with Arctic/Longyearbyen timezone
    DEBIAN_JESSIE_CORRECT(
        "./jvm/test/testcontainers/debian-jessie/correct-config/Dockerfile",
        "debian-jessie-timezone-test"
    ),

    DEBIAN_JESSIE_DEFAULT(
        "./jvm/test/testcontainers/debian-jessie/default-config/Dockerfile",
        "debian-jessie-timezone-test-default"
    ),

    DEBIAN_JESSIE_MISSING_LOCALTIME(
        "./jvm/test/testcontainers/debian-jessie/missing-localtime/Dockerfile",
        "debian-jessie-timezone-test-missing-localtime"
    ),

    // Standard Noble with Arctic/Longyearbyen timezone
    UBUNTU_NOBLE_CORRECT(
        "./jvm/test/testcontainers/ubuntu-noble/correct-config/Dockerfile",
        "ubuntu-noble-timezone-test"
    ),

    UBUNTU_NOBLE_DEFAULT(
        "./jvm/test/testcontainers/ubuntu-noble/default-config/Dockerfile",
        "ubuntu-noble-timezone-test-default"
    )
}

class TimezoneTestContainer(containerType: ContainerType, binaryDir: String) :
    GenericContainer<TimezoneTestContainer>(
        ImageFromDockerfile(containerType.imageName)
            .withDockerfile(Paths.get(containerType.dockerfilePath))
    ) {

    init {
        withCommand("tail -f /dev/null")
        withFileSystemBind(binaryDir, "/app", BindMode.READ_WRITE)
    }

    fun execCorrectRecognizesCurrentSystemTimeZone(): ExecResult {
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.correctRecognizesCurrentSystemTimeZone")
    }

    fun execFallsBackToUTC(): ExecResult {
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.fallsBackToUTC")
    }

    fun execFallsBackToUniversal(): ExecResult {
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.fallsBackToUniversal")
    }

    fun execThrowsExceptionWhenTimeZoneUndetermined(): ExecResult {
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.throwsExceptionWhenTimeZoneUndetermined")
    }

    fun execThrowsExceptionWhenTimeZoneInconsistent(): ExecResult {
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.throwsExceptionWhenTimeZoneInconsistent")
    }

    fun execCommonTimeZoneTests(): ExecResult {
        return execTest("kotlinx.datetime.test.TimeZoneTest.*")
    }

    private fun execTest(testFilter: String): ExecResult =
        exec("chmod +x /app/test.kexe && /app/test.kexe --ktest_filter=$testFilter")

    private fun exec(command: String): ExecResult = execInContainer("bash", "-c", command)
}

fun createTimezoneTestContainer(containerType: ContainerType): TimezoneTestContainer {
    return TimezoneTestContainer(containerType, "./build/bin/linuxArm64/debugTest/")
}