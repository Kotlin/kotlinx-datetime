/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DateTimePeriodTest {

    @Test
    fun normalization() {
        assertPeriodComponents(DateTimePeriod(years = 1) as DatePeriod, years = 1)
        assertPeriodComponents(DateTimePeriod(years = 1, months = 1) as DatePeriod, years = 1, months = 1)
        assertPeriodComponents(DateTimePeriod(years = 1, months = -1) as DatePeriod, months = 11)
        assertPeriodComponents(DateTimePeriod(years = -1, months = 1) as DatePeriod, months = -11)
        assertPeriodComponents(DateTimePeriod(years = -1, months = -1) as DatePeriod, years = -1, months = -1)
        assertPeriodComponents(DateTimePeriod(months = 11) as DatePeriod, months = 11)
        assertPeriodComponents(DateTimePeriod(months = 14) as DatePeriod, years = 1, months = 2)
        assertPeriodComponents(DateTimePeriod(months = -14) as DatePeriod, years = -1, months = -2)
        assertPeriodComponents(DateTimePeriod(months = 10, days = 5) as DatePeriod, months = 10, days = 5)
        assertPeriodComponents(DateTimePeriod(years = 1, days = 40) as DatePeriod, years = 1, days = 40)
        assertPeriodComponents(DateTimePeriod(years = 1, days = -40) as DatePeriod, years = 1, days = -40)
        assertPeriodComponents(DateTimePeriod(days = 5) as DatePeriod, days = 5)

        assertPeriodComponents(DateTimePeriod(hours = 3), hours = 3)
        assertPeriodComponents(DateTimePeriod(hours = 1, minutes = 120), hours = 3)
        assertPeriodComponents(DateTimePeriod(hours = 1, minutes = 119, seconds = 60), hours = 3)
        assertPeriodComponents(DateTimePeriod(hours = 1, minutes = 119, seconds = 59, nanoseconds = 1_000_000_000), hours = 3)
        assertPeriodComponents(DateTimePeriod(hours = 1, minutes = 121, seconds = -59, nanoseconds = -1_000_000_000), hours = 3)
        assertPeriodComponents(DateTimePeriod())
        assertPeriodComponents(DatePeriod())

        assertPeriodComponents(DateTimePeriod(days = 1, hours = -1), days = 1, hours = -1)
        assertPeriodComponents(DateTimePeriod(days = -1, hours = -1), days = -1, hours = -1)

        assertPeriodComponents(DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000),
            years = -1, months = -2, days = -3, hours = -4, minutes = -4, seconds = -59, nanoseconds = -500_000_000)

        assertPeriodComponents(DateTimePeriod(nanoseconds = 999_999_999_999_999L), hours = 277, minutes = 46, seconds = 39, nanoseconds = 999_999_999)
        assertPeriodComponents(DateTimePeriod(nanoseconds = -999_999_999_999_999L), hours = -277, minutes = -46, seconds = -39, nanoseconds = -999_999_999)
    }

    @Test
    fun toStringConversion() {
        assertEquals("P1Y", DateTimePeriod(years = 1).toString())
        assertEquals("P1Y1M", DatePeriod(years = 1, months = 1).toString())
        assertEquals("P11M", DateTimePeriod(months = 11).toString())
        assertEquals("P1Y2M", DateTimePeriod(months = 14).toString())
        assertEquals("P10M5D", DateTimePeriod(months = 10, days = 5).toString())
        assertEquals("P1Y40D", DateTimePeriod(years = 1, days = 40).toString())

        assertEquals("PT1H", DateTimePeriod(hours = 1).toString())
        assertEquals("P0D", DateTimePeriod().toString())
        assertEquals("P0D", DatePeriod().toString())

        assertEquals("P1DT-1H", DateTimePeriod(days = 1, hours = -1).toString())
        assertEquals("-P1DT1H", DateTimePeriod(days = -1, hours = -1).toString())
        assertEquals("-P1M", DateTimePeriod(months = -1).toString())

        assertEquals("-P1Y2M3DT4H4M59.500000000S",
                DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000).toString())

        assertEquals("PT277H46M39.999999999S", DateTimePeriod(nanoseconds = 999_999_999_999_999L).toString())
        assertEquals("PT0.999999999S", DateTimePeriod(seconds = 1, nanoseconds = -1L).toString())
        assertEquals("-PT0.000000001S", DateTimePeriod(nanoseconds = -1L).toString())
        assertEquals("P1DT-0.000000001S", DateTimePeriod(days = 1, nanoseconds = -1L).toString())
        assertEquals("-PT0.999999999S", DateTimePeriod(seconds = -1, nanoseconds = 1L).toString())
        assertEquals("P1DT-0.999999999S", DateTimePeriod(days = 1, seconds = -1, nanoseconds = 1L).toString())
    }

    private inline fun <reified T : IllegalArgumentException> assertFailsToParse(text: String): T {
        assertNull(DateTimePeriod.parseOrNull(text))
        return assertFailsWith<T> { DateTimePeriod.parse(text) }
    }

    @Test
    fun parseIsoString() {
        fun assertParsesTo(value: DateTimePeriod, string: String) {
            assertEquals(value, DateTimePeriod.parse(string))
            assertEquals(value, DateTimePeriod.parseOrNull(string))
        }

        assertParsesTo(DateTimePeriod(years = 1), "P1Y")
        assertParsesTo(DatePeriod(years = 1, months = 1), "P1Y1M")
        assertParsesTo(DateTimePeriod(months = 11), "P11M")
        assertParsesTo(DateTimePeriod(months = 10, days = 5), "P10M5D")
        assertParsesTo(DateTimePeriod(years = 1, days = 40), "P1Y40D")

        // Successful parsing tests
        assertParsesTo(DateTimePeriod(months = 14), "P14M")
        assertPeriodComponents(DateTimePeriod.parse("P14M") as DatePeriod, years = 1, months = 2)
        assertParsesTo(DateTimePeriod(hours = 1), "PT1H")
        assertParsesTo(DateTimePeriod(), "P0D")
        assertParsesTo(DatePeriod(), "P0D")
        assertParsesTo(DateTimePeriod(days = 1, hours = -1), "P1DT-1H")
        assertParsesTo(DateTimePeriod(days = -1, hours = -1), "-P1DT1H")
        assertParsesTo(DateTimePeriod(months = -1), "-P1M")
        assertParsesTo(DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000),
            "P-1Y-2M-3DT-4H-5M0.500000000S")
        assertPeriodComponents(DateTimePeriod.parse("P-1Y-2M-3DT-4H-5M0.500000000S"),
            years = -1, months = -2, days = -3, hours = -4, minutes = -4, seconds = -59, nanoseconds = -500_000_000)
        assertParsesTo(DateTimePeriod(nanoseconds = 999_999_999_999_999L), "PT277H46M39.999999999S")
        assertPeriodComponents(DateTimePeriod.parse("PT277H46M39.999999999S"),
            hours = 277, minutes = 46, seconds = 39, nanoseconds = 999_999_999)
        assertParsesTo(DateTimePeriod(nanoseconds = 999_999_999), "PT0.999999999S")
        assertParsesTo(DateTimePeriod(nanoseconds = -1), "-PT0.000000001S")
        assertParsesTo(DateTimePeriod(days = 1, nanoseconds = -1), "P1DT-0.000000001S")
        assertParsesTo(DateTimePeriod(nanoseconds = -999_999_999), "-PT0.999999999S")
        assertParsesTo(DateTimePeriod(days = 1, nanoseconds = -999_999_999), "P1DT-0.999999999S")
        assertPeriodComponents(DateTimePeriod.parse("P1DT-0.999999999S"), days = 1, nanoseconds = -999_999_999)
        assertParsesTo(DatePeriod(days = 1), "P000000000000000000000000000001D")

        // Failing parsing tests - IllegalArgumentException
        assertFailsToParse<IllegalArgumentException>("P")
        // overflow of `Int.MAX_VALUE` years
        assertFailsToParse<IllegalArgumentException>("P768614336404564651Y")
        assertFailsToParse<IllegalArgumentException>("P1Y9223372036854775805M")
        assertFailsToParse<IllegalArgumentException>("PT+-2H")

        // Failing parsing tests - DateTimeFormatException
        assertFailsToParse<DateTimeFormatException>("P3000000000Y")
        assertFailsToParse<DateTimeFormatException>("P3000000000M")
        assertFailsToParse<DateTimeFormatException>("P3000000000D")
        assertFailsToParse<DateTimeFormatException>("P3000000000H")
        assertFailsToParse<DateTimeFormatException>("P3000000000M")
        assertFailsToParse<DateTimeFormatException>("P3000000000S")

        // wrong order of signifiers
        assertFailsToParse<DateTimeFormatException>("P1Y2D3M")
        assertFailsToParse<DateTimeFormatException>("P0DT1M2H")

        // loss of precision in fractional seconds
        assertFailsToParse<DateTimeFormatException>("P0.000000000001S")

        // non-zero time components when parsing DatePeriod
        assertFailsWith<IllegalArgumentException> { DatePeriod.parse("P1DT1H") }
        DatePeriod.parse("P1DT0H")
    }

    @Test
    fun periodArithmetic() {
        val p1 = DateTimePeriod(years = 10)
        val p2 = DateTimePeriod(days = 3)
        val p3 = DateTimePeriod(hours = 2)
        val p4 = DateTimePeriod(hours = -2)

        val dp1 = DatePeriod(years = 1, months = 6)

        @Suppress("DEPRECATION")
        run {
            assertEquals(DateTimePeriod(years = 10, days = 3, hours = 2), p1 + p2 + p3)
            assertEquals(DatePeriod(years = 11, months = 6), dp1 + p1)
            assertEquals(DatePeriod(years = 2, months = 12), dp1 + dp1)
            assertEquals(DateTimePeriod(years = 1, months = 6, days = 3), p2 + dp1)

            val dp2 = dp1 + p3 + p4
            assertEquals(dp1, dp2)
            assertTrue(dp2 is DatePeriod)
        }
    }

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

    private fun assertPeriodComponents(period: DateTimePeriod,
                                       years: Int = 0, months: Int = 0, days: Int = 0,
                                       hours: Int = 0, minutes: Int = 0, seconds: Int = 0, nanoseconds: Int = 0) {
        assertEquals(years, period.years)
        assertEquals(months, period.months)
        assertEquals(days, period.days)
        assertEquals(hours, period.hours)
        assertEquals(minutes, period.minutes)
        assertEquals(seconds, period.seconds)
        assertEquals(nanoseconds, period.nanoseconds)
    }
}
