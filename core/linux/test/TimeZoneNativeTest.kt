/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.internal.root
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TimeZoneNativeTest {

    @Test
    fun correctSymlinkTest() {
        root = "${RESOURCES}correct-symlink/"

        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }

    @Test
    fun correctLocaltimeCopyTest() {
        root = "${RESOURCES}correct-localtime-copy/"

        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }

    @Test
    fun fallsBackToUTC() {
        root = "${RESOURCES}missing-localtime/"

        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.UTC, tz)
    }

    @Test
    fun missingTimezoneTest() {
        root = "${RESOURCES}missing-timezone/"

        val exception = assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }

        assertTrue(
            exception.message?.startsWith("Could not determine the timezone ID") == true,
            "Exception message did not match"
        )
    }

    @Test
    fun incorrectTimezoneTest() {
        root = "${RESOURCES}incorrect-timezone/"

        val exception = assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }

        assertTrue(
            exception.message?.startsWith("Could not determine the timezone ID") == true,
            "Exception message did not match"
        )
    }

    @Test
    fun differentTimezonesTest() {
        root = "${RESOURCES}different-timezones/"

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
    }
}