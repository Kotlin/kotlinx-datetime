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
        // Getting the number of whole seconds that passed since the Unix epoch
        val instant1 = Instant.fromEpochSeconds(999_999, nanosecondAdjustment = 123_456_789)
        check(instant1.epochSeconds == 999_999L)
        val instant2 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = 100_123_456_789)
        check(instant2.epochSeconds == 1_000_000 + 100L)
        val instant3 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = -100_876_543_211)
        check(instant3.epochSeconds == 1_000_000 - 101L)
    }

    @Test
    fun nanosecondsOfSecond() {
        // Getting the number of nanoseconds that passed since the start of the second
        val instant1 = Instant.fromEpochSeconds(999_999, nanosecondAdjustment = 123_456_789)
        check(instant1.nanosecondsOfSecond == 123_456_789)
        val instant2 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = 100_123_456_789)
        check(instant2.nanosecondsOfSecond == 123_456_789)
        val instant3 = Instant.fromEpochSeconds(1_000_000, nanosecondAdjustment = -100_876_543_211)
        check(instant3.nanosecondsOfSecond == 123_456_789)
    }

    @Test
    fun toEpochMilliseconds() {
        // Converting an Instant to the number of milliseconds since the Unix epoch
        check(Instant.fromEpochMilliseconds(0).toEpochMilliseconds() == 0L)
        check(Instant.fromEpochMilliseconds(1_000_000_000_123).toEpochMilliseconds() == 1_000_000_000_123L)
        check(Instant.fromEpochSeconds(1_000_000_000, nanosecondAdjustment = 123_999_999)
            .toEpochMilliseconds() == 1_000_000_000_123L)
    }

    @Test
    fun plusDuration() {
        // Finding a moment that's later than the starting point by the given amount of real time
        val instant = Instant.fromEpochSeconds(7 * 60 * 60, nanosecondAdjustment = 123_456_789)
        val fiveHoursLater = instant + 5.hours
        check(fiveHoursLater.epochSeconds == 12 * 60 * 60L)
        check(fiveHoursLater.nanosecondsOfSecond == 123_456_789)
    }

    @Test
    fun minusDuration() {
        // Finding a moment that's earlier than the starting point by the given amount of real time
        val instant = Instant.fromEpochSeconds(7 * 60 * 60, nanosecondAdjustment = 123_456_789)
        val fiveHoursEarlier = instant - 5.hours
        check(fiveHoursEarlier.epochSeconds == 2 * 60 * 60L)
        check(fiveHoursEarlier.nanosecondsOfSecond == 123_456_789)
    }

    @Test
    fun minusInstant() {
        // Finding the difference between two instants in terms of elapsed time
        check(Instant.fromEpochSeconds(0) - Instant.fromEpochSeconds(epochSeconds = 7 * 60 * 60) == (-7).hours)
    }

    @Test
    fun compareToSample() {
        // Finding out which of two instants is earlier
        fun randomInstant() = Instant.fromEpochMilliseconds(
            Random.nextLong(Instant.DISTANT_PAST.toEpochMilliseconds(), Instant.DISTANT_FUTURE.toEpochMilliseconds())
        )
        repeat(100) {
            val instant1 = randomInstant()
            val instant2 = randomInstant()
            // in the UTC time zone, earlier instants are represented as earlier datetimes
            check((instant1 < instant2) ==
                (instant1.toLocalDateTime(TimeZone.UTC) < instant2.toLocalDateTime(TimeZone.UTC)))
        }
    }

    @Test
    fun toStringSample() {
        // Converting an Instant to a string
        check(Instant.fromEpochSeconds(0).toString() == "1970-01-01T00:00:00Z")
    }

    @Test
    fun fromEpochMilliseconds() {
        // Constructing an Instant from the number of milliseconds since the Unix epoch
        check(Instant.fromEpochMilliseconds(epochMilliseconds = 0) == Instant.parse("1970-01-01T00:00:00Z"))
        check(Instant.fromEpochMilliseconds(epochMilliseconds = 1_000_000_000_123)
            == Instant.parse("2001-09-09T01:46:40.123Z"))
    }

    @Test
    fun fromEpochSeconds() {
        // Constructing an Instant from the number of seconds and nanoseconds since the Unix epoch
        check(Instant.fromEpochSeconds(epochSeconds = 0) == Instant.parse("1970-01-01T00:00:00Z"))
        check(Instant.fromEpochSeconds(epochSeconds = 1_000_001_234, nanosecondAdjustment = -1_234_000_000_001)
            == Instant.parse("2001-09-09T01:46:39.999999999Z"))
    }

    @Test
    fun fromEpochSecondsIntNanos() {
        // Constructing an Instant from the number of seconds and nanoseconds since the Unix epoch
        check(Instant.fromEpochSeconds(epochSeconds = 0) == Instant.parse("1970-01-01T00:00:00Z"))
        check(Instant.fromEpochSeconds(epochSeconds = 1_000_000_000, nanosecondAdjustment = -1) == Instant.parse("2001-09-09T01:46:39.999999999Z"))
    }

    @Test
    fun parsing() {
        // Parsing an Instant from a string using predefined and custom formats
        check(Instant.parse("1970-01-01T00:00:00Z") == Instant.fromEpochSeconds(0))
        check(Instant.parse("Thu, 01 Jan 1970 03:30:00 +0330", DateTimeComponents.Formats.RFC_1123) == Instant.fromEpochSeconds(0))
    }

    @Test
    fun isDistantPast() {
        // Checking if an instant is so far in the past that it's probably irrelevant
        val currentInstant = Clock.System.now()
        val tenThousandYearsAgo = currentInstant.minus(1_000, DateTimeUnit.YEAR, TimeZone.UTC)
        check(!tenThousandYearsAgo.isDistantPast)
        check(Instant.DISTANT_PAST.isDistantPast)
    }

    @Test
    fun isDistantFuture() {
        // Checking if an instant is so far in the future that it's probably irrelevant
        val currentInstant = Clock.System.now()
        val tenThousandYearsLater = currentInstant.plus(10_000, DateTimeUnit.YEAR, TimeZone.UTC)
        check(!tenThousandYearsLater.isDistantFuture)
        check(Instant.DISTANT_FUTURE.isDistantFuture)
    }

    @Test
    fun plusPeriod() {
        // Finding a moment that's later than the starting point by the given length of calendar time
        val startInstant = Instant.parse("2024-03-09T07:16:39.688Z")
        val period = DateTimePeriod(months = 1, days = -1) // one day short from a month later
        val afterPeriodInBerlin = startInstant.plus(period, TimeZone.of("Europe/Berlin"))
        check(afterPeriodInBerlin == Instant.parse("2024-04-08T06:16:39.688Z"))
        val afterPeriodInSydney = startInstant.plus(period, TimeZone.of("Australia/Sydney"))
        check(afterPeriodInSydney == Instant.parse("2024-04-08T08:16:39.688Z"))
    }

    @Test
    fun minusPeriod() {
        // Finding a moment that's earlier than the starting point by the given length of calendar time
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
        // Finding a period that it would take to get from the starting instant to the ending instant
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
        // Finding the difference between two instants in terms of the given calendar-based measurement unit
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
        // Finding the difference between two instants in terms of the given measurement unit
        val instant = Instant.fromEpochSeconds(0)
        val otherInstant = Instant.fromEpochSeconds(7 * 60 * 60, nanosecondAdjustment = 123_456_789)
        val hoursBetweenInstants = instant.until(otherInstant, DateTimeUnit.HOUR)
        check(hoursBetweenInstants == 7L)
    }

    @Test
    fun daysUntil() {
        // Finding the number of full days between two instants in the given time zone
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
        // Finding the number of months between two instants in the given time zone
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
        // Finding the number of full years between two instants in the given time zone
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
        // Finding a period that it would take to get from the starting instant to the ending instant
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
        // Finding a moment that's later than the starting point by the given length of calendar time
        val startInstant = Instant.parse("2024-04-05T22:51:45.586Z")
        val twoYearsLaterInBerlin = startInstant.plus(2, DateTimeUnit.YEAR, TimeZone.of("Europe/Berlin"))
        check(twoYearsLaterInBerlin == Instant.parse("2026-04-05T22:51:45.586Z"))
        val twoYearsLaterInSydney = startInstant.plus(2, DateTimeUnit.YEAR, TimeZone.of("Australia/Sydney"))
        check(twoYearsLaterInSydney == Instant.parse("2026-04-05T23:51:45.586Z"))
    }

    @Test
    fun minusDateTimeUnit() {
        // Finding a moment that's earlier than the starting point by the given length of calendar time
        val startInstant = Instant.parse("2024-03-28T02:04:56.256Z")
        val twoYearsEarlierInBerlin = startInstant.minus(2, DateTimeUnit.YEAR, TimeZone.of("Europe/Berlin"))
        check(twoYearsEarlierInBerlin == Instant.parse("2022-03-28T01:04:56.256Z"))
        val twoYearsEarlierInCairo = startInstant.minus(2, DateTimeUnit.YEAR, TimeZone.of("Africa/Cairo"))
        check(twoYearsEarlierInCairo == Instant.parse("2022-03-28T02:04:56.256Z"))
    }

    @Test
    fun plusTimeBasedUnit() {
        // Finding a moment that's later than the starting point by the given amount of real time
        val instant = Instant.fromEpochSeconds(7 * 60 * 60, nanosecondAdjustment = 123_456_789)
        val fiveHoursLater = instant.plus(5, DateTimeUnit.HOUR)
        check(fiveHoursLater.epochSeconds == 12 * 60 * 60L)
        check(fiveHoursLater.nanosecondsOfSecond == 123_456_789)
    }

    @Test
    fun minusTimeBasedUnit() {
        // Finding a moment that's earlier than the starting point by the given amount of real time
        val instant = Instant.fromEpochSeconds(7 * 60 * 60, nanosecondAdjustment = 123_456_789)
        val fiveHoursEarlier = instant.minus(5, DateTimeUnit.HOUR)
        check(fiveHoursEarlier.epochSeconds == 2 * 60 * 60L)
        check(fiveHoursEarlier.nanosecondsOfSecond == 123_456_789)
    }

    /** copy of [untilAsDateTimeUnit] */
    @Test
    fun minusAsDateTimeUnit() {
        // Finding a moment that's earlier than the starting point by the given length of calendar time
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
        // Finding a moment that's earlier than the starting point by a given amount of real time
        val instant = Instant.fromEpochSeconds(0)
        val otherInstant = Instant.fromEpochSeconds(7 * 60 * 60, nanosecondAdjustment = 123_456_789)
        val hoursBetweenInstants = otherInstant.minus(instant, DateTimeUnit.HOUR)
        check(hoursBetweenInstants == 7L)
    }

    @Test
    fun formatting() {
        // Formatting an Instant to a string using predefined and custom formats
        val epochStart = Instant.fromEpochSeconds(0)
        check(epochStart.toString() == "1970-01-01T00:00:00Z")
        check(epochStart.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET) == "1970-01-01T00:00:00Z")
        val customFormat = DateTimeComponents.Format {
            date(LocalDate.Formats.ISO_BASIC)
            hour(); minute(); second(); char('.'); secondFraction(3)
            offset(UtcOffset.Formats.FOUR_DIGITS)
        }
        check(epochStart.format(customFormat, UtcOffset(hours = 3, minutes = 30)) == "19700101033000.000+0330")
    }
}
