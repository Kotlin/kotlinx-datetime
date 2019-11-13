/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test
import kotlinx.datetime.*
import kotlin.test.*

class TimeZoneTest {

    @Test
    fun utc() {
        println(TimeZone.UTC)
        assertEquals("UTC", TimeZone.UTC.id)
    }

    @Test
    fun system() {
        println(TimeZone.SYSTEM)
        // TODO: decide how to assert system tz properties
    }

    @Test
    fun available() {
        val allTzIds = TimeZone.availableZoneIds
        println("Available TZs:")
        allTzIds.forEach(::println)

        assertNotEquals(0, allTzIds.size)
        assertTrue(TimeZone.SYSTEM.id in allTzIds)
        assertTrue(TimeZone.UTC.id in allTzIds)
    }

    @Test
    fun of() {
        val tzm = TimeZone.of("Europe/Moscow")
        assertNotNull(tzm)
        assertEquals("Europe/Moscow", tzm.id)
        // TODO: Check known offsets from UTC for particular moments

        // TODO: assert exception type?
        assertFails { TimeZone.of("Mars/Standard") }

    }
}