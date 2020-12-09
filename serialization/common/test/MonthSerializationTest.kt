/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.*
import kotlinx.serialization.json.*
import kotlin.test.*

class MonthSerializationTest {
    @Test
    fun serialization() {
        for (month in Month.values()) {
            val json = "\"${month.name}\""
            assertEquals(json, Json.encodeToString(MonthSerializer, month))
            assertEquals(month, Json.decodeFromString(MonthSerializer, json))
        }
    }
}