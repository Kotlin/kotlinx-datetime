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
        iso8601Serialization(LocalTimeIso8601Serializer)
    }

    @Test
    fun testComponentSerialization() {
        componentSerialization(LocalTimeComponentSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        // should be the same as the ISO-8601
        iso8601Serialization(Json.serializersModule.serializer())
    }
}
