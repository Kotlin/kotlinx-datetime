/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.cinterop.*
import kotlinx.datetime.internal.NANOS_PER_ONE
import platform.Foundation.*
import kotlin.math.*
import kotlin.random.*
import kotlin.test.*

class ConvertersTest {

    private val isoCalendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierISO8601)!!
    @OptIn(UnsafeNumber::class)
    private val utc = NSTimeZone.timeZoneForSecondsFromGMT(0)

    @Test
    fun testToFromNSDateNow() {
        // as of writing, the max difference in such round-trips across 10^8 iterations was 120 nanoseconds,
        // so we allow for four times that
        repeat(10) {
            val now = Clock.System.now()
            assertEqualUpToHalfMicrosecond(now, now.toNSDate().toKotlinInstant())
        }
        repeat(10) {
            val nowInSwift = NSDate()
            assertEqualUpToHalfMicrosecond(nowInSwift, nowInSwift.toKotlinInstant().toNSDate())
        }
    }

    @Test
    fun testToFromNSDate() {
        val secondsBound = NSDate.distantPast.timeIntervalSince1970.toLong() until
            NSDate.distantFuture.timeIntervalSince1970.toLong()
        repeat(STRESS_TEST_ITERATIONS) {
            val seconds = Random.nextLong(secondsBound)
            val nanos = Random.nextInt(0, NANOS_PER_ONE)
            val instant = Instant.fromEpochSeconds(seconds, nanos)
            // at most 6 microseconds difference was observed in 10^8 iterations
            assertEqualUpToTenMicroseconds(instant, instant.toNSDate().toKotlinInstant())
            // while here, no difference at all was observed
            assertEqualUpToOneNanosecond(instant.toNSDate(), instant.toNSDate().toKotlinInstant().toNSDate())
        }
    }

    @Test
    fun availableZoneIdsToNSTimeZone() {
        for (id in TimeZone.availableZoneIds) {
            val normalizedId = (NSTimeZone.abbreviationDictionary[id] ?: id) as String
            val timeZone = TimeZone.of(normalizedId)
            if (timeZone is FixedOffsetTimeZone) {
                continue
            }
            val nsTimeZone = try {
                timeZone.toNSTimeZone()
            } catch (e: IllegalArgumentException) {
                assertEquals("America/Ciudad_Juarez", id)
                continue
            }
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
            val test = TimeZone.of(str) as FixedOffsetTimeZone
            zoneOffsetCheck(test, hours, minutes)
        }
    }

    @Test
    fun localDateToNSDateComponentsTest() {
        val date = LocalDate(2019, 2, 4)
        val components = date.toNSDateComponents()
        components.timeZone = utc
        val nsDate = isoCalendar.dateFromComponents(components)!!
        val formatter = NSDateFormatter().apply {
            timeZone = utc
            dateFormat = "yyyy-MM-dd"
        }
        assertEquals("2019-02-04", formatter.stringFromDate(nsDate))
    }

    @Test
    fun localDateTimeToNSDateComponentsTest() {
        val dateTime = LocalDate(2019, 2, 4).atTime(23, 59, 30, 123456000)
        val components = dateTime.toNSDateComponents()
        components.timeZone = utc
        val nsDate = isoCalendar.dateFromComponents(components)!!
        assertEqualUpToHalfMicrosecond(dateTime.toInstant(TimeZone.UTC), nsDate.toKotlinInstant())
    }

    @OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
    private fun zoneOffsetCheck(timeZone: FixedOffsetTimeZone, hours: Int, minutes: Int) {
        val nsTimeZone = timeZone.toNSTimeZone()
        val kotlinTimeZone = nsTimeZone.toKotlinTimeZone()
        assertEquals(hours * 3600 + minutes * 60, nsTimeZone.secondsFromGMT.convert())
        assertIs<FixedOffsetTimeZone>(kotlinTimeZone)
        assertEquals(timeZone.offset, kotlinTimeZone.offset)
    }

    private fun assertEqualUpToTenMicroseconds(instant1: Instant, instant2: Instant) {
        if ((instant1 - instant2).inWholeMicroseconds.absoluteValue > 10) {
            throw AssertionError("Expected $instant1 to be equal to $instant2 up to 10 microseconds")
        }
    }

    private fun assertEqualUpToHalfMicrosecond(instant1: Instant, instant2: Instant) {
        if ((instant1 - instant2).inWholeNanoseconds.absoluteValue > 500) {
            throw AssertionError("Expected $instant1 to be equal to $instant2 up to 0.5 microseconds")
        }
    }

    private fun assertEqualUpToHalfMicrosecond(date1: NSDate, date2: NSDate) {
        val difference = abs(date1.timeIntervalSinceDate(date2) * 1000000)
        if (difference > 0.5) {
            throw AssertionError("Expected $date1 to be equal to $date2 up to 0.5 microseconds, " +
                    "but the difference was $difference microseconds")
        }
    }

    private fun assertEqualUpToOneNanosecond(date1: NSDate, date2: NSDate) {
        val difference = abs(date1.timeIntervalSinceDate(date2) * 1000000000)
        if (difference > 1) {
            throw AssertionError("Expected $date1 to be equal to $date2 up to 1 nanosecond, " +
                    "but the difference was $difference microseconds")
        }
    }
}
