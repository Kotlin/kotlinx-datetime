/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.math.*
import kotlin.random.*
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
            Pair(LocalTime(0, 0, 0, 9), "00:00:00.000000009"),
        )
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
    fun fromNanosecondOfDay() {
        val data = mapOf(
            0L to LocalTime(0, 0),
            5000000001L to LocalTime(0, 0, 5, 1),
            44105123456789L to LocalTime(12, 15, 5, 123456789),
            NANOS_PER_DAY - 1 to LocalTime(23, 59, 59, 999999999),
        ) + buildMap {
            repeat(STRESS_TEST_ITERATIONS) {
                val hour = Random.nextInt(24)
                val minute = Random.nextInt(60)
                val second = Random.nextInt(60)
                val nanosecond = Random.nextInt(1_000_000_000)
                val nanosecondOfDay =
                    hour * NANOS_PER_HOUR + minute * NANOS_PER_MINUTE + second * NANOS_PER_ONE.toLong() + nanosecond
                put(nanosecondOfDay, LocalTime(hour, minute, second, nanosecond))
            }
        }
        data.forEach { (nanosecondOfDay, localTime) ->
            assertEquals(nanosecondOfDay, localTime.toNanosecondOfDay())
            assertEquals(localTime, LocalTime.fromNanosecondOfDay(nanosecondOfDay))
        }
    }

    @Test
    fun fromNanosecondOfDayInvalid() {
        assertFailsWith<IllegalArgumentException> { LocalTime.fromNanosecondOfDay(-1) }
        assertFailsWith<IllegalArgumentException> { LocalTime.fromNanosecondOfDay(NANOS_PER_DAY) }
        repeat(STRESS_TEST_ITERATIONS) {
            assertFailsWith<IllegalArgumentException> {
                LocalTime.fromNanosecondOfDay(NANOS_PER_DAY + Random.nextLong().absoluteValue)
            }
        }
    }

    @Test
    fun fromMillisecondOfDay() {
        val data = mapOf(
            0 to LocalTime(0, 0),
            5001 to LocalTime(0, 0, 5, 1000000),
            44105123 to LocalTime(12, 15, 5, 123000000),
            MILLIS_PER_DAY - 1 to LocalTime(23, 59, 59, 999000000),
        ) + buildMap {
            repeat(STRESS_TEST_ITERATIONS) {
                val hour = Random.nextInt(24)
                val minute = Random.nextInt(60)
                val second = Random.nextInt(60)
                val millisecond = Random.nextInt(1000)
                val millisecondOfDay =
                    (hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second) * MILLIS_PER_ONE +
                        millisecond
                put(millisecondOfDay, LocalTime(hour, minute, second, millisecond * NANOS_PER_MILLI))
            }
        }
        data.forEach { (millisecondOfDay, localTime) ->
            assertEquals(millisecondOfDay, localTime.toMillisecondOfDay())
            assertEquals(localTime, LocalTime.fromMillisecondOfDay(millisecondOfDay))
        }
    }

    @Test
    fun fromMillisecondOfDayInvalid() {
        assertFailsWith<IllegalArgumentException> { LocalTime.fromMillisecondOfDay(-1) }
        assertFailsWith<IllegalArgumentException> { LocalTime.fromMillisecondOfDay(MILLIS_PER_DAY) }
        repeat(STRESS_TEST_ITERATIONS) {
            assertFailsWith<IllegalArgumentException> {
                LocalTime.fromMillisecondOfDay(MILLIS_PER_DAY + Random.nextInt().absoluteValue)
            }
        }
    }

    @Test
    fun fromSecondOfDay() {
        var t = LocalTime(0, 0, 0, 0)
        for (i in 0 until SECONDS_PER_DAY) {
            assertEquals(i, t.toSecondOfDay())
            assertEquals(t, LocalTime.fromSecondOfDay(t.toSecondOfDay()))
            t = t.plusSeconds(1)
        }
    }

    @Test
    fun fromSecondOfDayInvalid() {
        assertFailsWith<IllegalArgumentException> { LocalTime.fromSecondOfDay(-1) }
        assertFailsWith<IllegalArgumentException> { LocalTime.fromSecondOfDay(SECONDS_PER_DAY) }
        repeat(STRESS_TEST_ITERATIONS) {
            assertFailsWith<IllegalArgumentException> {
                LocalTime.fromSecondOfDay(SECONDS_PER_DAY + Random.nextInt().absoluteValue)
            }
        }
    }

    @Test
    fun fromSecondOfDayIgnoresNanosecond() {
        assertEquals(
            0,
            LocalTime(0, 0, 0, 100).toSecondOfDay(),
        )
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

private fun LocalTime.plusSeconds(secondsToAdd: Long): LocalTime {
    if (secondsToAdd == 0L) {
        return this
    }
    val sofd: Int = hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second
    val newSofd: Int = ((secondsToAdd % SECONDS_PER_DAY).toInt() + sofd + SECONDS_PER_DAY) % SECONDS_PER_DAY
    if (sofd == newSofd) {
        return this
    }
    val newHour: Int = newSofd / SECONDS_PER_HOUR
    val newMinute: Int = newSofd / SECONDS_PER_MINUTE % MINUTES_PER_HOUR
    val newSecond: Int = newSofd % SECONDS_PER_MINUTE
    return LocalTime(newHour, newMinute, newSecond, nanosecond)
}
