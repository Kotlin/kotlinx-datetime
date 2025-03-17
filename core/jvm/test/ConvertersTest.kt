/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.test.*
import java.time.Instant as JTInstant
import java.time.LocalDateTime as JTLocalDateTime
import java.time.LocalTime as JTLocalTime
import java.time.LocalDate as JTLocalDate
import java.time.Period as JTPeriod
import java.time.ZoneId
import java.time.ZoneOffset as JTZoneOffset

class ConvertersTest {

    @Test
    fun instant() {
        fun test(seconds: Long, nanosecond: Int) {
            val ktInstant = Instant.fromEpochSeconds(seconds, nanosecond.toLong())
            val jtInstant = JTInstant.ofEpochSecond(seconds, nanosecond.toLong())

            assertEquals(ktInstant, jtInstant.toKotlinInstant())
            assertEquals(jtInstant, ktInstant.toJavaInstant())

            assertEquals(ktInstant, jtInstant.toString().toInstant())
            assertEquals(jtInstant, ktInstant.toString().let(JTInstant::parse))
        }

        repeat(STRESS_TEST_ITERATIONS) {
            val seconds = Random.nextLong(1_000_000_000_000)
            val nanos = Random.nextInt()
            test(seconds, nanos)
        }
    }

    private fun randomDate(): LocalDate {
        val year = Random.nextInt(-20000, 20000)
        val month = Month.entries.random()
        val day = (1..java.time.YearMonth.of(year, month).lengthOfMonth()).random()
        return LocalDate(year, month.number, day)
    }

    private fun randomDateTime(): LocalDateTime = randomDate().atTime(
            Random.nextInt(24),
            Random.nextInt(60),
            Random.nextInt(60),
            Random.nextInt(1_000_000_000))

    private fun randomTime(): LocalTime {
        val hour = Random.nextInt(24)
        val minute = Random.nextInt(60)
        val second = Random.nextInt(60)
        val nanosecond = Random.nextInt(1_000_000_000)
        return LocalTime(hour, minute, second, nanosecond)
    }

    @Test
    fun localDateTime() {
        fun test(ktDateTime: LocalDateTime) {
            val jtDateTime = with(ktDateTime) { JTLocalDateTime.of(year, month, day, hour, minute, second, nanosecond) }

            assertEquals(ktDateTime, jtDateTime.toKotlinLocalDateTime())
            assertEquals(jtDateTime, ktDateTime.toJavaLocalDateTime())

            assertEquals(ktDateTime, jtDateTime.toString().toLocalDateTime())
            assertEquals(jtDateTime, ktDateTime.toString().let(JTLocalDateTime::parse))
        }

        repeat(STRESS_TEST_ITERATIONS) {
            test(randomDateTime())
        }
    }

    @Test
    fun localTime() {
        fun test(ktTime: LocalTime) {
            val jtTime = with(ktTime) { JTLocalTime.of(hour, minute, second, nanosecond) }

            assertEquals(ktTime, jtTime.toKotlinLocalTime())
            assertEquals(jtTime, ktTime.toJavaLocalTime())

            assertEquals(ktTime, jtTime.toString().toLocalTime())
            assertEquals(jtTime, ktTime.toString().let(JTLocalTime::parse))
        }

        repeat(STRESS_TEST_ITERATIONS) {
            test(randomTime())
        }
    }

    @Test
    fun localDate() {
        fun test(ktDate: LocalDate) {
            val jtDate = with(ktDate) { JTLocalDate.of(year, month, day) }

            assertEquals(ktDate, jtDate.toKotlinLocalDate())
            assertEquals(jtDate, ktDate.toJavaLocalDate())

            assertEquals(ktDate, jtDate.toString().toLocalDate())
            assertEquals(jtDate, ktDate.toString().let(JTLocalDate::parse))
        }

        repeat(STRESS_TEST_ITERATIONS) {
            test(randomDate())
        }
    }

    @Test
    fun datePeriod() {

        fun assertJtPeriodNormalizedEquals(a: JTPeriod, b: JTPeriod) {
            assertEquals(a.days, b.days)
            assertEquals(a.months + a.years * 12, b.months + b.years * 12)
        }

        fun test(years: Int, months: Int, days: Int) {
            val ktPeriod = DatePeriod(years, months, days)
            val jtPeriod = JTPeriod.of(years, months, days)

            assertEquals(ktPeriod, jtPeriod.toKotlinDatePeriod())
            assertJtPeriodNormalizedEquals(jtPeriod, ktPeriod.toJavaPeriod())

            assertEquals(ktPeriod, jtPeriod.toString().let(DatePeriod::parse))
            assertJtPeriodNormalizedEquals(jtPeriod, ktPeriod.toString().let(JTPeriod::parse))
        }

        repeat(STRESS_TEST_ITERATIONS) {
            test(Random.nextInt(-1000, 1000), Random.nextInt(-1000, 1000), Random.nextInt(-1000, 1000))
        }
    }

    @Test
    fun timeZone() {
        fun test(tzid: String) {
            val ktZone = TimeZone.of(tzid)
            val jtZone = ZoneId.of(tzid)

            assertEquals(ktZone, jtZone.toKotlinTimeZone())
            assertEquals(jtZone, ktZone.toJavaZoneId())
        }

        test("Z")
        test("Etc/UTC")
        test("+00")
        test("+0000")
        test("+00:00")
        test("America/New_York")
        test("Europe/Berlin")
    }

    @Test
    fun fixedOffsetTimeZone() {
        val zone = TimeZone.of("UTC") as FixedOffsetTimeZone

        val jtZone = zone.toJavaZoneId()
        val jtZoneOffset = zone.toJavaZoneOffset()

        assertEquals(zone.id, jtZone.id)
        assertNotEquals(jtZone, jtZoneOffset)
        assertEquals(jtZone.normalized(), jtZoneOffset)

        assertIs<FixedOffsetTimeZone>(jtZone.toKotlinTimeZone())
    }

    @Test
    fun zoneOffset() {
        fun test(offsetString: String) {
            val ktUtcOffset = TimeZone.of(offsetString).offsetAt(Instant.fromEpochMilliseconds(0))
            val ktZoneOffset = ktUtcOffset.asTimeZone()
            val jtZoneOffset = JTZoneOffset.of(offsetString)

            assertEquals(ktZoneOffset, jtZoneOffset.toKotlinFixedOffsetTimeZone())
            assertEquals(ktZoneOffset, jtZoneOffset.toKotlinTimeZone())
            assertEquals(jtZoneOffset, ktZoneOffset.toJavaZoneOffset())
            assertEquals(jtZoneOffset, ktZoneOffset.toJavaZoneId())
            assertEquals(jtZoneOffset, ktUtcOffset.toJavaZoneOffset())
        }

        test("Z")
        test("+1")
        test("-10")
        test("+08")
        test("+08")
        test("-103030")
    }
}
