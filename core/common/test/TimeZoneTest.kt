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
        println(TimeZone.UTC)
        assertEquals("Z", TimeZone.UTC.id)
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
        println("Available TZs:")
        allTzIds.forEach(::println)

        assertNotEquals(0, allTzIds.size)
        assertTrue(TimeZone.currentSystemDefault().id in allTzIds)
        assertTrue("UTC" in allTzIds)
    }

    @Test
    fun availableZonesAreAvailable() {
        for (zoneName in TimeZone.availableZoneIds) {
            TimeZone.of(zoneName)
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

    // from 310bp
    @Test
    fun newYorkOffset() {
        val test = TimeZone.of("America/New_York")
        val offset = TimeZone.of("-5")
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 1, 1).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 2, 1).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 3, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 4, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 5, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 6, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 7, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 8, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 9, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 10, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 11, 1).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 12, 1).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 1, 28).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 2, 28).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 3, 28).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 4, 28).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 5, 28).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 6, 28).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 7, 28).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 8, 28).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 9, 28).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 10, 28).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 11, 28).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 12, 28).offsetIn(test))
    }

    // from 310bp
    @Test
    fun newYorkOffsetToDST() {
        val test = TimeZone.of("America/New_York")
        val offset = TimeZone.of("-5")
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 3, 8).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 3, 9).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 3, 10).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 3, 11).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 3, 12).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 3, 13).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 3, 14).offsetIn(test))
        // cutover at 02:00 local
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 3, 9, 1, 59, 59, 999999999).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 3, 9, 2, 0, 0, 0).offsetIn(test))
    }

    // from 310bp
    @Test
    fun newYorkOffsetFromDST() {
        val test = TimeZone.of("America/New_York")
        val offset = TimeZone.of("-4")
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 11, 1).offsetIn(test))
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 11, 2).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 11, 3).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 11, 4).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 11, 5).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 11, 6).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 11, 7).offsetIn(test))
        // cutover at 02:00 local
        assertEquals(TimeZone.of("-4"), createInstant(offset, 2008, 11, 2, 1, 59, 59, 999999999).offsetIn(test))
        assertEquals(TimeZone.of("-5"), createInstant(offset, 2008, 11, 2, 2, 0, 0, 0).offsetIn(test))
    }

    // from 310bp
    private fun createInstant(offset: TimeZone, year: Int, month: Int, day: Int, hour: Int = 0, min: Int = 0,
                              sec: Int = 0, nano: Int = 0): Instant =
        LocalDateTime(year, month, day, hour, min, sec, nano).toInstant(offset)
}
