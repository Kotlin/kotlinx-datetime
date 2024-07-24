/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(FormatStringsInDatetimeFormats::class)

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class LocalDateFormatTest {

    @Test
    fun testErrorHandling() {
        LocalDate.Formats.ISO.apply {
            assertEquals(LocalDate(2023, 2, 28), parse("2023-02-28"))
            assertCanNotParse("2023-02-40")
            assertCanNotParse("2023-02-XX")
        }
    }

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
                put(LocalDate(10000, 1, 1), ("+10000${s}01${s}01" to setOf()))
                put(LocalDate(-10000, 1, 1), ("-10000${s}01${s}01" to setOf()))
                put(LocalDate(123456, 1, 1), ("+123456${s}01${s}01" to setOf()))
                put(LocalDate(-123456, 1, 1), ("-123456${s}01${s}01" to setOf()))
            }
            test(dates, LocalDate.Format {
                year()
                byUnicodePattern("${s}MM${s}dd")
            })
            test(dates, LocalDate.Format {
                year()
                char(s)
                monthNumber()
                char(s)
                day()
            })
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
                put(LocalDate(10000, 1, 1), ("01${s}01${s}+10000" to setOf()))
                put(LocalDate(-10000, 1, 1), ("01${s}01${s}-10000" to setOf()))
                put(LocalDate(123456, 1, 1), ("01${s}01${s}+123456" to setOf()))
                put(LocalDate(-123456, 1, 1), ("01${s}01${s}-123456" to setOf()))
            }
            test(dates, LocalDate.Format {
                byUnicodePattern("dd${s}MM${s}")
                year()
            })
            test(dates, LocalDate.Format {
                day()
                char(s)
                monthNumber()
                char(s)
                year()
            })
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
            put(LocalDate(10000, 1, 1), ("+100000101" to setOf()))
            put(LocalDate(-10000, 1, 1), ("-100000101" to setOf()))
            put(LocalDate(123456, 1, 1), ("+1234560101" to setOf()))
            put(LocalDate(-123456, 1, 1), ("-1234560101" to setOf()))
        }
        test(dates, LocalDate.Format {
            year()
            byUnicodePattern("MMdd")
        })
        test(dates, LocalDate.Format {
            year()
            monthNumber()
            day()
        })
    }

    @Test
    fun testDayMonthNameYear() {
        val dates = buildMap<LocalDate, Pair<String, Set<String>>> {
            put(LocalDate(2008, 7, 5), ("05 July 2008" to setOf()))
            put(LocalDate(2007, 12, 31), ("31 December 2007" to setOf()))
            put(LocalDate(999, 11, 30), ("30 November 0999" to setOf()))
            put(LocalDate(-1, 1, 2), ("02 January -0001" to setOf()))
            put(LocalDate(9999, 10, 31), ("31 October 9999" to setOf()))
            put(LocalDate(-9999, 9, 30), ("30 September -9999" to setOf()))
            put(LocalDate(10000, 8, 1), ("01 August +10000" to setOf()))
            put(LocalDate(-10000, 7, 1), ("01 July -10000" to setOf()))
            put(LocalDate(123456, 6, 1), ("01 June +123456" to setOf()))
            put(LocalDate(-123456, 5, 1), ("01 May -123456" to setOf()))
        }
        val format = LocalDate.Format {
            day()
            char(' ')
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            year()
        }
        test(dates, format)
    }

    @Test
    fun testRomanNumerals() {
        val dates = buildMap<LocalDate, Pair<String, Set<String>>> {
            put(LocalDate(2008, 7, 5), ("05 VII 2008" to setOf()))
            put(LocalDate(2007, 12, 31), ("31 XII 2007" to setOf()))
            put(LocalDate(999, 11, 30), ("30 XI 999" to setOf()))
            put(LocalDate(-1, 1, 2), ("02 I -1" to setOf()))
            put(LocalDate(9999, 10, 31), ("31 X 9999" to setOf()))
            put(LocalDate(-9999, 9, 30), ("30 IX -9999" to setOf()))
            put(LocalDate(10000, 8, 1), ("01 VIII +10000" to setOf()))
            put(LocalDate(-10000, 7, 1), ("01 VII -10000" to setOf()))
            put(LocalDate(123456, 6, 1), ("01 VI +123456" to setOf()))
            put(LocalDate(-123456, 5, 1), ("01 V -123456" to setOf()))
        }
        val format = LocalDate.Format {
            day()
            char(' ')
            monthName(MonthNames("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"))
            char(' ')
            year(Padding.NONE)
        }
        test(dates, format)
    }

    @Test
    fun testReducedYear() {
        val dates = buildMap<LocalDate, Pair<String, Set<String>>> {
            put(LocalDate(1960, 2, 3), ("600203" to setOf()))
            put(LocalDate(1961, 2, 3), ("610203" to setOf()))
            put(LocalDate(1959, 2, 3), ("+19590203" to setOf()))
            put(LocalDate(2059, 2, 3), ("590203" to setOf()))
            put(LocalDate(2060, 2, 3), ("+20600203" to setOf()))
            put(LocalDate(1, 2, 3), ("+10203" to setOf()))
            put(LocalDate(-1, 2, 3), ("-10203" to setOf()))
            put(LocalDate(-2003, 2, 3), ("-20030203" to setOf()))
            put(LocalDate(-12003, 2, 3), ("-120030203" to setOf()))
            put(LocalDate(12003, 2, 3), ("+120030203" to setOf()))
        }
        val format = LocalDate.Format {
            yearTwoDigits(baseYear = 1960)
            monthNumber()
            day()
        }
        test(dates, format)
    }

    @Test
    fun testIso() {
        val dates = buildMap<LocalDate, Pair<String, Set<String>>> {
            put(LocalDate(2008, 7, 5), ("2008-07-05" to setOf()))
            put(LocalDate(2007, 12, 31), ("2007-12-31" to setOf()))
            put(LocalDate(999, 11, 30), ("0999-11-30" to setOf()))
            put(LocalDate(-1, 1, 2), ("-0001-01-02" to setOf()))
            put(LocalDate(9999, 10, 31), ("9999-10-31" to setOf()))
            put(LocalDate(-9999, 9, 30), ("-9999-09-30" to setOf()))
            put(LocalDate(10000, 8, 1), ("+10000-08-01" to setOf()))
            put(LocalDate(-10000, 7, 1), ("-10000-07-01" to setOf()))
            put(LocalDate(123456, 6, 1), ("+123456-06-01" to setOf()))
            put(LocalDate(-123456, 5, 1), ("-123456-05-01" to setOf()))
        }
        test(dates, LocalDate.Formats.ISO)
    }

    @Test
    fun testBasicIso() {
        val dates = buildMap<LocalDate, Pair<String, Set<String>>> {
            put(LocalDate(2008, 7, 5), ("20080705" to setOf()))
            put(LocalDate(2007, 12, 31), ("20071231" to setOf()))
            put(LocalDate(999, 11, 30), ("09991130" to setOf()))
            put(LocalDate(-1, 1, 2), ("-00010102" to setOf()))
            put(LocalDate(9999, 10, 31), ("99991031" to setOf()))
            put(LocalDate(-9999, 9, 30), ("-99990930" to setOf()))
            put(LocalDate(10000, 8, 1), ("+100000801" to setOf()))
            put(LocalDate(-10000, 7, 1), ("-100000701" to setOf()))
            put(LocalDate(123456, 6, 1), ("+1234560601" to setOf()))
            put(LocalDate(-123456, 5, 1), ("-1234560501" to setOf()))
        }
        test(dates, LocalDate.Formats.ISO_BASIC)
    }

    @Test
    fun testDoc() {
        val format = LocalDate.Format {
          year()
          char(' ')
          monthName(MonthNames.ENGLISH_ABBREVIATED)
          char(' ')
          day()
        }
        assertEquals("2020 Jan 05", format.format(LocalDate(2020, 1, 5)))
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
    fun testEmptyDayOfWeekNames() {
        val names = DayOfWeekNames.ENGLISH_FULL.names
        for (i in 0 until 7) {
            val newNames = (0 until 7).map { if (it == i) "" else names[it] }
            assertFailsWith<IllegalArgumentException> { DayOfWeekNames(newNames) }
        }
    }

    @Test
    fun testIdenticalMonthNames() {
        assertFailsWith<IllegalArgumentException> {
            MonthNames("Jan", "Jan", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        }
    }

    @Test
    fun testIdenticalDayOfWeekNames() {
        assertFailsWith<IllegalArgumentException> {
            DayOfWeekNames("Mon", "Tue", "Tue", "Thu", "Fri", "Sat", "Sun")
        }
    }

    private fun test(strings: Map<LocalDate, Pair<String, Set<String>>>, format: DateTimeFormat<LocalDate>) {
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
