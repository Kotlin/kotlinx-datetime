/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory

@Testcontainers
class TimeZoneTest {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ParameterizedTest
    @MethodSource("containers")
    fun test(container: TimezoneTestContainer) {
        val execResult = container.runTest()

        logger.info("Container stdout: ${execResult.stdout}")
        logger.info("Container stderr: ${execResult.stderr}")
        logger.info("Container exit code: ${execResult.exitCode}")
    }

    companion object {
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
    }
}