/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.test.*

class DayOfWeekSamples {

    @Test
    fun usage() {
        // Providing different behavior based on what day of the week it is today
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        when (today.dayOfWeek) {
            DayOfWeek.MONDAY -> check(today.dayOfWeek.isoDayNumber == 1)
            DayOfWeek.TUESDAY -> check(today.dayOfWeek.isoDayNumber == 2)
            DayOfWeek.WEDNESDAY -> check(today.dayOfWeek.isoDayNumber == 3)
            DayOfWeek.THURSDAY -> check(today.dayOfWeek.isoDayNumber == 4)
            DayOfWeek.FRIDAY -> check(today.dayOfWeek.isoDayNumber == 5)
            DayOfWeek.SATURDAY -> check(today.dayOfWeek.isoDayNumber == 6)
            DayOfWeek.SUNDAY -> check(today.dayOfWeek.isoDayNumber == 7)
            else -> TODO("A new day was added to the week?")
        }
    }

    @Test
    fun isoDayNumber() {
        // Getting the ISO day-of-week number
        check(DayOfWeek.MONDAY.isoDayNumber == 1)
        check(DayOfWeek.TUESDAY.isoDayNumber == 2)
        // ...
        check(DayOfWeek.SUNDAY.isoDayNumber == 7)
    }

    @Test
    fun constructorFunction() {
        // Constructing a DayOfWeek from the ISO day-of-week number
        check(DayOfWeek(isoDayNumber = 1) == DayOfWeek.MONDAY)
        check(DayOfWeek(isoDayNumber = 2) == DayOfWeek.TUESDAY)
        // ...
        check(DayOfWeek(isoDayNumber = 7) == DayOfWeek.SUNDAY)
        try {
            DayOfWeek(0)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
