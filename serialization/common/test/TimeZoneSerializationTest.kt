/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.serialization.json.*
import kotlin.test.*

class TimeZoneSerializationTest {

    @Test
    fun zoneOffsetSerialization() {
        val offset2h = TimeZone.of("+02:00") as ZoneOffset
        assertEquals("\"+02:00\"", Json.encodeToString(ZoneOffsetSerializer, offset2h))
        assertEquals(offset2h, Json.decodeFromString(ZoneOffsetSerializer, "\"+02:00\""))
        assertEquals(offset2h, Json.decodeFromString(ZoneOffsetSerializer, "\"+02\""))
        assertEquals(offset2h, Json.decodeFromString(ZoneOffsetSerializer, "\"+2\""))
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(ZoneOffsetSerializer, "\"Europe/Berlin\"")
        }
    }

    @Test
    fun serialization() {
        for (zoneId in listOf("Europe/Berlin", "+02:00")) {
            val zone = TimeZone.of(zoneId)
            val json = "\"$zoneId\""
            assertEquals(json, Json.encodeToString(TimeZoneSerializer, zone))
            assertEquals(zone, Json.decodeFromString(TimeZoneSerializer, json))
        }
    }
}