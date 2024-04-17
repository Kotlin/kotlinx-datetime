/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.random.*
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

class InstantSamples {

    @Test
    fun epochSeconds() {
        val instant1 = Instant.fromEpochSeconds(999_999, nanosecondAdjustment = 123_456_789)
        check(instant1.epochSeconds == 999_999L)
        val instant2 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = 100_123_456_789)
        check(instant2.epochSeconds == 1_000_000 + 100L)
        val instant3 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = -100_876_543_211)
        check(instant3.epochSeconds == 1_000_000 - 101L)
    }

    @Test
    fun nanosecondsOfSecond() {
        val instant1 = Instant.fromEpochSeconds(999_999, nanosecondAdjustment = 123_456_789)
        check(instant1.nanosecondsOfSecond == 123_456_789)
        val instant2 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = 100_123_456_789)
        check(instant2.nanosecondsOfSecond == 123_456_789)
        val instant3 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = -100_876_543_211)
        check(instant3.nanosecondsOfSecond == 123_456_789)
    }

    @Test
    fun toEpochMilliseconds() {
        check(Instant.fromEpochMilliseconds(0).toEpochMilliseconds() == 0L)
        check(Instant.fromEpochMilliseconds(1_000_000_000_123).toEpochMilliseconds() == 1_000_000_000_123L)
        check(Instant.fromEpochSeconds(1_000_000_000, nanosecondAdjustment = 123_999_999)
            .toEpochMilliseconds() == 1_000_000_000_123L)
    }

    @Test
    fun plusDuration() {
        val instant = Instant.fromEpochMilliseconds(epochMilliseconds = 7 * 60 * 60 * 1000)
        val fiveHoursLater = instant + 5.hours
        check(fiveHoursLater.toEpochMilliseconds() == 12 * 60 * 60 * 1000L)
    }

    @Test
    fun minusDuration() {
        val instant = Instant.fromEpochMilliseconds(epochMilliseconds = 7 * 60 * 60 * 1000)
        val fiveHoursEarlier = instant - 5.hours
        check(fiveHoursEarlier.toEpochMilliseconds() == 2 * 60 * 60 * 1000L)
    }

    @Test
    fun minusInstant() {
        check(Instant.fromEpochSeconds(0) - Instant.fromEpochSeconds(epochSeconds = 7 * 60 * 60) == (-7).hours)
    }

    @Test
    fun compareToSample() {
        fun randomInstant() = Instant.fromEpochMilliseconds(
            Random.nextLong(Instant.DISTANT_PAST.toEpochMilliseconds(), Instant.DISTANT_FUTURE.toEpochMilliseconds())
        )
        repeat(100) {
            val instant1 = randomInstant()
            val instant2 = randomInstant()
            check((instant1 < instant2) == (instant1.toEpochMilliseconds() < instant2.toEpochMilliseconds()))
        }
    }

    @Test
    fun toStringSample() {
        check(Instant.fromEpochMilliseconds(0).toString() == "1970-01-01T00:00:00Z")
    }

    @Test
    fun fromEpochMilliseconds() {
        check(Instant.fromEpochMilliseconds(epochMilliseconds = 0) == Instant.parse("1970-01-01T00:00:00Z"))
        check(Instant.fromEpochMilliseconds(epochMilliseconds = 1_000_000_000_123)
            == Instant.parse("2001-09-09T01:46:40.123Z"))
    }

    @Test
    fun fromEpochSeconds() {
        check(Instant.fromEpochSeconds(epochSeconds = 0) == Instant.parse("1970-01-01T00:00:00Z"))
        check(Instant.fromEpochSeconds(epochSeconds = 1_000_001_234, nanosecondAdjustment = -1_234_000_000_001)
            == Instant.parse("2001-09-09T01:46:39.999999999Z"))
    }

    @Test
    fun fromEpochSecondsIntNanos() {
        check(Instant.fromEpochSeconds(epochSeconds = 0) == Instant.parse("1970-01-01T00:00:00Z"))
        check(Instant.fromEpochSeconds(epochSeconds = 1_000_000_000, nanosecondAdjustment = -1) == Instant.parse("2001-09-09T01:46:39.999999999Z"))
    }

    @Test
    fun parsing() {
        check(Instant.parse("1970-01-01T00:00:00Z") == Instant.fromEpochMilliseconds(0))
        check(Instant.parse("Thu, 01 Jan 1970 03:30:00 +0330", DateTimeComponents.Formats.RFC_1123) == Instant.fromEpochMilliseconds(0))
    }

    @Test
    fun isDistantPast() {
        val currentInstant = Clock.System.now()
        val tenThousandYearsAgo = currentInstant.minus(1_000, DateTimeUnit.YEAR, TimeZone.UTC)
        check(!tenThousandYearsAgo.isDistantPast)
        check(Instant.DISTANT_PAST.isDistantPast)
    }

    @Test
    fun isDistantFuture() {
        val currentInstant = Clock.System.now()
        val tenThousandYearsLater = currentInstant.plus(10_000, DateTimeUnit.YEAR, TimeZone.UTC)
        check(!tenThousandYearsLater.isDistantFuture)
        check(Instant.DISTANT_FUTURE.isDistantFuture)
    }

    @Test
    fun plusPeriod() {
        val startInstant = Instant.parse("2024-03-09T07:16:39.688Z")
        val period = DateTimePeriod(months = 1, days = -1) // one day short from a month later
        val afterPeriodInBerlin = startInstant.plus(period, TimeZone.of("Europe/Berlin"))
        check(afterPeriodInBerlin == Instant.parse("2024-04-08T06:16:39.688Z"))
        val afterPeriodInSydney = startInstant.plus(period, TimeZone.of("Australia/Sydney"))
        check(afterPeriodInSydney == Instant.parse("2024-04-08T08:16:39.688Z"))
    }

    @Test
    fun minusPeriod() {
        val period = DateTimePeriod(months = 1, days = -1) // one day short from a month earlier
        val startInstant = Instant.parse("2024-03-23T16:50:41.926Z")
        val afterPeriodInBerlin = startInstant.minus(period, TimeZone.of("Europe/Berlin"))
        check(afterPeriodInBerlin == Instant.parse("2024-02-24T16:50:41.926Z"))
        val afterPeriodInNewYork = startInstant.minus(period, TimeZone.of("America/New_York"))
        check(afterPeriodInNewYork == Instant.parse("2024-02-24T17:50:41.926Z"))
    }

    /** copy of [minusInstantInZone] */
    @Test
    fun periodUntil() {
        val startInstant = Instant.parse("2024-01-01T02:00:00Z")
        val endInstant = Instant.parse("2024-03-01T03:15:03Z")
        // In New York, we find the difference between 2023-12-31 and 2024-02-29, which is just short of two months
        val periodInNewYork = startInstant.periodUntil(endInstant, TimeZone.of("America/New_York"))
        check(periodInNewYork == DateTimePeriod(months = 1, days = 29, hours = 1, minutes = 15, seconds = 3))
        // In Berlin, we find the difference between 2024-01-01 and 2024-03-01, which is exactly two months
        val periodInBerlin = startInstant.periodUntil(endInstant, TimeZone.of("Europe/Berlin"))
        check(periodInBerlin == DateTimePeriod(months = 2, days = 0, hours = 1, minutes = 15, seconds = 3))
    }

    /** copy of [minusAsDateTimeUnit] */
    @Test
    fun untilAsDateTimeUnit() {
        val startInstant = Instant.parse("2024-01-01T02:00:00Z")
        val endInstant = Instant.parse("2024-03-01T02:00:00Z")
        // In New York, we find the difference between 2023-12-31 and 2024-02-29, which is just short of two months
        val monthsBetweenInNewYork = startInstant.until(endInstant, DateTimeUnit.MONTH, TimeZone.of("America/New_York"))
        check(monthsBetweenInNewYork == 1L)
        // In Berlin, we find the difference between 2024-01-01 and 2024-03-01, which is exactly two months
        val monthsBetweenInBerlin = startInstant.until(endInstant, DateTimeUnit.MONTH, TimeZone.of("Europe/Berlin"))
        check(monthsBetweenInBerlin == 2L)
    }

    /** copy of [minusAsTimeBasedUnit] */
    @Test
    fun untilAsTimeBasedUnit() {
        val instant = Instant.fromEpochMilliseconds(epochMilliseconds = 0)
        val otherInstant = Instant.fromEpochMilliseconds(epochMilliseconds = 7 * 60 * 60 * 1000)
        val hoursBetweenInstants = instant.until(otherInstant, DateTimeUnit.HOUR)
        check(hoursBetweenInstants == 7L)
    }

    @Test
    fun daysUntil() {
        val startInstant = Instant.parse("2023-03-26T00:30:00Z")
        val endInstant = Instant.parse("2023-03-28T00:15:00Z")
        // In New York, these days are both 24 hour long, so the difference is 15 minutes short of 2 days
        val daysBetweenInNewYork = startInstant.daysUntil(endInstant, TimeZone.of("America/New_York"))
        check(daysBetweenInNewYork == 1)
        // In Berlin, 2023-03-26 is 23 hours long, so the difference more than 2 days
        val daysBetweenInBerlin = startInstant.daysUntil(endInstant, TimeZone.of("Europe/Berlin"))
        check(daysBetweenInBerlin == 2)
    }

    @Test
    fun monthsUntil() {
        val startInstant = Instant.parse("2024-01-01T02:00:00Z")
        val endInstant = Instant.parse("2024-03-01T02:00:00Z")
        // In New York, we find the difference between 2023-12-31 and 2024-02-29, which is just short of two months
        val monthsBetweenInNewYork = startInstant.monthsUntil(endInstant, TimeZone.of("America/New_York"))
        check(monthsBetweenInNewYork == 1)
        // In Berlin, we find the difference between 2024-01-01 and 2024-03-01, which is exactly two months
        val monthsBetweenInBerlin = startInstant.monthsUntil(endInstant, TimeZone.of("Europe/Berlin"))
        check(monthsBetweenInBerlin == 2)
    }

    @Test
    fun yearsUntil() {
        val startInstant = Instant.parse("2024-03-01T02:01:00Z")
        val endInstant = Instant.parse("2025-03-01T02:01:00Z")
        // In New York, we find the difference between 2024-02-29 and 2025-02-28, which is just short of a year
        val yearsBetweenInNewYork = startInstant.yearsUntil(endInstant, TimeZone.of("America/New_York"))
        check(yearsBetweenInNewYork == 0)
        // In Berlin, we find the difference between 2024-03-01 and 2025-03-01, which is exactly a year
        val yearsBetweenInBerlin = startInstant.yearsUntil(endInstant, TimeZone.of("Europe/Berlin"))
        check(yearsBetweenInBerlin == 1)
    }

    /** copy of [periodUntil] */
    @Test
    fun minusInstantInZone() {
        val startInstant = Instant.parse("2024-01-01T02:00:00Z")
        val endInstant = Instant.parse("2024-03-01T03:15:03Z")
        // In New York, we find the difference between 2023-12-31 and 2024-02-29, which is just short of two months
        val periodInNewYork = endInstant.minus(startInstant, TimeZone.of("America/New_York"))
        check(periodInNewYork == DateTimePeriod(months = 1, days = 29, hours = 1, minutes = 15, seconds = 3))
        // In Berlin, we find the difference between 2024-01-01 and 2024-03-01, which is exactly two months
        val periodInBerlin = endInstant.minus(startInstant, TimeZone.of("Europe/Berlin"))
        check(periodInBerlin == DateTimePeriod(months = 2, days = 0, hours = 1, minutes = 15, seconds = 3))
    }

    @Test
    fun plusDateTimeUnit() {
        val startInstant = Instant.parse("2024-04-05T22:51:45.586Z")
        val twoYearsLaterInBerlin = startInstant.plus(2, DateTimeUnit.YEAR, TimeZone.of("Europe/Berlin"))
        check(twoYearsLaterInBerlin == Instant.parse("2026-04-05T22:51:45.586Z"))
        val twoYearsLaterInSydney = startInstant.plus(2, DateTimeUnit.YEAR, TimeZone.of("Australia/Sydney"))
        check(twoYearsLaterInSydney == Instant.parse("2026-04-05T23:51:45.586Z"))
    }

    @Test
    fun minusDateTimeUnit() {
        val startInstant = Instant.parse("2024-05-02T08:55:40.322Z")
        val twoYearsEarlierInBerlin = startInstant.minus(2, DateTimeUnit.YEAR, TimeZone.of("Europe/Berlin"))
        check(twoYearsEarlierInBerlin == Instant.parse("2022-05-02T08:55:40.322Z"))
        val twoYearsEarlierInCairo = startInstant.minus(2, DateTimeUnit.YEAR, TimeZone.of("Africa/Cairo"))
        check(twoYearsEarlierInCairo == Instant.parse("2022-05-02T09:55:40.322Z"))
    }

    @Test
    fun plusTimeBasedUnit() {
        val instant = Instant.fromEpochMilliseconds(epochMilliseconds = 7 * 60 * 60 * 1000)
        val fiveHoursLater = instant.plus(5, DateTimeUnit.HOUR)
        check(fiveHoursLater.toEpochMilliseconds() == 12 * 60 * 60 * 1000L)
    }

    @Test
    fun minusTimeBasedUnit() {
        val instant = Instant.fromEpochMilliseconds(epochMilliseconds = 7 * 60 * 60 * 1000)
        val fiveHoursEarlier = instant.minus(5, DateTimeUnit.HOUR)
        check(fiveHoursEarlier.toEpochMilliseconds() == 2 * 60 * 60 * 1000L)
    }

    @Test
    @Ignore // only the JVM has the range wide enough
    fun plusDateTimeUnitLong() {
        val zone = TimeZone.of("Europe/Berlin")
        val now = LocalDate(2024, Month.APRIL, 16).atTime(13, 30).toInstant(zone)
        val tenTrillionDaysLater = now.plus(10_000_000_000L, DateTimeUnit.DAY, zone)
        check(tenTrillionDaysLater.toLocalDateTime(zone).date == LocalDate(27_381_094, Month.MAY, 12))
    }

    @Test
    @Ignore // only the JVM has the range wide enough
    fun minusDateTimeUnitLong() {
        val zone = TimeZone.of("Europe/Berlin")
        val now = LocalDate(2024, Month.APRIL, 16).atTime(13, 30).toInstant(zone)
        val tenTrillionDaysAgo = now.minus(10_000_000_000L, DateTimeUnit.DAY, zone)
        check(tenTrillionDaysAgo.toLocalDateTime(zone).date == LocalDate(-27_377_046, Month.MARCH, 22))
    }

    @Test
    fun plusTimeBasedUnitLong() {
        val startInstant = Instant.fromEpochMilliseconds(epochMilliseconds = 0)
        val quadrillion = 1_000_000_000_000L
        val quadrillionSecondsLater = startInstant.plus(quadrillion, DateTimeUnit.SECOND)
        check(quadrillionSecondsLater.epochSeconds == quadrillion)
    }

    @Test
    fun minusTimeBasedUnitLong() {
        val startInstant = Instant.fromEpochMilliseconds(epochMilliseconds = 0)
        val quadrillion = 1_000_000_000_000L
        val quadrillionSecondsEarlier = startInstant.minus(quadrillion, DateTimeUnit.SECOND)
        check(quadrillionSecondsEarlier.epochSeconds == -quadrillion)
    }

    /** copy of [untilAsDateTimeUnit] */
    @Test
    fun minusAsDateTimeUnit() {
        val startInstant = Instant.parse("2024-01-01T02:00:00Z")
        val endInstant = Instant.parse("2024-03-01T02:00:00Z")
        // In New York, we find the difference between 2023-12-31 and 2024-02-29, which is just short of two months
        val monthsBetweenInNewYork = endInstant.minus(startInstant, DateTimeUnit.MONTH, TimeZone.of("America/New_York"))
        check(monthsBetweenInNewYork == 1L)
        // In Berlin, we find the difference between 2024-01-01 and 2024-03-01, which is exactly two months
        val monthsBetweenInBerlin = endInstant.minus(startInstant, DateTimeUnit.MONTH, TimeZone.of("Europe/Berlin"))
        check(monthsBetweenInBerlin == 2L)
    }

    /** copy of [untilAsTimeBasedUnit] */
    @Test
    fun minusAsTimeBasedUnit() {
        val instant = Instant.fromEpochMilliseconds(epochMilliseconds = 0)
        val otherInstant = Instant.fromEpochMilliseconds(epochMilliseconds = 7 * 60 * 60 * 1000)
        val hoursBetweenInstants = otherInstant.minus(instant, DateTimeUnit.HOUR)
        check(hoursBetweenInstants == 7L)
    }

    @Test
    fun formatting() {
        val epochStart = Instant.fromEpochMilliseconds(0)
        check(epochStart.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET) == "1970-01-01T00:00:00Z")
        val customFormat = DateTimeComponents.Format {
            date(LocalDate.Formats.ISO_BASIC)
            hour(); minute(); second(); char('.'); secondFraction(3)
            offset(UtcOffset.Formats.FOUR_DIGITS)
        }
        check(epochStart.format(customFormat, UtcOffset(hours = 3, minutes = 30)) == "19700101033000.000+0330")
    }
}
