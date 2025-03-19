/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package testcontainers

import kotlinx.datetime.*
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Paths
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertNotEquals

@Testcontainers
class TimeZoneTest {

    @Test
    fun test() {
        val tz = TimeZone.currentSystemDefault()
        assertNotEquals(TimeZone.UTC, tz)
        println("System time zone: $tz")

        val filePath = Paths.get("./jvm/test/testcontainers/original/Dockerfile")
        val content = String(Files.readAllBytes(filePath), Charsets.UTF_8)
        println("Dockerfile content: \n$content")

    }
}