/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class TimeZoneSamples {

    @Test
    fun usage() {
        val zone = TimeZone.of("Europe/Berlin")
        val localDateTime = LocalDate(2021, 3, 28).atTime(2, 16, 20)
        val instant = localDateTime.toInstant(zone)
        check(instant == Instant.parse("2021-03-28T01:16:20Z"))
        val newLocalDateTime = instant.toLocalDateTime(zone)
        check(newLocalDateTime == LocalDate(2021, 3, 28).atTime(3, 16, 20))
    }

    @Test
    fun id() {
        check(TimeZone.of("America/New_York").id == "America/New_York")
    }

    @Test
    fun toStringSample() {
        val zone = TimeZone.of("America/New_York")
        check(zone.toString() == "America/New_York")
        check(zone.toString() == zone.id)
    }

    @Test
    fun equalsSample() {
        val zone1 = TimeZone.of("America/New_York")
        val zone2 = TimeZone.of("America/New_York")
        check(zone1 == zone2) // different instances, but the same ID
        val zone3 = TimeZone.of("UTC+01:00")
        val zone4 = TimeZone.of("GMT+01:00")
        check(zone3 != zone4) // the same time zone rules, but different IDs
    }

    @Test
    fun currentSystemDefault() {
        // a fixed-width format for log entries
        val logTimeFormat = DateTimeComponents.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(3)
            offset(UtcOffset.Formats.FOUR_DIGITS)
        }
        fun logEntry(message: String, now: Instant = Clock.System.now()): String {
            val formattedTime = logTimeFormat.format {
                with(TimeZone.currentSystemDefault()) {
                    setDateTime(now.toLocalDateTime())
                    setOffset(offsetAt(now))
                }
            }
            return "[$formattedTime] $message"
        }
        logEntry("Starting the application")
    }

    @Test
    fun utc() {
        val localDateTime = LocalDate(2023, 6, 2).atTime(12, 30)
        val instant = localDateTime.toInstant(TimeZone.UTC)
        check(instant == Instant.parse("2023-06-02T12:30:00Z"))
        val newInstant = instant.plus(5, DateTimeUnit.DAY, TimeZone.UTC)
        check(newInstant == Instant.parse("2023-06-07T12:30:00Z"))
        val newLocalDateTime = newInstant.toLocalDateTime(TimeZone.UTC)
        check(newLocalDateTime == LocalDate(2023, 6, 7).atTime(12, 30))
    }

    @Test
    fun constructorFunction() {
        val zone = TimeZone.of("America/New_York")
        check(zone.id == "America/New_York")
    }

    @Test
    fun availableZoneIds() {
        for (zoneId in TimeZone.availableZoneIds) {
            val zone = TimeZone.of(zoneId)
            check(zone.id == zoneId)
        }
    }

    /**
     * @see instantToLocalDateTime
     */
    @Test
    fun toLocalDateTimeWithTwoReceivers() {
        val zone = TimeZone.of("America/New_York")
        val instant = Instant.parse("2023-06-02T12:30:00Z")
        val localDateTime = with(zone) {
            instant.toLocalDateTime()
        }
        check(localDateTime == LocalDate(2023, 6, 2).atTime(8, 30))
    }

    /**
     * @see localDateTimeToInstantInZone
     */
    @Test
    fun toInstantWithTwoReceivers() {
        val zone = TimeZone.of("America/New_York")
        val localDateTime = LocalDate(2023, 6, 2).atTime(12, 30)
        val instant = with(zone) {
            localDateTime.toInstant()
        }
        check(instant == Instant.parse("2023-06-02T16:30:00Z"))
    }

    /**
     * @see offsetIn
     */
    @Test
    fun offsetAt() {
        val zone = TimeZone.of("America/New_York")
        val instant = Instant.parse("2023-06-02T12:30:00Z")
        val offset = zone.offsetAt(instant)
        check(offset == UtcOffset(hours = -4))
    }

    @Test
    fun instantToLocalDateTime() {
        val zone = TimeZone.of("America/New_York")
        val instant = Instant.parse("2023-06-02T12:30:00Z")
        val localDateTime = instant.toLocalDateTime(zone)
        check(localDateTime == LocalDate(2023, 6, 2).atTime(8, 30))
    }

    @Test
    fun instantToLocalDateTimeInOffset() {
        val offset = UtcOffset.parse("+01:30")
        val instant = Instant.fromEpochMilliseconds(1685709000000) // "2023-06-02T12:30:00Z"
        val localDateTime = instant.toLocalDateTime(offset)
        check(localDateTime == LocalDate(2023, 6, 2).atTime(14, 0))
    }

    /**
     * @see offsetAt
     */
    @Test
    fun offsetIn() {
        val zone = TimeZone.of("America/New_York")
        val instant = Instant.parse("2023-06-02T12:30:00Z")
        val offset = instant.offsetIn(zone)
        check(offset == UtcOffset(hours = -4))
    }

    /**
     * @see toInstantWithTwoReceivers
     */
    @Test
    fun localDateTimeToInstantInZone() {
        val zone = TimeZone.of("America/New_York")
        val localDateTime = LocalDate(2023, 6, 2).atTime(12, 30)
        val instant = localDateTime.toInstant(zone)
        check(instant == Instant.parse("2023-06-02T16:30:00Z"))
    }

    @Test
    fun localDateTimeToInstantInOffset() {
        val offset = UtcOffset.parse("+01:30")
        val localDateTime = LocalDate(2023, 6, 2).atTime(12, 30)
        val instant = localDateTime.toInstant(offset)
        check(instant == Instant.parse("2023-06-02T11:00:00Z"))
    }

    @Test
    fun atStartOfDayIn() {
        val zone = TimeZone.of("America/Cuiaba")
        // The normal case where `atStartOfDayIn` returns the instant of 00:00:00 in the given time zone.
        val normalDate = LocalDate(2023, 6, 2)
        val startOfDay = normalDate.atStartOfDayIn(zone)
        check(startOfDay.toLocalDateTime(zone) == normalDate.atTime(hour = 0, minute = 0))
        // The edge case where 00:00:00 does not exist in this time zone on this date due to clocks moving forward.
        val dateWithoutMidnight = LocalDate(1985, 11, 2)
        val startOfDayWithoutMidnight = dateWithoutMidnight.atStartOfDayIn(zone)
        check(startOfDayWithoutMidnight.toLocalDateTime(zone) == dateWithoutMidnight.atTime(hour = 1, minute = 0))
    }

    class FixedOffsetTimeZoneSamples {
        @Test
        fun casting() {
            val localDateTime = LocalDate(2023, 6, 2).atTime(12, 30)
            for ((zoneId, expectedString) in listOf(
                "UTC+01:30" to "2023-06-02T12:30+01:30",
                "Europe/Berlin" to "2023-06-02T12:30+02:00[Europe/Berlin]",
            )) {
                val zone = TimeZone.of(zoneId)
                // format the local date-time with either just the offset or the offset and the full time zone
                val formatted = buildString {
                    append(localDateTime)
                    if (zone is FixedOffsetTimeZone) {
                        append(zone.offset)
                    } else {
                        append(localDateTime.toInstant(zone).offsetIn(zone))
                        append('[')
                        append(zone.id)
                        append(']')
                    }
                }
                check(formatted == expectedString)
            }
        }

        @Test
        fun constructorFunction() {
            val offset = UtcOffset(hours = 1, minutes = 30)
            val zone = FixedOffsetTimeZone(offset)
            check(zone.id == "+01:30")
            check(zone.offset == offset)
        }

        @Test
        fun offset() {
            val zone = TimeZone.of("UTC+01:30") as FixedOffsetTimeZone
            check(zone.id == "UTC+01:30")
            check(zone.offset == UtcOffset(hours = 1, minutes = 30))
        }
    }
}
