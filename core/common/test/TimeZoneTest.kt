/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime.test
import kotlinx.datetime.*
import kotlin.test.*

class TimeZoneTest {

    @Test
    fun utc() {
        val utc: FixedOffsetTimeZone = TimeZone.UTC
        println(utc)
        assertEquals("Z", utc.id)
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
        assertContains(allTzIds, "Europe/Berlin")
        assertContains(allTzIds, "Europe/Moscow")
        assertContains(allTzIds, "America/New_York")

        assertNotEquals(0, allTzIds.size)
        assertTrue(TimeZone.currentSystemDefault().id in allTzIds)
        assertTrue("UTC" in allTzIds)
    }

    @Test
    fun availableZonesAreAvailable() {
        for (zoneName in TimeZone.availableZoneIds) {
            val timezone = try {
                TimeZone.of(zoneName)
            } catch (e: Exception) {
                throw Exception("Zone $zoneName is not available", e)
            }
            Instant.DISTANT_FUTURE.toLocalDateTime(timezone).toInstant(timezone)
            Instant.DISTANT_PAST.toLocalDateTime(timezone).toInstant(timezone)
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
        val offset = UtcOffset.parse("-5")

        fun check(expectedOffset: String, dateTime: LocalDateTime) {
            assertEquals(UtcOffset.parse(expectedOffset), dateTime.toInstant(offset).offsetIn(test))
        }

        check("-5", LocalDateTime(2008, 1, 1))
        check("-5", LocalDateTime(2008, 2, 1))
        check("-5", LocalDateTime(2008, 3, 1))
        check("-4", LocalDateTime(2008, 4, 1))
        check("-4", LocalDateTime(2008, 5, 1))
        check("-4", LocalDateTime(2008, 6, 1))
        check("-4", LocalDateTime(2008, 7, 1))
        check("-4", LocalDateTime(2008, 8, 1))
        check("-4", LocalDateTime(2008, 9, 1))
        check("-4", LocalDateTime(2008, 10, 1))
        check("-4", LocalDateTime(2008, 11, 1))
        check("-5", LocalDateTime(2008, 12, 1))
        check("-5", LocalDateTime(2008, 1, 28))
        check("-5", LocalDateTime(2008, 2, 28))
        check("-4", LocalDateTime(2008, 3, 28))
        check("-4", LocalDateTime(2008, 4, 28))
        check("-4", LocalDateTime(2008, 5, 28))
        check("-4", LocalDateTime(2008, 6, 28))
        check("-4", LocalDateTime(2008, 7, 28))
        check("-4", LocalDateTime(2008, 8, 28))
        check("-4", LocalDateTime(2008, 9, 28))
        check("-4", LocalDateTime(2008, 10, 28))
        check("-5", LocalDateTime(2008, 11, 28))
        check("-5", LocalDateTime(2008, 12, 28))
    }

    // from 310bp
    @Test
    fun newYorkOffsetToDST() {
        val test = TimeZone.of("America/New_York")
        val offset = UtcOffset.parse("-5")

        fun check(expectedOffset: String, dateTime: LocalDateTime) {
            assertEquals(UtcOffset.parse(expectedOffset), dateTime.toInstant(offset).offsetIn(test))
        }

        check("-5", LocalDateTime(2008, 3, 8))
        check("-5", LocalDateTime(2008, 3, 9))
        check("-4", LocalDateTime(2008, 3, 10))
        check("-4", LocalDateTime(2008, 3, 11))
        check("-4", LocalDateTime(2008, 3, 12))
        check("-4", LocalDateTime(2008, 3, 13))
        check("-4", LocalDateTime(2008, 3, 14))
        // cutover at 02:00 local
        check("-5", LocalDateTime(2008, 3, 9, 1, 59, 59, 999999999))
        check("-4", LocalDateTime(2008, 3, 9, 2, 0, 0, 0))
    }

    // from 310bp
    @Test
    fun newYorkOffsetFromDST() {
        val test = TimeZone.of("America/New_York")
        val offset = UtcOffset.parse("-4")

        fun check(expectedOffset: String, dateTime: LocalDateTime) {
            assertEquals(UtcOffset.parse(expectedOffset), dateTime.toInstant(offset).offsetIn(test))
        }

        check("-4", LocalDateTime(2008, 11, 1))
        check("-4", LocalDateTime(2008, 11, 2))
        check("-5", LocalDateTime(2008, 11, 3))
        check("-5", LocalDateTime(2008, 11, 4))
        check("-5", LocalDateTime(2008, 11, 5))
        check("-5", LocalDateTime(2008, 11, 6))
        check("-5", LocalDateTime(2008, 11, 7))
        // cutover at 02:00 local
        check("-4", LocalDateTime(2008, 11, 2, 1, 59, 59, 999999999))
        check("-5", LocalDateTime(2008, 11, 2, 2, 0, 0, 0))
    }

    private fun LocalDateTime(year: Int, monthNumber: Int, dayOfMonth: Int) = LocalDateTime(year, monthNumber, dayOfMonth, 0, 0)

}
