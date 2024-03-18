/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class UtcOffsetFormatTest {

    @Test
    fun testErrorHandling() {
        UtcOffset.Format {
            isoOffset(
                zOnZero = true,
                useSeparator = true,
                outputMinute = WhenToOutput.ALWAYS,
                outputSecond = WhenToOutput.IF_NONZERO
            )
        }.apply {
            assertEquals(UtcOffset(hours = -4, minutes = -30), parse("-04:30"))
            assertCanNotParse("-04:60")
            assertCanNotParse("-04:XX")
        }
    }

    @Test
    fun testLenientIso8601() {
        val offsets = buildMap<UtcOffset, Pair<String, Set<String>>> {
            put(UtcOffset(-18, 0, 0), ("-18:00" to setOf("-18", "-1800", "-180000", "-18:00:00")))
            put(UtcOffset(-17, -59, -58), ("-17:59:58" to setOf()))
            put(UtcOffset(-4, -3, -2), ("-04:03:02" to setOf()))
            put(UtcOffset(0, 0, -1), ("-00:00:01" to setOf()))
            put(UtcOffset(0, -1, 0), ("-00:01" to setOf()))
            put(UtcOffset(0, -1, -1), ("-00:01:01" to setOf()))
            put(UtcOffset(-1, 0, 0), ("-01:00" to setOf()))
            put(UtcOffset(-1, 0, -1), ("-01:00:01" to setOf()))
            put(UtcOffset(-1, -1, 0), ("-01:01" to setOf()))
            put(UtcOffset(-1, -1, -1), ("-01:01:01" to setOf()))
            put(UtcOffset(0, 0, 0), ("Z" to setOf()))
            put(UtcOffset(0, 1, 0), ("+00:01" to setOf()))
            put(UtcOffset(0, 1, 1), ("+00:01:01" to setOf()))
            put(UtcOffset(1, 0, 0), ("+01:00" to setOf()))
            put(UtcOffset(1, 0, 1), ("+01:00:01" to setOf()))
            put(UtcOffset(1, 1, 0), ("+01:01" to setOf()))
            put(UtcOffset(1, 1, 1), ("+01:01:01" to setOf()))
            put(UtcOffset(4, 3, 2), ("+04:03:02" to setOf()))
            put(UtcOffset(17, 59, 58), ("+17:59:58" to setOf()))
            put(UtcOffset(18, 0, 0), ("+18:00" to setOf()))
        }
        val lenientFormat = UtcOffsetFormat.build {
            alternativeParsing({
                isoOffset(
                    zOnZero = false,
                    useSeparator = false,
                    outputMinute = WhenToOutput.IF_NONZERO,
                    outputSecond = WhenToOutput.IF_NONZERO
                )
            }) {
                isoOffset(
                    zOnZero = true,
                    useSeparator = true,
                    outputMinute = WhenToOutput.ALWAYS,
                    outputSecond = WhenToOutput.IF_NONZERO
                )
            }
        }
        test(offsets, lenientFormat)
    }

    @Test
    fun testIso() {
        val offsets = buildMap<UtcOffset, Pair<String, Set<String>>> {
            put(UtcOffset(-18, -0, -0), ("-18:00" to setOf()))
            put(UtcOffset(-17, -59, -58), ("-17:59:58" to setOf()))
            put(UtcOffset(-4, -3, -2), ("-04:03:02" to setOf()))
            put(UtcOffset(-0, -0, -1), ("-00:00:01" to setOf()))
            put(UtcOffset(-0, -1, -0), ("-00:01" to setOf()))
            put(UtcOffset(-0, -1, -1), ("-00:01:01" to setOf()))
            put(UtcOffset(-1, -0, -0), ("-01:00" to setOf()))
            put(UtcOffset(-1, -0, -1), ("-01:00:01" to setOf()))
            put(UtcOffset(-1, -1, -0), ("-01:01" to setOf()))
            put(UtcOffset(-1, -1, -1), ("-01:01:01" to setOf()))
            put(UtcOffset(+0, 0, 0), ("Z" to setOf("z")))
            put(UtcOffset(+0, 1, 0), ("+00:01" to setOf()))
            put(UtcOffset(+0, 1, 1), ("+00:01:01" to setOf()))
            put(UtcOffset(+1, 0, 0), ("+01:00" to setOf()))
            put(UtcOffset(+1, 0, 1), ("+01:00:01" to setOf()))
            put(UtcOffset(+1, 1, 0), ("+01:01" to setOf()))
            put(UtcOffset(+1, 1, 1), ("+01:01:01" to setOf()))
            put(UtcOffset(+4, 3, 2), ("+04:03:02" to setOf()))
            put(UtcOffset(+17, 59, 58), ("+17:59:58" to setOf()))
            put(UtcOffset(+18, 0, 0), ("+18:00" to setOf()))
        }
        test(offsets, UtcOffset.Formats.ISO)
    }

    @Test
    fun testBasicIso() {
        val offsets = buildMap<UtcOffset, Pair<String, Set<String>>> {
            put(UtcOffset(-18, -0, -0), ("-18" to setOf()))
            put(UtcOffset(-17, -59, -58), ("-175958" to setOf()))
            put(UtcOffset(-4, -3, -2), ("-040302" to setOf()))
            put(UtcOffset(-0, -0, -1), ("-000001" to setOf()))
            put(UtcOffset(-0, -1, -0), ("-0001" to setOf()))
            put(UtcOffset(-0, -1, -1), ("-000101" to setOf()))
            put(UtcOffset(-1, -0, -0), ("-01" to setOf()))
            put(UtcOffset(-1, -0, -1), ("-010001" to setOf()))
            put(UtcOffset(-1, -1, -0), ("-0101" to setOf()))
            put(UtcOffset(-1, -1, -1), ("-010101" to setOf()))
            put(UtcOffset(+0, 0, 0), ("Z" to setOf("z")))
            put(UtcOffset(+0, 1, 0), ("+0001" to setOf()))
            put(UtcOffset(+0, 1, 1), ("+000101" to setOf()))
            put(UtcOffset(+1, 0, 0), ("+01" to setOf()))
            put(UtcOffset(+1, 0, 1), ("+010001" to setOf()))
            put(UtcOffset(+1, 1, 0), ("+0101" to setOf()))
            put(UtcOffset(+1, 1, 1), ("+010101" to setOf()))
            put(UtcOffset(+4, 3, 2), ("+040302" to setOf()))
            put(UtcOffset(+17, 59, 58), ("+175958" to setOf()))
            put(UtcOffset(+18, 0, 0), ("+18" to setOf()))
        }
        test(offsets, UtcOffset.Formats.ISO_BASIC)
    }

    @Test
    fun testCompact() {
        val offsets = buildMap<UtcOffset, Pair<String, Set<String>>> {
            put(UtcOffset(-18, 0), ("-1800" to setOf()))
            put(UtcOffset(-17, -59), ("-1759" to setOf()))
            put(UtcOffset(-4, -3), ("-0403" to setOf()))
            put(UtcOffset(-0, -0), ("-0000" to setOf()))
            put(UtcOffset(-0, -1), ("-0001" to setOf()))
            put(UtcOffset(-1, -0), ("-0100" to setOf()))
            put(UtcOffset(-1, -1), ("-0101" to setOf()))
            put(UtcOffset(+0, 0), ("+0000" to setOf()))
            put(UtcOffset(+0, 1), ("+0001" to setOf()))
            put(UtcOffset(+1, 0), ("+0100" to setOf()))
            put(UtcOffset(+1, 1), ("+0101" to setOf()))
            put(UtcOffset(+4, 3), ("+0403" to setOf()))
            put(UtcOffset(+17, 59), ("+1759" to setOf()))
            put(UtcOffset(+18, 0), ("+1800" to setOf()))
        }
        test(offsets, UtcOffset.Formats.FOUR_DIGITS)
        // formatting that loses precision and can't be parsed back:
        for ((offset, string) in listOf(
            UtcOffset(-17, -59, -58) to "-1759",
            UtcOffset(-4, -3, -2) to "-0403",
            UtcOffset(-0, -0, -1) to "-0000",
            UtcOffset(-0, -1, -1) to "-0001",
            UtcOffset(-1, -0, -1) to "-0100",
            UtcOffset(-1, -1, -1) to "-0101",
            UtcOffset(+0, 1, 1) to "+0001",
            UtcOffset(+1, 0, 1) to "+0100",
            UtcOffset(+1, 1, 1) to "+0101",
            UtcOffset(+4, 3, 2) to "+0403",
            UtcOffset(+17, 59, 58) to "+1759",
        )) {
            assertEquals(string, UtcOffset.Formats.FOUR_DIGITS.format(offset))
        }
    }

    @Test
    fun testDoc() {
        val format = UtcOffset.Format {
          optional("GMT") {
            offsetHours(Padding.NONE)
            char(':')
            offsetMinutesOfHour()
            optional {
              char(':')
              offsetSecondsOfMinute()
            }
          }
        }
        assertEquals("GMT", UtcOffset.ZERO.format(format))
        assertEquals("+4:30:15", UtcOffset(4, 30, 15).format(format))
    }

    private fun test(strings: Map<UtcOffset, Pair<String, Set<String>>>, format: DateTimeFormat<UtcOffset>) {
        for ((offset, stringsForDate) in strings) {
            val (canonicalString, otherStrings) = stringsForDate
            assertEquals(canonicalString, format.format(offset), "formatting $offset with $format")
            assertEquals(offset, format.parse(canonicalString), "parsing '$canonicalString' with $format")
            for (otherString in otherStrings) {
                assertEquals(offset, format.parse(otherString), "parsing '$otherString' with $format")
            }
        }
    }
}
