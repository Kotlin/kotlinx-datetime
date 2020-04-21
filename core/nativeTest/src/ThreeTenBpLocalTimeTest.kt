/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class ThreeTenBpLocalTimeTest {

    @Test
    fun ofSecondOfDay() {
        val localTime = LocalTime.ofSecondOfDay(2 * 60 * 60 + 17 * 60 + 23, 5)
        check(localTime, 2, 17, 23, 5)
    }

    @Test
    fun ofNanoOfDay() {
        val localTime = LocalTime.ofNanoOfDay(60 * 60 * 1000000000L + 17)
        check(localTime, 1, 0, 0, 17)
    }

    @Test
    fun toSecondOfDay() {
        var t = LocalTime(0, 0, 0, 0)
        for (i in 0 until 24 * 60 * 60) {
            assertEquals(i, t.toSecondOfDay())
            t = t.plusSeconds(1)
        }
    }

    @Test
    fun toSecondOfDaySymmetricWithFromNanoOfDay() {
        var t = LocalTime(0, 0, 0, 0)
        for (i in 0 until 24 * 60 * 60) {
            assertEquals(t, LocalTime.ofSecondOfDay(t.toSecondOfDay().toLong(), 0))
            t = t.plusSeconds(1)
        }
    }

    @Test
    fun toNanoOfDay() {
        var t = LocalTime(0, 0, 0, 0)
        for (i in 0..999999) {
            assertEquals(i.toLong(), t.toNanoOfDay())
            t = t.plusNanos(1)
        }
        t = LocalTime(0, 0, 0, 0)
        for (i in 1..1000000) {
            t = t.minusNanos(1)
            assertEquals(24 * 60 * 60 * 1000000000L - i, t.toNanoOfDay())
        }
    }

    @Test
    fun toNanoOfDaySymmetricWithFromNanoOfDay() {
        var t = LocalTime(0, 0, 0, 0)
        for (i in 0..999999) {
            assertEquals(t, LocalTime.ofNanoOfDay(t.toNanoOfDay()))
            t = t.plusNanos(1)
        }
        t = LocalTime(0, 0, 0, 0)
        for (i in 1..1000000) {
            t = t.minusNanos(1)
            assertEquals(t, LocalTime.ofNanoOfDay(t.toNanoOfDay()))
        }
    }

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

    private fun LocalTime.plusNanos(nanosToAdd: Long): LocalTime {
        if (nanosToAdd == 0L) {
            return this
        }
        val nofd = toNanoOfDay()
        val newNofd: Long = (nanosToAdd % NANOS_PER_DAY + nofd + NANOS_PER_DAY) % NANOS_PER_DAY
        if (nofd == newNofd) {
            return this
        }
        val newHour = (newNofd / NANOS_PER_HOUR).toInt()
        val newNano = (newNofd % NANOS_PER_ONE).toInt()
        return LocalTime(newHour,
            (newNofd / NANOS_PER_MINUTE % MINUTES_PER_HOUR).toInt(),
            (newNofd / NANOS_PER_ONE % SECONDS_PER_MINUTE).toInt(),
            newNano)
    }

    private fun LocalTime.minusNanos(nanosToSubtract: Long): LocalTime =
        plusNanos(-(nanosToSubtract % NANOS_PER_DAY))

    private fun check(time: LocalTime, hour: Int, minute: Int, second: Int, nanosecond: Int) {
        assertEquals(hour, time.hour)
        assertEquals(minute, time.minute)
        assertEquals(second, time.second)
        assertEquals(nanosecond, time.nanosecond)
    }
}
