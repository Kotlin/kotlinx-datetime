/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class LocalDateSerializationTest {
    @Test
    fun iso8601Serialization() {
        for ((localDate, json) in listOf(
            Pair(LocalDate(2020, 12, 9), "\"2020-12-09\""),
            Pair(LocalDate(-2020, 1, 1), "\"-2020-01-01\""),
            Pair(LocalDate(2019, 10, 1), "\"2019-10-01\""),
        )) {
            assertEquals(json, Json.encodeToString(LocalDateISO8601Serializer, localDate))
            assertEquals(localDate, Json.decodeFromString(LocalDateISO8601Serializer, json))
        }
    }

    @Test
    fun componentSerialization() {
        for ((localDate, json) in listOf(
            Pair(LocalDate(2020, 12, 9), "{\"year\":2020,\"month\":12,\"day\":9}"),
            Pair(LocalDate(-2020, 1, 1), "{\"year\":-2020,\"month\":1,\"day\":1}"),
            Pair(LocalDate(2019, 10, 1), "{\"year\":2019,\"month\":10,\"day\":1}"),
        )) {
            assertEquals(json, Json.encodeToString(LocalDateComponentSerializer, localDate))
            assertEquals(localDate, Json.decodeFromString(LocalDateComponentSerializer, json))
        }
        // all components must be present
        assertFailsWith<SerializationException> {
            Json.decodeFromString(LocalDateComponentSerializer, "{}")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString(LocalDateComponentSerializer, "{\"year\":3,\"month\":12}")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString(LocalDateComponentSerializer, "{\"year\":3,\"day\":12}")
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString(LocalDateComponentSerializer, "{\"month\":3,\"day\":12}")
        }
        // invalid values must fail to construct
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(LocalDateComponentSerializer, "{\"year\":1000000000000,\"month\":3,\"day\":12}")
        }
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(LocalDateComponentSerializer, "{\"year\":2020,\"month\":30,\"day\":12}")
        }
    }

}
