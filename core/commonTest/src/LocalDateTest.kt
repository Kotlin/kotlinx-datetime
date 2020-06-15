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

        assertFailsWith<UnsupportedOperationException> { startDate + CalendarPeriod(hours = 7) }
        assertFailsWith<UnsupportedOperationException> { startDate.plus(7, CalendarUnit.HOUR) }
    }

    @Test
    fun tomorrow() {
        val today = Instant.now().toLocalDateTime(TimeZone.SYSTEM).date

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

    @Test
    fun invalidTime() {
        fun localTime(hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0): LocalDateTime =
            LocalDateTime(2020, 1, 1, hour, minute, second, nanosecond)
        localTime(23, 59)
        assertFailsWith<Throwable> { localTime(-1, 0) }
        assertFailsWith<Throwable> { localTime(24, 0) }
        assertFailsWith<Throwable> { localTime(0, -1) }
        assertFailsWith<Throwable> { localTime(0, 60) }
        assertFailsWith<Throwable> { localTime(0, 0, -1) }
        assertFailsWith<Throwable> { localTime(0, 0, 60) }
        assertFailsWith<Throwable> { localTime(0, 0, 0, -1) }
        assertFailsWith<Throwable> { localTime(0, 0, 0, 1_000_000_000) }
    }
}
