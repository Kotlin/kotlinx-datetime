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
    fun testErrorHandling() {
        val format = LocalTime.Format.ISO
        assertEquals(LocalTime(15, 36), format.parse("15:36"))
        val error = assertFailsWith<DateTimeFormatException> { format.parse("40:36") }
        assertContains(error.message!!, "40")
        assertFailsWith<DateTimeFormatException> { format.parse("XX:36") }
    }

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
        test(times, LocalTime.Format {
            appendUnicodeFormatString("HH:mm")
        })
        test(times, LocalTime.Format {
            appendHour()
            appendLiteral(':')
            appendMinute()
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
        test(times, LocalTime.Format {
            appendUnicodeFormatString("HH:mm:ss")
        })
        test(times, LocalTime.Format {
            appendHour()
            appendLiteral(':')
            appendMinute()
            appendLiteral(':')
            appendSecond()
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
        test(times, LocalTime.Format {
            appendAmPmHour()
            appendLiteral(':')
            appendMinute()
            appendLiteral(' ')
            appendAmPmMarker("AM", "PM")
        })
    }

    @Test
    fun testIso() {
        val times = buildMap<LocalTime, Pair<String, Set<String>>> {
            put(LocalTime(0, 0, 0, 0), ("00:00" to setOf("00:00:00")))
            put(LocalTime(1, 0, 0, 0), ("01:00" to setOf()))
            put(LocalTime(23, 0, 0, 0), ("23:00" to setOf()))
            put(LocalTime(0, 1, 0, 0), ("00:01" to setOf()))
            put(LocalTime(12, 30, 0, 0), ("12:30" to setOf()))
            put(LocalTime(23, 59, 0, 0), ("23:59" to setOf()))
            put(LocalTime(0, 0, 1, 0), ("00:00:01" to setOf()))
            put(LocalTime(0, 0, 59, 0), ("00:00:59" to setOf()))
            put(LocalTime(0, 0, 0, 100000000), ("00:00:00.100" to setOf("00:00:00.1")))
            put(LocalTime(0, 0, 0, 10000000), ("00:00:00.010" to setOf("00:00:00.01")))
            put(LocalTime(0, 0, 0, 1000000), ("00:00:00.001" to setOf()))
            put(LocalTime(0, 0, 0, 100000), ("00:00:00.000100" to setOf("00:00:00.0001")))
            put(LocalTime(0, 0, 0, 10000), ("00:00:00.000010" to setOf()))
            put(LocalTime(0, 0, 0, 1000), ("00:00:00.000001" to setOf()))
            put(LocalTime(0, 0, 0, 100), ("00:00:00.000000100" to setOf()))
            put(LocalTime(0, 0, 0, 10), ("00:00:00.000000010" to setOf()))
            put(LocalTime(0, 0, 0, 1), ("00:00:00.000000001" to setOf()))
            put(LocalTime(0, 0, 0, 999999999), ("00:00:00.999999999" to setOf()))
            put(LocalTime(0, 0, 0, 99999999), ("00:00:00.099999999" to setOf()))
            put(LocalTime(0, 0, 0, 9999999), ("00:00:00.009999999" to setOf()))
            put(LocalTime(0, 0, 0, 999999), ("00:00:00.000999999" to setOf()))
            put(LocalTime(0, 0, 0, 99999), ("00:00:00.000099999" to setOf()))
            put(LocalTime(0, 0, 0, 9999), ("00:00:00.000009999" to setOf()))
            put(LocalTime(0, 0, 0, 999), ("00:00:00.000000999" to setOf()))
            put(LocalTime(0, 0, 0, 99), ("00:00:00.000000099" to setOf()))
            put(LocalTime(0, 0, 0, 9), ("00:00:00.000000009" to setOf()))
        }
        test(times, LocalTime.Format.ISO)
    }

    @Test
    fun testBasicIso() {
        val times = buildMap<LocalTime, Pair<String, Set<String>>> {
            put(LocalTime(0, 0, 0, 0), ("T0000" to setOf("T000000", "T0000", "t0000")))
            put(LocalTime(1, 0, 0, 0), ("T0100" to setOf()))
            put(LocalTime(23, 0, 0, 0), ("T2300" to setOf()))
            put(LocalTime(0, 1, 0, 0), ("T0001" to setOf()))
            put(LocalTime(12, 30, 0, 0), ("T1230" to setOf()))
            put(LocalTime(23, 59, 0, 0), ("T2359" to setOf()))
            put(LocalTime(0, 0, 1, 0), ("T000001" to setOf()))
            put(LocalTime(0, 0, 59, 0), ("T000059" to setOf()))
            put(LocalTime(0, 0, 0, 100000000), ("T000000.100" to setOf("T000000.1")))
            put(LocalTime(0, 0, 0, 10000000), ("T000000.010" to setOf("T000000.01")))
            put(LocalTime(0, 0, 0, 1000000), ("T000000.001" to setOf()))
            put(LocalTime(0, 0, 0, 100000), ("T000000.000100" to setOf("T000000.0001")))
            put(LocalTime(0, 0, 0, 10000), ("T000000.000010" to setOf()))
            put(LocalTime(0, 0, 0, 1000), ("T000000.000001" to setOf()))
            put(LocalTime(0, 0, 0, 100), ("T000000.000000100" to setOf()))
            put(LocalTime(0, 0, 0, 10), ("T000000.000000010" to setOf()))
            put(LocalTime(0, 0, 0, 1), ("T000000.000000001" to setOf()))
            put(LocalTime(0, 0, 0, 999999999), ("T000000.999999999" to setOf()))
            put(LocalTime(0, 0, 0, 99999999), ("T000000.099999999" to setOf()))
            put(LocalTime(0, 0, 0, 9999999), ("T000000.009999999" to setOf()))
            put(LocalTime(0, 0, 0, 999999), ("T000000.000999999" to setOf()))
            put(LocalTime(0, 0, 0, 99999), ("T000000.000099999" to setOf()))
            put(LocalTime(0, 0, 0, 9999), ("T000000.000009999" to setOf()))
            put(LocalTime(0, 0, 0, 999), ("T000000.000000999" to setOf()))
            put(LocalTime(0, 0, 0, 99), ("T000000.000000099" to setOf()))
            put(LocalTime(0, 0, 0, 9), ("T000000.000000009" to setOf()))
        }
        test(times, LocalTime.Format.ISO_BASIC)
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
