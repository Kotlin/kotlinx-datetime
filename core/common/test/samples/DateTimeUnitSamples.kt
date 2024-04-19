/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

class DateTimeUnitSamples {
    @Test
    fun construction() {
        // Constructing various measurement units
        check(DateTimeUnit.HOUR == DateTimeUnit.TimeBased(nanoseconds = 60 * 60 * 1_000_000_000L))
        check(DateTimeUnit.WEEK == DateTimeUnit.DayBased(days = 7))
        check(DateTimeUnit.WEEK * 2 == DateTimeUnit.DayBased(days = 14))
        check(DateTimeUnit.CENTURY == DateTimeUnit.MonthBased(months = 12 * 100))
    }

    @Test
    fun multiplication() {
        // Obtaining a measurement unit that's several times larger than another one
        val twoWeeks = DateTimeUnit.WEEK * 2
        check(twoWeeks.days == 14)
    }

    @Test
    fun timeBasedUnit() {
        // Constructing various time-based measurement units
        val halfDay = DateTimeUnit.TimeBased(nanoseconds = 12 * 60 * 60 * 1_000_000_000L)
        check(halfDay.nanoseconds == 12 * 60 * 60 * 1_000_000_000L)
        check(halfDay.duration == 12.hours)
        check(halfDay == DateTimeUnit.HOUR * 12)
        check(halfDay == DateTimeUnit.MINUTE * 720)
        check(halfDay == DateTimeUnit.SECOND * 43_200)
    }

    @Test
    fun dayBasedUnit() {
        // Constructing various day-based measurement units
        val iteration = DateTimeUnit.DayBased(days = 14)
        check(iteration.days == 14)
        check(iteration == DateTimeUnit.DAY * 14)
        check(iteration == DateTimeUnit.WEEK * 2)
    }

    @Test
    fun monthBasedUnit() {
        // Constructing various month-based measurement units
        val halfYear = DateTimeUnit.MonthBased(months = 6)
        check(halfYear.months == 6)
        check(halfYear == DateTimeUnit.QUARTER * 2)
        check(halfYear == DateTimeUnit.MONTH * 6)
    }
}
