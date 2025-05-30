/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.random.*
import kotlin.test.*

class DateTimeUnitSerializationTest {
    private fun timeBasedSerialization(serializer: KSerializer<DateTimeUnit.TimeBased>) {
        repeat(100) {
            val nanoseconds = Random.nextLong(1, Long.MAX_VALUE)
            val unit = DateTimeUnit.TimeBased(nanoseconds)
            val json = "{\"nanoseconds\":$nanoseconds}"
            assertEquals(json, Json.encodeToString(serializer, unit))
            assertEquals(unit, Json.decodeFromString(serializer, json))
        }
    }

    private fun dayBasedSerialization(serializer: KSerializer<DateTimeUnit.DayBased>) {
        repeat(100) {
            val days = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DayBased(days)
            val json = "{\"days\":$days}"
            assertEquals(json, Json.encodeToString(serializer, unit))
            assertEquals(unit, Json.decodeFromString(serializer, json))
        }
    }

    private fun monthBasedSerialization(serializer: KSerializer<DateTimeUnit.MonthBased>) {
        repeat(100) {
            val months = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.MonthBased(months)
            val json = "{\"months\":$months}"
            assertEquals(json, Json.encodeToString(serializer, unit))
            assertEquals(unit, Json.decodeFromString(serializer, json))
        }
    }

    private fun dateBasedSerialization(serializer: KSerializer<DateTimeUnit.DateBased>) {
        repeat(100) {
            val days = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DayBased(days)
            val json = "{\"type\":\"kotlinx.datetime.DayBased\",\"days\":$days}"
            assertEquals(json, Json.encodeToString(serializer, unit))
            assertEquals(unit, Json.decodeFromString(serializer, json))
        }
        repeat(100) {
            val months = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.MonthBased(months)
            val json = "{\"type\":\"kotlinx.datetime.MonthBased\",\"months\":$months}"
            assertEquals(json, Json.encodeToString(serializer, unit))
            assertEquals(unit, Json.decodeFromString(serializer, json))
        }
    }

    private fun serialization(serializer: KSerializer<DateTimeUnit>) {
        repeat(100) {
            val nanoseconds = Random.nextLong(1, Long.MAX_VALUE)
            val unit = DateTimeUnit.TimeBased(nanoseconds)
            val json = "{\"type\":\"kotlinx.datetime.TimeBased\",\"nanoseconds\":$nanoseconds}"
            assertEquals(json, Json.encodeToString(serializer, unit))
            assertEquals(unit, Json.decodeFromString(serializer, json))
        }
        repeat(100) {
            val days = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DayBased(days)
            val json = "{\"type\":\"kotlinx.datetime.DayBased\",\"days\":$days}"
            assertEquals(json, Json.encodeToString(serializer, unit))
            assertEquals(unit, Json.decodeFromString(serializer, json))
        }
        repeat(100) {
            val months = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.MonthBased(months)
            val json = "{\"type\":\"kotlinx.datetime.MonthBased\",\"months\":$months}"
            assertEquals(json, Json.encodeToString(serializer, unit))
            assertEquals(unit, Json.decodeFromString(serializer, json))
        }
    }

    @Test
    fun testTimeBasedUnitSerialization() {
        assertKSerializerName("kotlinx.datetime.TimeBased", TimeBasedDateTimeUnitSerializer)
        timeBasedSerialization(TimeBasedDateTimeUnitSerializer)
    }

    @Test
    fun testDayBasedSerialization() {
        assertKSerializerName("kotlinx.datetime.DayBased", DayBasedDateTimeUnitSerializer)
        dayBasedSerialization(DayBasedDateTimeUnitSerializer)
    }

    @Test
    fun testMonthBasedSerialization() {
        assertKSerializerName("kotlinx.datetime.MonthBased", MonthBasedDateTimeUnitSerializer)
        monthBasedSerialization(MonthBasedDateTimeUnitSerializer)
    }

    @Test
    fun testDateBasedSerialization() {
        assertKSerializerName("kotlinx.datetime.DateTimeUnit.DateBased", DateBasedDateTimeUnitSerializer)
        dateBasedSerialization(DateBasedDateTimeUnitSerializer)
    }

    @Test
    fun testSerialization() {
        serialization(DateTimeUnitSerializer)
    }

    @Test
    fun testDefaultSerializers() {
        monthBasedSerialization(Json.serializersModule.serializer())
        timeBasedSerialization(Json.serializersModule.serializer())
        dayBasedSerialization(Json.serializersModule.serializer())
        dateBasedSerialization(Json.serializersModule.serializer())
        serialization(Json.serializersModule.serializer())
    }

}
