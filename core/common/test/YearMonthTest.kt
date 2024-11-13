/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class YearMonthTest {

    private fun checkEquals(expected: YearMonth, actual: YearMonth) {
        assertEquals(expected, actual)
        assertEquals(expected.hashCode(), actual.hashCode())
        assertEquals(expected.toString(), actual.toString())
    }

    private fun checkComponents(value: YearMonth, year: Int, month: Int) {
        assertEquals(year, value.year)
        assertEquals(month, value.monthNumber)
        assertEquals(Month(month), value.month)

        val fromComponents = YearMonth(year, month)
        checkEquals(fromComponents, value)
    }

    private fun checkLocalDatePart(yearMonth: YearMonth, date: LocalDate) {
        checkEquals(yearMonth, date.yearMonth)
        checkComponents(yearMonth, date.year, date.month.number)
    }

    @Test
    fun parseIsoString() {
        fun checkParsedComponents(value: String, year: Int, month: Int) {
            checkComponents(YearMonth.parse(value), year, month)
            assertEquals(value, YearMonth(year, month).toString())
        }
        checkParsedComponents("2019-10", 2019, 10)
        checkParsedComponents("2016-02", 2016, 2)
        checkParsedComponents("2017-10", 2017, 10)
        assertInvalidFormat { LocalDate.parse("102017-10") }
        assertInvalidFormat { LocalDate.parse("2017--10") }
        assertInvalidFormat { LocalDate.parse("2017-+10") }
        // this date is currently larger than the largest representable one any of the platforms:
        assertInvalidFormat { LocalDate.parse("+1000000000-10") }
        // threetenbp
        checkParsedComponents("2008-07", 2008, 7)
        checkParsedComponents("2007-12", 2007, 12)
        checkParsedComponents("0999-12", 999, 12)
        checkParsedComponents("-0001-01", -1, 1)
        checkParsedComponents("9999-12", 9999, 12)
        checkParsedComponents("-9999-12", -9999, 12)
        checkParsedComponents("+10000-01", 10000, 1)
        checkParsedComponents("-10000-01", -10000, 1)
        checkParsedComponents("+123456-01", 123456, 1)
        checkParsedComponents("-123456-01", -123456, 1)
        for (i in 1..30) {
            checkComponents(YearMonth.parse("+${"0".repeat(i)}2024-01"), 2024, 1)
            checkComponents(YearMonth.parse("-${"0".repeat(i)}2024-01"), -2024, 1)
        }
    }

    @Test
    fun localDatePart() {
        val date = LocalDate(2016, Month.FEBRUARY, 29)
        checkLocalDatePart(YearMonth(2016, 2), date)
    }

    @Test
    fun onDay() {
        assertEquals(LocalDate(2016, 2, 29), YearMonth(2016, Month.FEBRUARY).onDay(29))
        assertFailsWith<IllegalArgumentException> { YearMonth(2016, Month.FEBRUARY).onDay(30) }
        assertFailsWith<IllegalArgumentException> { YearMonth(2016, Month.FEBRUARY).onDay(0) }
        assertFailsWith<IllegalArgumentException> { YearMonth(2016, Month.FEBRUARY).onDay(-1) }
        assertFailsWith<IllegalArgumentException> { YearMonth(2015, Month.FEBRUARY).onDay(29) }
    }

    @Test
    fun addComponents() {
        val start = YearMonth(2016, 2)
        checkComponents(start.plus(1, DateTimeUnit.MONTH), 2016, 3)
        checkComponents(start.plus(1, DateTimeUnit.YEAR), 2017, 2)
        assertEquals(start, start.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.MONTH))
        assertEquals(start, start.plus(3, DateTimeUnit.MONTH).minus(3, DateTimeUnit.MONTH))
    }

    @Test
    fun unitsUntil() {
        val data = listOf<Triple<Pair<String, String>, Long, Int>>(
            Triple(Pair("2012-06", "2012-06"), 0, 0),
            Triple(Pair("2012-06", "2012-07"), 1, 0),
            Triple(Pair("2012-06", "2013-07"), 13, 1),
            Triple(Pair("-0001-01", "0001-01"), 24, 2),
            Triple(Pair("-10000-01", "+10000-01"), 240000, 20000),
        )
        for ((values, months, years) in data) {
            val (v1, v2) = values
            val start = YearMonth.parse(v1)
            val end = YearMonth.parse(v2)
            assertEquals(months, start.until(end, DateTimeUnit.MONTH))
            assertEquals(-months, end.until(start, DateTimeUnit.MONTH))
            assertEquals(years.toLong(), start.until(end, DateTimeUnit.YEAR))
            assertEquals(-years.toLong(), end.until(start, DateTimeUnit.YEAR))
            if (months <= Int.MAX_VALUE) {
                assertEquals(months.toInt(), start.monthsUntil(end))
                assertEquals(-months.toInt(), end.monthsUntil(start))
            }
            assertEquals(years, start.yearsUntil(end))
            assertEquals(-years, end.yearsUntil(start))
        }

    }

    @Test
    fun unitMultiplesUntil() {
        val start = YearMonth(2000, 1)
        val end = YearMonth(2030, 3)
        val yearsBetween = start.until(end, DateTimeUnit.MONTH * 12)
        assertEquals(30, yearsBetween)
        assertEquals(15, start.until(end, DateTimeUnit.MONTH * 24))
        assertEquals(10, start.until(end, DateTimeUnit.MONTH * 36))
        assertEquals(5, start.until(end, DateTimeUnit.MONTH * 72))
        assertEquals(2, start.until(end, DateTimeUnit.MONTH * 180))
        assertEquals(1, start.until(end, DateTimeUnit.MONTH * 360))
        val monthsBetween = start.until(end, DateTimeUnit.MONTH)
        assertEquals(yearsBetween * 12 + 2, monthsBetween) // 362
        assertEquals(181, start.until(end, DateTimeUnit.MONTH * 2))
        assertEquals(120, start.until(end, DateTimeUnit.MONTH * 3))
        assertEquals(90, start.until(end, DateTimeUnit.MONTH * 4))
        assertEquals(72, start.until(end, DateTimeUnit.MONTH * 5))
        assertEquals(60, start.until(end, DateTimeUnit.MONTH * 6))
    }

    @Test
    fun constructInvalidYearMonth() {
        assertFailsWith<IllegalArgumentException> { YearMonth(Int.MIN_VALUE, 1) }
        assertFailsWith<IllegalArgumentException> { YearMonth(2007, 0) }
        assertFailsWith<IllegalArgumentException> { YearMonth(2007, 13) }
    }

    @Test
    fun unitArithmeticOutOfRange() {
        maxYearMonth.plus(-1, DateTimeUnit.MONTH)
        minYearMonth.plus(1, DateTimeUnit.MONTH)
        // Arithmetic overflow
        assertArithmeticFails { maxYearMonth.plus(Long.MAX_VALUE, DateTimeUnit.YEAR) }
        assertArithmeticFails { maxYearMonth.plus(Long.MAX_VALUE - 2, DateTimeUnit.YEAR) }
        assertArithmeticFails { minYearMonth.plus(Long.MIN_VALUE, DateTimeUnit.YEAR) }
        assertArithmeticFails { minYearMonth.plus(Long.MIN_VALUE + 2, DateTimeUnit.YEAR) }
        assertArithmeticFails { minYearMonth.plus(Long.MAX_VALUE, DateTimeUnit.MONTH) }
        assertArithmeticFails { maxYearMonth.plus(Long.MIN_VALUE, DateTimeUnit.MONTH) }
        // Exceeding the boundaries of LocalDate
        assertArithmeticFails { maxYearMonth.plus(1, DateTimeUnit.YEAR) }
        assertArithmeticFails { minYearMonth.plus(-1, DateTimeUnit.YEAR) }
    }

    @Test
    fun monthsUntilClamping() {
        val preciseDifference = minYearMonth.until(maxYearMonth, DateTimeUnit.MONTH)
        // TODO: remove the condition after https://github.com/Kotlin/kotlinx-datetime/pull/453
        if (preciseDifference > Int.MAX_VALUE) {
            assertEquals(Int.MAX_VALUE, minYearMonth.monthsUntil(maxYearMonth))
            assertEquals(Int.MIN_VALUE, maxYearMonth.monthsUntil(minYearMonth))
        }
    }

    @Test
    fun firstAndLastDay() {
        fun test(year: Int, month: Int) {
            val yearMonth = YearMonth(year, month)
            assertEquals(LocalDate(year, month, 1), yearMonth.firstDay)
            assertEquals(LocalDate(year, month, yearMonth.numberOfDays), yearMonth.lastDay)
            assertEquals(yearMonth.plusMonth().firstDay, yearMonth.lastDay.plus(1, DateTimeUnit.DAY))
        }
        for (month in 1..12) {
            for (year in 2000..2005) {
                test(year, month)
            }
            for (year in -2005..-2000) {
                test(year, month)
            }
        }
    }

    private val minYearMonth = LocalDate.MIN.yearMonth
    private val maxYearMonth = LocalDate.MAX.yearMonth
}
