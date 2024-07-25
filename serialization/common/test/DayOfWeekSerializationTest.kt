/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.json.*
import kotlin.test.*

class DayOfWeekSerializationTest {
    @Test
    fun testSerialization() {
        assertKSerializerName("kotlinx.datetime.DayOfWeek", DayOfWeekSerializer)
        for (dayOfWeek in DayOfWeek.entries) {
            val json = "\"${dayOfWeek.name}\""
            assertEquals(json, Json.encodeToString(DayOfWeekSerializer, dayOfWeek))
            assertEquals(dayOfWeek, Json.decodeFromString(DayOfWeekSerializer, json))
        }
    }
}
