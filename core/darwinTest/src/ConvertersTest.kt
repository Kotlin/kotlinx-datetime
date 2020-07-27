/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime
import kotlinx.cinterop.*
import platform.Foundation.*
import kotlin.math.*
import kotlin.random.*
import kotlin.test.*

class ConvertersTest {

    private val dateFormatter = NSDateFormatter()
    private val locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
    private val gregorian = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)!!
    private val utc = NSTimeZone.timeZoneForSecondsFromGMT(0)

    init {
        dateFormatter.locale = locale
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ"
        dateFormatter.calendar = gregorian
        dateFormatter.timeZone = utc
    }

    @Test
    fun testToFromNSDate() {
        // The first day on the Gregorian calendar. The day before it is 1582-10-04 in the Julian calendar.
        val gregorianCalendarStart = Instant.parse("1582-10-15T00:00:00Z").toEpochMilliseconds()
        val minBoundMillis = (NSDate.distantPast.timeIntervalSince1970 * 1000 + 0.5).toLong()
        val maxBoundMillis = (NSDate.distantFuture.timeIntervalSince1970 * 1000 - 0.5).toLong()
        repeat (1000) {
            val millis = Random.nextLong(minBoundMillis, maxBoundMillis)
            val instant = Instant.fromEpochMilliseconds(millis)
            val date = instant.toNSDate()
            // Darwin's date printer dynamically adjusts to switching between calendars, while our Instant does not.
            if (millis >= gregorianCalendarStart) {
                assertEquals(instant, Instant.parse(dateFormatter.stringFromDate(date)))
            }
            assertEquals(instant, date.toKotlinInstant())
        }
    }

    @Test
    fun availableZoneIdsToNSTimeZone() {
        for (id in TimeZone.availableZoneIds) {
            val normalizedId = (NSTimeZone.abbreviationDictionary[id] ?: id) as String
            val timeZone = TimeZone.of(normalizedId)
            if (timeZone is ZoneOffset) {
                continue
            }
            val nsTimeZone = timeZone.toNSTimeZone()
            assertEquals(normalizedId, nsTimeZone.name)
            assertEquals(timeZone, nsTimeZone.toKotlinTimeZone())
        }
    }

    // from threetenbp's tests for time zones
    @Test
    fun zoneOffsetToNSTimeZone() {
        for (i in -18 * 60..18 * 60) {
            val hours = i / 60
            val minutes = i % 60
            val str = (if (i < 0) "-" else "+") +
                (abs(hours) + 100).toString().substring(1) + ":" +
                (abs(minutes) + 100).toString().substring(1) + ":" +
                "00"
            val test = TimeZone.of(str)
            zoneOffsetCheck(test, hours, minutes)
        }
    }

    @Test
    fun localDateToNSDateComponentsTest() {
        val date = LocalDate.parse("2019-02-04")
        val components = date.toNSDateComponents()
        components.timeZone = utc
        val nsDate = gregorian.dateFromComponents(components)!!
        val formatter = NSDateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        assertEquals("2019-02-04", formatter.stringFromDate(nsDate))
    }

    @Test
    fun localDateTimeToNSDateComponentsTest() {
        val str = "2019-02-04T23:59:30.543"
        val dateTime = LocalDateTime.parse(str)
        val components = dateTime.toNSDateComponents()
        components.timeZone = utc
        val nsDate = gregorian.dateFromComponents(components)!!
        assertEquals(str + "Z", dateFormatter.stringFromDate(nsDate))
    }

    private fun zoneOffsetCheck(timeZone: TimeZone, hours: Int, minutes: Int) {
        val nsTimeZone = timeZone.toNSTimeZone()
        assertEquals(hours * 3600 + minutes * 60, nsTimeZone.secondsFromGMT.convert())
        assertEquals(timeZone, nsTimeZone.toKotlinTimeZone())
    }
}
