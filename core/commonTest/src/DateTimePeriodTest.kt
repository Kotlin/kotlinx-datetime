/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlin.test.*
import kotlinx.datetime.*
import kotlin.time.*


class DateTimePeriodTest {

    @Test
    fun toStringConversion() {
        assertEquals("P1Y", DateTimePeriod(years = 1).toString())
        assertEquals("P1Y1M", DatePeriod(years = 1, months = 1).toString())
        assertEquals("P11M", DateTimePeriod(months = 11).toString())
        assertEquals("P14M", DateTimePeriod(months = 14).toString()) // TODO: normalize or not
        assertEquals("P10M5D", DateTimePeriod(months = 10, days = 5).toString())
        assertEquals("P1Y40D", DateTimePeriod(years = 1, days = 40).toString())

        assertEquals("PT1H", DateTimePeriod(hours = 1).toString())
        assertEquals("P0D", DateTimePeriod().toString())
        assertEquals("P0D", DatePeriod().toString())

        assertEquals("P1DT-1H", DateTimePeriod(days = 1, hours = -1).toString())
        assertEquals("-P1DT1H", DateTimePeriod(days = -1, hours = -1).toString())
        assertEquals("-P1M", DateTimePeriod(months = -1).toString())

        assertEquals("P-1Y-2M-3DT-4H-5M0.500000000S",
                DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000).toString())
    }

    @Test
    fun periodArithmetic() {
        val p1 = DateTimePeriod(years = 10)
        val p2 = DateTimePeriod(days = 3)
        val p3 = DateTimePeriod(hours = 2)
        val p4 = DateTimePeriod(hours = -2)

        val dp1 = DatePeriod(years = 1, months = 6)

        assertEquals(DateTimePeriod(years = 10, days = 3, hours = 2), p1 + p2 + p3)
        assertEquals(DatePeriod(years = 11, months = 6), dp1 + p1)
        assertEquals(DatePeriod(years = 2, months = 12), dp1 + dp1)
        assertEquals(DateTimePeriod(years = 1, months = 6, days = 3), p2 + dp1)

        val dp2 = dp1 + p3 + p4
        assertEquals(dp1, dp2)
        assertTrue(dp2 is DatePeriod)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun durationConversion() {
        val periodZero = Duration.ZERO.toDateTimePeriod()
        assertEquals(DateTimePeriod(), periodZero)
        assertEquals(DatePeriod(), periodZero)
        assertTrue(periodZero is DatePeriod)

        for ((period, duration) in listOf(
                DateTimePeriod(hours = 1) to 1.hours,
                DateTimePeriod(hours = 2) to 120.minutes,
                DateTimePeriod(minutes = 2, seconds = 30) to 150.seconds,
                DateTimePeriod(seconds = 2) to 2e9.nanoseconds
        )) {
            assertEquals(period, duration.toDateTimePeriod())
        }
    }
}