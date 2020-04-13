/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlin.test.*

class ThreeTenBpLocalDateTest {
    @Test
    fun ofEpochDay() {
        val date_0000_01_01 = -678941 - 40587L
        assertEquals(LocalDate(1970, 1, 1), LocalDate.ofEpochDay(0))
        assertEquals(LocalDate(0, 1, 1), LocalDate.ofEpochDay(date_0000_01_01))
        assertEquals(LocalDate(-1, 12, 31), LocalDate.ofEpochDay(date_0000_01_01 - 1))
        var test = LocalDate(0, 1, 1)
        for (i in date_0000_01_01..699999) {
            assertEquals(test, LocalDate.ofEpochDay(i))
            test = next(test)
        }
        test = LocalDate(0, 1, 1)
        for (i in date_0000_01_01 downTo -2000000 + 1) {
            assertEquals(test, LocalDate.ofEpochDay(i))
            test = previous(test)
        }
    }

    @Test
    fun toEpochDay() {
        val date_0000_01_01 = -678941 - 40587.toLong()

        var test: LocalDate = LocalDate(0, 1, 1)
        for (i in date_0000_01_01..699999) {
            assertEquals(i, test.toEpochDay())
            test = next(test)
        }
        test = LocalDate(0, 1, 1)
        for (i in date_0000_01_01 downTo -2000000 + 1) {
            assertEquals(i, test.toEpochDay())
            test = previous(test)
        }

        assertEquals(-40587, LocalDate(1858, 11, 17).toEpochDay())
        assertEquals(-678575 - 40587, LocalDate(1, 1, 1).toEpochDay())
        assertEquals(49987 - 40587, LocalDate(1995, 9, 27).toEpochDay())
        assertEquals(0, LocalDate(1970, 1, 1).toEpochDay())
        assertEquals(-678942 - 40587, LocalDate(-1, 12, 31).toEpochDay())
    }

    @Test
    fun dayOfWeek() {
        var dow = DayOfWeek.MONDAY
        for (month in Month.values()) {
            val length = month.length(false)
            for (i in 1..length) {
                val d = LocalDate(2007, month.number, i)
                assertSame(d.dayOfWeek, dow)
                dow = DayOfWeek(dow.number % 7 + 1)
            }
        }
    }

    @Test
    fun getDayOfYear() {
        val dates = arrayOf(
            Triple(2008, 7, 5),
            Triple(2007, 7, 5),
            Triple(2006, 7, 5),
            Triple(2005, 7, 5),
            Triple(2004, 1, 1),
            Triple(-1, 1, 2))
        for ((y, m, d) in dates) {
            val a: LocalDate = LocalDate(y, m, d)
            var total = 0
            for (i in 1 until m) {
                total += Month(i).length(isLeapYear(y))
            }
            val doy = total + d
            assertEquals(a.dayOfYear, doy)
        }
    }

    @Test
    fun plusYears() {
        assertEquals(LocalDate(2008, 7, 15), LocalDate(2007, 7, 15).plusYears(1))
        assertEquals(LocalDate(2006, 7, 15), LocalDate(2007, 7, 15).plusYears(-1))
        assertEquals(LocalDate(2009, 2, 28), LocalDate(2008, 2, 29).plusYears(1))
    }


    @Test
    fun plusMonths() {
        val date = LocalDate(2007, 7, 15)
        assertEquals(LocalDate(2007, 8, 15), date.plusMonths(1))
        assertEquals(LocalDate(2009, 8, 15), date.plusMonths(25))
        assertEquals(LocalDate(2007, 6, 15), date.plusMonths(-1))
        assertEquals(LocalDate(2006, 12, 15), date.plusMonths(-7))
        assertEquals(LocalDate(2004, 12, 15), date.plusMonths(-31))
        assertEquals(LocalDate(2009, 2, 28), LocalDate(2008, 2, 29).plusMonths(12))
        assertEquals(LocalDate(2007, 4, 30), LocalDate(2007, 3, 31).plusMonths(1))
    }

    @Test
    fun plusWeeks() {
        val date = LocalDate(2007, 7, 15)
        assertEquals(LocalDate(2007, 7, 22), date.plusWeeks(1))
        assertEquals(LocalDate(2007, 9, 16), date.plusWeeks(9))
        assertEquals(date, LocalDate(2006, 7, 16).plusWeeks(52))
        assertEquals(LocalDate(2008, 7, 12), date.plusYears(-1).plusWeeks(104))
        assertEquals(LocalDate(2007, 7, 8), date.plusWeeks(-1))
        assertEquals(LocalDate(2006, 12, 31), date.plusWeeks(-28))
        assertEquals(LocalDate(2005, 7, 17), date.plusWeeks(-104))
    }

    @Test
    fun plusDays() {
        val date = LocalDate(2007, 7, 15)
        assertEquals(LocalDate(2007, 7, 16), date.plusDays(1))
        assertEquals(LocalDate(2007, 9, 15), date.plusDays(62))
        assertEquals(date, LocalDate(2006, 7, 14).plusDays(366))
        assertEquals(LocalDate(2008, 7, 15), date.plusYears(-1).plusDays(365 + 366))
        assertEquals(LocalDate(2007, 7, 14), date.plusDays(-1))
        assertEquals(LocalDate(2006, 12, 31), date.plusDays(-196))
        assertEquals(LocalDate(2005, 7, 15), date.plusDays(-730))
    }

    @Test
    fun until() {
        val data = arrayOf(
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
            assertEquals(length, start.until(end, unit).toInt(), "$v2 - $v1 = $length($unit)")
            assertEquals(-length, end.until(start, unit).toInt(), "$v1 - $v2 = -$length($unit)")
        }
    }

    @Test
    fun strings() {
        val data = arrayOf(
            Pair(LocalDate(2008, 7, 5), "2008-07-05"),
            Pair(LocalDate(2007, 12, 31), "2007-12-31"),
            Pair(LocalDate(999, 12, 31), "0999-12-31"),
            Pair(LocalDate(-1, 1, 2), "-0001-01-02"),
            Pair(LocalDate(9999, 12, 31), "9999-12-31"),
            Pair(LocalDate(-9999, 12, 31), "-9999-12-31"),
            Pair(LocalDate(10000, 1, 1), "+10000-01-01"),
            Pair(LocalDate(-10000, 1, 1), "-10000-01-01"),
            Pair(LocalDate(12345678, 1, 1), "+12345678-01-01"),
            Pair(LocalDate(-12345678, 1, 1), "-12345678-01-01"))
        for ((date, str) in data) {
            assertEquals(date, LocalDate.parse(str))
            assertEquals(str, date.toString())
        }
    }

    private fun next(localDate: LocalDate): LocalDate {
        var date = localDate
        val newDayOfMonth: Int = date.dayOfMonth + 1
        if (newDayOfMonth <= date.month.length(isLeapYear(date.year))) {
            return LocalDate(date.year, date.monthNumber, newDayOfMonth)
        }
        date = LocalDate(date.year, date.monthNumber, 1)
        if (date.month === Month.DECEMBER) {
            date = date.withYear(date.year + 1)
        }
        return LocalDate(date.year, date.monthNumber % 12 + 1, 1)
    }

    private fun previous(localDate: LocalDate): LocalDate {
        var date = localDate
        val newDayOfMonth: Int = date.dayOfMonth - 1
        if (newDayOfMonth > 0) {
            return LocalDate(date.year, date.monthNumber, newDayOfMonth)
        }
        date = LocalDate(date.year, (date.monthNumber + 10) % 12 + 1, date.dayOfMonth)
        if (date.month === Month.DECEMBER) {
            date = date.withYear(date.year - 1)
        }
        return LocalDate(date.year, date.monthNumber, date.month.length(isLeapYear(date.year)))
    }
}
