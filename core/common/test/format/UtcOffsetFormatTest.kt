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
            appendAlternatives({
                appendIsoOffset(zOnZero = true, useSeparator = true, outputMinute = WhenToOutput.ALWAYS, outputSecond = WhenToOutput.IF_NONZERO)
            }, {
                appendIsoOffset(zOnZero = false, useSeparator = false, outputMinute = WhenToOutput.IF_NONZERO, outputSecond = WhenToOutput.IF_NONZERO)
            })
        }
        test(lenientFormat, offsets)
    }

    private fun test(format: UtcOffsetFormat, strings: Map<UtcOffset, Pair<String, Set<String>>>) {
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
