/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.random.*
import kotlin.test.*

class LocalDateTest {

    fun checkEquals(expected: LocalDate, actual: LocalDate) {
        assertEquals(expected, actual)
        assertEquals(expected.hashCode(), actual.hashCode())
        assertEquals(expected.toString(), actual.toString())
    }

    fun checkComponents(value: LocalDate, year: Int, month: Int, day: Int, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
        assertEquals(year, value.year)
        assertEquals(month, value.monthNumber)
        assertEquals(Month(month), value.month)
        assertEquals(day, value.dayOfMonth)
        if (dayOfWeek != null) assertEquals(dayOfWeek, value.dayOfWeek.number)
        if (dayOfYear != null) assertEquals(dayOfYear, value.dayOfYear)

        val fromComponents = LocalDate(year, month, day)
        checkEquals(fromComponents, value)
    }

    fun checkLocalDateTimePart(date: LocalDate, datetime: LocalDateTime) {
        checkEquals(date, datetime.date)
        checkComponents(date, datetime.year, datetime.monthNumber, datetime.dayOfMonth, datetime.dayOfWeek.number, datetime.dayOfYear)
    }

    @Test
    fun localDateParsing() {
        fun checkParsedComponents(value: String, year: Int, month: Int, day: Int, dayOfWeek: Int, dayOfYear: Int) {
            checkComponents(LocalDate.parse(value), year, month, day, dayOfWeek, dayOfYear)
        }
        checkParsedComponents("2019-10-01", 2019, 10, 1, 2, 274)
        checkParsedComponents("2016-02-29", 2016, 2, 29, 1, 60)
        checkParsedComponents("2017-10-01", 2017, 10, 1,  7, 274)
        assertFailsWith<Throwable> { LocalDate.parse("102017-10-01") }
        assertFailsWith<Throwable> { LocalDate.parse("2017--10-01") }
        assertFailsWith<Throwable> { LocalDate.parse("2017-+10-01") }
        assertFailsWith<Throwable> { LocalDate.parse("2017-10-+01") }
        assertFailsWith<Throwable> { LocalDate.parse("2017-10--01") }
    }

    @Test
    fun localDateTimePart() {
        val datetime = LocalDateTime.parse("2016-02-29T23:59")
        val date = LocalDate(2016, 2, 29)
        checkLocalDateTimePart(date, datetime)
    }

    @Test
    fun addComponents() {
        val startDate = LocalDate(2016, 2, 29)
        checkComponents(startDate + 1.calendarDays, 2016, 3, 1)
        checkComponents(startDate + 1.calendarYears, 2017, 2, 28)
        checkComponents(startDate + 4.calendarYears, 2020, 2, 29)

        checkComponents(LocalDate.parse("2016-01-31") + 1.calendarMonths, 2016, 2, 29)

        assertFailsWith<IllegalArgumentException> { startDate + CalendarPeriod(hours = 7) }
        assertFailsWith<IllegalArgumentException> { startDate.plus(7, CalendarUnit.HOUR) }
    }

    @Test
    fun tomorrow() {
        val today = Clock.System.todayAt(TimeZone.SYSTEM)

        val nextMonthPlusDay1 = today + 1.calendarMonths + 1.calendarDays
        val nextMonthPlusDay2 = today + (1.calendarMonths + 1.calendarDays)
        val nextMonthPlusDay3 = today + 1.calendarDays + 1.calendarMonths

    }

    @Test
    fun diffInvariant() {
        val origin = LocalDate(2001, 1, 1)

        repeat(1000) {
            val days1 = Random.nextInt(-3652..3652)
            val days2 = Random.nextInt(-3652..3652)
            val ldtBefore = origin + days1.calendarDays
            val ldtNow = origin + days2.calendarDays

            val diff = ldtNow - ldtBefore
            val ldtAfter = ldtBefore + diff
            if (ldtAfter != ldtNow)
                println("start: $ldtBefore, end: $ldtNow, start + diff: ${ldtBefore + diff}, diff: $diff")
        }
    }

    // based on threetenbp test for until()
    @Test
    fun until() {
        val data = listOf(
                Pair(Pair("2012-06-30", "2012-06-30"), Pair(CalendarUnit.DAY, 0)),
                Pair(Pair("2012-06-30", "2012-06-30"), Pair(CalendarUnit.WEEK, 0)),
                Pair(Pair("2012-06-30", "2012-06-30"), Pair(CalendarUnit.MONTH, 0)),
                Pair(Pair("2012-06-30", "2012-06-30"), Pair(CalendarUnit.YEAR, 0)),
                Pair(Pair("2012-06-30", "2012-07-01"), Pair(CalendarUnit.DAY, 1)),
                Pair(Pair("2012-06-30", "2012-07-01"), Pair(CalendarUnit.WEEK, 0)),
                Pair(Pair("2012-06-30", "2012-07-01"), Pair(CalendarUnit.MONTH, 0)),
                Pair(Pair("2012-06-30", "2012-07-01"), Pair(CalendarUnit.YEAR, 0)),
                Pair(Pair("2012-06-30", "2012-07-07"), Pair(CalendarUnit.DAY, 7)),
                Pair(Pair("2012-06-30", "2012-07-07"), Pair(CalendarUnit.WEEK, 1)),
                Pair(Pair("2012-06-30", "2012-07-07"), Pair(CalendarUnit.MONTH, 0)),
                Pair(Pair("2012-06-30", "2012-07-07"), Pair(CalendarUnit.YEAR, 0)),
                Pair(Pair("2012-06-30", "2012-07-29"), Pair(CalendarUnit.MONTH, 0)),
                Pair(Pair("2012-06-30", "2012-07-30"), Pair(CalendarUnit.MONTH, 1)),
                Pair(Pair("2012-06-30", "2012-07-31"), Pair(CalendarUnit.MONTH, 1)))
        for ((values, interval) in data) {
            val (v1, v2) = values
            val (unit, length) = interval
            val start = LocalDate.parse(v1)
            val end = LocalDate.parse(v2)
            assertEquals(length, start.until(end, unit), "$v2 - $v1 = $length($unit)")
            assertEquals(-length, end.until(start, unit), "$v1 - $v2 = -$length($unit)")
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (unit) {
                CalendarUnit.YEAR -> assertEquals(length, start.yearsUntil(end))
                CalendarUnit.MONTH -> assertEquals(length, start.monthsUntil(end))
                CalendarUnit.WEEK -> assertEquals(length, start.daysUntil(end) / 7)
                CalendarUnit.DAY -> assertEquals(length, start.daysUntil(end))
            }
        }

        val d1 = LocalDate(2012, 6, 21)
        val d2 = LocalDate(2012, 7, 21)

        for (unit in listOf(CalendarUnit.HOUR, CalendarUnit.MINUTE, CalendarUnit.SECOND, CalendarUnit.NANOSECOND))
            assertFailsWith<UnsupportedOperationException> { d1.until(d2, unit) }
    }

    @Test
    fun invalidDate() {
        assertFailsWith<Throwable> { LocalDate(2007, 2, 29) }
        LocalDate(2008, 2, 29)
        assertFailsWith<Throwable> { LocalDate(2007, 4, 31) }
        assertFailsWith<Throwable> { LocalDate(2007, 1, 0) }
        assertFailsWith<Throwable> { LocalDate(2007,1, 32) }
        assertFailsWith<Throwable> { LocalDate(Int.MIN_VALUE, 1, 1) }
        assertFailsWith<Throwable> { LocalDate(2007, 1, 32) }
        assertFailsWith<Throwable> { LocalDate(2007, 0, 1) }
        assertFailsWith<Throwable> { LocalDate(2007, 13, 1) }
    }

}
