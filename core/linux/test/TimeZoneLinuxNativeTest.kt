/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class TimeZoneLinuxNativeTest {
    @Test
    fun defaultTimeZoneTest() {
        val tz = TimeZone.currentSystemDefault()
        println("LINUX TIMEZONE: $tz")
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }
}