/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class YearMonthFormatTest {
    @Test
    fun testIso() {
        val yearMonths = buildMap<YearMonth, Pair<String, Set<String>>> {
            put(YearMonth(2008, 7), ("2008-07" to setOf()))
            put(YearMonth(2007, 12), ("2007-12" to setOf()))
            put(YearMonth(999, 11), ("0999-11" to setOf()))
            put(YearMonth(-1, 1), ("-0001-01" to setOf()))
            put(YearMonth(9999, 10), ("9999-10" to setOf()))
            put(YearMonth(-9999, 9), ("-9999-09" to setOf()))
            put(YearMonth(10000, 8), ("+10000-08" to setOf()))
            put(YearMonth(-10000, 7), ("-10000-07" to setOf()))
            put(YearMonth(123456, 6), ("+123456-06" to setOf()))
            put(YearMonth(-123456, 5), ("-123456-05" to setOf()))
        }
        test(yearMonths, YearMonth.Formats.ISO)
        @OptIn(FormatStringsInDatetimeFormats::class)
        test(yearMonths, YearMonth.Format { byUnicodePattern("yyyy-MM") })
    }

    @Test
    fun testIsoWithoutSeparators() {
        val yearMonths = buildMap<YearMonth, Pair<String, Set<String>>> {
            put(YearMonth(2008, 7), ("200807" to setOf()))
            put(YearMonth(2007, 12), ("200712" to setOf()))
            put(YearMonth(999, 11), ("099911" to setOf()))
            put(YearMonth(-1, 1), ("-000101" to setOf()))
            put(YearMonth(9999, 10), ("999910" to setOf()))
            put(YearMonth(-9999, 9), ("-999909" to setOf()))
            put(YearMonth(10000, 8), ("+1000008" to setOf()))
            put(YearMonth(-10000, 7), ("-1000007" to setOf()))
            put(YearMonth(123456, 6), ("+12345606" to setOf()))
            put(YearMonth(-123456, 5), ("-12345605" to setOf()))
        }
        test(yearMonths, YearMonth.Format { year(); monthNumber() })
        @OptIn(FormatStringsInDatetimeFormats::class)
        test(yearMonths, YearMonth.Format { byUnicodePattern("yyyyMM") })
    }

    @Test
    fun testErrorHandling() {
        YearMonth.Formats.ISO.apply {
            assertEquals(YearMonth(2023, 2), parse("2023-02"))
            assertCanNotParse("2023-XX")
            assertCanNotParse("2023-40")
        }
    }

    @Test
    fun testEmptyMonthNames() {
        val names = MonthNames.ENGLISH_FULL.names
        for (i in 0 until 12) {
            val newNames = (0 until 12).map { if (it == i) "" else names[it] }
            assertFailsWith<IllegalArgumentException> { MonthNames(newNames) }
        }
    }

    @Test
    fun testIdenticalMonthNames() {
        assertFailsWith<IllegalArgumentException> {
            MonthNames("Jan", "Jan", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        }
    }

    private fun test(strings: Map<YearMonth, Pair<String, Set<String>>>, format: DateTimeFormat<YearMonth>) {
        for ((yearMonth, stringsForYearMonth) in strings) {
            val (canonicalString, otherStrings) = stringsForYearMonth
            assertEquals(canonicalString, format.format(yearMonth), "formatting $yearMonth with $format")
            assertEquals(yearMonth, format.parse(canonicalString), "parsing '$canonicalString' with $format")
            for (otherString in otherStrings) {
                assertEquals(yearMonth, format.parse(otherString), "parsing '$otherString' with $format")
            }
        }
    }
}
