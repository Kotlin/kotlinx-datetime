/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.internal.systemTimezoneSearchRoot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TimeZoneNativeTest {

    @Test
    fun correctSymlinkTest() = withFakeRoot("${RESOURCES}correct-symlink/") {
        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }

    @Test
    fun correctLocaltimeCopyTest() = withFakeRoot("${RESOURCES}correct-localtime-copy/") {
        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }

    @Test
    fun fallbackToUTCWhenNoLocaltime() = withFakeRoot("${RESOURCES}missing-localtime/") {
        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.UTC, tz)
    }

    @Test
    fun missingTimezoneWhenLocaltimeIsNotSymlinkTest() = withFakeRoot("${RESOURCES}missing-timezone/") {
        assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }
    }

    @Test
    fun incorrectTimezoneTest() = withFakeRoot("${RESOURCES}incorrect-timezone/") {
        assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }
    }

    @Test
    fun differentTimezonesTest() = withFakeRoot("${RESOURCES}different-timezones/") {
        val exception = assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }

        assertTrue(
            exception.message?.startsWith("Timezone mismatch") == true,
            "Exception message did not start with 'Timezone mismatch'"
        )
    }

    companion object {
        const val RESOURCES = "./linux/test/time-zone-native-test-resources/"

        private fun withFakeRoot(fakeRoot: String, action: () -> Unit) {
            val defaultRoot = systemTimezoneSearchRoot
            systemTimezoneSearchRoot = fakeRoot
            try {
                action()
            } finally {
                systemTimezoneSearchRoot = defaultRoot
            }
        }
    }
}