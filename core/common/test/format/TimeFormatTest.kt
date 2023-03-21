/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class TimeFormatTest {
    @Test
    fun testHoursMinutes() {
        val times = buildMap<LocalTime, Pair<String, Set<String>>> {
            put(LocalTime(0, 0, 0, 0), ("00:00" to setOf()))
            put(LocalTime(1, 0, 0, 0), ("01:00" to setOf()))
            put(LocalTime(23, 0, 0, 0), ("23:00" to setOf()))
            put(LocalTime(0, 1, 0, 0), ("00:01" to setOf()))
            put(LocalTime(12, 30, 0, 0), ("12:30" to setOf()))
            put(LocalTime(23, 59, 0, 0), ("23:59" to setOf()))
        }
        test(LocalTimeFormat.fromFormatString("hh:mm"), times)
        test(LocalTimeFormat.build {
            appendHour(2)
            appendLiteral(':')
            appendMinute(2)
        }, times)
    }

    @Test
    fun testHoursMinutesSeconds() {
        val times = buildMap<LocalTime, Pair<String, Set<String>>> {
            put(LocalTime(0, 0, 0, 0), ("00:00:00" to setOf()))
            put(LocalTime(1, 0, 0, 0), ("01:00:00" to setOf()))
            put(LocalTime(23, 0, 0, 0), ("23:00:00" to setOf()))
            put(LocalTime(0, 1, 0, 0), ("00:01:00" to setOf()))
            put(LocalTime(12, 30, 0, 0), ("12:30:00" to setOf()))
            put(LocalTime(23, 59, 0, 0), ("23:59:00" to setOf()))
            put(LocalTime(0, 0, 1, 0), ("00:00:01" to setOf()))
            put(LocalTime(0, 0, 59, 0), ("00:00:59" to setOf()))
        }
        test(LocalTimeFormat.fromFormatString("hh:mm:ss"), times)
        test(LocalTimeFormat.build {
            appendHour(2)
            appendLiteral(':')
            appendMinute(2)
            appendLiteral(':')
            appendSecond(2)
        }, times)
    }

    private fun test(format: LocalTimeFormat, strings: Map<LocalTime, Pair<String, Set<String>>>) {
        for ((date, stringsForDate) in strings) {
            val (canonicalString, otherStrings) = stringsForDate
            assertEquals(canonicalString, format.format(date), "formatting $date with $format")
            assertEquals(date, format.parse(canonicalString), "parsing '$canonicalString' with $format")
            for (otherString in otherStrings) {
                assertEquals(date, format.parse(otherString), "parsing '$otherString' with $format")
            }
        }
    }
}
