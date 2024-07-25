/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.datetime.format.char
import kotlinx.datetime.serializers.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.test.*

class LocalTimeSerializationTest {
    private fun iso8601Serialization(serializer: KSerializer<LocalTime>) {
        for ((localTime, json) in listOf(
            Pair(LocalTime(2, 1), "\"02:01\""),
            Pair(LocalTime(23, 59, 1), "\"23:59:01\""),
            Pair(LocalTime(23, 59, 59, 990000000), "\"23:59:59.990\""),
            Pair(LocalTime(23, 59, 59, 999990000), "\"23:59:59.999990\""),
            Pair(LocalTime(23, 59, 59, 999999990), "\"23:59:59.999999990\""),
        )) {
            assertEquals(json, Json.encodeToString(serializer, localTime))
            assertEquals(localTime, Json.decodeFromString(serializer, json))
        }
    }

    private fun componentSerialization(serializer: KSerializer<LocalTime>) {
        for ((localTime, json) in listOf(
            Pair(LocalTime(2, 1), "{\"hour\":2,\"minute\":1}"),
            Pair(LocalTime(23, 59, 1), "{\"hour\":23,\"minute\":59,\"second\":1}"),
            Pair(LocalTime(23, 59, 59, 990000000),
                "{\"hour\":23,\"minute\":59,\"second\":59,\"nanosecond\":990000000}"),
            Pair(LocalTime(23, 59, 59, 999990000),
                "{\"hour\":23,\"minute\":59,\"second\":59,\"nanosecond\":999990000}"),
            Pair(LocalTime(23, 59, 59, 999999990),
                "{\"hour\":23,\"minute\":59,\"second\":59,\"nanosecond\":999999990}"),
            Pair(LocalTime(23, 59, 0, 1),
                "{\"hour\":23,\"minute\":59,\"second\":0,\"nanosecond\":1}"),
        )) {
            assertEquals(json, Json.encodeToString(serializer, localTime))
            assertEquals(localTime, Json.decodeFromString(serializer, json))
        }
        // adding omitted values shouldn't break deserialization
        assertEquals(LocalTime(2, 1),
            Json.decodeFromString(serializer,
                "{\"hour\":2,\"minute\":1,\"second\":0}"
            ))
        assertEquals(LocalTime(2, 1),
            Json.decodeFromString(serializer,
                "{\"hour\":2,\"minute\":1,\"nanosecond\":0}"
            ))
        assertEquals(LocalTime(2, 1),
            Json.decodeFromString(serializer,
                "{\"hour\":2,\"minute\":1,\"second\":0,\"nanosecond\":0}"
            ))
    }

    @Test
    fun testIso8601Serialization() {
        assertKSerializerName("kotlinx.datetime.LocalTime", LocalTimeIso8601Serializer)
        iso8601Serialization(LocalTimeIso8601Serializer)
    }

    @Test
    fun testComponentSerialization() {
        assertKSerializerName("kotlinx.datetime.LocalTime", LocalTimeComponentSerializer)
        componentSerialization(LocalTimeComponentSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        // should be the same as the ISO 8601
        assertKSerializerName<LocalTime>("kotlinx.datetime.LocalTime", Json.serializersModule.serializer())
        iso8601Serialization(Json.serializersModule.serializer())
    }

    object FixedWidthTimeSerializer : FormattedLocalTimeSerializer("FixedWidth", LocalTime.Format {
        hour(); char(':'); minute(); char(':'); second(); char('.'); secondFraction(3)
    })

    @Test
    fun testCustomSerializer() {
        assertKSerializerName("kotlinx.datetime.LocalTime serializer FixedWidth", FixedWidthTimeSerializer)
        for ((localTime, json) in listOf(
            Pair(LocalTime(2, 1), "\"02:01:00.000\""),
            Pair(LocalTime(23, 59, 1), "\"23:59:01.000\""),
            Pair(LocalTime(23, 59, 59, 990000000), "\"23:59:59.990\""),
            Pair(LocalTime(23, 59, 59, 999000000), "\"23:59:59.999\""),
        )) {
            assertEquals(json, Json.encodeToString(FixedWidthTimeSerializer, localTime))
            assertEquals(localTime, Json.decodeFromString(FixedWidthTimeSerializer, json))
        }
        assertEquals("\"12:34:56.123\"", Json.encodeToString(FixedWidthTimeSerializer,
            LocalTime(12, 34, 56, 123999999)))
    }
}
