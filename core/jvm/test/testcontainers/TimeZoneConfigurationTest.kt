/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Container.ExecResult

@Testcontainers
class TimeZoneConfigurationTest {

    @Container
    private val container = createTimezoneTestContainer(ContainerType.DEBIAN_JESSIE)

    @Test
    fun defaultTimeZoneTest() {
        assertExecSuccess(container.execDefaultTimeZoneTest())
    }

    @Test
    fun commonTimeZoneTests() {
        assertExecSuccess(container.execCommonTimeZoneTests())
    }

    private fun assertExecSuccess(execResult: ExecResult) {
        logger.info("Container stdout:\n${execResult.stdout}")
        logger.info("Container stderr:\n${execResult.stderr}")
        logger.info("Container exit code: ${execResult.exitCode}")

        if (execResult.exitCode != 0) {
            throw AssertionError(
                """
                |Command execution failed with exit code ${execResult.exitCode}.
                |Stdout:
                |${execResult.stdout}
                |Stderr:
                |${execResult.stderr}
                """.trimMargin()
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimeZoneConfigurationTest::class.java)

        @JvmStatic
        @BeforeAll
        fun buildTestBinary() {
            logger.info("Building test binary...")

            val process = ProcessBuilder()
                .command("../gradlew", "linkDebugTestLinuxArm64")
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().use { reader ->
                reader.lines().forEach { line ->
                    logger.info("Build: {}", line)
                }
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IllegalStateException("Failed to build test binary: exit code $exitCode")
            }

            logger.info("Test binary built successfully")
        }
    }
}