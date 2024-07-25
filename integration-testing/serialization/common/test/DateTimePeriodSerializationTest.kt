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

class DateTimePeriodSerializationTest {

    private fun datePeriodIso8601Serialization(
        datePeriodSerializer: KSerializer<DatePeriod>,
        dateTimePeriodSerializer: KSerializer<DateTimePeriod>
    ) {
        for ((period, json) in listOf(
            Pair(DatePeriod(1, 2, 3), "\"P1Y2M3D\""),
            Pair(DatePeriod(years = 1), "\"P1Y\""),
            Pair(DatePeriod(years = 1, months = 1), "\"P1Y1M\""),
            Pair(DatePeriod(months = 11), "\"P11M\""),
            Pair(DatePeriod(months = 14), "\"P1Y2M\""),
            Pair(DatePeriod(months = 10, days = 5), "\"P10M5D\""),
            Pair(DatePeriod(years = 1, days = 40), "\"P1Y40D\""),
        )) {
            assertEquals(json, Json.encodeToString(datePeriodSerializer, period))
            assertEquals(period, Json.decodeFromString(datePeriodSerializer, json))
            assertEquals(json, Json.encodeToString(dateTimePeriodSerializer, period))
            assertEquals(period, Json.decodeFromString(dateTimePeriodSerializer, json) as DatePeriod)
        }
        // time-based keys should not be considered unknown here
        assertFailsWith<IllegalArgumentException> {
            Json { ignoreUnknownKeys = true }.decodeFromString(datePeriodSerializer, "\"P3DT1H\"")
        }
        // presence of time-based keys should not be a problem if the values are 0
        Json.decodeFromString(datePeriodSerializer, "\"P3DT0H\"")
    }

    private fun datePeriodComponentSerialization(
        datePeriodSerializer: KSerializer<DatePeriod>,
        dateTimePeriodSerializer: KSerializer<DateTimePeriod>
    ) {
        for ((period, json) in listOf(
            Pair(DatePeriod(1, 2, 3), "{\"years\":1,\"months\":2,\"days\":3}"),
            Pair(DatePeriod(years = 1), "{\"years\":1}"),
            Pair(DatePeriod(years = 1, months = 1), "{\"years\":1,\"months\":1}"),
            Pair(DatePeriod(months = 11), "{\"months\":11}"),
            Pair(DatePeriod(months = 14), "{\"years\":1,\"months\":2}"),
            Pair(DatePeriod(months = 10, days = 5), "{\"months\":10,\"days\":5}"),
            Pair(DatePeriod(years = 1, days = 40), "{\"years\":1,\"days\":40}"),
        )) {
            assertEquals(json, Json.encodeToString(datePeriodSerializer, period))
            assertEquals(period, Json.decodeFromString(datePeriodSerializer, json))
            assertEquals(json, Json.encodeToString(dateTimePeriodSerializer, period))
            assertEquals(period, Json.decodeFromString(dateTimePeriodSerializer, json) as DatePeriod)
        }
        // time-based keys should not be considered unknown here
        assertFailsWith<SerializationException> {
            Json { ignoreUnknownKeys = true }.decodeFromString(datePeriodSerializer, "{\"hours\":3}")
        }
        // presence of time-based keys should not be a problem if the values are 0
        Json.decodeFromString(datePeriodSerializer, "{\"hours\":0}")
    }

    private fun dateTimePeriodIso8601Serialization(dateTimePeriodSerializer: KSerializer<DateTimePeriod>) {
        for ((period, json) in listOf(
          Pair(DateTimePeriod(), "\"P0D\""),
          Pair(DateTimePeriod(hours = 1), "\"PT1H\""),
          Pair(DateTimePeriod(days = 1, hours = -1), "\"P1DT-1H\""),
          Pair(DateTimePeriod(days = -1, hours = -1), "\"-P1DT1H\""),
          Pair(DateTimePeriod(months = -1), "\"-P1M\""),
          Pair(DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000),
            "\"-P1Y2M3DT4H4M59.500000000S\""),
        )) {
            assertEquals(json, Json.encodeToString(dateTimePeriodSerializer, period))
            assertEquals(period, Json.decodeFromString(dateTimePeriodSerializer, json))
        }
    }

    private fun dateTimePeriodComponentSerialization(dateTimePeriodSerializer: KSerializer<DateTimePeriod>) {
        for ((period, json) in listOf(
            Pair(DateTimePeriod(), "{}"),
            Pair(DateTimePeriod(hours = 1), "{\"hours\":1}"),
            Pair(DateTimePeriod(days = 1, hours = -1), "{\"days\":1,\"hours\":-1}"),
            Pair(DateTimePeriod(days = -1, hours = -1), "{\"days\":-1,\"hours\":-1}"),
            Pair(DateTimePeriod(months = -1), "{\"months\":-1}"),
            Pair(DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000),
                "{\"years\":-1,\"months\":-2,\"days\":-3,\"hours\":-4,\"minutes\":-4,\"seconds\":-59,\"nanoseconds\":-500000000}"),
        )) {
            assertEquals(json, Json.encodeToString(dateTimePeriodSerializer, period))
            assertEquals(period, Json.decodeFromString(dateTimePeriodSerializer, json))
        }
    }

    @Test
    fun testDatePeriodIso8601Serialization() {
        assertKSerializerName("kotlinx.datetime.DatePeriod ISO", DatePeriodIso8601Serializer)
        datePeriodIso8601Serialization(DatePeriodIso8601Serializer, DateTimePeriodIso8601Serializer)
    }

    @Test
    fun testDatePeriodComponentSerialization() {
        assertKSerializerName("kotlinx.datetime.DatePeriod components", DatePeriodComponentSerializer)
        datePeriodComponentSerialization(DatePeriodComponentSerializer, DateTimePeriodComponentSerializer)
    }

    @Test
    fun testDateTimePeriodIso8601Serialization() {
        assertKSerializerName("kotlinx.datetime.DateTimePeriod ISO", DateTimePeriodIso8601Serializer)
        dateTimePeriodIso8601Serialization(DateTimePeriodIso8601Serializer)
    }

    @Test
    fun testDateTimePeriodComponentSerialization() {
        assertKSerializerName("kotlinx.datetime.DateTimePeriod components", DateTimePeriodComponentSerializer)
        dateTimePeriodComponentSerialization(DateTimePeriodComponentSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        // Check that they behave the same as the ISO 8601 serializers
        assertKSerializerName<DateTimePeriod>("kotlinx.datetime.DateTimePeriod", Json.serializersModule.serializer())
        dateTimePeriodIso8601Serialization(Json.serializersModule.serializer())
        assertKSerializerName<DatePeriod>("kotlinx.datetime.DatePeriod", Json.serializersModule.serializer())
        datePeriodIso8601Serialization(Json.serializersModule.serializer(), Json.serializersModule.serializer())
    }

}
