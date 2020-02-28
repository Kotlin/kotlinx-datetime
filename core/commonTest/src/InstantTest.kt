/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test
import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.*

class InstantTest {

    @Test
    fun testNow() {
        val instant = Instant.now()
        val millis = instant.toUnixMillis()

        assertTrue(millis > 1_500_000_000_000L)

        println(instant)
        println(instant.toUnixMillis())

        val millisInstant = Instant.fromUnixMillis(millis)

        assertEquals(millis, millisInstant.toUnixMillis())

        val notEqualInstant = Instant.fromUnixMillis(millis + 1)
        assertNotEquals(notEqualInstant, instant)
    }

    @UseExperimental(ExperimentalTime::class)
    @Test
    fun instantArithmetic() {
        val instant = Instant.now().toUnixMillis().let { Instant.fromUnixMillis(it) } // round to millis
        val diffMillis = Random.nextLong(1000, 1_000_000_000)
        val diff = diffMillis.milliseconds

        val nextInstant = (instant.toUnixMillis() + diffMillis).let { Instant.fromUnixMillis(it) }

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



    @Test
    fun instantParsing() {
        val instant = Instant.parse("2019-10-09T09:02:00.123Z")

        assertEquals(1570611720_123L, instant.toUnixMillis())

        Instant.parse("gibberish")
    }



    @UseExperimental(ExperimentalTime::class)
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

    @UseExperimental(ExperimentalTime::class)
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
    fun diffInvariant() {
        repeat(1000) {
            val millis1 = Random.nextLong(2_000_000_000_000L)
            val millis2 = Random.nextLong(2_000_000_000_000L)
            val instant1 = Instant.fromUnixMillis(millis1)
            val instant2 = Instant.fromUnixMillis(millis2)

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
                val date1 = Instant.fromUnixMillis(millis1).toLocalDateTime().date
                val date2 = Instant.fromUnixMillis(millis2).toLocalDateTime().date
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

}