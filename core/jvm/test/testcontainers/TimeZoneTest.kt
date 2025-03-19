/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

import org.junit.jupiter.api.BeforeAll
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory

@Testcontainers
class TimeZoneTest {

    @ParameterizedTest
    @MethodSource("containers")
    fun test(container: TimezoneTestContainer) {
        val execResult = container.runTest()

        logger.info("Container stdout: ${execResult.stdout}")
        logger.info("Container stderr: ${execResult.stderr}")
        logger.info("Container exit code: ${execResult.exitCode}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(javaClass)

        @JvmStatic
        @Container
        val originalContainer = createTimezoneTestContainer("original")

        @JvmStatic
        @Container
        val modifiedContainer = createTimezoneTestContainer("modified")

        @JvmStatic
        fun containers(): List<TimezoneTestContainer> {
            return listOf(originalContainer, modifiedContainer)
        }

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