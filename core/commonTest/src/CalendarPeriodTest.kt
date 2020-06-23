/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlin.test.*
import kotlinx.datetime.*


class CalendarPeriodTest {

    @Test
    fun toStringConversion() {
        assertEquals("P1Y", CalendarPeriod(years = 1).toString())
        assertEquals("P1Y1M", CalendarPeriod(years = 1, months = 1).toString())
        assertEquals("P11M", CalendarPeriod(months = 11).toString())
        assertEquals("P14M", CalendarPeriod(months = 14).toString()) // TODO: normalize or not
        assertEquals("P10M5D", CalendarPeriod(months = 10, days = 5).toString())
        assertEquals("P1Y40D", CalendarPeriod(years = 1, days = 40).toString())

        assertEquals("PT1H", CalendarPeriod(hours = 1).toString())
        assertEquals("P0D", CalendarPeriod().toString())

        assertEquals("P1DT-1H", CalendarPeriod(days = 1, hours = -1).toString())
        assertEquals("-P1DT1H", CalendarPeriod(days = -1, hours = -1).toString())
        assertEquals("-P1M", CalendarPeriod(months = -1).toString())

        assertEquals("P-1Y-2M-3DT-4H-5M0.500000000S",
                CalendarPeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000).toString())
    }
}