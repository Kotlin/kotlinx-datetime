/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class TimeZoneLinuxNativeTest {

    /**
     * Verifies that the default system time zone is recognized correctly
     * and matches the expected value.
     */
    @Test
    fun defaultTimeZoneTest() = Testcontainers.runIfAvailable {
        val tz = TimeZone.currentSystemDefault()
        println("Default timezone: $tz")
        assertTrue(tz == TimeZone.of("Europe/Oslo") || tz == TimeZone.of("Arctic/Longyearbyen"))
    }
}