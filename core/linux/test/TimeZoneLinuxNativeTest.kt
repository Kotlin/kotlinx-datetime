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
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }

    /**
     * Tests Debian behavior where /etc/localtime is a copy instead of a symlink
     * but /etc/timezone contains the correct timezone identifier
     */
    @Test
    fun debianCopyTimeZoneTest() = Testcontainers.runIfAvailable {
        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("Europe/Berlin"), tz)
    }

    /**
     * Tests behavior when /etc/localtime and /etc/timezone mismatch
     */
    @Test
    fun timezoneMismatchTest() = Testcontainers.runIfAvailable {
        assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }
    }

    /**
     * Tests UTC fallback when /etc/timezone is missing
     */
    @Test
    fun missingEtcTimezoneTest() = Testcontainers.runIfAvailable {
        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }

    /**
     * Tests UTC fallback when all timezone files are missing
     */
    @Test
    fun allTimeZoneFilesMissingTest() = Testcontainers.runIfAvailable {
        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("UTC"), tz)
    }
}