/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class LocalDateTimeSamples {

    @Test
    fun alternativeConstruction() {
        // Constructing a LocalDateTime value by specifying its components
        val dateTime1 = LocalDateTime(year = 2021, month = 3, day = 27, hour = 2, minute = 16, second = 20)
        val dateTime2 = LocalDateTime(
            year = 2021, month = Month.MARCH, day = 27,
            hour = 2, minute = 16, second = 20, nanosecond = 0
        )
        check(dateTime1 == dateTime2)
    }

    @Test
    fun simpleParsingAndFormatting() {
        // Parsing and formatting LocalDateTime values
        val dateTime = LocalDateTime.parse("2024-02-15T08:30:15.1234567")
        check(dateTime == LocalDate(2024, 2, 15).atTime(8, 30, 15, 123_456_700))
        val formatted = dateTime.toString()
        check(formatted == "2024-02-15T08:30:15.123456700")
    }

    @Test
    fun parsing() {
        // Parsing LocalDateTime values using predefined and custom formats
        check(LocalDateTime.parse("2024-02-15T08:30:15.123456789") ==
            LocalDate(2024, 2, 15).atTime(8, 30, 15, 123_456_789))
        check(LocalDateTime.parse("2024-02-15T08:30") ==
            LocalDate(2024, 2, 15).atTime(8, 30))
        val customFormat = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour(); char(':'); minute(); char(':'); second()
            char(','); secondFraction(fixedLength = 3)
        }
        check(LocalDateTime.parse("2024-02-15 08:30:15,123", customFormat) ==
            LocalDate(2024, 2, 15).atTime(8, 30, 15, 123_000_000))
    }

    @Test
    fun customFormat() {
        // Parsing and formatting LocalDateTime values using a custom format
        val customFormat = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour(); char(':'); minute(); char(':'); second()
            char(','); secondFraction(fixedLength = 3)
        }
        val dateTime = LocalDate(2024, 2, 15)
            .atTime(8, 30, 15, 123_456_789)
        check(dateTime.format(customFormat) == "2024-02-15 08:30:15,123")
        check(customFormat.parse("2024-02-15 08:30:15,123") ==
            LocalDate(2024, 2, 15).atTime(8, 30, 15, 123_000_000)
        )
        check(dateTime.format(LocalDateTime.Formats.ISO) == "2024-02-15T08:30:15.123456789")
    }

    @Test
    fun constructorFunctionWithMonthNumber() {
        // Constructing a LocalDateTime value using its constructor
        val dateTime = LocalDateTime(
            year = 2024,
            month = 2,
            day = 15,
            hour = 16,
            minute = 48,
            second = 59,
            nanosecond = 999_999_999
        )
        check(dateTime.date == LocalDate(2024, 2, 15))
        check(dateTime.time == LocalTime(16, 48, 59, 999_999_999))
        val dateTimeWithoutSeconds = LocalDateTime(
            year = 2024,
            month = 2,
            day = 15,
            hour = 16,
            minute = 48
        )
        check(dateTimeWithoutSeconds.date == LocalDate(2024, 2, 15))
        check(dateTimeWithoutSeconds.time == LocalTime(16, 48))
    }

    @Test
    fun constructorFunction() {
        // Constructing a LocalDateTime value using its constructor
        val dateTime = LocalDateTime(
            year = 2024,
            month = Month.FEBRUARY,
            day = 15,
            hour = 16,
            minute = 48,
            second = 59,
            nanosecond = 999_999_999
        )
        check(dateTime.date == LocalDate(2024, Month.FEBRUARY, 15))
        check(dateTime.time == LocalTime(16, 48, 59, 999_999_999))
        val dateTimeWithoutSeconds = LocalDateTime(
            year = 2024,
            month = Month.FEBRUARY,
            day = 15,
            hour = 16,
            minute = 48
        )
        check(dateTimeWithoutSeconds.date == LocalDate(2024, Month.FEBRUARY, 15))
        check(dateTimeWithoutSeconds.time == LocalTime(16, 48))
    }

    @Test
    fun fromDateAndTime() {
        // Converting a LocalDate and a LocalTime to a LocalDateTime value and getting them back
        val date = LocalDate(2024, 2, 15)
        val time = LocalTime(16, 48)
        val dateTime = LocalDateTime(date, time)
        check(dateTime.date == date)
        check(dateTime.time == time)
        check(dateTime == date.atTime(time))
        check(dateTime == time.atDate(date))
    }

    @Test
    fun dateComponents() {
        // Accessing the date components of a LocalDateTime value
        val date = LocalDate(2024, 2, 15)
        val time = LocalTime(hour = 16, minute = 48, second = 59, nanosecond = 999_999_999)
        val dateTime = LocalDateTime(date, time)
        check(dateTime.year == dateTime.date.year)
        check(dateTime.month == dateTime.date.month)
        check(dateTime.day == dateTime.date.day)
        check(dateTime.dayOfWeek == dateTime.date.dayOfWeek)
        check(dateTime.dayOfYear == dateTime.date.dayOfYear)
    }

    @Test
    fun timeComponents() {
        // Accessing the time components of a LocalDateTime value
        val date = LocalDate(2024, 2, 15)
        val time = LocalTime(hour = 16, minute = 48, second = 59, nanosecond = 999_999_999)
        val dateTime = LocalDateTime(date, time)
        check(dateTime.hour == dateTime.time.hour)
        check(dateTime.minute == dateTime.time.minute)
        check(dateTime.second == dateTime.time.second)
        check(dateTime.nanosecond == dateTime.time.nanosecond)
    }

    @Test
    fun dateAndTime() {
        // Constructing a LocalDateTime value from a LocalDate and a LocalTime
        val date = LocalDate(2024, 2, 15)
        val time = LocalTime(16, 48)
        val dateTime = LocalDateTime(date, time)
        check(dateTime.date == date)
        check(dateTime.time == time)
    }

    @Test
    fun compareToSample() {
        // Comparing LocalDateTime values
        val date = LocalDate(2024, 2, 15)
        val laterDate = LocalDate(2024, 2, 16)
        check(date.atTime(hour = 23, minute = 59) < laterDate.atTime(hour = 0, minute = 0))
        check(date.atTime(hour = 8, minute = 30) < date.atTime(hour = 17, minute = 10))
        check(date.atTime(hour = 8, minute = 30) < date.atTime(hour = 8, minute = 31))
        check(date.atTime(hour = 8, minute = 30) < date.atTime(hour = 8, minute = 30, second = 1))
        check(date.atTime(hour = 8, minute = 30) < date.atTime(hour = 8, minute = 30, second = 0, nanosecond = 1))
    }

    @Test
    fun toStringSample() {
        // Converting LocalDateTime values to strings
        check(LocalDate(2024, 2, 15).atTime(16, 48).toString() == "2024-02-15T16:48")
        check(LocalDate(2024, 2, 15).atTime(16, 48, 15).toString() == "2024-02-15T16:48:15")
        check(LocalDate(2024, 2, 15).atTime(16, 48, 15, 120_000_000).toString() == "2024-02-15T16:48:15.120")
    }

    @Test
    fun formatting() {
        // Formatting LocalDateTime values using predefined and custom formats
        check(LocalDate(2024, 2, 15).atTime(16, 48).toString() == "2024-02-15T16:48")
        check(LocalDate(2024, 2, 15).atTime(16, 48).format(LocalDateTime.Formats.ISO) == "2024-02-15T16:48:00")
        val customFormat = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour(); char(':'); minute()
            optional {
                char(':'); second()
                optional {
                    char('.'); secondFraction(minLength = 3)
                }
            }
        }
        val dateTime1 = LocalDate(2024, 2, 15).atTime(8, 30)
        check(dateTime1.format(customFormat) == "2024-02-15 08:30")
        val dateTime2 = LocalDate(2023, 12, 31).atTime(8, 30, 0, 120_000_000)
        check(dateTime2.format(customFormat) == "2023-12-31 08:30:00.120")
    }

    class Formats {
        @Test
        fun iso() {
            // Parsing and formatting LocalDateTime values using the ISO format
            val dateTime1 = LocalDate(2024, 2, 15)
                .atTime(hour = 8, minute = 30, second = 15, nanosecond = 160_000_000)
            val dateTime2 = LocalDate(2024, 2, 15)
                .atTime(hour = 8, minute = 30, second = 15)
            val dateTime3 = LocalDate(2024, 2, 15)
                .atTime(hour = 8, minute = 30)
            check(LocalDateTime.Formats.ISO.parse("2024-02-15T08:30:15.16") == dateTime1)
            check(LocalDateTime.Formats.ISO.parse("2024-02-15T08:30:15") == dateTime2)
            check(LocalDateTime.Formats.ISO.parse("2024-02-15T08:30") == dateTime3)
            check(LocalDateTime.Formats.ISO.format(dateTime1) == "2024-02-15T08:30:15.16")
            check(LocalDateTime.Formats.ISO.format(dateTime2) == "2024-02-15T08:30:15")
            check(LocalDateTime.Formats.ISO.format(dateTime3) == "2024-02-15T08:30:00")
        }
    }
}
