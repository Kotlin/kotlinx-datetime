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

    fun execDefaultTimeZoneTest(): ExecResult {
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.defaultTimeZoneTest")
    }

    fun execDebianCopyTimeZoneTest(): ExecResult {
        exec("rm /etc/localtime")
        exec("cp /usr/share/zoneinfo/Europe/Berlin /etc/localtime")
        exec("echo 'Europe/Berlin' > /etc/timezone")
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.debianCopyTimeZoneTest")
    }

    fun execTimezoneMismatchTest(): ExecResult {
        exec("rm -f /etc/localtime")
        exec("cp /usr/share/zoneinfo/Europe/Berlin /etc/localtime")
        exec("echo 'Europe/Paris' > /etc/timezone")
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.timezoneMismatchTest")
    }

    fun execMissingEtcTimezoneTest(): ExecResult {
        exec("rm -f /etc/timezone")
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.missingEtcTimezoneTest")
    }

    fun execAllTimeZoneFilesMissingTest(): ExecResult {
        exec("rm -f /etc/localtime")
        exec("rm -f /etc/timezone")
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.allTimeZoneFilesMissingTest")
    }

    fun execSymlinkTimeZoneTest(): ExecResult {
        exec("rm -f /etc/localtime")
        exec("ln -sf /usr/share/zoneinfo/Europe/Paris /etc/localtime")
        exec("echo 'Europe/Paris' > /etc/timezone")
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.symlinkTimeZoneTest")
    }

    fun execInvalidTimezoneFormatTest(): ExecResult {
        exec("rm -f /etc/localtime")
        exec("cp /usr/share/zoneinfo/Europe/Berlin /etc/localtime")
        exec("echo 'Invalid/Timezone/Format' > /etc/timezone")
        return execTest("kotlinx.datetime.test.TimeZoneLinuxNativeTest.invalidTimezoneFormatTest")
    }

    private fun execTest(testFilter: String): ExecResult =
        exec("chmod +x /app/test.kexe && /app/test.kexe --ktest_filter=$testFilter")

    private fun exec(command: String): ExecResult = execInContainer("bash", "-c", command)
}

fun createTimezoneTestContainer(): TimezoneTestContainer {
    return TimezoneTestContainer(
        Paths.get("./jvm/test/testcontainers/Dockerfile"),
        "./build/bin/linuxArm64/debugTest/",
        "ubuntu-arctic-longyearbyen"
    )
}