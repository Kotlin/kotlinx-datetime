/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateFormatTest {
    @Test
    fun testBigEndianDates() {
        for (s in listOf('/', '-')) {
            val dates = buildMap<LocalDate, Pair<String, Set<String>>> {
                put(LocalDate(2008, 7, 5), ("2008${s}07${s}05" to setOf()))
                put(LocalDate(2007, 12, 31), ("2007${s}12${s}31" to setOf()))
                put(LocalDate(999, 12, 31), ("0999${s}12${s}31" to setOf()))
                put(LocalDate(-1, 1, 2), ("-0001${s}01${s}02" to setOf()))
                put(LocalDate(9999, 12, 31), ("9999${s}12${s}31" to setOf()))
                put(LocalDate(-9999, 12, 31), ("-9999${s}12${s}31" to setOf()))
                put(LocalDate(10000, 1, 1), ("10000${s}01${s}01" to setOf()))
                put(LocalDate(-10000, 1, 1), ("-10000${s}01${s}01" to setOf()))
                put(LocalDate(123456, 1, 1), ("123456${s}01${s}01" to setOf()))
                put(LocalDate(-123456, 1, 1), ("-123456${s}01${s}01" to setOf()))
            }
            test(LocalDateFormat.fromFormatString("yyyy'$s'mm'$s'dd"), dates)
            test(LocalDateFormat.build {
                appendYear(4)
                appendLiteral(s)
                appendMonthNumber(2)
                appendLiteral(s)
                appendDayOfMonth(2)
            }, dates)
        }
    }

    @Test
    fun testSmallEndianDates() {
        for (s in listOf('/', '-')) {
            val dates = buildMap<LocalDate, Pair<String, Set<String>>> {
                put(LocalDate(2008, 7, 5), ("05${s}07${s}2008" to setOf()))
                put(LocalDate(2007, 12, 31), ("31${s}12${s}2007" to setOf()))
                put(LocalDate(999, 12, 31), ("31${s}12${s}0999" to setOf()))
                put(LocalDate(-1, 1, 2), ("02${s}01${s}-0001" to setOf()))
                put(LocalDate(9999, 12, 31), ("31${s}12${s}9999" to setOf()))
                put(LocalDate(-9999, 12, 31), ("31${s}12${s}-9999" to setOf()))
                put(LocalDate(10000, 1, 1), ("01${s}01${s}10000" to setOf()))
                put(LocalDate(-10000, 1, 1), ("01${s}01${s}-10000" to setOf()))
                put(LocalDate(123456, 1, 1), ("01${s}01${s}123456" to setOf()))
                put(LocalDate(-123456, 1, 1), ("01${s}01${s}-123456" to setOf()))
            }
            test(LocalDateFormat.fromFormatString("dd'$s'mm'$s'yyyy"), dates)
            test(LocalDateFormat.build {
                appendDayOfMonth(2)
                appendLiteral(s)
                appendMonthNumber(2)
                appendLiteral(s)
                appendYear(4)
            }, dates)
        }
    }

    @Test
    fun testSingleNumberDates() {
        val dates = buildMap<LocalDate, Pair<String, Set<String>>> {
            put(LocalDate(2008, 7, 5), ("20080705" to setOf()))
            put(LocalDate(2007, 12, 31), ("20071231" to setOf()))
            put(LocalDate(999, 12, 31), ("09991231" to setOf()))
            put(LocalDate(-1, 1, 2), ("-00010102" to setOf()))
            put(LocalDate(9999, 12, 31), ("99991231" to setOf()))
            put(LocalDate(-9999, 12, 31), ("-99991231" to setOf()))
            put(LocalDate(10000, 1, 1), ("100000101" to setOf()))
            put(LocalDate(-10000, 1, 1), ("-100000101" to setOf()))
            put(LocalDate(123456, 1, 1), ("1234560101" to setOf()))
            put(LocalDate(-123456, 1, 1), ("-1234560101" to setOf()))
        }
        test(LocalDateFormat.fromFormatString("yyyymmdd"), dates)
        test(LocalDateFormat.build {
            appendYear(4)
            appendMonthNumber(2)
            appendDayOfMonth(2)
        }, dates)
    }

    private fun test(format: LocalDateFormat, strings: Map<LocalDate, Pair<String, Set<String>>>) {
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
