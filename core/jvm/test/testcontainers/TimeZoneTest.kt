/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Paths
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@Testcontainers
class TimeZoneTest {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Container
    val originalContainer = TimezoneTestContainer(
        Paths.get("./jvm/test/testcontainers/original/Dockerfile"),
        "ubuntu-arctic-longyearbyen"
    )

    @Container
    val modifiedContainer = TimezoneTestContainer(
        Paths.get("./jvm/test/testcontainers/modified/Dockerfile"),
        "ubuntu-new-longyearbyen-modified"
    )

    @Test
    fun test() {
        val originalExecResult = originalContainer.runTest()
        val modifiedExecResult = modifiedContainer.runTest()
        logger.info("Original container stdout: ${originalExecResult.stdout}")
        logger.info("Modified container stdout: ${modifiedExecResult.stdout}")
    }
}