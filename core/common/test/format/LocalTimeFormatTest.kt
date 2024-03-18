/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(FormatStringsInDatetimeFormats::class)

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class LocalTimeFormatTest {

    @Test
    fun testErrorHandling() {
        LocalTime.Formats.ISO.apply {
            assertEquals(LocalTime(15, 36), parse("15:36"))
            assertCanNotParse("40:36")
            assertCanNotParse("XX:36")
        }
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
            byUnicodePattern("HH:mm")
        })
        test(times, LocalTime.Format {
            hour()
            char(':')
            minute()
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
            byUnicodePattern("HH:mm:ss")
        })
        test(times, LocalTime.Format {
            hour()
            char(':')
            minute()
            char(':')
            second()
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
            amPmHour()
            char(':')
            minute()
            char(' ')
            amPmMarker("AM", "PM")
        })
    }

    @Test
    fun testIso() {
        val times = buildMap<LocalTime, Pair<String, Set<String>>> {
            put(LocalTime(0, 0, 0, 0), ("00:00:00" to setOf("00:00")))
            put(LocalTime(1, 0, 0, 0), ("01:00:00" to setOf("01:00")))
            put(LocalTime(23, 0, 0, 0), ("23:00:00" to setOf("23:00")))
            put(LocalTime(0, 1, 0, 0), ("00:01:00" to setOf("00:01")))
            put(LocalTime(12, 30, 0, 0), ("12:30:00" to setOf("12:30")))
            put(LocalTime(23, 59, 0, 0), ("23:59:00" to setOf("23:59")))
            put(LocalTime(0, 0, 1, 0), ("00:00:01" to setOf()))
            put(LocalTime(0, 0, 59, 0), ("00:00:59" to setOf()))
            put(LocalTime(0, 0, 0, 100000000), ("00:00:00.1" to setOf("00:00:00.10")))
            put(LocalTime(0, 0, 0, 10000000), ("00:00:00.01" to setOf("00:00:00.010")))
            put(LocalTime(0, 0, 0, 1000000), ("00:00:00.001" to setOf()))
            put(LocalTime(0, 0, 0, 100000), ("00:00:00.0001" to setOf("00:00:00.000100")))
            put(LocalTime(0, 0, 0, 10000), ("00:00:00.00001" to setOf()))
            put(LocalTime(0, 0, 0, 1000), ("00:00:00.000001" to setOf()))
            put(LocalTime(0, 0, 0, 100), ("00:00:00.0000001" to setOf()))
            put(LocalTime(0, 0, 0, 10), ("00:00:00.00000001" to setOf()))
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
        test(times, LocalTime.Formats.ISO)
    }

    @Test
    fun testFormattingSecondFractions() {
        fun check(nanoseconds: Int, minLength: Int?, maxLength: Int?, string: String) {
            fun DateTimeFormatBuilder.WithTime.secondFractionWithThisLength() {
                when {
                    minLength != null && maxLength != null -> secondFraction(minLength, maxLength)
                    maxLength != null -> secondFraction(maxLength = maxLength)
                    minLength != null -> secondFraction(minLength = minLength)
                    else -> secondFraction()
                }
            }
            val format = LocalTime.Format { secondFractionWithThisLength() }
            val time = LocalTime(0, 0, 0, nanoseconds)
            assertEquals(string, format.format(time))
            val format2 = LocalTime.Format {
                hour(); minute(); second()
                char('.'); secondFractionWithThisLength()
            }
            val time2 = format2.parse("123456.$string")
            assertEquals((string + "0".repeat(9 - string.length)).toInt(), time2.nanosecond)
        }
        check(1, null, null, "000000001")
        check(1, null, 9, "000000001")
        check(1, null, 8, "0")
        check(1, null, 7, "0")
        check(999_999_999, null, null, "999999999")
        check(999_999_999, null, 9, "999999999")
        check(999_999_999, null, 8, "99999999")
        check(999_999_999, null, 7, "9999999")
        check(100000000, null, null, "1")
        check(100000000, null, 4, "1")
        check(100000000, null, 3, "1")
        check(100000000, null, 2, "1")
        check(100000000, null, 1, "1")
        check(100000000, 4, null, "1000")
        check(100000000, 3, null, "100")
        check(100000000, 2, null, "10")
        check(100000000, 1, null, "1")
        check(100000000, 4, 4, "1000")
        check(100000000, 3, 4, "100")
        check(100000000, 3, 3, "100")
        check(100000000, 2, 3, "10")
        check(100000000, 2, 2, "10")
        check(987654321, null, null, "987654321")
        check(987654320, null, null, "98765432")
        check(987654300, null, null, "9876543")
        check(987654000, null, null, "987654")
        check(987650000, null, null, "98765")
        check(987600000, null, null, "9876")
        check(987000000, null, null, "987")
        check(980000000, null, null, "98")
        check(900000000, null, null, "9")
        check(0, null, null, "0")
        check(987654321, null, 9, "987654321")
        check(987654321, null, 8, "98765432")
        check(987654321, null, 7, "9876543")
        check(987654321, null, 6, "987654")
        check(987654321, null, 5, "98765")
        check(987654321, null, 4, "9876")
        check(987654321, null, 3, "987")
        check(987654321, null, 2, "98")
        check(987654321, null, 1, "9")
    }

    @Test
    fun testDoc() {
        val format = LocalTime.Format {
          hour()
          char(':')
          minute()
          char(':')
          second()
          optional {
            char('.')
            secondFraction()
          }
        }
        assertEquals("12:34:56", format.format(LocalTime(12, 34, 56)))
        assertEquals("12:34:56.123", format.format(LocalTime(12, 34, 56, 123000000)))
    }

    @Test
    fun testParsingDisagreeingComponents() {
        LocalTime.Format {
            hour()
            char(':')
            minute()
            char('(')
            amPmHour()
            char(' ')
            amPmMarker("AM", "PM")
            char(')')
        }.apply {
            assertEquals(LocalTime(23, 59), parse("23:59(11 PM)"))
            assertCanNotParse("23:59(11 AM)")
        }
    }

    @Test
    fun testEmptyAmPmMarkers() {
        assertFailsWith<IllegalArgumentException> {
            LocalTime.Format {
                amPmMarker("", "pm")
            }
        }
    }

    @Test
    fun testIdenticalAmPmMarkers() {
        assertFailsWith<IllegalArgumentException> {
            LocalTime.Format {
                amPmMarker("pm", "pm")
            }
        }
    }

    private fun test(strings: Map<LocalTime, Pair<String, Set<String>>>, format: DateTimeFormat<LocalTime>) {
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
