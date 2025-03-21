/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class TimeZoneLinuxNativeTest {

    /**
     * Verifies that the current system time zone is recognized correctly
     * and matches one of the expected values for the test environment.
     */
    @Test
    fun currentSystemTimeZoneTest() = Testcontainers.runIfAvailable {
        val tz = TimeZone.currentSystemDefault()
        assertTrue(tz == TimeZone.of("Europe/Oslo") || tz == TimeZone.of("Arctic/Longyearbyen"))
    }

    /**
     * Verifies that the system time zone defaults to UTC
     * when no valid time zone can be determined.
     */
    @Test
    fun fallbackToUTCTest() = Testcontainers.runIfAvailable {
        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.UTC, tz)
    }

    /**
     * Verifies that TimeZone.currentSystemDefault() throws IllegalTimeZoneException
     * with the expected error message when the time zone ID cannot be determined.
     */
    @Test
    fun undeterminedTimeZoneExceptionTest() = Testcontainers.runIfAvailable {
        val exception = assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }
        assertTrue(
            exception.message?.startsWith("Could not determine the timezone ID") == true,
            "Exception message did not match"
        )
    }

    /**
     * Verifies that TimeZone.currentSystemDefault() throws IllegalTimeZoneException
     * with the expected error message when time zone settings are inconsistent.
     */
    @Test
    fun inconsistentTimeZoneExceptionTest() = Testcontainers.runIfAvailable {
        val exception = assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }
        assertTrue(
            exception.message?.startsWith("Timezone mismatch") == true,
            "Exception message did not start with 'Timezone mismatch'"
        )
    }
}