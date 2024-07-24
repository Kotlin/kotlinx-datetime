/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateTimeComponentsFormatTest {

    @Test
    fun testErrorHandling() {
        val format = DateTimeComponents.Formats.RFC_1123
        assertDateTimeComponentsEqual(
            dateTimeComponents(LocalDate(2008, 6, 3), LocalTime(11, 5, 30), UtcOffset.ZERO),
            format.parse("Tue, 3 Jun 2008 11:05:30 GMT"))
        // the same date, but with an incorrect day-of-week:
        assertDateTimeComponentsEqual(
            DateTimeComponents().apply {
                year = 2008; monthNumber = 6; day = 3; dayOfWeek = DayOfWeek.MONDAY
                setTime(LocalTime(11, 5, 30))
                setOffset(UtcOffset.ZERO)
            },
            format.parse("Mon, 3 Jun 2008 11:05:30 GMT"))
        assertDateTimeComponentsEqual(
            DateTimeComponents().apply {
                year = 2008; monthNumber = 6; day = 40; dayOfWeek = DayOfWeek.TUESDAY
                setTime(LocalTime(11, 5, 30))
                setOffset(UtcOffset.ZERO)
            },
            format.parse("Tue, 40 Jun 2008 11:05:30 GMT"))
        format.assertCanNotParse("Bue, 3 Jun 2008 11:05:30 GMT")
    }

    @Test
    fun testInconsistentLocalTime() {
        val formatTime = LocalTime.Format {
            hour(); char(':'); minute()
            chars(" ("); amPmHour(); char(':'); minute(); char(' '); amPmMarker("AM", "PM"); char(')')
        }
        val format = DateTimeComponents.Format { time(formatTime) }
        val time1 = "23:15 (11:15 PM)" // a normal time after noon
        assertDateTimeComponentsEqual(
            DateTimeComponents().apply { hour = 23; hourOfAmPm = 11; minute = 15; amPm = AmPmMarker.PM },
            format.parse(time1)
        )
        assertEquals(LocalTime(23, 15), formatTime.parse(time1))
        val time2 = "23:15 (11:15 AM)" // a time with an inconsistent AM/PM marker
        assertDateTimeComponentsEqual(
            DateTimeComponents().apply { hour = 23; hourOfAmPm = 11; minute = 15; amPm = AmPmMarker.AM },
            format.parse(time2)
        )
        formatTime.assertCanNotParse(time2)
        val time3 = "23:15 (10:15 PM)" // a time with an inconsistent number of hours
        assertDateTimeComponentsEqual(
            DateTimeComponents().apply { hour = 23; hourOfAmPm = 10; minute = 15; amPm = AmPmMarker.PM },
            format.parse(time3)
        )
        formatTime.assertCanNotParse(time3)
        val time4 = "23:15 (11:16 PM)" // a time with an inconsistently duplicated field
        format.assertCanNotParse(time4)
        formatTime.assertCanNotParse(time4)
    }

    @Test
    fun testRfc1123() {
        val bags = buildMap<DateTimeComponents, Pair<String, Set<String>>> {
            put(dateTimeComponents(LocalDate(2008, 6, 3), LocalTime(11, 5, 30), UtcOffset.ZERO), ("Tue, 3 Jun 2008 11:05:30 GMT" to setOf("3 Jun 2008 11:05:30 UT", "3 Jun 2008 11:05:30 Z")))
            put(dateTimeComponents(LocalDate(2008, 6, 30), LocalTime(11, 5, 30), UtcOffset.ZERO), ("Mon, 30 Jun 2008 11:05:30 GMT" to setOf()))
            put(dateTimeComponents(LocalDate(2008, 6, 3), LocalTime(11, 5, 30), UtcOffset(hours = 2)), ("Tue, 3 Jun 2008 11:05:30 +0200" to setOf()))
            put(dateTimeComponents(LocalDate(2008, 6, 30), LocalTime(11, 5, 30), UtcOffset(hours = -3)), ("Mon, 30 Jun 2008 11:05:30 -0300" to setOf()))
            put(dateTimeComponents(LocalDate(2008, 6, 30), LocalTime(11, 5, 0), UtcOffset(hours = -3)), ("Mon, 30 Jun 2008 11:05 -0300" to setOf("Mon, 30 Jun 2008 11:05:00 -0300")))
        }
        test(bags, DateTimeComponents.Formats.RFC_1123)
    }

    @Test
    fun testZonedDateTime() {
        val format = DateTimeComponents.Format {
            dateTime(LocalDateTime.Formats.ISO)
            offset(UtcOffset.Formats.ISO)
            char('[')
            timeZoneId()
            char(']')
        }
        val berlin = "Europe/Berlin"
        val dateTime = LocalDateTime(2008, 6, 3, 11, 5, 30, 123_456_789)
        val offset = UtcOffset(hours = 1)
        val formatted = "2008-06-03T11:05:30.123456789+01:00[Europe/Berlin]"
        assertEquals(formatted, format.format { setDateTime(dateTime); setOffset(offset); timeZoneId = berlin })
        val bag = format.parse("2008-06-03T11:05:30.123456789+01:00[Europe/Berlin]")
        assertEquals(dateTime, bag.toLocalDateTime())
        assertEquals(offset, bag.toUtcOffset())
        assertEquals(berlin, bag.timeZoneId)
        format.assertCanNotParse("2008-06-03T11:05:30.123456789+01:00[Mars/New_York]")
        for (zone in TimeZone.availableZoneIds) {
            assertEquals(zone, format.parse("2008-06-03T11:05:30.123456789+01:00[$zone]").timeZoneId)
        }
    }

    @Test
    fun testTimeZoneGreedyParsing() {
        val format = DateTimeComponents.Format { timeZoneId(); chars("X") }
        for (zone in TimeZone.availableZoneIds) {
            assertEquals(zone, format.parse("${zone}X").timeZoneId)
        }
    }

    private fun dateTimeComponents(
        date: LocalDate? = null,
        time: LocalTime? = null,
        offset: UtcOffset? = null,
        zone: TimeZone? = null
    ) = DateTimeComponents().apply {
        date?.let { setDate(it) }
        time?.let { setTime(it) }
        offset?.let { setOffset(it) }
        timeZoneId = zone?.id
    }

    private fun assertDateTimeComponentsEqual(a: DateTimeComponents, b: DateTimeComponents, message: String? = null) {
        assertEquals(a.year, b.year, message)
        assertEquals(a.monthNumber, b.monthNumber, message)
        assertEquals(a.day, b.day, message)
        if (a.dayOfWeek != null && b.dayOfWeek != null)
            assertEquals(a.dayOfWeek, b.dayOfWeek, message)
        assertEquals(a.hour, b.hour, message)
        assertEquals(a.minute, b.minute, message)
        assertEquals(a.second ?: 0, b.second ?: 0, message)
        assertEquals(a.nanosecond ?: 0, b.nanosecond ?: 0, message)
        assertEquals(a.toUtcOffset(), b.toUtcOffset(), message)
        assertEquals(a.timeZoneId, b.timeZoneId, message)
    }

    @Test
    fun testDocFormatting() {
        val str = DateTimeComponents.Formats.RFC_1123.format {
           setDateTime(LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999))
           setOffset(UtcOffset(hours = 3))
        }
        assertEquals("Mon, 16 Mar 2020 23:59:59 +0300", str)
    }

    @Test
    fun testDocOutOfBoundsParsing() {
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
        assertEquals(LocalTime(0, 0, 0), time)
        assertTrue(extraDay)
    }

    @Test
    fun testDocCombinedParsing() {
        val input = "2020-03-16T23:59:59.999999999+03:00"
        val bag = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET.parse(input)
        val localDateTime = bag.toLocalDateTime()
        val instant = bag.toInstantUsingOffset()
        val offset = bag.toUtcOffset()
        assertEquals(LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999), localDateTime)
        assertEquals(Instant.parse("2020-03-16T20:59:59.999999999Z"), instant)
        assertEquals(UtcOffset(hours = 3), offset)
    }

    @Test
    fun testDefaultValueAssignment() {
        val input = "2020-03-16T23:59"
        val bagWithOptional = DateTimeComponents.Format {
            date(ISO_DATE); char('T')
            hour(); char(':'); minute()
            optional {
                char(':'); second()
                optional { char('.'); secondFraction() }
            }
        }.parse(input)
        assertEquals(0, bagWithOptional.second)
        assertEquals(0, bagWithOptional.nanosecond)
        val bagWithAlternative = DateTimeComponents.Format {
            date(ISO_DATE); char('T')
            hour(); char(':'); minute()
            alternativeParsing({}) {
                char(':'); second()
                optional { char('.'); secondFraction() }
            }
        }.parse(input)
        assertNull(bagWithAlternative.second)
        assertNull(bagWithAlternative.nanosecond)
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    @Test
    fun testByUnicodePatternDoc() {
        val format = DateTimeComponents.Format {
            byUnicodePattern("uuuu-MM-dd'T'HH:mm[:ss[.SSS]]xxxxx'['VV']'")
        }
        format.parse("2023-01-20T23:53:16.312+03:30[Asia/Tehran]")
    }

    private fun test(strings: Map<DateTimeComponents, Pair<String, Set<String>>>, format: DateTimeFormat<DateTimeComponents>) {
        for ((value, stringsForValue) in strings) {
            val (canonicalString, otherStrings) = stringsForValue
            assertEquals(canonicalString, format.format(value), "formatting $value with $format")
            assertDateTimeComponentsEqual(value, format.parse(canonicalString), "parsing '$canonicalString' with $format")
            for (otherString in otherStrings) {
                assertDateTimeComponentsEqual(value, format.parse(otherString), "parsing '$otherString' with $format")
            }
        }
    }
}
