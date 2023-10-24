/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class ValueBagFormatTest {

    @Test
    fun testErrorHandling() {
        val format = ValueBag.Formats.RFC_1123
        assertValueBagsEqual(
            valueBag(LocalDate(2008, 6, 3), LocalTime(11, 5, 30), UtcOffset.ZERO),
            format.parse("Tue, 3 Jun 2008 11:05:30 GMT"))
        assertValueBagsEqual(
            ValueBag().apply {
                year = 2008
                monthNumber = 6
                dayOfMonth = 40
                populateFrom(LocalTime(11, 5, 30))
                populateFrom(UtcOffset.ZERO)
            },
            format.parse("Tue, 40 Jun 2008 11:05:30 GMT"))
        assertFailsWith<DateTimeFormatException> { format.parse("Bue, 3 Jun 2008 11:05:30 GMT") }
    }

    @Test
    fun testRfc1123() {
        val bags = buildMap<ValueBag, Pair<String, Set<String>>> {
            put(valueBag(LocalDate(2008, 6, 3), LocalTime(11, 5, 30), UtcOffset.ZERO), ("Tue, 3 Jun 2008 11:05:30 GMT" to setOf("3 Jun 2008 11:05:30 UT", "3 Jun 2008 11:05:30 Z")))
            put(valueBag(LocalDate(2008, 6, 30), LocalTime(11, 5, 30), UtcOffset.ZERO), ("Mon, 30 Jun 2008 11:05:30 GMT" to setOf()))
            put(valueBag(LocalDate(2008, 6, 3), LocalTime(11, 5, 30), UtcOffset(hours = 2)), ("Tue, 3 Jun 2008 11:05:30 +0200" to setOf()))
            put(valueBag(LocalDate(2008, 6, 30), LocalTime(11, 5, 30), UtcOffset(hours = -3)), ("Mon, 30 Jun 2008 11:05:30 -0300" to setOf()))
            put(valueBag(LocalDate(2008, 6, 30), LocalTime(11, 5, 0), UtcOffset(hours = -3)), ("Mon, 30 Jun 2008 11:05 -0300" to setOf("Mon, 30 Jun 2008 11:05:00 -0300")))
        }
        test(bags, ValueBag.Formats.RFC_1123)
    }

    @Test
    fun testZonedDateTime() {
        val format = ValueBag.Format {
            appendDateTime(LocalDateTime.Formats.ISO)
            appendOffset(UtcOffset.Formats.ISO)
            char('[')
            appendTimeZoneId()
            char(']')
        }
        val berlin = "Europe/Berlin"
        val dateTime = LocalDateTime(2008, 6, 3, 11, 5, 30, 123_456_789)
        val offset = UtcOffset(hours = 1)
        val formatted = "2008-06-03T11:05:30.123456789+01:00[Europe/Berlin]"
        assertEquals(formatted, format.format { populateFrom(dateTime); populateFrom(offset); timeZoneId = berlin })
        val bag = format.parse("2008-06-03T11:05:30.123456789+01:00[Europe/Berlin]")
        assertEquals(dateTime, bag.toLocalDateTime())
        assertEquals(offset, bag.toUtcOffset())
        assertEquals(berlin, bag.timeZoneId)
        assertFailsWith<DateTimeFormatException> { format.parse("2008-06-03T11:05:30.123456789+01:00[Mars/New_York]") }
        for (zone in TimeZone.availableZoneIds) {
            assertEquals(zone, format.parse("2008-06-03T11:05:30.123456789+01:00[$zone]").timeZoneId)
        }
    }

    @Test
    fun testTimeZoneGreedyParsing() {
        val format = ValueBag.Format { appendTimeZoneId(); chars("X") }
        for (zone in TimeZone.availableZoneIds) {
            assertEquals(zone, format.parse("${zone}X").timeZoneId)
        }
    }

    private fun valueBag(
        date: LocalDate? = null,
        time: LocalTime? = null,
        offset: UtcOffset? = null,
        zone: TimeZone? = null
    ) = ValueBag().apply {
        date?.let { populateFrom(it) }
        time?.let { populateFrom(it) }
        offset?.let { populateFrom(it) }
        timeZoneId = zone?.id
    }

    private fun assertValueBagsEqual(a: ValueBag, b: ValueBag, message: String? = null) {
        assertEquals(a.year, b.year, message)
        assertEquals(a.monthNumber, b.monthNumber, message)
        assertEquals(a.dayOfMonth, b.dayOfMonth, message)
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
        val str = ValueBag.Formats.RFC_1123.format {
           populateFrom(LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999))
           populateFrom(UtcOffset(hours = 3))
        }
        assertEquals("Mon, 16 Mar 2020 23:59:59 +0300", str)
    }

    @Test
    fun testDocOutOfBoundsParsing() {
        val input = "23:59:60"
        val extraDay: Boolean
        val time = ValueBag.Format {
          appendTime(LocalTime.Formats.ISO)
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
        val bag = ValueBag.Formats.ISO_DATE_TIME_OFFSET.parse(input)
        val localDateTime = bag.toLocalDateTime()
        val instant = bag.toInstantUsingUtcOffset()
        val offset = bag.toUtcOffset()
        assertEquals(LocalDateTime(2020, 3, 16, 23, 59, 59, 999_999_999), localDateTime)
        assertEquals(Instant.parse("2020-03-16T20:59:59.999999999Z"), instant)
        assertEquals(UtcOffset(hours = 3), offset)
    }

    private fun test(strings: Map<ValueBag, Pair<String, Set<String>>>, format: DateTimeFormat<ValueBag>) {
        for ((value, stringsForValue) in strings) {
            val (canonicalString, otherStrings) = stringsForValue
            assertEquals(canonicalString, format.format(value), "formatting $value with $format")
            assertValueBagsEqual(value, format.parse(canonicalString), "parsing '$canonicalString' with $format")
            for (otherString in otherStrings) {
                assertValueBagsEqual(value, format.parse(otherString), "parsing '$otherString' with $format")
            }
        }
    }
}
