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

class InstantSerializationTest {

    private fun iso8601Serialization(serializer: KSerializer<Instant>) {
        for ((instant, json) in listOf(
            Pair(Instant.fromEpochSeconds(1607505416, 124000),
                "\"2020-12-09T09:16:56.000124Z\""),
            Pair(Instant.fromEpochSeconds(-1607505416, -124000),
                "\"1919-01-23T14:43:03.999876Z\""),
            Pair(Instant.fromEpochSeconds(987654321, 123456789),
                "\"2001-04-19T04:25:21.123456789Z\""),
            Pair(Instant.fromEpochSeconds(987654321, 0),
                "\"2001-04-19T04:25:21Z\""),
        )) {
            assertEquals(json, Json.encodeToString(serializer, instant))
            assertEquals(instant, Json.decodeFromString(serializer, json))
        }
    }

    private fun componentSerialization(serializer: KSerializer<Instant>) {
        for ((instant, json) in listOf(
            Pair(Instant.fromEpochSeconds(1607505416, 124000),
                "{\"epochSeconds\":1607505416,\"nanosecondsOfSecond\":124000}"),
            Pair(Instant.fromEpochSeconds(-1607505416, -124000),
                "{\"epochSeconds\":-1607505417,\"nanosecondsOfSecond\":999876000}"),
            Pair(Instant.fromEpochSeconds(987654321, 123456789),
                "{\"epochSeconds\":987654321,\"nanosecondsOfSecond\":123456789}"),
            Pair(Instant.fromEpochSeconds(987654321, 0),
                "{\"epochSeconds\":987654321}"),
        )) {
            assertEquals(json, Json.encodeToString(serializer, instant))
            assertEquals(instant, Json.decodeFromString(serializer, json))
        }
        // check that having a `"nanosecondsOfSecond": 0` field doesn't break deserialization
        assertEquals(Instant.fromEpochSeconds(987654321, 0),
            Json.decodeFromString(serializer,
                "{\"epochSeconds\":987654321,\"nanosecondsOfSecond\":0}"))
        // "epochSeconds" should always be present
        assertFailsWith<SerializationException> { Json.decodeFromString(serializer, "{}") }
        assertFailsWith<SerializationException> { Json.decodeFromString(serializer, "{\"nanosecondsOfSecond\":3}") }
    }

    @Test
    fun testIso8601Serialization() {
        iso8601Serialization(InstantIso8601Serializer)
    }

    @Test
    fun testComponentSerialization() {
        componentSerialization(InstantComponentSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        // should be the same as the ISO 8601
        iso8601Serialization(Json.serializersModule.serializer())
    }
}
