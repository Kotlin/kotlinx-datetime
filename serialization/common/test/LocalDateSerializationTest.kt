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

class LocalDateSerializationTest {
    private fun iso8601Serialization(serializer: KSerializer<LocalDate>) {
        for ((localDate, json) in listOf(
            Pair(LocalDate(2020, 12, 9), "\"2020-12-09\""),
            Pair(LocalDate(-2020, 1, 1), "\"-2020-01-01\""),
            Pair(LocalDate(2019, 10, 1), "\"2019-10-01\""),
        )) {
            assertEquals(json, Json.encodeToString(serializer, localDate))
            assertEquals(localDate, Json.decodeFromString(serializer, json))
        }
    }

    private fun componentSerialization(serializer: KSerializer<LocalDate>) {
        for ((localDate, json) in listOf(
            Pair(LocalDate(2020, 12, 9), "{\"year\":2020,\"month\":12,\"day\":9}"),
            Pair(LocalDate(-2020, 1, 1), "{\"year\":-2020,\"month\":1,\"day\":1}"),
            Pair(LocalDate(2019, 10, 1), "{\"year\":2019,\"month\":10,\"day\":1}"),
        )) {
            assertEquals(json, Json.encodeToString(serializer, localDate))
            assertEquals(localDate, Json.decodeFromString(serializer, json))
        }
        // all components must be present
        assertFailsWith<SerializationException> {
            Json.decodeFromString(serializer, "{}")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString(serializer, "{\"year\":3,\"month\":12}")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString(serializer, "{\"year\":3,\"day\":12}")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString(serializer, "{\"month\":3,\"day\":12}")
        }
        // invalid values must fail to construct
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(serializer, "{\"year\":1000000000000,\"month\":3,\"day\":12}")
        }
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(serializer, "{\"year\":2020,\"month\":30,\"day\":12}")
        }
    }

    @Test
    fun testIso8601Serialization() {
        iso8601Serialization(LocalDateIso8601Serializer)
    }

    @Test
    fun testComponentSerialization() {
        componentSerialization(LocalDateComponentSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        // should be the same as the ISO 8601
        iso8601Serialization(Json.serializersModule.serializer())
    }

    object IsoBasicLocalDateSerializer : FormattedLocalDateSerializer("ISO_BASIC", LocalDate.Formats.ISO_BASIC)

    @Test
    fun testCustomSerializer() {
        assertKSerializerName("kotlinx.datetime.LocalDate serializer ISO_BASIC", IsoBasicLocalDateSerializer)
        for ((localDate, json) in listOf(
            Pair(LocalDate(2020, 12, 9), "\"20201209\""),
            Pair(LocalDate(-2020, 1, 1), "\"-20200101\""),
            Pair(LocalDate(2019, 10, 1), "\"20191001\""),
        )) {
            assertEquals(json, Json.encodeToString(IsoBasicLocalDateSerializer, localDate))
            assertEquals(localDate, Json.decodeFromString(IsoBasicLocalDateSerializer, json))
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun <T> assertKSerializerName(expectedName: String, serializer: KSerializer<T>) {
    assertEquals(expectedName, serializer.descriptor.serialName)
}
