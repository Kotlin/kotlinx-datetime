/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.reflect.KMutableProperty1
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

    @Test
    fun testFormattingWithUnsetFields() {
        class PropertyAndItsValue<Target, Value>(val property: KMutableProperty1<Target, Value>, val value: Value) {
            fun set(target: Target) {
                property.set(target, value)
            }
        }
        val fields = listOf<PropertyAndItsValue<DateTimeComponents, *>>(
            PropertyAndItsValue(DateTimeComponents::timeZoneId, "Europe/Berlin"),
            PropertyAndItsValue(DateTimeComponents::year, 2020),
            PropertyAndItsValue(DateTimeComponents::monthNumber, 3),
            PropertyAndItsValue(DateTimeComponents::day, 16),
            PropertyAndItsValue(DateTimeComponents::dayOfWeek, DayOfWeek.MONDAY),
            PropertyAndItsValue(DateTimeComponents::dayOfYear, 76),
            PropertyAndItsValue(DateTimeComponents::hour, 23),
            PropertyAndItsValue(DateTimeComponents::hourOfAmPm, 11),
            PropertyAndItsValue(DateTimeComponents::amPm, AmPmMarker.PM),
            PropertyAndItsValue(DateTimeComponents::minute, 59),
            PropertyAndItsValue(DateTimeComponents::second, 45),
            PropertyAndItsValue(DateTimeComponents::nanosecond, 123_456_789),
            PropertyAndItsValue(DateTimeComponents::offsetHours, 3),
            PropertyAndItsValue(DateTimeComponents::offsetMinutesOfHour, 30),
            PropertyAndItsValue(DateTimeComponents::offsetSecondsOfMinute, 15),
        )
        val formatWithEverything = DateTimeComponents.Format {
            timeZoneId()
            dateTime(LocalDateTime.Formats.ISO)
            char(' ')
            dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            char(' ')
            dayOfYear()
            char(' ')
            amPmHour(); char(' '); amPmMarker("AM", "PM")
            char(' ')
            offset(UtcOffset.Formats.ISO)
        }
        for (index in fields.indices) {
            val propertyName = fields[index].property.name 
            assertFailsWith<IllegalStateException>("No error if the field $propertyName is unset") {
                formatWithEverything.format {
                    for (i in fields.indices) {
                        if (i != index) fields[i].set(this)
                    }
                }
            }.let {
                assertTrue(it.message!!.contains(fields[index].property.name), "Error message '$it' does not contain $propertyName")
            }
        }
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

    private object TimezoneTestData {
        val correctParsableOffsets = listOf(
            "1", "9", "0",                     // Single digit hours (H format)
            "09", "11", "18",                  // Two-digit hours (HH format)
            "0110", "0230", "0930",            // Hours and minutes without a separator (HHMM format)
            "010000", "000100", "012345",      // Hours, minutes, and seconds without a separator (HHMMSS format)
            "01:15", "02:35", "09:35",         // Hours and minutes with colon separator (HH:MM format)
            "01:10:32", "15:51:00", "17:54:32" // Hours, minutes, and seconds with colon separators (HH:MM:SS format)
        )

        val incorrectParsableOffsets = listOf(
            "19", "99", "20",                       // Invalid hours (exceeding typical timezone ranges)
            "2010", "0260", "0999", "9999",         // HHMM format with invalid minutes (>59) or hours (>18)
            "180000", "006000", "000099", "999999", // HHMMSS format with invalid hours, minutes, or seconds
            "30:10", "02:70", "99:99",              // HH:MM format with invalid hours or minutes
            "18:00:00", "00:60:00", "99:99:99",     // HH:MM:SS format with invalid hours, minutes, or seconds
        )

        val incorrectUnparsableOffsets = listOf(
            "a", "_", "+",                                      // Single non-digit characters
            "a9", "y!", "1#",                                   // Two characters: letter+digit, letter+symbol, digit+symbol
            "110", "020",                                       // Three digits (invalid length - not 2 or 4 digits)
            "18000", "02300",                                   // Five digits (invalid length - not 4 or 6 digits)
            "3:10", "2:70", "99:", ":20",                       // HH:MM format violations: single digit hour, missing minute, missing hour
            "12:3456", "1234:56",                               // Invalid colon-separated formats: too many digits in an hour/minute component
            "1:00:00", "00:6:00", "09:99:9",                    // HH:MM:SS format violations: single digit hour, single digit minute, single digit second
            ":00:00", "00::00", "09:99:", "::00", "00::", "::", // Colon placement errors
            "180:00:00", "00:610:00", "99:99:199"               // HH:MM:SS format violations: 3-digit hour, 3-digit minute, 3-digit second
        )

        val zuluTimezones = listOf("Z", "z")

        val tzPrefixes = listOf("UTC", "GMT", "UT")

        val timezoneDbIdentifiers = listOf(
            "America/New_York", "Europe/London", "Asia/Tokyo", "Australia/Sydney",
            "Pacific/Auckland", "Africa/Cairo", "America/Los_Angeles", "Europe/Paris",
            "Asia/Singapore", "Australia/Melbourne", "Africa/Johannesburg", "Europe/Isle_of_Man"
        )

        val invalidTimezoneIds = listOf("INVALID", "XYZ", "ABC/DEF", "NOT_A_TIMEZONE", "SYSTEM")
    }

    @Test
    fun testZuluTimeZone() {
        // TODO: Change SHOULD_PARSE_INCORRECTLY to SHOULD_PARSE_CORRECTLY when TimeZone.of("z") will work correctly
        assertTimezoneParsingBehavior(TimezoneTestData.zuluTimezones, ParseExpectation.SHOULD_PARSE_INCORRECTLY)
    }

    @Test
    fun testSpecialNamedTimezones() {
        assertTimezoneParsingBehavior(TimezoneTestData.tzPrefixes, ParseExpectation.SHOULD_PARSE_CORRECTLY)
    }

    @Test
    fun testPrefixWithCorrectParsableOffset() {
        val timezoneIds = generateTimezoneIds(TimezoneTestData.tzPrefixes + "", TimezoneTestData.correctParsableOffsets)
        assertTimezoneParsingBehavior(timezoneIds, ParseExpectation.SHOULD_PARSE_CORRECTLY)
    }

    @Test
    fun testPrefixWithIncorrectParsableOffset() {
        val timezoneIds = generateTimezoneIds(TimezoneTestData.tzPrefixes + "", TimezoneTestData.incorrectParsableOffsets)
        assertTimezoneParsingBehavior(timezoneIds, ParseExpectation.SHOULD_PARSE_INCORRECTLY)
    }

    @Test
    fun testPrefixWithIncorrectUnparsableOffset() {
        val timezoneIds = generateTimezoneIds(TimezoneTestData.tzPrefixes + "", TimezoneTestData.incorrectUnparsableOffsets)
        assertTimezoneParsingBehavior(timezoneIds, ParseExpectation.SHOULD_FAIL_TO_PARSE)
    }

    @Test
    fun testTimezoneDBIdentifiers() {
        assertTimezoneParsingBehavior(TimezoneTestData.timezoneDbIdentifiers, ParseExpectation.SHOULD_PARSE_CORRECTLY)
    }

    @Test
    fun testInvalidTimezoneIds() {
        assertTimezoneParsingBehavior(TimezoneTestData.invalidTimezoneIds, ParseExpectation.SHOULD_FAIL_TO_PARSE)
    }

    enum class ParseExpectation {
        SHOULD_PARSE_CORRECTLY,
        SHOULD_PARSE_INCORRECTLY,
        SHOULD_FAIL_TO_PARSE
    }

    private fun generateTimezoneIds(prefixes: List<String>, offsets: List<String>): List<String> = buildList {
        for (prefix in prefixes) {
            for (sign in listOf('+', '-')) {
                for (offset in offsets) {
                    add("$prefix$sign$offset")
                }
            }
        }
    }

    private fun assertTimezoneParsingBehavior(zoneIds: List<String>, expectation: ParseExpectation) {
        zoneIds.forEach { zoneId ->
            when (expectation) {
                ParseExpectation.SHOULD_PARSE_CORRECTLY -> {
                    TimeZone.of(zoneId)
                    val result = DateTimeComponents.Format { timeZoneId() }.parse(zoneId)
                    assertEquals(zoneId, result.timeZoneId)
                }

                ParseExpectation.SHOULD_PARSE_INCORRECTLY -> {
                    val result = DateTimeComponents.Format { timeZoneId() }.parse(zoneId)
                    assertEquals(zoneId, result.timeZoneId)
                }

                ParseExpectation.SHOULD_FAIL_TO_PARSE -> {
                    assertFailsWith<DateTimeFormatException> {
                        DateTimeComponents.Format { timeZoneId() }.parse(zoneId)
                    }
                }
            }
        }
    }
}
