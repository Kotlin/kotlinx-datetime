/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlin.random.*
import kotlin.test.*

class LocalDateTest {

    private fun checkEquals(expected: LocalDate, actual: LocalDate) {
        assertEquals(expected, actual)
        assertEquals(expected.hashCode(), actual.hashCode())
        assertEquals(expected.toString(), actual.toString())
    }

    private fun checkComponents(value: LocalDate, year: Int, month: Int, day: Int, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
        assertEquals(year, value.year)
        assertEquals(month, value.monthNumber)
        assertEquals(Month(month), value.month)
        assertEquals(day, value.dayOfMonth)
        if (dayOfWeek != null) assertEquals(dayOfWeek, value.dayOfWeek.isoDayNumber)
        if (dayOfYear != null) assertEquals(dayOfYear, value.dayOfYear)

        val fromComponents = LocalDate(year, month, day)
        checkEquals(fromComponents, value)
    }

    private fun checkLocalDateTimePart(date: LocalDate, datetime: LocalDateTime) {
        checkEquals(date, datetime.date)
        checkComponents(date, datetime.year, datetime.monthNumber, datetime.dayOfMonth, datetime.dayOfWeek.isoDayNumber, datetime.dayOfYear)
    }

    @Test
    fun parseIsoString() {
        fun checkParsedComponents(value: String, year: Int, month: Int, day: Int, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
            checkComponents(LocalDate.parse(value), year, month, day, dayOfWeek, dayOfYear)
            assertEquals(value, LocalDate(year, month, day).toString())
        }
        checkParsedComponents("2019-10-01", 2019, 10, 1, 2, 274)
        checkParsedComponents("2016-02-29", 2016, 2, 29, 1, 60)
        checkParsedComponents("2017-10-01", 2017, 10, 1, 7, 274)
        assertInvalidFormat { LocalDate.parse("102017-10-01") }
        assertInvalidFormat { LocalDate.parse("2017--10-01") }
        assertInvalidFormat { LocalDate.parse("2017-+10-01") }
        assertInvalidFormat { LocalDate.parse("2017-10-+01") }
        assertInvalidFormat { LocalDate.parse("2017-10--01") }
        // this date is currently larger than the largest representable one any of the platforms:
        assertInvalidFormat { LocalDate.parse("+1000000000-10-01") }
        // threetenbp
        checkParsedComponents("2008-07-05", 2008, 7, 5)
        checkParsedComponents("2007-12-31", 2007, 12, 31)
        checkParsedComponents("0999-12-31", 999, 12, 31)
        checkParsedComponents("-0001-01-02", -1, 1, 2)
        checkParsedComponents("9999-12-31", 9999, 12, 31)
        checkParsedComponents("-9999-12-31", -9999, 12, 31)
        checkParsedComponents("+10000-01-01", 10000, 1, 1)
        checkParsedComponents("-10000-01-01", -10000, 1, 1)
        checkParsedComponents("+123456-01-01", 123456, 1, 1)
        checkParsedComponents("-123456-01-01", -123456, 1, 1)
    }

    @Test
    fun localDateTimePart() {
        val datetime = LocalDateTime.parse("2016-02-29T23:59")
        val date = LocalDate(2016, Month.FEBRUARY, 29)
        checkLocalDateTimePart(date, datetime)
    }

    @Test
    fun atTime() {
        val date = LocalDate(2016, Month.FEBRUARY, 29)
        val datetime = date.atTime(12, 1, 59)
        val datetimeWithLocalTime = date.atTime(LocalTime(12, 1, 59))
        assertEquals(datetime, datetimeWithLocalTime)
        checkComponents(datetime, 2016, 2, 29, 12, 1, 59)
        checkLocalDateTimePart(date, datetime)
    }

    @Test
    fun atStartOfDay() {
        val paris = TimeZone.of("Europe/Paris")
        val parisDate = LocalDate(2008, 6, 30)
        assertEquals(parisDate.atTime(0, 0).toInstant(paris),
                parisDate.atStartOfDayIn(paris), "paris")

        // TODO: Find another TZ transition that works in Windows
//        val gaza = TimeZone.of("Asia/Gaza")
//        val gazaDate = LocalDate(2007, 4, 1)
//        assertEquals(gazaDate.atTime(1, 0).toInstant(gaza),
//                gazaDate.atStartOfDayIn(gaza), "gaza")

        val fixed = TimeZone.of("UTC+14")
        val fixedDate = LocalDate(2007, 4, 1)
        assertEquals(fixedDate.atTime(0, 0).toInstant(fixed),
                fixedDate.atStartOfDayIn(fixed), "fixed")
    }

    @Test
    fun addComponents() {
        val startDate = LocalDate(2016, 2, 29)
        checkComponents(startDate.plus(1, DateTimeUnit.DAY), 2016, 3, 1)
        checkComponents(startDate.plus(1, DateTimeUnit.YEAR), 2017, 2, 28)
        checkComponents(startDate + DatePeriod(years = 4), 2020, 2, 29)
        assertEquals(startDate, startDate.plus(1, DateTimeUnit.DAY).minus(1, DateTimeUnit.DAY))
        assertEquals(startDate, startDate.plus(3, DateTimeUnit.DAY).minus(3, DateTimeUnit.DAY))
        assertEquals(startDate, startDate + DatePeriod(years = 4) - DatePeriod(years = 4))

        checkComponents(LocalDate.parse("2016-01-31") + DatePeriod(months = 1), 2016, 2, 29)

//        assertFailsWith<IllegalArgumentException> { startDate + DateTimePeriod(hours = 7) } // won't compile
//        assertFailsWith<IllegalArgumentException> { startDate.plus(7, ChronoUnit.MINUTE) } // won't compile
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun tomorrow() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val nextMonthPlusDay1 = today.plus(1, DateTimeUnit.MONTH).plus(1, DateTimeUnit.DAY)
        val nextMonthPlusDay2 = today + DatePeriod(months = 1, days = 1)
        val nextMonthPlusDay3 = today.plus(1, DateTimeUnit.DAY).plus(1, DateTimeUnit.MONTH)
    }

    @Test
    fun diffInvariant() {
        val origin = LocalDate(2001, 1, 1)
        assertEquals(origin, origin.plus(0, DateTimeUnit.DAY))
        assertEquals(origin, origin.plus(DatePeriod(days = 0)))

        repeat(STRESS_TEST_ITERATIONS) {
            val days1 = Random.nextInt(-3652..3652)
            val days2 = Random.nextInt(-3652..3652)
            val ldtBefore = origin + DatePeriod(days = days1)
            val ldtNow = origin.plus(days2, DateTimeUnit.DAY)

            val diff = ldtNow - ldtBefore
            val ldtAfter = ldtBefore + diff
            if (ldtAfter != ldtNow)
                println("start: $ldtBefore, end: $ldtNow, start + diff: ${ldtBefore + diff}, diff: $diff")
        }
    }

    // based on threetenbp test for until()
    @Test
    fun unitsUntil() {
        val data = listOf(
                Pair(Pair("2012-06-30", "2012-06-30"), Pair(DateTimeUnit.DAY, 0)),
                Pair(Pair("2012-06-30", "2012-06-30"), Pair(DateTimeUnit.WEEK, 0)),
                Pair(Pair("2012-06-30", "2012-06-30"), Pair(DateTimeUnit.MONTH, 0)),
                Pair(Pair("2012-06-30", "2012-06-30"), Pair(DateTimeUnit.YEAR, 0)),
                Pair(Pair("2012-06-30", "2012-07-01"), Pair(DateTimeUnit.DAY, 1)),
                Pair(Pair("2012-06-30", "2012-07-01"), Pair(DateTimeUnit.WEEK, 0)),
                Pair(Pair("2012-06-30", "2012-07-01"), Pair(DateTimeUnit.MONTH, 0)),
                Pair(Pair("2012-06-30", "2012-07-01"), Pair(DateTimeUnit.YEAR, 0)),
                Pair(Pair("2012-06-30", "2012-07-07"), Pair(DateTimeUnit.DAY, 7)),
                Pair(Pair("2012-06-30", "2012-07-07"), Pair(DateTimeUnit.WEEK, 1)),
                Pair(Pair("2012-06-30", "2012-07-07"), Pair(DateTimeUnit.MONTH, 0)),
                Pair(Pair("2012-06-30", "2012-07-07"), Pair(DateTimeUnit.YEAR, 0)),
                Pair(Pair("2012-06-30", "2012-07-29"), Pair(DateTimeUnit.MONTH, 0)),
                Pair(Pair("2012-06-30", "2012-07-30"), Pair(DateTimeUnit.MONTH, 1)),
                Pair(Pair("2012-06-30", "2012-07-31"), Pair(DateTimeUnit.MONTH, 1)))
        for ((values, interval) in data) {
            val (v1, v2) = values
            val (unit, length) = interval
            val start = LocalDate.parse(v1)
            val end = LocalDate.parse(v2)
            assertEquals(length, start.until(end, unit), "$v2 - $v1 = $length($unit)")
            assertEquals(-length, end.until(start, unit), "$v1 - $v2 = -$length($unit)")
            when (unit) {
                DateTimeUnit.YEAR -> assertEquals(length, start.yearsUntil(end))
                DateTimeUnit.MONTH -> assertEquals(length, start.monthsUntil(end))
                DateTimeUnit.WEEK -> assertEquals(length, start.daysUntil(end) / 7)
                DateTimeUnit.DAY -> assertEquals(length, start.daysUntil(end))
                else -> error(unit)
            }
        }

    }

    @Test
    fun unitMultiplesUntil() {
        val unit1000days = DateTimeUnit.DAY * 1000
        val unit4years = DateTimeUnit.YEAR * 4 // longer than 1000-DAY

        val diffDays = LocalDate.MIN.until(LocalDate.MAX, unit1000days)
        val diffYears = LocalDate.MIN.until(LocalDate.MAX, unit4years)
        assertTrue(diffDays in 0..Int.MAX_VALUE, "difference in $unit1000days should fit in Int, was $diffDays")
        assertTrue(diffDays > diffYears, "difference in $unit1000days unit must be more than in $unit4years unit, was $diffDays $diffYears")
    }

    @Test
    fun constructInvalidDate() = checkInvalidDate(::LocalDate)

    @Test
    fun unitArithmeticOutOfRange() {
        // LocalDate.plus(Long, DateTimeUnit)
        LocalDate.MAX.plus(-1, DateTimeUnit.DAY)
        LocalDate.MIN.plus(1, DateTimeUnit.DAY)
        // Arithmetic overflow
        assertArithmeticFails { LocalDate.MAX.plus(Long.MAX_VALUE, DateTimeUnit.YEAR) }
        assertArithmeticFails { LocalDate.MAX.plus(Long.MAX_VALUE - 2, DateTimeUnit.YEAR) }
        assertArithmeticFails { LocalDate.MIN.plus(Long.MIN_VALUE, DateTimeUnit.YEAR) }
        assertArithmeticFails { LocalDate.MIN.plus(Long.MIN_VALUE + 2, DateTimeUnit.YEAR) }
        assertArithmeticFails { LocalDate.MIN.plus(Long.MAX_VALUE, DateTimeUnit.DAY) }
        // Exceeding the boundaries of LocalDate
        assertArithmeticFails { LocalDate.MAX.plus(1, DateTimeUnit.YEAR) }
        assertArithmeticFails { LocalDate.MIN.plus(-1, DateTimeUnit.YEAR) }
    }

    @Test
    fun periodArithmeticOutOfRange() {
        // LocalDate.plus(DatePeriod)
        LocalDate.MAX.plus(DatePeriod(years = -2, months = 12, days = 31))
        // Exceeding the boundaries in result
        assertArithmeticFails {
            LocalDate.MAX.plus(DatePeriod(years = -2, months = 24, days = 1))
        }
        // Exceeding the boundaries in intermediate computations
        assertArithmeticFails {
            LocalDate.MAX.plus(DatePeriod(years = -2, months = 25, days = -1000))
        }
    }

    @Test
    fun unitsUntilClamping() {
        val diffInYears = LocalDate.MIN.until(LocalDate.MAX, DateTimeUnit.YEAR)
        if (diffInYears > Int.MAX_VALUE / 365) {
            assertEquals(Int.MAX_VALUE, LocalDate.MIN.until(LocalDate.MAX, DateTimeUnit.DAY))
            assertEquals(Int.MIN_VALUE, LocalDate.MAX.until(LocalDate.MIN, DateTimeUnit.DAY))
        }
    }
    @Test
    fun fromEpochDays() {
        /** This test uses [LocalDate.next] and [LocalDate.previous] and not [LocalDate.plus] because, on Native,
         * [LocalDate.plus] is implemented via [LocalDate.toEpochDays]/[LocalDate.fromEpochDays], and so it's better to
         * test those independently. */
        if (LocalDate.fromEpochDays(0).daysUntil(LocalDate.MIN) > Int.MIN_VALUE) {
            assertEquals(LocalDate.MIN, LocalDate.fromEpochDays(LocalDate.MIN.toEpochDays()))
            assertFailsWith<IllegalArgumentException> { LocalDate.fromEpochDays(LocalDate.MIN.toEpochDays() - 1) }
            assertFailsWith<IllegalArgumentException> { LocalDate.fromEpochDays(Int.MIN_VALUE) }
        }
        if (LocalDate.fromEpochDays(0).daysUntil(LocalDate.MAX) < Int.MAX_VALUE) {
            assertEquals(LocalDate.MAX, LocalDate.fromEpochDays(LocalDate.MAX.toEpochDays()))
            assertFailsWith<IllegalArgumentException> { LocalDate.fromEpochDays(LocalDate.MAX.toEpochDays() + 1) }
            assertFailsWith<IllegalArgumentException> { LocalDate.fromEpochDays(Int.MAX_VALUE) }
        }
        val eraBeginning = -678941 - 40587
        assertEquals(LocalDate(1970, 1, 1), LocalDate.fromEpochDays(0))
        assertEquals(LocalDate(0, 1, 1), LocalDate.fromEpochDays(eraBeginning))
        assertEquals(LocalDate(-1, 12, 31), LocalDate.fromEpochDays(eraBeginning - 1))
        var test = LocalDate(0, 1, 1)
        for (i in eraBeginning..699999) {
            assertEquals(test, LocalDate.fromEpochDays(i))
            test = test.next
        }
        test = LocalDate(0, 1, 1)
        for (i in eraBeginning downTo -2000000 + 1) {
            assertEquals(test, LocalDate.fromEpochDays(i))
            test = test.previous
        }
    }

    // threetenbp
    @Test
    fun toEpochDays() {
        /** This test uses [LocalDate.next] and [LocalDate.previous] and not [LocalDate.plus] because, on Native,
         * [LocalDate.plus] is implemented via [LocalDate.toEpochDays]/[LocalDate.fromEpochDays], and so it's better to
         * test those independently. */
        val startOfEra = -678941 - 40587
        var date = LocalDate(0, 1, 1)
        for (i in startOfEra..699999) {
            assertEquals(i, date.toEpochDays())
            date = date.next
        }
        date = LocalDate(0, 1, 1)
        for (i in startOfEra downTo -2000000 + 1) {
            assertEquals(i, date.toEpochDays())
            date = date.previous
        }
        assertEquals(-40587, LocalDate(1858, 11, 17).toEpochDays())
        assertEquals(-678575 - 40587, LocalDate(1, 1, 1).toEpochDays())
        assertEquals(49987 - 40587, LocalDate(1995, 9, 27).toEpochDays())
        assertEquals(0, LocalDate(1970, 1, 1).toEpochDays())
        assertEquals(-678942 - 40587, LocalDate(-1, 12, 31).toEpochDays())
    }
}

fun checkInvalidDate(constructor: (year: Int, month: Int, day: Int) -> LocalDate) {
    assertFailsWith<IllegalArgumentException> { constructor(2007, 2, 29) }
    assertEquals(29, constructor(2008, 2, 29).dayOfMonth)
    assertFailsWith<IllegalArgumentException> { constructor(2007, 4, 31) }
    assertFailsWith<IllegalArgumentException> { constructor(2007, 1, 0) }
    assertFailsWith<IllegalArgumentException> { constructor(2007,1, 32) }
    assertFailsWith<IllegalArgumentException> { constructor(Int.MIN_VALUE, 1, 1) }
    assertFailsWith<IllegalArgumentException> { constructor(2007, 1, 32) }
    assertFailsWith<IllegalArgumentException> { constructor(2007, 0, 1) }
    assertFailsWith<IllegalArgumentException> { constructor(2007, 13, 1) }
}

private val LocalDate.next: LocalDate get() =
    if (dayOfMonth != monthNumber.monthLength(isLeapYear(year))) {
        LocalDate(year, monthNumber, dayOfMonth + 1)
    } else if (monthNumber != 12) {
        LocalDate(year, monthNumber + 1, 1)
    } else {
        LocalDate(year + 1, 1, 1)
    }

private val LocalDate.previous: LocalDate get() =
    if (dayOfMonth != 1) {
        LocalDate(year, monthNumber, dayOfMonth - 1)
    } else if (monthNumber != 1) {
        val newMonthNumber = monthNumber - 1
        LocalDate(year, newMonthNumber, newMonthNumber.monthLength(isLeapYear(year)))
    } else {
        LocalDate(year - 1, 12, 31)
    }
