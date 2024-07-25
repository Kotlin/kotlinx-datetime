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

    private fun testSerializationAsPrimitive(serializer: KSerializer<UtcOffset>) {
        val offset2h = UtcOffset(hours = 2)
        assertEquals("\"+02:00\"", Json.encodeToString(serializer, offset2h))
        assertEquals(offset2h, Json.decodeFromString(serializer, "\"+02:00\""))
        assertEquals(offset2h, Json.decodeFromString(serializer, "\"+02:00:00\""))

        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(serializer, "\"UTC+02:00\"") // not an offset
        }
    }

    @Test
    fun defaultSerializer() {
        testSerializationAsPrimitive(Json.serializersModule.serializer())
    }

    @Test
    fun stringPrimitiveSerializer() {
        testSerializationAsPrimitive(UtcOffsetSerializer)
        testSerializationAsPrimitive(UtcOffset.serializer())
    }

    object FourDigitOffsetSerializer : FormattedUtcOffsetSerializer(UtcOffset.Formats.FOUR_DIGITS)

    @Test
    fun testCustomSerializer() {
        for ((utcOffset, json) in listOf(
            Pair(UtcOffset.ZERO, "\"+0000\""),
            Pair(UtcOffset(2), "\"+0200\""),
            Pair(UtcOffset(2, 30), "\"+0230\""),
            Pair(UtcOffset(-2, -30), "\"-0230\""),
        )) {
            assertEquals(json, Json.encodeToString(FourDigitOffsetSerializer, utcOffset))
            assertEquals(utcOffset, Json.decodeFromString(FourDigitOffsetSerializer, json))
        }
        assertEquals("\"+1234\"", Json.encodeToString(FourDigitOffsetSerializer, UtcOffset(12, 34, 56)))
    }
}
