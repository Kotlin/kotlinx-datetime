/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.serialization.json.*
import kotlin.random.*
import kotlin.test.*

class DateTimeUnitSerializationTest {
    @Test
    fun timeBasedSerialization() {
        repeat(100) {
            val nanoseconds = Random.nextLong(1, Long.MAX_VALUE)
            val unit = DateTimeUnit.TimeBased(nanoseconds)
            val json = "{\"nanoseconds\":$nanoseconds}"
            assertEquals(json, Json.encodeToString(TimeBasedSerializer, unit))
            assertEquals(unit, Json.decodeFromString(TimeBasedSerializer, json))
        }
    }

    @Test
    fun dayBasedSerialization() {
        repeat(100) {
            val days = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.DayBased(days)
            val json = "{\"days\":$days}"
            assertEquals(json, Json.encodeToString(DayBasedSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DayBasedSerializer, json))
        }
    }

    @Test
    fun monthBasedSerialization() {
        repeat(100) {
            val months = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.MonthBased(months)
            val json = "{\"months\":$months}"
            assertEquals(json, Json.encodeToString(MonthBasedSerializer, unit))
            assertEquals(unit, Json.decodeFromString(MonthBasedSerializer, json))
        }
    }

    @Test
    fun dateBasedSerialization() {
        repeat(100) {
            val days = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.DayBased(days)
            val json = "{\"type\":\"DayBased\",\"days\":$days}"
            assertEquals(json, Json.encodeToString(DateBasedSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DateBasedSerializer, json))
        }
        repeat(100) {
            val months = Random.nextInt(1, Int.MAX_VALUE)
            val unit = DateTimeUnit.DateBased.MonthBased(months)
            val json = "{\"type\":\"MonthBased\",\"months\":$months}"
            assertEquals(json, Json.encodeToString(DateBasedSerializer, unit))
            assertEquals(unit, Json.decodeFromString(DateBasedSerializer, json))
        }
    }

    @Test
    fun serialization() {
        repeat(100) {
            val nanoseconds = Random.nextLong(1, Long.MAX_VALUE)
            val unit = DateTimeUnit.TimeBased(nanoseconds)
            val json = "{\"type\":\"TimeBased\",\"nanoseconds\":$nanoseconds}"
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