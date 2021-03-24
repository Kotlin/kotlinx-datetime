/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.json.*
import kotlin.random.*
import kotlin.test.*

class DateTimeUnitSerializationTest {
    @Test
    fun timeBasedSerialization() {
        repeat(100) {
            val nanoseconds = Random.nextLong(1, Long.MAX_VALUE)
            val unit = DateTimeUnit.TimeBased(nanoseconds)
            val json = "{\"nanoseconds\":${nanoseconds.toString()}}" // https://youtrack.jetbrains.com/issue/KT-39891
            assertEquals(json, Json.encodeToString(TimeBasedDateTimeUnitSerializer, unit))
            assertEquals(unit, Json.decodeFromString(TimeBasedDateTimeUnitSerializer, json))
        }
    }

    @Test
    fun dayBasedSerialization() {
        repeat(100) {
            val days = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.DayBased(days)
            val json = "{\"days\":$days}"
            assertEquals(json, Json.encodeToString(DayBasedDateTimeUnitSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DayBasedDateTimeUnitSerializer, json))
        }
    }

    @Test
    fun monthBasedSerialization() {
        repeat(100) {
            val months = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.MonthBased(months)
            val json = "{\"months\":$months}"
            assertEquals(json, Json.encodeToString(MonthBasedDateTimeUnitSerializer, unit))
            assertEquals(unit, Json.decodeFromString(MonthBasedDateTimeUnitSerializer, json))
        }
    }

    @Test
    fun dateBasedSerialization() {
        repeat(100) {
            val days = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.DayBased(days)
            val json = "{\"type\":\"DayBased\",\"days\":$days}"
            assertEquals(json, Json.encodeToString(DateBasedDateTimeUnitSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DateBasedDateTimeUnitSerializer, json))
        }
        repeat(100) {
            val months = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.MonthBased(months)
            val json = "{\"type\":\"MonthBased\",\"months\":$months}"
            assertEquals(json, Json.encodeToString(DateBasedDateTimeUnitSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DateBasedDateTimeUnitSerializer, json))
        }
    }

    @Test
    fun serialization() {
        repeat(100) {
            val nanoseconds = Random.nextLong(1, Long.MAX_VALUE)
            val unit = DateTimeUnit.TimeBased(nanoseconds)
            val json = "{\"type\":\"TimeBased\",\"nanoseconds\":${nanoseconds.toString()}}" // https://youtrack.jetbrains.com/issue/KT-39891
            assertEquals(json, Json.encodeToString(DateTimeUnitSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DateTimeUnitSerializer, json))
        }
        repeat(100) {
            val days = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.DayBased(days)
            val json = "{\"type\":\"DayBased\",\"days\":$days}"
            assertEquals(json, Json.encodeToString(DateTimeUnitSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DateTimeUnitSerializer, json))
        }
        repeat(100) {
            val months = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.MonthBased(months)
            val json = "{\"type\":\"MonthBased\",\"months\":$months}"
            assertEquals(json, Json.encodeToString(DateTimeUnitSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DateTimeUnitSerializer, json))
        }
    }

}