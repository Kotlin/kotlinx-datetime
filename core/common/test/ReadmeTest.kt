/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*
import kotlin.time.*

/**
 * Tests the code snippets in the README.md file.
 */
@Suppress("UNUSED_VARIABLE")
class ReadmeTest {
    @Test
    fun testGettingCurrentMoment() {
        val currentMoment = Clock.System.now()
    }

    @Test
    fun testConvertingAnInstantToLocalDateAndTimeComponents() {
        val currentMoment: Instant = Clock.System.now()
        val datetimeInUtc: LocalDateTime = currentMoment.toLocalDateTime(TimeZone.UTC)
        val datetimeInSystemZone: LocalDateTime = currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())

        val tzBerlin = TimeZone.of("Europe/Berlin")
        val datetimeInBerlin = currentMoment.toLocalDateTime(tzBerlin)

        val kotlinReleaseDateTime = LocalDateTime(2016, 2, 15, 16, 57, 0, 0)

        val kotlinReleaseInstant = kotlinReleaseDateTime.toInstant(TimeZone.of("UTC+3"))
    }

    @Test
    fun testGettingLocalDateComponents() {
        val now: Instant = Clock.System.now()
        val today: LocalDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        // or shorter
        val today2: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val knownDate = LocalDate(2020, 2, 21)
    }

    @Test
    fun testGettingLocalTimeComponents() {
        val now: Instant = Clock.System.now()
        val thisTime: LocalTime = now.toLocalDateTime(TimeZone.currentSystemDefault()).time

        val knownTime = LocalTime(hour = 23, minute = 59, second = 12)
        val timeWithNanos = LocalTime(hour = 23, minute = 59, second = 12, nanosecond = 999)
        val hourMinute = LocalTime(hour = 12, minute = 13)
    }

    @Test
    fun testConvertingInstantToAndFromUnixTime() {
        Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
    }

    @Test
    fun testConvertingInstantAndLocalDateTimeToAndFromIso8601String() {
        val instantNow = Clock.System.now()
        instantNow.toString()  // returns something like 2015-12-31T12:30:00Z
        val instantBefore = Instant.parse("2010-06-01T22:19:44.475Z")

        LocalDateTime.parse("2010-06-01T22:19:44")
        LocalDate.parse("2010-06-01")
        LocalTime.parse("12:01:03")
        LocalTime.parse("12:00:03.999")
        assertFailsWith<IllegalArgumentException> { LocalTime.parse("12:0:03.999") }
    }

    @Test
    fun testWorkingWithOtherStringFormats() {
        val dateFormat = LocalDate.Format {
            monthNumber(padding = Padding.SPACE)
            char('/')
            day()
            char(' ')
            year()
        }

        val date = dateFormat.parse("12/24 2023")
        assertEquals("20231224", date.format(LocalDate.Formats.ISO_BASIC))
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    @Test
    fun testUnicodePatterns() {
        assertEquals("""
                date(LocalDate.Formats.ISO)
                char('T')
                hour()
                char(':')
                minute()
                char(':')
                second()
                alternativeParsing({
                }) {
                    char('.')
                    secondFraction(3)
                }
                offset(UtcOffset.Formats.FOUR_DIGITS)
            """.trimIndent(),
            DateTimeFormat.formatAsKotlinBuilderDsl(DateTimeComponents.Format {
                byUnicodePattern("uuuu-MM-dd'T'HH:mm:ss[.SSS]Z")
            })
        )
        @OptIn(FormatStringsInDatetimeFormats::class)
        val dateTimeFormat = LocalDateTime.Format {
            byUnicodePattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]")
        }
        dateTimeFormat.parse("2023-12-24T23:59:59")
    }

    @Test
    fun testParsingAndFormattingPartialCompoundOrOutOfBoundsData() {
        val yearMonth = DateTimeComponents.Format { year(); char('-'); monthNumber() }
            .parse("2024-01")
        assertEquals(2024, yearMonth.year)
        assertEquals(1, yearMonth.monthNumber)

        val dateTimeOffset = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
            .parse("2023-01-07T23:16:15.53+02:00")
        assertEquals("+02:00", dateTimeOffset.toUtcOffset().toString())
        assertEquals("2023-01-07T23:16:15.530", dateTimeOffset.toLocalDateTime().toString())

        val time = DateTimeComponents.Format { time(LocalTime.Formats.ISO) }
            .parse("23:59:60").apply {
                if (second == 60) second = 59
            }.toLocalTime()
        assertEquals(LocalTime(23, 59, 59), time)

        assertEquals("Sat, 7 Jan 2023 23:59:60 +0200", DateTimeComponents.Formats.RFC_1123.format {
            // the receiver of this lambda is DateTimeComponents
            setDate(LocalDate(2023, 1, 7))
            hour = 23
            minute = 59
            second = 60
            setOffset(UtcOffset(hours = 2))
        })
    }

    @Test
    fun testInstantArithmetic() {
        run {
            val now = Clock.System.now()
            val instantInThePast: Instant = Instant.parse("2020-01-01T00:00:00Z")
            val durationSinceThen: Duration = now - instantInThePast
            val equidistantInstantInTheFuture: Instant = now + durationSinceThen

            val period: DateTimePeriod = instantInThePast.periodUntil(Clock.System.now(), TimeZone.UTC)

            instantInThePast.yearsUntil(Clock.System.now(), TimeZone.UTC)
            instantInThePast.monthsUntil(Clock.System.now(), TimeZone.UTC)
            instantInThePast.daysUntil(Clock.System.now(), TimeZone.UTC)

            val diffInMonths = instantInThePast.until(Clock.System.now(), DateTimeUnit.MONTH, TimeZone.UTC)
        }

        run {
            val now = Clock.System.now()
            val systemTZ = TimeZone.currentSystemDefault()
            val tomorrow = now.plus(2, DateTimeUnit.DAY, systemTZ)
            val threeYearsAndAMonthLater = now.plus(DateTimePeriod(years = 3, months = 1), systemTZ)
        }
    }

    @Test
    fun testDateArithmetic() {
        val date = LocalDate(2023, 1, 7)
        val date2 = date.plus(1, DateTimeUnit.DAY)
        date.plus(DatePeriod(days = 1))
        date.until(date2, DateTimeUnit.DAY)
        date.yearsUntil(date2)
        date.monthsUntil(date2)
        date.daysUntil(date2)
        date.periodUntil(date2)
        date2 - date
    }

    @Test
    fun testDateTimeArithmetic() {
        val timeZone = TimeZone.of("Europe/Berlin")
        val localDateTime = LocalDateTime.parse("2021-03-27T02:16:20")
        val instant = localDateTime.toInstant(timeZone)

        val instantOneDayLater = instant.plus(1, DateTimeUnit.DAY, timeZone)
        val localDateTimeOneDayLater = instantOneDayLater.toLocalDateTime(timeZone)
        assertEquals(LocalDateTime(2021, 3, 28, 3, 16, 20), localDateTimeOneDayLater)
        // 2021-03-28T03:16:20, as 02:16:20 that day is in a time gap

        val instantTwoDaysLater = instant.plus(2, DateTimeUnit.DAY, timeZone)
        val localDateTimeTwoDaysLater = instantTwoDaysLater.toLocalDateTime(timeZone)
        assertEquals(LocalDateTime(2021, 3, 29, 2, 16, 20), localDateTimeTwoDaysLater)
        // 2021-03-29T02:16:20
    }
}
