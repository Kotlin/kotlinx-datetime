/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.test.*

class TimeZoneSerializationTest {

    private fun zoneOffsetSerialization(serializer: KSerializer<FixedOffsetTimeZone>) {
        val offset2h = TimeZone.of("+02:00") as FixedOffsetTimeZone
        assertEquals("\"+02:00\"", Json.encodeToString(serializer, offset2h))
        assertEquals(offset2h, Json.decodeFromString(serializer, "\"+02:00\""))
        assertEquals(offset2h, Json.decodeFromString(serializer, "\"+02\""))
        assertEquals(offset2h, Json.decodeFromString(serializer, "\"+2\""))
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(serializer, "\"Europe/Berlin\"")
        }
    }

    private fun serialization(serializer: KSerializer<TimeZone>) {
        for (zoneId in listOf("Europe/Berlin", "+02:00")) {
            val zone = TimeZone.of(zoneId)
            val json = "\"$zoneId\""
            assertEquals(json, Json.encodeToString(serializer, zone))
            assertEquals(zone, Json.decodeFromString(serializer, json))
        }
    }

    @Test
    fun testZoneOffsetSerialization() {
        zoneOffsetSerialization(FixedOffsetTimeZoneSerializer)
    }

    @Test
    fun testSerialization() {
        serialization(TimeZoneSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        assertKSerializerName<FixedOffsetTimeZone>(
            "kotlinx.datetime.FixedOffsetTimeZone", Json.serializersModule.serializer()
        )
        zoneOffsetSerialization(Json.serializersModule.serializer())
        assertKSerializerName<TimeZone>("kotlinx.datetime.TimeZone", Json.serializersModule.serializer())
        serialization(Json.serializersModule.serializer())
    }
}
