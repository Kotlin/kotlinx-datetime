/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class ValueBagTest {
    @Test
    fun testAssigningIllegalValues() {
        val valueBag = ValueBag().apply { populateFrom(instant, timeZone.offsetAt(instant)) }
        for (field in twoDigitFields) {
            for (invalidValue in listOf(-1, 100, Int.MIN_VALUE, Int.MAX_VALUE, 1000, 253)) {
                assertFailsWith<IllegalArgumentException> { field.set(valueBag, invalidValue) }
            }
            assertEquals(field.get(currentTimeValueBag), field.get(valueBag),
                "ValueBag should not be modified if an exception is thrown"
            )
        }
        for (invalidNanosecond in listOf(-5, -1, 1_000_000_000, 1_000_000_001)) {
            assertFailsWith<IllegalArgumentException> { valueBag.nanosecond = invalidNanosecond }
        }
        assertEquals(currentTimeValueBag.nanosecond, valueBag.nanosecond,
            "ValueBag should not be modified if an exception is thrown"
        )
    }

    @Test
    fun testAssigningLegalValues() {
        val valueBag = ValueBag().apply { populateFrom(instant, timeZone.offsetAt(instant)) }
        for (field in twoDigitFields) {
            for (validValue in listOf(null, 0, 5, 10, 43, 99)) {
                field.set(valueBag, validValue)
                assertEquals(validValue, field.get(valueBag))
            }
        }
    }

    val twoDigitFields = listOf(
        ValueBag::monthNumber,
        ValueBag::dayOfMonth,
        ValueBag::hour,
        ValueBag::minute,
        ValueBag::second,
        ValueBag::offsetTotalHours,
        ValueBag::offsetMinutesOfHour,
        ValueBag::offsetSecondsOfMinute,
    )
    val instant = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()
    val currentTimeValueBag = ValueBag().apply { populateFrom(instant, timeZone.offsetAt(instant)) }
}
