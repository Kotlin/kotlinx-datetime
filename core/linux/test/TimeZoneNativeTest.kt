/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.TimeZoneContext
import kotlinx.datetime.internal.systemTimezoneSearchRoot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimeZoneNativeTest {

    @Test
    fun correctSymlinkTest() = withFakeRoot("${RESOURCES}correct-symlink/") {
        val tz = TimeZoneContext.System.currentTimeZone()
        assertEquals(TimeZoneContext.System.get("Europe/Oslo"), tz)
    }

    @Test
    fun timezoneFileAgreesWithLocaltimeContentsTest() = withFakeRoot("${RESOURCES}timezone-file-agrees-with-localtime-contents/") {
        val tz = TimeZoneContext.System.currentTimeZone()
        assertEquals(TimeZoneContext.System.get("Europe/Oslo"), tz)
    }

    @Test
    fun fallbackToUTCWhenNoLocaltimeTest() = withFakeRoot("${RESOURCES}fallback-to-utc-when-no-localtime/") {
        val tz = TimeZoneContext.System.currentTimeZone()
        assertEquals(TimeZone.UTC, tz)
    }

    @Test
    fun missingTimezoneWhenLocaltimeIsNotSymlinkTest() = withFakeRoot("${RESOURCES}missing-timezone-when-localtime-is-not-symlink/") {
        assertFailsWith<IllegalTimeZoneException> {
            TimeZoneContext.System.currentTimeZone()
        }
    }

    @Test
    fun nonExistentTimezoneInTimezoneFileTest() = withFakeRoot("${RESOURCES}non-existent-timezone-in-timezone-file/") {
        assertFailsWith<IllegalTimeZoneException> {
            TimeZoneContext.System.currentTimeZone()
        }
    }

    @Test
    fun timezoneFileDisagreesWithLocaltimeContentsTest() = withFakeRoot("${RESOURCES}timezone-file-disagrees-with-localtime-contents/") {
        val exception = assertFailsWith<IllegalTimeZoneException> {
            TimeZoneContext.System.currentTimeZone()
        }

        assertEquals(
            true,
            exception.message?.contains("Europe/Oslo"),
            "Exception message does not contain 'Europe/Oslo' as expected"
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
