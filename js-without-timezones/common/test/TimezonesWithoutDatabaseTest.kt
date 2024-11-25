/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class TimezonesWithoutDatabaseTest {
    @Test
    fun system() {
        val tz = TimeZone.currentSystemDefault()
        assertEquals("SYSTEM", tz.id)
        assertEquals("SYSTEM", tz.toString())
        val now = Clock.System.now()
        assertEquals(now, now.toLocalDateTime(tz).toInstant(tz))
        val offset = now.offsetIn(tz)
        println("$now = ${now.toLocalDateTime(tz)}$offset")
        assertEquals(now, now.toLocalDateTime(tz).toInstant(offset))
        assertEquals(Instant.DISTANT_PAST, Instant.DISTANT_PAST.toLocalDateTime(tz).toInstant(tz))
        assertEquals(Instant.DISTANT_FUTURE, Instant.DISTANT_FUTURE.toLocalDateTime(tz).toInstant(tz))
        val today = now.toLocalDateTime(tz).date
        assertEquals(today.atTime(0, 0).toInstant(tz), today.atStartOfDayIn(tz))
    }

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
    fun available() {
        assertEquals(setOf("UTC"), TimeZone.availableZoneIds)
    }

    @Test
    fun of() {
        assertFailsWith<IllegalTimeZoneException> { TimeZone.of("Europe/Moscow") }
        assertSame(TimeZone.currentSystemDefault(), TimeZone.of("SYSTEM"))
    }

    // from 310bp
    @Test
    fun timeZoneEquals() {
        val test1 = TimeZone.of("SYSTEM")
        val test2 = TimeZone.of("UTC")
        val test2b = TimeZone.of("UTC+00:00")
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

}
