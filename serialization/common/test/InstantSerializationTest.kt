/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class InstantSerializationTest {

    @Test
    fun iso8601Serialization() {
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
            assertEquals(json, Json.encodeToString(InstantISO8601Serializer, instant))
            assertEquals(instant, Json.decodeFromString(InstantISO8601Serializer, json))
        }
    }

    @Test
    fun componentSerialization() {
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
            assertEquals(json, Json.encodeToString(InstantComponentSerializer, instant))
            assertEquals(instant, Json.decodeFromString(InstantComponentSerializer, json))
        }
        // check that having a `"nanosecondsOfSecond": 0` field doesn't break deserialization
        assertEquals(Instant.fromEpochSeconds(987654321, 0),
            Json.decodeFromString(InstantComponentSerializer,
                "{\"epochSeconds\":987654321,\"nanosecondsOfSecond\":0}"))
        // "epochSeconds" should always be present
        assertFailsWith<SerializationException> { Json.decodeFromString(InstantComponentSerializer, "{}") }
        assertFailsWith<SerializationException> { Json.decodeFromString(InstantComponentSerializer, "{\"nanosecondsOfSecond\":3}") }
    }
}