/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlinx.datetime.format.migration.*
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
        test(times, LocalTime.Format.build {
            appendUnicodeFormatString("HH:mm")
        })
        test(times, LocalTime.Format.build {
            appendHour(2)
            appendLiteral(':')
            appendMinute(2)
        })
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
        test(times, LocalTime.Format.build {
            appendUnicodeFormatString("HH:mm:ss")
        })
        test(times, LocalTime.Format.build {
            appendHour(2)
            appendLiteral(':')
            appendMinute(2)
            appendLiteral(':')
            appendSecond(2)
        })
    }

    @Test
    fun testAmPmHour() {
        val times = buildMap<LocalTime, Pair<String, Set<String>>> {
            put(LocalTime(0, 0, 0, 0), ("12:00 AM" to setOf()))
            put(LocalTime(1, 0, 0, 0), ("01:00 AM" to setOf()))
            put(LocalTime(11, 0, 0, 0), ("11:00 AM" to setOf()))
            put(LocalTime(12, 0, 0, 0), ("12:00 PM" to setOf()))
            put(LocalTime(13, 0, 0, 0), ("01:00 PM" to setOf()))
            put(LocalTime(23, 0, 0, 0), ("11:00 PM" to setOf()))
        }
        test(times, LocalTime.Format.build {
            appendAmPmHour(2)
            appendLiteral(':')
            appendMinute(2)
            appendLiteral(' ')
            appendAmPmMarker("AM", "PM")
        })
    }

    private fun test(strings: Map<LocalTime, Pair<String, Set<String>>>, format: Format<LocalTime>) {
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
