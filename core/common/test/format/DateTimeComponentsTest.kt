/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateTimeComponentsTest {
    @Test
    fun testToLocalTimeOrNull() {
        // Valid case - should return a LocalTime
        val validComponents = DateTimeComponents().apply {
            hour = 15
            minute = 30
            second = 45
            nanosecond = 123_456_789
        }
        val validTime = validComponents.toLocalTimeOrNull()
        assertEquals(LocalTime(15, 30, 45, 123_456_789), validTime)

        // Missing hour - should return null
        val missingHourComponents = DateTimeComponents().apply {
            minute = 30
            second = 45
        }
        assertNull(missingHourComponents.toLocalTimeOrNull())

        // Missing minute - should return null
        val missingMinuteComponents = DateTimeComponents().apply {
            hour = 15
            second = 45
        }
        assertNull(missingMinuteComponents.toLocalTimeOrNull())

        // Inconsistent hour and hourOfAmPm - should return null
        val inconsistentHourComponents = DateTimeComponents().apply {
            hour = 15
            hourOfAmPm = 2 // Should be 3 for hour 15
            minute = 30
        }
        assertNull(inconsistentHourComponents.toLocalTimeOrNull())

        // Inconsistent hour and amPm - should return null
        val inconsistentAmPmComponents = DateTimeComponents().apply {
            hour = 15
            amPm = AmPmMarker.AM // Should be PM for hour 15
            minute = 30
        }
        assertNull(inconsistentAmPmComponents.toLocalTimeOrNull())

        // Valid case with hourOfAmPm and amPm - should return a LocalTime
        val validAmPmComponents = DateTimeComponents().apply {
            hourOfAmPm = 3
            amPm = AmPmMarker.PM
            minute = 30
        }
        assertEquals(LocalTime(15, 30), validAmPmComponents.toLocalTimeOrNull())

        // Valid case with hourOfAmPm, amPm, and simply hour - should return a LocalTime
        val validAmPmComponents2 = DateTimeComponents().apply {
            hour = 15
            hourOfAmPm = 3
            amPm = AmPmMarker.PM
            minute = 30
        }
        assertEquals(LocalTime(15, 30), validAmPmComponents2.toLocalTimeOrNull())
    }

    @Test
    fun testAssigningIllegalValues() {
        val dateTimeComponents = DateTimeComponents().apply { setDateTimeOffset(instant, timeZone.offsetAt(instant)) }
        for (field in twoDigitFields) {
            for (invalidValue in listOf(-1, 100, Int.MIN_VALUE, Int.MAX_VALUE, 1000, 253)) {
                assertFailsWith<IllegalArgumentException> { field.set(dateTimeComponents, invalidValue) }
            }
            assertEquals(field.get(currentTimeDateTimeComponents), field.get(dateTimeComponents),
                "DateTimeComponents should not be modified if an exception is thrown"
            )
        }
        for (invalidNanosecond in listOf(-5, -1, 1_000_000_000, 1_000_000_001)) {
            assertFailsWith<IllegalArgumentException> { dateTimeComponents.nanosecond = invalidNanosecond }
        }
        assertEquals(currentTimeDateTimeComponents.nanosecond, dateTimeComponents.nanosecond,
            "DateTimeComponents should not be modified if an exception is thrown"
        )
    }

    @Test
    fun testAssigningLegalValues() {
        val dateTimeComponents = DateTimeComponents().apply { setDateTimeOffset(instant, timeZone.offsetAt(instant)) }
        for (field in twoDigitFields) {
            for (validValue in listOf(null, 0, 5, 10, 43, 99)) {
                field.set(dateTimeComponents, validValue)
                assertEquals(validValue, field.get(dateTimeComponents))
            }
        }
    }

    @Test
    fun testGettingInvalidMonth() {
        for (month in 1..12) {
            assertEquals(Month(month), DateTimeComponents().apply { monthNumber = month }.month)
        }
        for (month in listOf(0, 13, 60, 99)) {
            val components = DateTimeComponents().apply { monthNumber = month }
            assertFailsWith<IllegalArgumentException> { components.month }
        }
    }

    val twoDigitFields = listOf(
        DateTimeComponents::monthNumber,
        DateTimeComponents::day,
        DateTimeComponents::hour,
        DateTimeComponents::minute,
        DateTimeComponents::second,
        DateTimeComponents::offsetHours,
        DateTimeComponents::offsetMinutesOfHour,
        DateTimeComponents::offsetSecondsOfMinute,
    )
    val instant = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()
    val currentTimeDateTimeComponents = DateTimeComponents().apply { setDateTimeOffset(instant, timeZone.offsetAt(instant)) }
}
