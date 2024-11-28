/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.time.*
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
        // Parsing an Instant from a string
        check(Instant.parse("1970-01-01T00:00:00Z") == Instant.fromEpochSeconds(0))
    }

    @Test
    fun isDistantPast() {
        // Checking if an instant is so far in the past that it's probably irrelevant
        val currentInstant = Clock.System.now()
        val tenThousandYearsAgo = currentInstant.minus(24.hours * 365 * 1_000)
        check(!tenThousandYearsAgo.isDistantPast)
        check(Instant.DISTANT_PAST.isDistantPast)
    }

    @Test
    fun isDistantFuture() {
        // Checking if an instant is so far in the future that it's probably irrelevant
        val currentInstant = Clock.System.now()
        val tenThousandYearsLater = currentInstant.plus(24.hours * 365 * 10_000)
        check(!tenThousandYearsLater.isDistantFuture)
        check(Instant.DISTANT_FUTURE.isDistantFuture)
    }
}
