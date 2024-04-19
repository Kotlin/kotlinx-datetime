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
