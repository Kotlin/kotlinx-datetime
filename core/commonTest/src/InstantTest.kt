/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test
import kotlinx.datetime.*
import kotlin.random.*
import kotlin.test.*
import kotlin.time.*

class InstantTest {

    @Test
    fun testNow() {
        val instant = Instant.now()
        val millis = instant.toEpochMilliseconds()

        assertTrue(millis > 1_500_000_000_000L)

        println(instant)
        println(instant.toEpochMilliseconds())

        val millisInstant = Instant.fromEpochMilliseconds(millis)

        assertEquals(millis, millisInstant.toEpochMilliseconds())

        val notEqualInstant = Instant.fromEpochMilliseconds(millis + 1)
        assertNotEquals(notEqualInstant, instant)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun instantArithmetic() {
        val instant = Instant.now().toEpochMilliseconds().let { Instant.fromEpochMilliseconds(it) } // round to millis
        val diffMillis = Random.nextLong(1000, 1_000_000_000)
        val diff = diffMillis.milliseconds

        val nextInstant = (instant.toEpochMilliseconds() + diffMillis).let { Instant.fromEpochMilliseconds(it) }

        assertEquals(diff, nextInstant - instant)
        assertEquals(nextInstant, instant + diff)
        assertEquals(instant, nextInstant - diff)

        println("this: $instant, next: $nextInstant, diff: ${diff.toIsoString()}")
    }

    @Test
    fun instantToLocalDTConversion() {
        val now = Instant.now()
        println(now.toLocalDateTime(TimeZone.UTC))
        println(now.toLocalDateTime(TimeZone.SYSTEM))
    }

    /* Based on the ThreeTenBp project.
     * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
     */
    @Test
    fun instantParsing() {
        val instants = arrayOf(
            Triple("1970-01-01T00:00:00Z", 0, 0),
            Triple("1970-01-01t00:00:00Z", 0, 0),
            Triple("1970-01-01T00:00:00z", 0, 0),
            Triple("1970-01-01T00:00:00.0Z", 0, 0),
            Triple("1970-01-01T00:00:00.000000000Z", 0, 0),
            Triple("1970-01-01T00:00:00.000000001Z", 0, 1),
            Triple("1970-01-01T00:00:00.100000000Z", 0, 100000000),
            Triple("1970-01-01T00:00:01Z", 1, 0),
            Triple("1970-01-01T00:01:00Z", 60, 0),
            Triple("1970-01-01T00:01:01Z", 61, 0),
            Triple("1970-01-01T00:01:01.000000001Z", 61, 1),
            Triple("1970-01-01T01:00:00.000000000Z", 3600, 0),
            Triple("1970-01-01T01:01:01.000000001Z", 3661, 1),
            Triple("1970-01-02T01:01:01.100000000Z", 90061, 100000000))
        instants.forEach {
            val (str, seconds, nanos) = it
            val instant = Instant.parse(str)
            assertEquals(seconds.toLong() * 1000 + nanos / 1000000, instant.toEpochMilliseconds())
        }
    }


    @OptIn(ExperimentalTime::class)
    @Test
    fun instantCalendarArithmetic() {
        val zone = TimeZone.of("Europe/Berlin")
        val instant1 = LocalDateTime(2019, 10, 27, 2, 59, 0, 0).toInstant(zone)
        checkComponents(instant1.toLocalDateTime(zone), 2019, 10, 27, 2, 59)

        val instant2 = instant1.plus(CalendarPeriod(hours = 24), zone)
        checkComponents(instant2.toLocalDateTime(zone), 2019, 10, 28, 1, 59)
        assertEquals(24.hours, instant2 - instant1)
        assertEquals(24, instant1.until(instant2, CalendarUnit.HOUR, zone))

        val instant3 = instant1.plus(1, CalendarUnit.DAY, zone)
        checkComponents(instant3.toLocalDateTime(zone), 2019, 10, 28, 2, 59)
        assertEquals(25.hours, instant3 - instant1)
        assertEquals(1, instant1.until(instant3, CalendarUnit.DAY, zone))
        assertEquals(1, instant1.daysUntil(instant3, zone))

        val period = CalendarPeriod(days = 1, hours = 1)
        val instant4 = instant1.plus(period, zone)
        checkComponents(instant4.toLocalDateTime(zone), 2019, 10, 28, 3, 59)
        assertEquals(period, instant1.periodUntil(instant4, zone))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun instantOffset() {
        val zone = TimeZone.of("Europe/Berlin")
        val instant1 = LocalDateTime(2019, 10, 27, 2, 59, 0, 0).toInstant(zone)
        val ldt1 = instant1.toLocalDateTime(zone)
        val offset1 = instant1.offsetAt(zone)
        checkComponents(ldt1, 2019, 10, 27, 2, 59)
        assertEquals(instant1, ldt1.toInstant(offset1))

        val instant2 = instant1 + 1.hours
        val ldt2 = instant2.toLocalDateTime(zone)
        val offset2 = instant2.offsetAt(zone)
        assertEquals(ldt1, ldt2)
        assertEquals(instant2, ldt2.toInstant(offset2))
        assertNotEquals(offset1, offset2)
        assertEquals(offset1.totalSeconds.seconds, offset2.totalSeconds.seconds + 1.hours)

        val instant3 = instant2 - 2.hours
        val offset3 = instant3.offsetAt(zone)
        assertEquals(offset1, offset3)
    }

    @Test
    fun changingTimeZoneRules() {
        val start = Instant.parse("1991-01-25T23:15:15.855Z")
        val end = Instant.parse("2006-04-24T22:07:32.561Z")
        val diff = start.periodUntil(end, TimeZone.of("Europe/Moscow"))
        val end2 = start.plus(diff, TimeZone.of("Europe/Moscow"))
        assertEquals(end, end2)
    }

    @Test
    fun diffInvariant() {
        repeat(1000) {
            val millis1 = Random.nextLong(2_000_000_000_000L)
            val millis2 = Random.nextLong(2_000_000_000_000L)
            val instant1 = Instant.fromEpochMilliseconds(millis1)
            val instant2 = Instant.fromEpochMilliseconds(millis2)

            val diff = instant1.periodUntil(instant2, TimeZone.SYSTEM)
            val instant3 = instant1.plus(diff, TimeZone.SYSTEM)

            if (instant2 != instant3)
                println("start: $instant1, end: $instant2, start + diff: $instant3, diff: $diff")
        }
    }

    @Test
    fun diffInvariantSameAsDate() {
        repeat(1000) {
            val millis1 = Random.nextLong(2_000_000_000_000L)
            val millis2 = Random.nextLong(2_000_000_000_000L)
            with(TimeZone.UTC) TZ@ {
                val date1 = Instant.fromEpochMilliseconds(millis1).toLocalDateTime().date
                val date2 = Instant.fromEpochMilliseconds(millis2).toLocalDateTime().date
                fun LocalDate.instantAtStartOfDay() = LocalDateTime(year, monthNumber, dayOfMonth, 0, 0, 0, 0).toInstant()
                val instant1 = date1.instantAtStartOfDay()
                val instant2 = date2.instantAtStartOfDay()

                val diff1 = instant1.periodUntil(instant2, this@TZ)
                val diff2 = date1.periodUntil(date2)

                if (diff1 != diff2)
                    println("start: $instant1, end: $instant2, diff by instants: $diff1, diff by dates: $diff2")
            }
        }
    }


    @Test
    fun zoneDependentDiff() {
        val instant1 = Instant.parse("2019-04-01T00:00:00Z")
        val instant2 = Instant.parse("2019-05-01T04:00:00Z")

        for (zone in (-12..12 step 3).map { h -> TimeZone.of("${if (h >= 0) "+" else ""}$h") }) {
            val dt1 = instant1.toLocalDateTime(zone)
            val dt2 = instant2.toLocalDateTime(zone)
            val diff = instant1.periodUntil(instant2, zone)
            println("diff between $dt1 and $dt2 at zone $zone: $diff")
        }
    }

    /* Based on the ThreeTenBp project.
     * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
     */
    @Test
    fun nanosecondAdjustment() {
        for (i in -2..2L) {
            for (j in 0..9L) {
                val t: Instant = Instant.fromEpochSeconds(i, j)
                assertEquals(i, t.epochSeconds)
                assertEquals(j, t.nanosecondsOfSecond.toLong())
            }
            for (j in -10..-1L) {
                val t: Instant = Instant.fromEpochSeconds(i, j)
                assertEquals(i - 1, t.epochSeconds)
                assertEquals(j + 1000000000, t.nanosecondsOfSecond.toLong())
            }
            for (j in 999_999_990..999_999_999L) {
                val t: Instant = Instant.fromEpochSeconds(i, j)
                assertEquals(i, t.epochSeconds)
                assertEquals(j, t.nanosecondsOfSecond.toLong())
            }
        }
    }

    /* Based on the ThreeTenBp project.
     * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
     */
    @ExperimentalTime
    @Test
    fun strings() {
        assertEquals("0000-01-02T00:00:00Z", LocalDateTime(0, 1, 2, 0, 0, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("0000-01-01T12:30:00Z", LocalDateTime(0, 1, 1, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("0000-01-01T00:00:00.000000001Z", LocalDateTime(0, 1, 1, 0, 0, 0, 1).toInstant(TimeZone.UTC).toString())
        assertEquals("0000-01-01T00:00:00Z", LocalDateTime(0, 1, 1, 0, 0, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-0001-12-31T23:59:59.999999999Z", LocalDateTime(-1, 12, 31, 23, 59, 59, 999999999).toInstant(TimeZone.UTC).toString())
        assertEquals("-0001-12-31T12:30:00Z", LocalDateTime(-1, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-0001-12-30T12:30:00Z", LocalDateTime(-1, 12, 30, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-9999-01-02T12:30:00Z", LocalDateTime(-9999, 1, 2, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-9999-01-01T12:30:00Z", LocalDateTime(-9999, 1, 1, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-9999-01-01T00:00:00Z", LocalDateTime(-9999, 1, 1, 0, 0, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-10000-12-31T23:59:59.999999999Z", LocalDateTime(-10000, 12, 31, 23, 59, 59, 999999999).toInstant(TimeZone.UTC).toString())
        assertEquals("-10000-12-31T12:30:00Z", LocalDateTime(-10000, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-10000-12-30T12:30:00Z", LocalDateTime(-10000, 12, 30, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-15000-12-31T12:30:00Z", LocalDateTime(-15000, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-19999-01-02T12:30:00Z", LocalDateTime(-19999, 1, 2, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-19999-01-01T12:30:00Z", LocalDateTime(-19999, 1, 1, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-19999-01-01T00:00:00Z", LocalDateTime(-19999, 1, 1, 0, 0, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-20000-12-31T23:59:59.999999999Z", LocalDateTime(-20000, 12, 31, 23, 59, 59, 999999999).toInstant(TimeZone.UTC).toString())
        assertEquals("-20000-12-31T12:30:00Z", LocalDateTime(-20000, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-20000-12-30T12:30:00Z", LocalDateTime(-20000, 12, 30, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("-25000-12-31T12:30:00Z", LocalDateTime(-25000, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("9999-12-30T12:30:00Z", LocalDateTime(9999, 12, 30, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("9999-12-31T12:30:00Z", LocalDateTime(9999, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("9999-12-31T23:59:59.999999999Z", LocalDateTime(9999, 12, 31, 23, 59, 59, 999999999).toInstant(TimeZone.UTC).toString())
        assertEquals("+10000-01-01T00:00:00Z", LocalDateTime(10000, 1, 1, 0, 0, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+10000-01-01T12:30:00Z", LocalDateTime(10000, 1, 1, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+10000-01-02T12:30:00Z", LocalDateTime(10000, 1, 2, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+15000-12-31T12:30:00Z", LocalDateTime(15000, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-30T12:30:00Z", LocalDateTime(19999, 12, 30, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T12:30:00Z", LocalDateTime(19999, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.999999999Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 999999999).toInstant(TimeZone.UTC).toString())
        assertEquals("+20000-01-01T00:00:00Z", LocalDateTime(20000, 1, 1, 0, 0, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+20000-01-01T12:30:00Z", LocalDateTime(20000, 1, 1, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+20000-01-02T12:30:00Z", LocalDateTime(20000, 1, 2, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+25000-12-31T12:30:00Z", LocalDateTime(25000, 12, 31, 12, 30, 0, 0).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.009999999Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 9999999).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.999999Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 999999000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.009999Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 9999000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.123Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 123000000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.100Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 100000000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.020Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 20000000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.003Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 3000000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.000400Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 400000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.000050Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 50000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.000006Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 6000).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.000000700Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 700).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.000000080Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 80).toInstant(TimeZone.UTC).toString())
        assertEquals("+19999-12-31T23:59:59.000000009Z", LocalDateTime(19999, 12, 31, 23, 59, 59, 9).toInstant(TimeZone.UTC).toString())
    }

}
