/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime.test
import kotlinx.datetime.*
import kotlinx.datetime.testing.*
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.nanoseconds

class TimeZoneTest {

    @Test
    fun utc() {
        val utc: FixedOffsetTimeZone = TimeZone.UTC
        println(utc)
        assertEquals("UTC", utc.id)
        assertEquals(utc, TimeZone.of("UTC"))
        assertEquals(UtcOffset.ZERO, utc.offset)
        assertEquals(0, utc.offset.totalSeconds)
        assertEquals(utc.offset, utc.offsetAt(Clock.System.now()))
    }

    @Test
    fun system() {
        val tz = TimeZone.currentSystemDefault()
        println(tz)
        val offset = Clock.System.now().offsetIn(tz)
        assertTrue(offset.totalSeconds in -18 * 60 * 60 .. 18 * 60 * 60)
        // assertTrue(tz.id.contains('/')) // does not work on build agents, whose timezone is "UTC"
        // TODO: decide how to assert system tz properties
    }

    @Test
    fun available() {
        val allTzIds = TimeZone.availableZoneIds
        assertContains(allTzIds, "Europe/Berlin", "Europe/Berlin not in $allTzIds")
        assertContains(allTzIds, "Europe/Moscow", "Europe/Moscow not in $allTzIds")
        assertContains(allTzIds, "America/New_York", "America/New_York not in $allTzIds")

        assertTrue(TimeZone.currentSystemDefault().id in allTzIds,
            "The current system timezone ${TimeZone.currentSystemDefault().id} is not in $allTzIds")
        assertTrue("UTC" in allTzIds, "The UTC timezone not in $allTzIds")
    }

    @Test
    fun availableZonesAreAvailable() {
        val availableZones = mutableListOf<String>()
        val nonAvailableZones = mutableListOf<Exception>()
        for (zoneName in TimeZone.availableZoneIds) {
            val timezone = try {
                TimeZone.of(zoneName)
            } catch (e: Exception) {
                nonAvailableZones.add(e)
                continue
            }
            availableZones.add(zoneName)
            Instant.DISTANT_FUTURE.toLocalDateTime(timezone).toInstant(timezone, TransitionHandler.USE_OFFSET_BEFORE)
            Instant.DISTANT_PAST.toLocalDateTime(timezone).toInstant(timezone, TransitionHandler.USE_OFFSET_BEFORE)
        }
        if (nonAvailableZones.isNotEmpty()) {
            println("Available zones: $availableZones")
            println("Non-available zones: $nonAvailableZones")
            throw nonAvailableZones[0]
        }
    }

    @Test
    fun of() {
        val tzm = TimeZone.of("Europe/Moscow")
        assertNotNull(tzm)
        assertEquals("Europe/Moscow", tzm.id)
        // TODO: Check known offsets from UTC for particular moments

        assertFailsWith<IllegalTimeZoneException> { TimeZone.of("Mars/Standard") }
        assertFailsWith<IllegalTimeZoneException> { TimeZone.of("UTC+X") }
    }

    @Test
    fun ofFailsOnInvalidOffset() {
        for (v in UtcOffsetTest.invalidUtcOffsetStrings) {
            assertFailsWith<IllegalTimeZoneException> { TimeZone.of(v) }
        }
    }

    // from 310bp
    @Test
    fun timeZoneEquals() {
        val test1 = TimeZone.of("Europe/London")
        val test2 = TimeZone.of("Europe/Paris")
        val test2b = TimeZone.of("Europe/Paris")
        assertEquals(false, test1 == test2)
        assertEquals(false, test2 == test1)

        assertEquals(true, test1 == test1)
        assertEquals(true, test2 == test2)
        assertEquals(true, test2 == test2b)

        assertEquals(test1.hashCode(), test1.hashCode())
        assertEquals(test2.hashCode(), test2.hashCode())
        assertEquals(test2.hashCode(), test2b.hashCode())
    }

    // from 310bp
    @Test
    fun timeZoneToString() {
        val idToString = arrayOf(
            Pair("Europe/London", "Europe/London"),
            Pair("Europe/Paris", "Europe/Paris"),
            Pair("Europe/Berlin", "Europe/Berlin"),
            Pair("Z", "Z"),
            Pair("z", "Z"),
            Pair("UTC", "UTC"),
            Pair("UTC+01:00", "UTC+01:00"),
            Pair("GMT+01:00", "GMT+01:00"),
            Pair("UT+01:00", "UT+01:00"))
        for ((id, str) in idToString) {
            assertEquals(str, TimeZone.of(id).toString())
        }
    }

    @Test
    fun utcOffsetNormalization() {
        val sameOffsetTZs = listOf("+04", "+04:00", "UTC+4", "UT+04", "GMT+04:00:00").map { TimeZone.of(it) }
        for (tz in sameOffsetTZs) {
            assertIs<FixedOffsetTimeZone>(tz)
        }
        val offsets = sameOffsetTZs.map { (it as FixedOffsetTimeZone).offset }
        val zoneIds = sameOffsetTZs.map { it.id }

        assertTrue(offsets.distinct().size == 1, "Expected all offsets to be equal: $offsets")
        assertTrue(offsets.map { it.toString() }.distinct().size == 1, "Expected all offsets to have the same string representation: $offsets")

        assertTrue(zoneIds.distinct().size > 1, "Expected some fixed offset zones to have different ids: $zoneIds")
    }

    // from 310bp
    @Test
    fun newYorkOffset() {
        val test = TimeZone.of("America/New_York")
        val offset = UtcOffset(hours = -5)

        fun check(expectedHours: Int, dateTime: LocalDateTime) {
            assertEquals(UtcOffset(hours = expectedHours), dateTime.toInstant(offset).offsetIn(test))
        }

        check(-5, LocalDateTime(2008, 1, 1))
        check(-5, LocalDateTime(2008, 2, 1))
        check(-5, LocalDateTime(2008, 3, 1))
        check(-4, LocalDateTime(2008, 4, 1))
        check(-4, LocalDateTime(2008, 5, 1))
        check(-4, LocalDateTime(2008, 6, 1))
        check(-4, LocalDateTime(2008, 7, 1))
        check(-4, LocalDateTime(2008, 8, 1))
        check(-4, LocalDateTime(2008, 9, 1))
        check(-4, LocalDateTime(2008, 10, 1))
        check(-4, LocalDateTime(2008, 11, 1))
        check(-5, LocalDateTime(2008, 12, 1))
        check(-5, LocalDateTime(2008, 1, 28))
        check(-5, LocalDateTime(2008, 2, 28))
        check(-4, LocalDateTime(2008, 3, 28))
        check(-4, LocalDateTime(2008, 4, 28))
        check(-4, LocalDateTime(2008, 5, 28))
        check(-4, LocalDateTime(2008, 6, 28))
        check(-4, LocalDateTime(2008, 7, 28))
        check(-4, LocalDateTime(2008, 8, 28))
        check(-4, LocalDateTime(2008, 9, 28))
        check(-4, LocalDateTime(2008, 10, 28))
        check(-5, LocalDateTime(2008, 11, 28))
        check(-5, LocalDateTime(2008, 12, 28))
    }

    // from 310bp
    @Test
    fun newYorkOffsetToDST() {
        val test = TimeZone.of("America/New_York")
        val offset = UtcOffset(hours = -5)

        fun check(expectedHours: Int, dateTime: LocalDateTime) {
            assertEquals(UtcOffset(hours = expectedHours), dateTime.toInstant(offset).offsetIn(test))
        }

        check(-5, LocalDateTime(2008, 3, 8))
        check(-5, LocalDateTime(2008, 3, 9))
        check(-4, LocalDateTime(2008, 3, 10))
        check(-4, LocalDateTime(2008, 3, 11))
        check(-4, LocalDateTime(2008, 3, 12))
        check(-4, LocalDateTime(2008, 3, 13))
        check(-4, LocalDateTime(2008, 3, 14))
        // cutover at 02:00 local
        check(-5, LocalDateTime(2008, 3, 9, 1, 59, 59, 999999999))
        check(-4, LocalDateTime(2008, 3, 9, 2, 0, 0, 0))
    }

    // from 310bp
    @Test
    fun newYorkOffsetFromDST() {
        val test = TimeZone.of("America/New_York")
        val offset = UtcOffset(hours = -4)

        fun check(expectedHours: Int, dateTime: LocalDateTime) {
            assertEquals(UtcOffset(hours = expectedHours), dateTime.toInstant(offset).offsetIn(test))
        }

        check(-4, LocalDateTime(2008, 11, 1))
        check(-4, LocalDateTime(2008, 11, 2))
        check(-5, LocalDateTime(2008, 11, 3))
        check(-5, LocalDateTime(2008, 11, 4))
        check(-5, LocalDateTime(2008, 11, 5))
        check(-5, LocalDateTime(2008, 11, 6))
        check(-5, LocalDateTime(2008, 11, 7))
        // cutover at 02:00 local
        check(-4, LocalDateTime(2008, 11, 2, 1, 59, 59, 999999999))
        check(-5, LocalDateTime(2008, 11, 2, 2, 0, 0, 0))
    }

    @Test
    fun checkKnownTimezoneDatabaseRecords() {
        with(TimeZone.of("America/New_York")) {
            checkRegular(this, LocalDateTime(2019, 3, 8, 23, 0), UtcOffset(hours = -5))
            checkGap(this, LocalDateTime(2019, 3, 10, 2, 0))
            checkRegular(this, LocalDateTime(2019, 6, 2, 23, 0), UtcOffset(hours = -4))
            checkOverlap(this, LocalDateTime(2019, 11, 3, 2, 0))
            checkRegular(this, LocalDateTime(2019, 12, 5, 23, 0), UtcOffset(hours = -5))
        }
        with(TimeZone.of("Europe/Berlin")) {
            checkRegular(this, LocalDateTime(2019, 1, 31, 1, 0), UtcOffset(hours = 1))
            checkGap(this, LocalDateTime(2019, 3, 31, 2, 0))
            checkRegular(this, LocalDateTime(2019, 6, 27, 1, 0), UtcOffset(hours = 2))
            checkOverlap(this, LocalDateTime(2019, 10, 27, 3, 0))
            checkRegular(this, LocalDateTime(2019, 12, 5, 23, 0), UtcOffset(hours = 1))
        }
        with(TimeZone.of("Europe/Moscow")) {
            checkRegular(this, LocalDateTime(2019, 1, 31, 1, 0), UtcOffset(hours = 3))
            checkRegular(this, LocalDateTime(2011, 1, 31, 1, 0), UtcOffset(hours = 3))
            checkGap(this, LocalDateTime(2011, 3, 27, 2, 0))
            checkRegular(this, LocalDateTime(2011, 5, 3, 1, 0), UtcOffset(hours = 4))
        }
        with(TimeZone.of("Australia/Sydney")) {
            checkRegular(this, LocalDateTime(2019, 1, 31, 1, 0), UtcOffset(hours = 11))
            checkOverlap(this, LocalDateTime(2019, 4, 7, 3, 0))
            checkRegular(this, LocalDateTime(2019, 10, 6, 1, 0), UtcOffset(hours = 10))
            checkGap(this, LocalDateTime(2019, 10, 6, 2, 0))
            checkRegular(this, LocalDateTime(2019, 12, 5, 23, 0), UtcOffset(hours = 11))
        }
    }

    private fun LocalDateTime(year: Int, month: Int, day: Int) = LocalDateTime(year, month, day, 0, 0)

}

