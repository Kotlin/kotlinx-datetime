/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.*

class LocalDateTimeTest {



    @Test
    fun localDateTimeParsing() {
        fun checkParsedComponents(value: String, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
            checkComponents(value.toLocalDateTime(), year, month, day, hour, minute, second, nanosecond, dayOfWeek, dayOfYear)
        }
        checkParsedComponents("2019-10-01T18:43:15.100500", 2019, 10, 1, 18, 43, 15, 100500000, 2, 274)
        checkParsedComponents("2019-10-01T18:43:15", 2019, 10, 1, 18, 43, 15, 0, 2, 274)
        checkParsedComponents("2019-10-01T18:12", 2019, 10, 1, 18, 12, 0, 0, 2, 274)

        /* Based on the ThreeTenBp project.
         * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
         */
        checkParsedComponents("2008-07-05T02:01", 2008, 7, 5, 2, 1, 0, 0)
        checkParsedComponents("2007-12-31T23:59:01", 2007, 12, 31, 23, 59, 1, 0)
        checkParsedComponents("0999-12-31T23:59:59.990", 999, 12, 31, 23, 59, 59, 990000000)
        checkParsedComponents("-0001-01-02T23:59:59.999990", -1, 1, 2, 23, 59, 59, 999990000)
        checkParsedComponents("-2008-01-02T23:59:59.999999990", -2008, 1, 2, 23, 59, 59, 999999990)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun localDtToInstantConversion() {
        val ldt1 = "2019-10-01T18:43:15.100500".toLocalDateTime()
        val ldt2 = "2019-10-01T19:50:00.500600".toLocalDateTime()

        val diff = with(TimeZone.UTC) { ldt2.toInstant() - ldt1.toInstant() }
        assertEquals(1.hours + 7.minutes - 15.seconds + 400100.microseconds, diff)
    }

    @OptIn(ExperimentalTime::class)
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
    }

    @Test
    fun getCurrentHMS() {
        with(Instant.now().toLocalDateTime(TimeZone.SYSTEM)) {
            println("${hour}h ${minute}m")
        }
    }


    @OptIn(ExperimentalTime::class)
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

}

fun checkComponents(value: LocalDateTime, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
    assertEquals(year, value.year, "years")
    assertEquals(month, value.monthNumber, "months")
    assertEquals(Month(month), value.month)
    assertEquals(day, value.dayOfMonth, "days")
    assertEquals(hour, value.hour, "hours")
    assertEquals(minute, value.minute, "minutes")
    assertEquals(second, value.second, "seconds")
    assertEquals(nanosecond, value.nanosecond, "nanoseconds")
    if (dayOfWeek != null) assertEquals(dayOfWeek, value.dayOfWeek.number, "weekday")
    if (dayOfYear != null) assertEquals(dayOfYear, value.dayOfYear, "day of year")

    val fromComponents = LocalDateTime(year, month, day, hour, minute, second, nanosecond)
    checkEquals(fromComponents, value)
}

fun checkEquals(expected: LocalDateTime, actual: LocalDateTime) {
    assertEquals(expected, actual)
    assertEquals(expected.hashCode(), actual.hashCode())
    assertEquals(expected.toString(), actual.toString())
}

