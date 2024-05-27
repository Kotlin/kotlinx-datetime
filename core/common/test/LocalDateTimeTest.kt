/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

class LocalDateTimeTest {



    @Test
    fun localDateTimeParsing() {
        fun checkParsedComponents(value: String, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
            checkComponents(value.toLocalDateTime(), year, month, day, hour, minute, second, nanosecond, dayOfWeek, dayOfYear)
        }
        checkParsedComponents("2019-10-01T18:43:15.100500", 2019, 10, 1, 18, 43, 15, 100500000, 2, 274)
        checkParsedComponents("2019-10-01T18:43:15", 2019, 10, 1, 18, 43, 15, 0, 2, 274)
        checkParsedComponents("2019-10-01T18:12", 2019, 10, 1, 18, 12, 0, 0, 2, 274)

        assertFailsWith<DateTimeFormatException> { LocalDateTime.parse("x") }
        assertFailsWith<DateTimeFormatException> { "+1000000000-03-26T04:00:00".toLocalDateTime() }

        /* Based on the ThreeTenBp project.
         * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
         */
        checkParsedComponents("2008-07-05T02:01", 2008, 7, 5, 2, 1, 0, 0)
        checkParsedComponents("2007-12-31T23:59:01", 2007, 12, 31, 23, 59, 1, 0)
        checkParsedComponents("0999-12-31T23:59:59.990", 999, 12, 31, 23, 59, 59, 990000000)
        checkParsedComponents("-0001-01-02T23:59:59.999990", -1, 1, 2, 23, 59, 59, 999990000)
        checkParsedComponents("-2008-01-02T23:59:59.999999990", -2008, 1, 2, 23, 59, 59, 999999990)
    }

    @Test
    fun localDtToInstantConversion() {
        val ldt1 = "2019-10-01T18:43:15.100500".toLocalDateTime()
        val ldt2 = "2019-10-01T19:50:00.500600".toLocalDateTime()

        val diff = with(TimeZone.UTC) { ldt2.toInstant() - ldt1.toInstant() }
        assertEquals(with(Duration) { 1.hours + 7.minutes - 15.seconds + 400100.microseconds }, diff)
        assertFailsWith<DateTimeArithmeticException> { (Instant.MAX - 3.days).toLocalDateTime(TimeZone.UTC) }
        assertFailsWith<DateTimeArithmeticException> { (Instant.MIN + 6.hours).toLocalDateTime(TimeZone.UTC) }
    }

    @Test
    fun localDtToInstantConversionRespectsTimezones() {
        val ldt1 = "2011-03-26T04:00:00".toLocalDateTime()
        val ldt2 = "2011-03-27T04:00:00".toLocalDateTime()
        val diff = with(TimeZone.of("Europe/Moscow")) { ldt2.toInstant() - ldt1.toInstant() }
        assertEquals(23.hours, diff)
    }

    @Test
    fun instantToLocalConversion() {
        val instant = Instant.parse("2019-10-01T18:43:15.100500Z")
        val datetime = instant.toLocalDateTime(TimeZone.UTC)
        checkComponents(datetime, 2019, 10, 1, 18, 43, 15, 100500000)
        assertFailsWith<DateTimeArithmeticException> { Instant.MAX.toLocalDateTime(TimeZone.UTC) }
    }

    @Test
    fun getCurrentHMS() {
        with(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())) {
            println("${hour}h ${minute}m")
        }
    }

    @Test
    fun tomorrow() {
        val localFixed = LocalDateTime(2019, 1, 30, 0, 0, 0, 0)

//        println(localFixed + 1.calendarMonths + 1.calendarDays)
//        println(localFixed + (1.calendarMonths + 1.calendarDays))
//        println(localFixed + period { 1.days + 1.months })
//        println(localFixed + Period(days = -2, months = 1))
//
//        println(localFixed + 1.days.toCalendarPeriod())

        println(localFixed.dayOfWeek)
    }

    @Test
    fun constructFromParts() {
        val dt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val dt2 = LocalDateTime(dt.date, dt.time)
        assertEquals(dt2, dt)
        assertEquals(dt2.date, dt.date)
        assertEquals(dt2.time, dt.time)
    }

    /* Based on the ThreeTenBp project.
     * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
     */
    @Test
    fun strings() {
        assertEquals("2008-07-05T02:01", LocalDateTime(2008, 7, 5, 2, 1, 0, 0).toString())
        assertEquals("2007-12-31T23:59:01", LocalDateTime(2007, 12, 31, 23, 59, 1, 0).toString())
        assertEquals("0999-12-31T23:59:59.990", LocalDateTime(999, 12, 31, 23, 59, 59, 990000000).toString())
        assertEquals("-0001-01-02T23:59:59.999990", LocalDateTime(-1, 1, 2, 23, 59, 59, 999990000).toString())
        assertEquals("-2008-01-02T23:59:59.999999990", LocalDateTime(-2008, 1, 2, 23, 59, 59, 999999990).toString())
    }

    @Test
    fun constructInvalidDate() = checkInvalidDate { year, month, day -> LocalDateTime(year, month, day, 0, 0).date }

    @Test
    fun constructInvalidTime() {
        fun localTime(hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0): LocalDateTime =
            LocalDateTime(2020, Month.JANUARY, 1, hour, minute, second, nanosecond)
        localTime(23, 59)
        assertFailsWith<IllegalArgumentException> { localTime(-1, 0) }
        assertFailsWith<IllegalArgumentException> { localTime(24, 0) }
        assertFailsWith<IllegalArgumentException> { localTime(0, -1) }
        assertFailsWith<IllegalArgumentException> { localTime(0, 60) }
        assertFailsWith<IllegalArgumentException> { localTime(0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { localTime(0, 0, 60) }
        assertFailsWith<IllegalArgumentException> { localTime(0, 0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { localTime(0, 0, 0, 1_000_000_000) }
    }

}

fun checkComponents(value: LocalDateTime, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
    assertEquals(year, value.year, "years")
    assertEquals(month, value.month.number, "months")
    assertEquals(Month(month), value.month)
    assertEquals(day, value.day, "days")
    assertEquals(hour, value.hour, "hours")
    assertEquals(minute, value.minute, "minutes")
    assertEquals(second, value.second, "seconds")
    assertEquals(nanosecond, value.nanosecond, "nanoseconds")
    if (dayOfWeek != null) assertEquals(dayOfWeek, value.dayOfWeek.isoDayNumber, "weekday")
    if (dayOfYear != null) assertEquals(dayOfYear, value.dayOfYear, "day of year")

    val fromComponents = LocalDateTime(year, month, day, hour, minute, second, nanosecond)
    checkEquals(fromComponents, value)
}

fun checkEquals(expected: LocalDateTime, actual: LocalDateTime) {
    assertEquals(expected, actual)
    assertEquals(expected.hashCode(), actual.hashCode())
    assertEquals(expected.toString(), actual.toString())
}
