/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.Clock
import kotlin.math.min
import kotlin.test.*

class LocalTimeTest {
    
    @Test
    fun localTimeParsing() {
        fun checkParsedComponents(value: String, hour: Int, minute: Int, second: Int, nanosecond: Int) {
            checkComponents(value.toLocalTime(), hour, minute, second, nanosecond)
        }
        checkParsedComponents("18:43:15.100500", 18, 43, 15, 100500000)
        checkParsedComponents("18:43:15", 18, 43, 15, 0)
        checkParsedComponents("18:12", 18, 12, 0, 0)

        assertFailsWith<DateTimeFormatException> { LocalTime.parse("x") }
        assertFailsWith<DateTimeFormatException> { "+10000000004:00:00".toLocalDateTime() }

        /* Based on the ThreeTenBp project.
         * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
         */
        checkParsedComponents("02:01", 2, 1, 0, 0)
        checkParsedComponents("23:59:01", 23, 59, 1, 0)
        checkParsedComponents("23:59:59.990", 23, 59, 59, 990000000)
        checkParsedComponents("23:59:59.999990", 23, 59, 59, 999990000)
        checkParsedComponents("23:59:59.999999990", 23, 59, 59, 999999990)
    }

    /* Based on the ThreeTenBp project.
     * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
     */
    @Test
    fun strings() {
        val data = arrayOf(
            Pair(LocalTime(0, 0, 0, 0), "00:00"),
            Pair(LocalTime(1, 0, 0, 0), "01:00"),
            Pair(LocalTime(23, 0, 0, 0), "23:00"),
            Pair(LocalTime(0, 1, 0, 0), "00:01"),
            Pair(LocalTime(12, 30, 0, 0), "12:30"),
            Pair(LocalTime(23, 59, 0, 0), "23:59"),
            Pair(LocalTime(0, 0, 1, 0), "00:00:01"),
            Pair(LocalTime(0, 0, 59, 0), "00:00:59"),
            Pair(LocalTime(0, 0, 0, 100000000), "00:00:00.100"),
            Pair(LocalTime(0, 0, 0, 10000000), "00:00:00.010"),
            Pair(LocalTime(0, 0, 0, 1000000), "00:00:00.001"),
            Pair(LocalTime(0, 0, 0, 100000), "00:00:00.000100"),
            Pair(LocalTime(0, 0, 0, 10000), "00:00:00.000010"),
            Pair(LocalTime(0, 0, 0, 1000), "00:00:00.000001"),
            Pair(LocalTime(0, 0, 0, 100), "00:00:00.000000100"),
            Pair(LocalTime(0, 0, 0, 10), "00:00:00.000000010"),
            Pair(LocalTime(0, 0, 0, 1), "00:00:00.000000001"),
            Pair(LocalTime(0, 0, 0, 999999999), "00:00:00.999999999"),
            Pair(LocalTime(0, 0, 0, 99999999), "00:00:00.099999999"),
            Pair(LocalTime(0, 0, 0, 9999999), "00:00:00.009999999"),
            Pair(LocalTime(0, 0, 0, 999999), "00:00:00.000999999"),
            Pair(LocalTime(0, 0, 0, 99999), "00:00:00.000099999"),
            Pair(LocalTime(0, 0, 0, 9999), "00:00:00.000009999"),
            Pair(LocalTime(0, 0, 0, 999), "00:00:00.000000999"),
            Pair(LocalTime(0, 0, 0, 99), "00:00:00.000000099"),
            Pair(LocalTime(0, 0, 0, 9), "00:00:00.000000009"))
        for ((time, str) in data) {
            assertEquals(str, time.toString())
            assertEquals(time, LocalTime.parse(str))
        }
    }

    @Test
    fun constructInvalidTime() {
        LocalTime(23, 59)
        assertFailsWith<IllegalArgumentException> { LocalTime(-1, 0) }
        assertFailsWith<IllegalArgumentException> { LocalTime(24, 0) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, -1) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 60) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 0, 60) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 0, 0, 1_000_000_000) }
    }

    @Test
    fun atDate() {
        val time = LocalTime(12, 1, 59)
        val datetime = time.atDate(2016, 2, 29)
        val datetimeWithLocalDate = time.atDate(LocalDate(2016, 2, 29))
        assertEquals(datetime, datetimeWithLocalDate)
        checkComponents(datetime, 2016, 2, 29, 12, 1, 59)
        checkLocalDateTimePart(time, datetime)
    }

    private fun checkLocalDateTimePart(time: LocalTime, datetime: LocalDateTime) {
        checkEquals(time, datetime.time)
        checkComponents(time, datetime.hour, datetime.minute, datetime.second, datetime.nanosecond)
    }
}

fun checkComponents(value: LocalTime, hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0) {
    assertEquals(hour, value.hour, "hours")
    assertEquals(minute, value.minute, "minutes")
    assertEquals(second, value.second, "seconds")
    assertEquals(nanosecond, value.nanosecond, "nanoseconds")

    val fromComponents = LocalTime(hour, minute, second, nanosecond)
    checkEquals(fromComponents, value)
}

fun checkEquals(expected: LocalTime, actual: LocalTime) {
    assertEquals(expected, actual)
    assertEquals(expected.hashCode(), actual.hashCode())
    assertEquals(expected.toString(), actual.toString())
}
