/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class DateTimePeriodSerializationTest {

    @Test
    fun datePeriodISO8601Serialization() {
        for ((period, json) in listOf(
            Pair(DatePeriod(1, 2, 3), "\"P1Y2M3D\""),
            Pair(DatePeriod(years = 1), "\"P1Y\""),
            Pair(DatePeriod(years = 1, months = 1), "\"P1Y1M\""),
            Pair(DatePeriod(months = 11), "\"P11M\""),
            Pair(DatePeriod(months = 14), "\"P1Y2M\""),
            Pair(DatePeriod(months = 10, days = 5), "\"P10M5D\""),
            Pair(DatePeriod(years = 1, days = 40), "\"P1Y40D\""),
        )) {
            assertEquals(json, Json.encodeToString(DatePeriodISO8601Serializer, period))
            assertEquals(period, Json.decodeFromString(DatePeriodISO8601Serializer, json))
            assertEquals(json, Json.encodeToString(DateTimePeriodISO8601Serializer, period))
            assertEquals(period, Json.decodeFromString(DateTimePeriodISO8601Serializer, json) as DatePeriod)
        }
        // time-based keys should not be considered unknown here
        assertFailsWith<IllegalArgumentException> {
            Json { ignoreUnknownKeys = true }.decodeFromString(DatePeriodISO8601Serializer, "\"P3DT1H\"")
        }
        // presence of time-based keys should not be a problem if the values are 0
        Json.decodeFromString(DatePeriodISO8601Serializer, "\"P3DT0H\"")
    }

    @Test
    fun datePeriodComponentSerialization() {
        for ((period, json) in listOf(
            Pair(DatePeriod(1, 2, 3), "{\"years\":1,\"months\":2,\"days\":3}"),
            Pair(DatePeriod(years = 1), "{\"years\":1}"),
            Pair(DatePeriod(years = 1, months = 1), "{\"years\":1,\"months\":1}"),
            Pair(DatePeriod(months = 11), "{\"months\":11}"),
            Pair(DatePeriod(months = 14), "{\"years\":1,\"months\":2}"),
            Pair(DatePeriod(months = 10, days = 5), "{\"months\":10,\"days\":5}"),
            Pair(DatePeriod(years = 1, days = 40), "{\"years\":1,\"days\":40}"),
        )) {
            assertEquals(json, Json.encodeToString(DatePeriodComponentSerializer, period))
            assertEquals(period, Json.decodeFromString(DatePeriodComponentSerializer, json))
            assertEquals(json, Json.encodeToString(DateTimePeriodComponentSerializer, period))
            assertEquals(period, Json.decodeFromString(DateTimePeriodComponentSerializer, json) as DatePeriod)
        }
        // time-based keys should not be considered unknown here
        assertFailsWith<SerializationException> {
            Json { ignoreUnknownKeys = true }.decodeFromString(DatePeriodComponentSerializer, "{\"hours\":3}")
        }
        // presence of time-based keys should not be a problem if the values are 0
        Json.decodeFromString(DatePeriodComponentSerializer, "{\"hours\":0}")
    }

    @Test
    fun dateTimePeriodISO8601Serialization() {
        for ((period, json) in listOf(
          Pair(DateTimePeriod(), "\"P0D\""),
          Pair(DateTimePeriod(hours = 1), "\"PT1H\""),
          Pair(DateTimePeriod(days = 1, hours = -1), "\"P1DT-1H\""),
          Pair(DateTimePeriod(days = -1, hours = -1), "\"-P1DT1H\""),
          Pair(DateTimePeriod(months = -1), "\"-P1M\""),
          Pair(DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000),
            "\"-P1Y2M3DT4H4M59.500000000S\""),
        )) {
            assertEquals(json, Json.encodeToString(DateTimePeriodISO8601Serializer, period))
            assertEquals(period, Json.decodeFromString(DateTimePeriodISO8601Serializer, json))
        }
    }

    @Test
    fun dateTimePeriodComponentSerialization() {
        for ((period, json) in listOf(
            Pair(DateTimePeriod(), "{}"),
            Pair(DateTimePeriod(hours = 1), "{\"hours\":1}"),
            Pair(DateTimePeriod(days = 1, hours = -1), "{\"days\":1,\"hours\":-1}"),
            Pair(DateTimePeriod(days = -1, hours = -1), "{\"days\":-1,\"hours\":-1}"),
            Pair(DateTimePeriod(months = -1), "{\"months\":-1}"),
            Pair(DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5, seconds = 0, nanoseconds = 500_000_000),
                "{\"years\":-1,\"months\":-2,\"days\":-3,\"hours\":-4,\"minutes\":-4,\"seconds\":-59,\"nanoseconds\":-500000000}"),
        )) {
            assertEquals(json, Json.encodeToString(DateTimePeriodComponentSerializer, period))
            assertEquals(period, Json.decodeFromString(DateTimePeriodComponentSerializer, json))
        }
    }

}
