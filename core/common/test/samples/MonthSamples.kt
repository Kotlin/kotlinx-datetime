/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.test.*

class MonthSamples {

    @Test
    fun usage() {
        // Providing different behavior based on what month it is today
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        when (today.month) {
            Month.JANUARY -> check(today.month.number == 1)
            Month.FEBRUARY -> check(today.month.number == 2)
            Month.MARCH -> check(today.month.number == 3)
            Month.APRIL -> check(today.month.number == 4)
            Month.MAY -> check(today.month.number == 5)
            Month.JUNE -> check(today.month.number == 6)
            Month.JULY -> check(today.month.number == 7)
            Month.AUGUST -> check(today.month.number == 8)
            Month.SEPTEMBER -> check(today.month.number == 9)
            Month.OCTOBER -> check(today.month.number == 10)
            Month.NOVEMBER -> check(today.month.number == 11)
            Month.DECEMBER -> check(today.month.number == 12)
            else -> TODO("A new month was added to the calendar?")
        }
    }

    @Test
    fun number() {
        // Getting the number of a month
        check(Month.JANUARY.number == 1)
        check(Month.FEBRUARY.number == 2)
        // ...
        check(Month.DECEMBER.number == 12)
    }

    @Test
    fun constructorFunction() {
        // Constructing a Month using the constructor function
        check(Month(1) == Month.JANUARY)
        check(Month(2) == Month.FEBRUARY)
        // ...
        check(Month(12) == Month.DECEMBER)
        try {
            Month(0)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
