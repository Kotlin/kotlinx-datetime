/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.test.*

class UtcOffsetSerializationTest {

    private fun iso8601Serialization(serializer: KSerializer<UtcOffset>) {
        // the default form is obtainable and parsable
        for ((offset, json) in listOf(
            Pair(UtcOffset(hours = 0), "\"Z\""),
            Pair(UtcOffset(hours = 1), "\"+01:00\""),
            Pair(UtcOffset(hours = 1, minutes = 30), "\"+01:30\""),
            Pair(UtcOffset(hours = 1, minutes = 30, seconds = 59), "\"+01:30:59\""),
        )) {
            assertEquals(json, Json.encodeToString(serializer, offset))
            assertEquals(offset, Json.decodeFromString(serializer, json))
        }
        // alternative forms are also parsable
        for ((offset, json) in listOf(
            Pair(UtcOffset(hours = 0), "\"+00:00\""),
            Pair(UtcOffset(hours = 0), "\"z\""),
        )) {
            assertEquals(offset, Json.decodeFromString(serializer, json))
        }
        // some strings aren't parsable
        for (json in listOf(
            "\"+3\"",
            "\"+03\"",
            "\"+03:0\"",
            "\"UTC+02:00\"",
        )) {
            assertFailsWith<IllegalArgumentException> {
                Json.decodeFromString(serializer, json)
            }
        }
    }

    @Test
    fun testIso8601Serialization() {
        iso8601Serialization(UtcOffsetIso8601Serializer)
    }

    @Test
    fun testDefaultSerializers() {
        // should be the same as the ISO 8601
        iso8601Serialization(Json.serializersModule.serializer())
        iso8601Serialization(UtcOffset.serializer())
    }
}
