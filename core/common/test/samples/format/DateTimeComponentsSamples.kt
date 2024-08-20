/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateTimeComponentsSamples {

    @Test
    fun parsingComplexInput() {
        // Parsing a complex date-time string and extracting all its components
        val input = "2020-03-16T23:59:59.999999999+03:00"
        val components = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parse(input)
        check(components.toLocalDateTime() == LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999))
        check(components.toInstantUsingOffset() == Instant.parse("2020-03-16T20:59:59.999999999Z"))
        check(components.toUtcOffset() == UtcOffset(3, 0))
    }

    @Test
    fun parsingInvalidInput() {
        // Parsing an invalid input and handling the error
        val input = "23:59:60"
        val extraDay: Boolean
        val time = DateTimeComponents.Format {
            time(LocalTime.Formats.ISO)
        }.parse(input).apply {
            if (hour == 23 && minute == 59 && second == 60) {
                hour = 0; minute = 0; second = 0; extraDay = true
            } else {
                extraDay = false
            }
        }.toLocalTime()
        check(time == LocalTime(0, 0))
        check(extraDay)
    }

    @Test
    fun simpleFormatting() {
        // Formatting a multi-component date-time entity
        val formatted = DateTimeComponents.Formats.RFC_1123.format {
            setDateTimeOffset(
                LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999),
                UtcOffset(hours = 3)
            )
        }
        check(formatted == "Mon, 16 Mar 2020 23:59:59 +0300")
    }

    @Test
    fun customFormat() {
        // Formatting and parsing a complex entity with a custom format
        val customFormat = DateTimeComponents.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(3)
            char(' ')
            offset(UtcOffset.Formats.FOUR_DIGITS)
        }
        val formatted = customFormat.format {
            setDate(LocalDate(2023, 1, 2))
            setTime(LocalTime(3, 46, 58, 530_000_000))
            setOffset(UtcOffset(3, 30))
        }
        check(formatted == "2023-01-02 03:46:58.530 +0330")
        val parsed = customFormat.parse("2023-01-31 24:00:00.530 +0330").apply {
            // components can be out of bounds
            if (hour == 24 && minute == 0 && second == 0) {
                setTime(LocalTime(0, 0))
                setDate(toLocalDate().plus(1, DateTimeUnit.DAY))
            }
        }
        check(parsed.toLocalDate() == LocalDate(2023, 2, 1))
        check(parsed.toLocalTime() == LocalTime(0, 0))
        check(parsed.toUtcOffset() == UtcOffset(3, 30))
    }

    @Test
    fun setDateTime() {
        // Setting the date-time components for formatting
        val dateTime = LocalDate(2021, 3, 28).atTime(2, 16, 20)
        val customFormat = DateTimeComponents.Format {
            dateTime(LocalDateTime.Formats.ISO)
            char('[')
            timeZoneId()
            char(']')
        }
        val formatted = customFormat.format {
            setDateTime(dateTime)
            timeZoneId = "America/New_York"
        }
        check(formatted == "2021-03-28T02:16:20[America/New_York]")
    }

    @Test
    fun setDateTimeOffsetInstant() {
        // Setting the Instant and UTC offset components for formatting
        val instant = Instant.parse("2021-03-28T02:16:20+03:00")
        val offset = UtcOffset(3, 0)
        val formatted = DateTimeComponents.Formats.RFC_1123.format {
            setDateTimeOffset(instant, offset)
        }
        check(formatted == "Sun, 28 Mar 2021 02:16:20 +0300")
    }

    @Test
    fun setDateTimeOffset() {
        // Setting the date-time and UTC offset components for parsing
        val localDateTime = LocalDate(2021, 3, 28).atTime(2, 16, 20)
        val offset = UtcOffset(3, 0)
        val formatted = DateTimeComponents.Formats.RFC_1123.format {
            setDateTimeOffset(localDateTime, offset)
        }
        check(formatted == "Sun, 28 Mar 2021 02:16:20 +0300")
    }

    @Test
    fun dayOfWeek() {
        // Formatting and parsing a date with the day of the week in complex scenarios
        val formatWithDayOfWeek = DateTimeComponents.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
            char(' ')
            date(LocalDate.Formats.ISO)
        }
        val formattedWithDayOfWeek = formatWithDayOfWeek.format {
            setDate(LocalDate(2021, 3, 28))
            check(dayOfWeek == DayOfWeek.SUNDAY) // `setDate` sets the day of the week automatically
        }
        check(formattedWithDayOfWeek == "Sun 2021-03-28")
        val parsedWithDayOfWeek = formatWithDayOfWeek.parse("Sun 2021-03-28")
        check(parsedWithDayOfWeek.toLocalDate() == LocalDate(2021, 3, 28))
        check(parsedWithDayOfWeek.dayOfWeek == DayOfWeek.SUNDAY)
        // Note: the day of the week is only parsed when it's present in the format
        val formatWithoutDayOfWeek = DateTimeComponents.Format {
            date(LocalDate.Formats.ISO)
        }
        val parsedWithoutDayOfWeek = formatWithoutDayOfWeek.parse("2021-03-28")
        check(parsedWithoutDayOfWeek.dayOfWeek == null)
    }

    @Test
    fun date() {
        // Formatting and parsing a date in complex scenarios
        val format = DateTimeComponents.Format {
            year(); char('-'); monthNumber(); char('-'); day()
        }
        val formattedDate = format.format {
            setDate(LocalDate(2023, 1, 2))
            check(year == 2023)
            check(month == Month.JANUARY)
            check(day == 2)
            check(dayOfWeek == DayOfWeek.MONDAY)
        }
        check(formattedDate == "2023-01-02")
        val parsedDate = format.parse("2023-01-02")
        check(parsedDate.toLocalDate() == LocalDate(2023, 1, 2))
        check(parsedDate.year == 2023)
        check(parsedDate.month == Month.JANUARY)
        check(parsedDate.day == 2)
        check(parsedDate.dayOfWeek == null)
    }

    @Test
    fun setMonth() {
        // Setting the month using the `month` property
        val input = "Mon, 30 Jul 2008 11:05:30 GMT"
        val parsed = DateTimeComponents.Formats.RFC_1123.parse(input)
        check(parsed.monthNumber == 7)
        check(parsed.month == Month.JULY)
        parsed.month = Month.JUNE
        check(parsed.monthNumber == 6)
        check(parsed.month == Month.JUNE)
    }

    @Test
    fun timeAmPm() {
        // Formatting and parsing a time with AM/PM marker in complex scenarios
        val format = DateTimeComponents.Format {
            amPmHour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(1, 9)
            char(' '); amPmMarker("AM", "PM")
        }
        val formattedTime = format.format {
            setTime(LocalTime(3, 46, 58, 123_456_789))
            check(hour == 3)
            check(minute == 46)
            check(second == 58)
            check(nanosecond == 123_456_789)
            check(hourOfAmPm == 3)
            check(amPm == AmPmMarker.AM)
        }
        check(formattedTime == "03:46:58.123456789 AM")
        val parsedTime = format.parse("03:46:58.123456789 AM")
        check(parsedTime.toLocalTime() == LocalTime(3, 46, 58, 123_456_789))
        check(parsedTime.hour == null)
        check(parsedTime.minute == 46)
        check(parsedTime.second == 58)
        check(parsedTime.nanosecond == 123_456_789)
        check(parsedTime.hourOfAmPm == 3)
        check(parsedTime.amPm == AmPmMarker.AM)
    }

    @Test
    fun time() {
        // Formatting and parsing a time in complex scenarios
        val format = DateTimeComponents.Format {
            hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(1, 9)
        }
        val formattedTime = format.format {
            setTime(LocalTime(3, 46, 58, 123_456_789))
            check(hour == 3)
            check(minute == 46)
            check(second == 58)
            check(nanosecond == 123_456_789)
            check(hourOfAmPm == 3)
            check(amPm == AmPmMarker.AM)
        }
        check(formattedTime == "03:46:58.123456789")
        val parsedTime = format.parse("03:46:58.123456789")
        check(parsedTime.toLocalTime() == LocalTime(3, 46, 58, 123_456_789))
        check(parsedTime.hour == 3)
        check(parsedTime.minute == 46)
        check(parsedTime.second == 58)
        check(parsedTime.nanosecond == 123_456_789)
        check(parsedTime.hourOfAmPm == null)
        check(parsedTime.amPm == null)
    }

    @Test
    fun offset() {
        // Formatting and parsing a UTC offset in complex scenarios
        val format = DateTimeComponents.Format { offset(UtcOffset.Formats.ISO) }
        val formattedOffset = format.format {
            setOffset(UtcOffset(-3, -30, -15))
            check(offsetHours == 3)
            check(offsetMinutesOfHour == 30)
            check(offsetSecondsOfMinute == 15)
            check(offsetIsNegative == true)
        }
        check(formattedOffset == "-03:30:15")
        val parsedOffset = format.parse("-03:30:15")
        check(parsedOffset.toUtcOffset() == UtcOffset(-3, -30, -15))
        check(parsedOffset.offsetHours == 3)
        check(parsedOffset.offsetMinutesOfHour == 30)
        check(parsedOffset.offsetSecondsOfMinute == 15)
        check(parsedOffset.offsetIsNegative == true)
    }

    @Test
    fun timeZoneId() {
        // Formatting and parsing a time zone ID as part of a complex format
        val formatWithTimeZone = DateTimeComponents.Format {
            dateTime(LocalDateTime.Formats.ISO)
            char('[')
            timeZoneId()
            char(']')
        }
        val formattedWithTimeZone = formatWithTimeZone.format {
            setDateTime(LocalDate(2021, 3, 28).atTime(2, 16, 20))
            timeZoneId = "America/New_York"
        }
        check(formattedWithTimeZone == "2021-03-28T02:16:20[America/New_York]")
        val parsedWithTimeZone = DateTimeComponents.parse(formattedWithTimeZone, formatWithTimeZone)
        check(parsedWithTimeZone.timeZoneId == "America/New_York")
        try {
            formatWithTimeZone.parse("2021-03-28T02:16:20[Mars/Phobos]")
            fail("Expected an exception")
        } catch (e: DateTimeFormatException) {
            // expected: the time zone ID is invalid
        }
    }

    @Test
    fun toUtcOffset() {
        // Obtaining a UTC offset from the parsed data
        val rfc1123Input = "Sun, 06 Nov 1994 08:49:37 +0300"
        val parsed = DateTimeComponents.Formats.RFC_1123.parse(rfc1123Input)
        val offset = parsed.toUtcOffset()
        check(offset == UtcOffset(3, 0))
    }

    @Test
    fun toLocalDate() {
        // Obtaining a LocalDate from the parsed data
        val rfc1123Input = "Sun, 06 Nov 1994 08:49:37 +0300"
        val parsed = DateTimeComponents.Formats.RFC_1123.parse(rfc1123Input)
        val localDate = parsed.toLocalDate()
        check(localDate == LocalDate(1994, 11, 6))
    }

    @Test
    fun toLocalTime() {
        // Obtaining a LocalTime from the parsed data
        val rfc1123Input = "Sun, 06 Nov 1994 08:49:37 +0300"
        val parsed = DateTimeComponents.Formats.RFC_1123.parse(rfc1123Input)
        val localTime = parsed.toLocalTime()
        check(localTime == LocalTime(8, 49, 37))
    }

    @Test
    fun toLocalDateTime() {
        // Obtaining a LocalDateTime from the parsed data
        val rfc1123Input = "Sun, 06 Nov 1994 08:49:37 +0300"
        val parsed = DateTimeComponents.Formats.RFC_1123.parse(rfc1123Input)
        val localDateTime = parsed.toLocalDateTime()
        check(localDateTime == LocalDateTime(1994, 11, 6, 8, 49, 37))
    }

    @Test
    fun toInstantUsingOffset() {
        // Obtaining an Instant from the parsed data using the given UTC offset
        val rfc1123Input = "Sun, 06 Nov 1994 08:49:37 +0300"
        val parsed = DateTimeComponents.Formats.RFC_1123.parse(rfc1123Input)
        val instant = parsed.toInstantUsingOffset()
        check(instant == Instant.parse("1994-11-06T08:49:37+03:00"))
        val localDateTime = parsed.toLocalDateTime()
        val offset = parsed.toUtcOffset()
        check(localDateTime.toInstant(offset) == instant)
    }

    @Test
    fun formatting() {
        // Formatting partial, complex, or broken data
        // DateTimeComponents can be used to format complex data that consists of multiple components
        val compoundFormat = DateTimeComponents.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(3)
            char(' ')
            offsetHours(); char(':'); offsetMinutesOfHour(); char(':'); offsetSecondsOfMinute()
        }
        val formattedCompoundData = compoundFormat.format {
            setDate(LocalDate(2023, 1, 2))
            setTime(LocalTime(3, 46, 58, 531_000_000))
            setOffset(UtcOffset(3, 30))
        }
        check(formattedCompoundData == "2023-01-02 03:46:58.531 +03:30:00")
        // It can also be used to format partial data that is missing some components
        val partialFormat = DateTimeComponents.Format {
            year(); char('-'); monthNumber()
        }
        val formattedPartialData = partialFormat.format {
            year = 2023
            month = Month.JANUARY
        }
        check(formattedPartialData == "2023-01")
    }

    @Test
    fun parsing() {
        // Parsing partial, complex, or broken data
        // DateTimeComponents can be used to parse complex data that consists of multiple components
        val compoundFormat = DateTimeComponents.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(3)
            char(' ')
            offsetHours(); char(':'); offsetMinutesOfHour(); optional { char(':'); offsetSecondsOfMinute() }
        }
        val parsedCompoundData = DateTimeComponents.parse("2023-01-02 03:46:58.531 +03:30", compoundFormat)
        check(parsedCompoundData.toLocalTime() == LocalTime(3, 46, 58, 531_000_000))
        check(parsedCompoundData.toLocalDate() == LocalDate(2023, 1, 2))
        check(parsedCompoundData.toUtcOffset() == UtcOffset(3, 30))
        check(parsedCompoundData.toInstantUsingOffset() == Instant.parse("2023-01-02T03:46:58.531+03:30"))
        // It can also be used to parse partial data that is missing some components
        val partialFormat = DateTimeComponents.Format {
            year(); char('-'); monthNumber()
        }
        val parsedPartialData = DateTimeComponents.parse("2023-01", partialFormat)
        check(parsedPartialData.year == 2023)
        check(parsedPartialData.month == Month.JANUARY)
        try {
            parsedPartialData.toLocalDate()
            fail("Expected an exception")
        } catch (e: IllegalArgumentException) {
            // expected: the day is missing, so LocalDate cannot be constructed
        }
    }

    class Formats {
        @Test
        fun rfc1123parsing() {
            // Parsing a date-time string in the RFC 1123 format and extracting all its components
            val rfc1123string = "Mon, 30 Jun 2008 11:05:30 -0300"
            val parsed = DateTimeComponents.Formats.RFC_1123.parse(rfc1123string)
            check(parsed.toLocalDate() == LocalDate(2008, 6, 30))
            check(parsed.toLocalTime() == LocalTime(11, 5, 30))
            check(parsed.toUtcOffset() == UtcOffset(-3, 0))
        }

        @Test
        fun rfc1123formatting() {
            // Formatting a date-time using the given UTC offset in the RFC 1123 format
            val today = Instant.fromEpochSeconds(1713182461)
            val offset = today.offsetIn(TimeZone.of("Europe/Berlin"))
            val formatted = DateTimeComponents.Formats.RFC_1123.format {
                setDateTimeOffset(today, offset)
            }
            check(formatted == "Mon, 15 Apr 2024 14:01:01 +0200")
        }

        @Test
        fun iso() {
            // Using the ISO format for dates, times, and offsets combined
            val formatted = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.format {
                setDate(LocalDate(2023, 1, 2))
                setTime(LocalTime(3, 46, 58, 530_000_000))
                setOffset(UtcOffset(3, 30))
            }
            check(formatted == "2023-01-02T03:46:58.53+03:30")
            val parsed = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parse("2023-01-02T03:46:58.53+03:30")
            check(parsed.toLocalDate() == LocalDate(2023, 1, 2))
            check(parsed.toLocalTime() == LocalTime(3, 46, 58, 530_000_000))
            check(parsed.toUtcOffset() == UtcOffset(3, 30))
        }
    }
}
