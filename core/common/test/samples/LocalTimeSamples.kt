/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.random.*
import kotlin.test.*

class LocalTimeSamples {

    @Test
    fun construction() {
        // Constructing a LocalTime using its constructor
        val night = LocalTime(hour = 23, minute = 13, second = 16, nanosecond = 153_200_001)
        check(night.hour == 23)
        check(night.minute == 13)
        check(night.second == 16)
        check(night.nanosecond == 153_200_001)

        val noon = LocalTime(12, 0)
        check(noon.hour == 12)
        check(noon.minute == 0)
        check(noon.second == 0)
        check(noon.nanosecond == 0)

        val evening = LocalTime(hour = 18, minute = 31, second = 54)
        check(evening.nanosecond == 0)
    }

    @Test
    fun representingAsNumbers() {
        // Representing a LocalTime as the number of seconds, milliseconds, or nanoseconds since the start of the day
        val time = LocalTime(hour = 8, minute = 30, second = 15, nanosecond = 123_456_789)
        // The number of whole seconds since the start of the day:
        val timeAsSecondOfDay = time.toSecondOfDay()
        check(timeAsSecondOfDay == 8 * 60 * 60 + 30 * 60 + 15)
        // The number of whole milliseconds since the start of the day:
        val timeAsMillisecondOfDay = time.toMillisecondOfDay()
        check(timeAsMillisecondOfDay == timeAsSecondOfDay * 1_000 + 123)
        // The number of nanoseconds since the start of the day:
        val timeAsNanosecondOfDay = time.toNanosecondOfDay()
        check(timeAsNanosecondOfDay == timeAsMillisecondOfDay * 1_000_000L + 456_789)
        // The local time is completely defined by the number of nanoseconds since the start of the day:
        val reconstructedTime = LocalTime.fromNanosecondOfDay(timeAsNanosecondOfDay)
        check(reconstructedTime == time)
    }

    @Test
    fun simpleParsingAndFormatting() {
        // Parsing a LocalTime from a string and formatting it back
        val time = LocalTime.parse("08:30:15.1234567")
        check(time == LocalTime(8, 30, 15, 123_456_700))
        val formatted = time.toString()
        check(formatted == "08:30:15.123456700")
    }

    @Test
    fun parsing() {
        // Parsing a LocalTime from a string using predefined and custom formats
        check(LocalTime.parse("08:30:15.123456789") == LocalTime(8, 30, 15, 123_456_789))
        check(LocalTime.parse("08:30:15") == LocalTime(8, 30, 15))
        check(LocalTime.parse("08:30") == LocalTime(8, 30))
        val customFormat = LocalTime.Format {
            hour(); char(':'); minute(); char(':'); second()
            alternativeParsing({ char(',') }) { char('.') } // parse either a dot or a comma
            secondFraction(fixedLength = 3)
        }
        check(LocalTime.parse("08:30:15,123", customFormat) == LocalTime(8, 30, 15, 123_000_000))
    }

    @Test
    fun fromAndToSecondOfDay() {
        // Converting a LocalTime to the number of seconds since the start of the day and back
        val secondsInDay = 24 * 60 * 60
        val randomNumberOfSeconds = Random.nextInt(secondsInDay)
        val time = LocalTime.fromSecondOfDay(randomNumberOfSeconds)
        check(time.toSecondOfDay() == randomNumberOfSeconds)
        check(time.nanosecond == 0) // sub-second part is zero
    }

    @Test
    fun fromAndToMillisecondOfDay() {
        // Converting a LocalTime to the number of milliseconds since the start of the day and back
        val millisecondsInDay = 24 * 60 * 60 * 1_000
        val randomNumberOfMilliseconds = Random.nextInt(millisecondsInDay)
        val time = LocalTime.fromMillisecondOfDay(randomNumberOfMilliseconds)
        check(time.toMillisecondOfDay() == randomNumberOfMilliseconds)
        check(time.nanosecond % 1_000_000 == 0) // sub-millisecond part is zero
    }

    @Test
    fun fromAndToNanosecondOfDay() {
        // Converting a LocalTime to the number of nanoseconds since the start of the day and back
        val originalTime = LocalTime(
            hour = Random.nextInt(24),
            minute = Random.nextInt(60),
            second = Random.nextInt(60),
            nanosecond = Random.nextInt(1_000_000_000)
        )
        val nanosecondOfDay = originalTime.toNanosecondOfDay()
        val reconstructedTime = LocalTime.fromNanosecondOfDay(nanosecondOfDay)
        check(originalTime == reconstructedTime)
    }

    @Test
    fun customFormat() {
        // Parsing and formatting LocalTime values using a custom format
        val customFormat = LocalTime.Format {
            hour(); char(':'); minute(); char(':'); second()
            char(','); secondFraction(fixedLength = 3)
        }
        val time = LocalTime(8, 30, 15, 123_456_789)
        check(time.format(customFormat) == "08:30:15,123")
        check(time.format(LocalTime.Formats.ISO) == "08:30:15.123456789")
    }

    @Test
    fun constructorFunction() {
        // Constructing a LocalTime using its constructor
        val time = LocalTime(8, 30, 15, 123_456_789)
        check(time.hour == 8)
        check(time.minute == 30)
        check(time.second == 15)
        check(time.nanosecond == 123_456_789)
        val timeWithoutSeconds = LocalTime(23, 30)
        check(timeWithoutSeconds.hour == 23)
        check(timeWithoutSeconds.minute == 30)
        check(timeWithoutSeconds.second == 0)
        check(timeWithoutSeconds.nanosecond == 0)
    }

    @Test
    fun hour() {
        // Getting the number of whole hours shown on the clock
        check(LocalTime(8, 30, 15, 123_456_789).hour == 8)
    }

    @Test
    fun minute() {
        // Getting the number of whole minutes that don't form a whole hour
        check(LocalTime(8, 30, 15, 123_456_789).minute == 30)
    }

    @Test
    fun second() {
        // Getting the number of whole seconds that don't form a whole minute
        check(LocalTime(8, 30).second == 0)
        check(LocalTime(8, 30, 15, 123_456_789).second == 15)
    }

    @Test
    fun nanosecond() {
        // Getting the sub-second part of a LocalTime
        check(LocalTime(8, 30).nanosecond == 0)
        check(LocalTime(8, 30, 15).nanosecond == 0)
        check(LocalTime(8, 30, 15, 123_456_789).nanosecond == 123_456_789)
    }

    @Test
    fun toSecondOfDay() {
        // Obtaining the number of seconds a clock has to advance since 00:00 to reach the given time
        check(LocalTime(0, 0, 0, 0).toSecondOfDay() == 0)
        check(LocalTime(0, 0, 0, 1).toSecondOfDay() == 0)
        check(LocalTime(0, 0, 1, 0).toSecondOfDay() == 1)
        check(LocalTime(0, 1, 0, 0).toSecondOfDay() == 60)
        check(LocalTime(1, 0, 0, 0).toSecondOfDay() == 3_600)
        check(LocalTime(1, 1, 1, 0).toSecondOfDay() == 3_600 + 60 + 1)
        check(LocalTime(1, 1, 1, 999_999_999).toSecondOfDay() == 3_600 + 60 + 1)
    }

    @Test
    fun toMillisecondOfDay() {
        // Obtaining the number of milliseconds a clock has to advance since 00:00 to reach the given time
        check(LocalTime(0, 0, 0, 0).toMillisecondOfDay() == 0)
        check(LocalTime(0, 0, 0, 1).toMillisecondOfDay() == 0)
        check(LocalTime(0, 0, 1, 0).toMillisecondOfDay() == 1000)
        check(LocalTime(0, 1, 0, 0).toMillisecondOfDay() == 60_000)
        check(LocalTime(1, 0, 0, 0).toMillisecondOfDay() == 3_600_000)
        check(LocalTime(1, 1, 1, 1).toMillisecondOfDay() == 3_600_000 + 60_000 + 1_000)
        check(LocalTime(1, 1, 1, 1_000_000).toMillisecondOfDay() == 3_600_000 + 60_000 + 1_000 + 1)
    }

    @Test
    fun toNanosecondOfDay() {
        // Obtaining the number of nanoseconds a clock has to advance since 00:00 to reach the given time
        check(LocalTime(0, 0, 0, 0).toNanosecondOfDay() == 0L)
        check(LocalTime(0, 0, 0, 1).toNanosecondOfDay() == 1L)
        check(LocalTime(0, 0, 1, 0).toNanosecondOfDay() == 1_000_000_000L)
        check(LocalTime(0, 1, 0, 0).toNanosecondOfDay() == 60_000_000_000L)
        check(LocalTime(1, 0, 0, 0).toNanosecondOfDay() == 3_600_000_000_000L)
        check(LocalTime(1, 1, 1, 1).toNanosecondOfDay() == 3_600_000_000_000L + 60_000_000_000 + 1_000_000_000 + 1)
    }

    @Test
    fun compareTo() {
        // Comparing LocalTime values
        check(LocalTime(hour = 8, minute = 30) < LocalTime(hour = 17, minute = 10))
        check(LocalTime(hour = 8, minute = 30) < LocalTime(hour = 8, minute = 31))
        check(LocalTime(hour = 8, minute = 30) < LocalTime(hour = 8, minute = 30, second = 1))
        check(LocalTime(hour = 8, minute = 30) < LocalTime(hour = 8, minute = 30, second = 0, nanosecond = 1))
    }

    @Test
    fun toStringSample() {
        // Converting a LocalTime to a human-readable string
        check(LocalTime(hour = 8, minute = 30).toString() == "08:30")
        check(LocalTime(hour = 8, minute = 30, second = 15).toString() == "08:30:15")
        check(LocalTime(hour = 8, minute = 30, second = 0, nanosecond = 120000000).toString() == "08:30:00.120")
    }

    @Test
    fun formatting() {
        // Formatting LocalTime values using predefined and custom formats
        check(LocalTime(hour = 8, minute = 30).toString() == "08:30")
        check(LocalTime(hour = 8, minute = 30).format(LocalTime.Formats.ISO) == "08:30:00")
        val customFormat = LocalTime.Format {
            hour(); char(':'); minute()
            optional {
                char(':'); second()
                optional {
                    char('.'); secondFraction(minLength = 3)
                }
            }
        }
        val timeWithZeroSeconds = LocalTime(hour = 8, minute = 30)
        check(timeWithZeroSeconds.format(customFormat) == "08:30")
        val timeWithNonZeroSecondFraction = LocalTime(hour = 8, minute = 30, second = 0, nanosecond = 120000000)
        check(timeWithNonZeroSecondFraction.format(customFormat) == "08:30:00.120")
    }

    /**
     * @see atDateComponentWise
     */
    @Test
    fun atDateComponentWiseMonthNumber() {
        // Using the `atDate` function to covert a sequence of `LocalDate` values to `LocalDateTime`
        val morning = LocalTime(8, 30)
        val firstMorningOfEveryMonth = (1..12).map { month ->
            morning.atDate(2021, month, 1)
        }
        check(firstMorningOfEveryMonth.all { it.time == morning && it.day == 1 })
    }

    /**
     * @see atDateComponentWiseMonthNumber
     */
    @Test
    fun atDateComponentWise() {
        // Using the `atDate` function to covert a sequence of `LocalDate` values to `LocalDateTime`
        val morning = LocalTime(8, 30)
        val firstMorningOfEveryMonth = Month.entries.map { month ->
            morning.atDate(2021, month, 1)
        }
        check(firstMorningOfEveryMonth.all { it.time == morning && it.day == 1 })
    }

    @Test
    fun atDate() {
        // Using the `atDate` function to covert a sequence of `LocalDate` values to `LocalDateTime`
        val workdayStart = LocalTime(8, 30)
        val startDate = LocalDate(2021, Month.JANUARY, 1)
        val endDate = LocalDate(2021, Month.DECEMBER, 31)
        val allWorkdays = buildList {
            var currentDate = startDate
            while (currentDate <= endDate) {
                if (currentDate.dayOfWeek != DayOfWeek.SATURDAY && currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
                    add(currentDate)
                }
                currentDate = currentDate.plus(1, DateTimeUnit.DAY)
            }
        }
        val allStartsOfWorkdays = allWorkdays.map {
            workdayStart.atDate(it)
        }
        check(allStartsOfWorkdays.all { it.time == workdayStart })
    }

    class Formats {
        @Test
        fun iso() {
            // Parsing and formatting LocalTime values using the ISO format
            val timeWithNanoseconds = LocalTime(hour = 8, minute = 30, second = 15, nanosecond = 160_000_000)
            val timeWithSeconds = LocalTime(hour = 8, minute = 30, second = 15)
            val timeWithoutSeconds = LocalTime(hour = 8, minute = 30)
            check(LocalTime.Formats.ISO.parse("08:30:15.16") == timeWithNanoseconds)
            check(LocalTime.Formats.ISO.parse("08:30:15") == timeWithSeconds)
            check(LocalTime.Formats.ISO.parse("08:30") == timeWithoutSeconds)
            check(LocalTime.Formats.ISO.format(timeWithNanoseconds) == "08:30:15.16")
            check(LocalTime.Formats.ISO.format(timeWithSeconds) == "08:30:15")
            check(LocalTime.Formats.ISO.format(timeWithoutSeconds) == "08:30:00")
        }
    }
}
