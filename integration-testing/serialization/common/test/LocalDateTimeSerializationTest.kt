/*
 * Copyright 2019-2020 JetBrains s.r.o.
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

class LocalDateTimeSerializationTest {
    private fun iso8601Serialization(serializer: KSerializer<LocalDateTime>) {
        for ((localDateTime, json) in listOf(
            Pair(LocalDateTime(2008, 7, 5, 2, 1), "\"2008-07-05T02:01\""),
            Pair(LocalDateTime(2007, 12, 31, 23, 59, 1), "\"2007-12-31T23:59:01\""),
            Pair(LocalDateTime(999, 12, 31, 23, 59, 59, 990000000), "\"0999-12-31T23:59:59.990\""),
            Pair(LocalDateTime(-1, 1, 2, 23, 59, 59, 999990000), "\"-0001-01-02T23:59:59.999990\""),
            Pair(LocalDateTime(-2008, 1, 2, 23, 59, 59, 999999990), "\"-2008-01-02T23:59:59.999999990\""),
        )) {
            assertEquals(json, Json.encodeToString(serializer, localDateTime))
            assertEquals(localDateTime, Json.decodeFromString(serializer, json))
        }
    }

    private fun componentSerialization(serializer: KSerializer<LocalDateTime>) {
        for ((localDateTime, json) in listOf(
            Pair(LocalDateTime(2008, 7, 5, 2, 1), "{\"year\":2008,\"month\":7,\"day\":5,\"hour\":2,\"minute\":1}"),
            Pair(LocalDateTime(2007, 12, 31, 23, 59, 1),
                "{\"year\":2007,\"month\":12,\"day\":31,\"hour\":23,\"minute\":59,\"second\":1}"),
            Pair(LocalDateTime(999, 12, 31, 23, 59, 59, 990000000),
                "{\"year\":999,\"month\":12,\"day\":31,\"hour\":23,\"minute\":59,\"second\":59,\"nanosecond\":990000000}"),
            Pair(LocalDateTime(-1, 1, 2, 23, 59, 59, 999990000),
                "{\"year\":-1,\"month\":1,\"day\":2,\"hour\":23,\"minute\":59,\"second\":59,\"nanosecond\":999990000}"),
            Pair(LocalDateTime(-2008, 1, 2, 23, 59, 59, 999999990),
                "{\"year\":-2008,\"month\":1,\"day\":2,\"hour\":23,\"minute\":59,\"second\":59,\"nanosecond\":999999990}"),
            Pair(LocalDateTime(-2008, 1, 2, 23, 59, 0, 1),
                "{\"year\":-2008,\"month\":1,\"day\":2,\"hour\":23,\"minute\":59,\"second\":0,\"nanosecond\":1}"),
        )) {
            assertEquals(json, Json.encodeToString(serializer, localDateTime))
            assertEquals(localDateTime, Json.decodeFromString(serializer, json))
        }
        // adding omitted values shouldn't break deserialization
        assertEquals(LocalDateTime(2008, 7, 5, 2, 1),
            Json.decodeFromString(serializer,
                "{\"year\":2008,\"month\":7,\"day\":5,\"hour\":2,\"minute\":1,\"second\":0}"
            ))
        assertEquals(LocalDateTime(2008, 7, 5, 2, 1),
            Json.decodeFromString(serializer,
                "{\"year\":2008,\"month\":7,\"day\":5,\"hour\":2,\"minute\":1,\"nanosecond\":0}"
            ))
        assertEquals(LocalDateTime(2008, 7, 5, 2, 1),
            Json.decodeFromString(serializer,
                "{\"year\":2008,\"month\":7,\"day\":5,\"hour\":2,\"minute\":1,\"second\":0,\"nanosecond\":0}"
            ))
        // invalid values must fail to construct
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(serializer,
                "{\"year\":1000000000000,\"month\":3,\"day\":12,\"hour\":10,\"minute\":2}")
        }
        assertFailsWith<IllegalArgumentException> {
            Json.decodeFromString(serializer,
                "{\"year\":2020,\"month\":30,\"day\":12,\"hour\":10,\"minute\":2}")
        }
    }

    @Test
    fun testIso8601Serialization() {
        iso8601Serialization(LocalDateTimeIso8601Serializer)
    }

    @Test
    fun testComponentSerialization() {
        componentSerialization(LocalDateTimeComponentSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        // should be the same as the ISO 8601
        iso8601Serialization(Json.serializersModule.serializer())
    }

    object PythonDateTimeSerializer : FormattedLocalDateTimeSerializer(LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char(' ')
        time(LocalTime.Formats.ISO)
    })

    @Test
    fun testCustomSerializer() {
        for ((localDateTime, json) in listOf(
            Pair(LocalDateTime(2008, 7, 5, 2, 1), "\"2008-07-05 02:01:00\""),
            Pair(LocalDateTime(2007, 12, 31, 23, 59, 1), "\"2007-12-31 23:59:01\""),
            Pair(LocalDateTime(999, 12, 31, 23, 59, 59, 990000000), "\"0999-12-31 23:59:59.99\""),
            Pair(LocalDateTime(-1, 1, 2, 23, 59, 59, 999990000), "\"-0001-01-02 23:59:59.99999\""),
            Pair(LocalDateTime(-2008, 1, 2, 23, 59, 59, 999999990), "\"-2008-01-02 23:59:59.99999999\""),
        )) {
            assertEquals(json, Json.encodeToString(PythonDateTimeSerializer, localDateTime))
            assertEquals(localDateTime, Json.decodeFromString(PythonDateTimeSerializer, json))
        }
    }
}
