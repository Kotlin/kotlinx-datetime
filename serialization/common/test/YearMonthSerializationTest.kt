/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class YearMonthSerializationTest {
    private fun iso8601Serialization(serializer: KSerializer<YearMonth>) {
        for ((yearMonth, json) in listOf(
            Pair(YearMonth(2020, 12), "\"2020-12\""),
            Pair(YearMonth(-2020, 1), "\"-2020-01\""),
            Pair(YearMonth(2019, 10), "\"2019-10\""),
        )) {
            assertEquals(json, Json.encodeToString(serializer, yearMonth))
            assertEquals(yearMonth, Json.decodeFromString(serializer, json))
        }
    }

    private fun componentSerialization(serializer: KSerializer<YearMonth>) {
        for ((yearMonth, json) in listOf(
            Pair(YearMonth(2020, 12), "{\"year\":2020,\"month\":12}"),
            Pair(YearMonth(-2020, 1), "{\"year\":-2020,\"month\":1}"),
            Pair(YearMonth(2019, 10), "{\"year\":2019,\"month\":10}"),
        )) {
            assertEquals(json, Json.encodeToString(serializer, yearMonth))
            assertEquals(yearMonth, Json.decodeFromString(serializer, json))
        }
        // all components must be present
        assertFailsWith<SerializationException> {
            Json.decodeFromString(serializer, "{}")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString(serializer, "{\"year\":3}")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString(serializer, "{\"month\":3}")
        }
        // invalid values must fail to construct
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(serializer, "{\"year\":1000000000000,\"month\":3}")
        }
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(serializer, "{\"year\":2020,\"month\":30}")
        }
    }

    @Test
    fun testIso8601Serialization() {
        iso8601Serialization(YearMonthIso8601Serializer)
    }

    @Test
    fun testComponentSerialization() {
        componentSerialization(YearMonthComponentSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        // should be the same as the ISO 8601
        iso8601Serialization(Json.serializersModule.serializer())
    }

}
