/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.datetime.*
import kotlin.test.*
import platform.posix.getenv

class TimeZoneLinuxNativeTest {

    private var shouldRunTests = false

    @OptIn(ExperimentalForeignApi::class)
    @BeforeTest
    fun setup() {
        shouldRunTests = getenv("INSIDE_TESTCONTAINERS")?.toKString() != null
    }

    @Test
    fun defaultTimeZoneTest() {
        if (!shouldRunTests) return

        val tz = TimeZone.currentSystemDefault()
        println("TIMEZONE: $tz")
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }
}