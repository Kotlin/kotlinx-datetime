/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(FormatStringsInDatetimeFormats::class)

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class LocalDateTimeFormatTest {

    @Test
    fun testErrorHandling() {
        LocalDateTime.Formats.ISO.apply {
            assertEquals(LocalDateTime(2023, 2, 28, 15, 36), parse("2023-02-28T15:36"))
            assertCanNotParse("2023-02-40T15:36")
            assertCanNotParse("2023-02-XXT15:36")
        }
    }

    @Test
    fun testPythonDateTime() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("2008-07-05 00:00:00" to setOf()))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("2007-12-31 01:00:00" to setOf()))
            put(LocalDateTime(999, 12, 31, 23, 0, 0, 0), ("0999-12-31 23:00:00" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("-0001-01-02 00:01:00" to setOf()))
            put(LocalDateTime(9999, 12, 31, 12, 30, 0, 0), ("9999-12-31 12:30:00" to setOf()))
            put(LocalDateTime(-9999, 12, 31, 23, 59, 0, 0), ("-9999-12-31 23:59:00" to setOf()))
            put(LocalDateTime(10000, 1, 1, 0, 0, 1, 0), ("+10000-01-01 00:00:01" to setOf()))
            put(LocalDateTime(-10000, 1, 1, 0, 0, 59, 0), ("-10000-01-01 00:00:59" to setOf()))
            put(LocalDateTime(123456, 1, 1, 13, 44, 0, 0), ("+123456-01-01 13:44:00" to setOf()))
            put(LocalDateTime(-123456, 1, 1, 13, 44, 0, 0), ("-123456-01-01 13:44:00" to setOf()))
        }
        test(dateTimes, LocalDateTime.Format {
            year()
            byUnicodePattern("-MM-dd HH:mm:ss")
        })
        test(dateTimes, LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        })
    }

    @Test
    fun testPythonDateTimeWithoutSeconds() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("2008-07-05 00:00" to setOf()))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("2007-12-31 01:00" to setOf()))
            put(LocalDateTime(999, 12, 31, 23, 0, 0, 0), ("0999-12-31 23:00" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("-0001-01-02 00:01" to setOf()))
            put(LocalDateTime(9999, 12, 31, 12, 30, 0, 0), ("9999-12-31 12:30" to setOf()))
            put(LocalDateTime(-9999, 12, 31, 23, 59, 0, 0), ("-9999-12-31 23:59" to setOf()))
            put(LocalDateTime(10000, 1, 1, 0, 0, 0, 0), ("+10000-01-01 00:00" to setOf()))
            put(LocalDateTime(-10000, 1, 1, 0, 0, 0, 0), ("-10000-01-01 00:00" to setOf()))
            put(LocalDateTime(123456, 1, 1, 13, 44, 0, 0), ("+123456-01-01 13:44" to setOf()))
            put(LocalDateTime(-123456, 1, 1, 13, 44, 0, 0), ("-123456-01-01 13:44" to setOf()))
        }
        test(dateTimes, LocalDateTime.Format {
            year()
            byUnicodePattern("-MM-dd HH:mm")
        })
        test(dateTimes, LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char(' ')
            hour()
            char(':')
            minute()
        })
    }

    @Test
    fun testSingleNumberDateTimes() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("20080705000000" to setOf()))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("20071231010000" to setOf()))
            put(LocalDateTime(999, 12, 31, 23, 0, 0, 0), ("09991231230000" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("-00010102000100" to setOf()))
            put(LocalDateTime(9999, 12, 31, 12, 30, 0, 0), ("99991231123000" to setOf()))
            put(LocalDateTime(-9999, 12, 31, 23, 59, 0, 0), ("-99991231235900" to setOf()))
            put(LocalDateTime(10000, 1, 1, 0, 0, 1, 0), ("+100000101000001" to setOf()))
            put(LocalDateTime(-10000, 1, 1, 0, 0, 59, 0), ("-100000101000059" to setOf()))
            put(LocalDateTime(123456, 1, 1, 13, 44, 0, 0), ("+1234560101134400" to setOf()))
            put(LocalDateTime(-123456, 1, 1, 13, 44, 0, 0), ("-1234560101134400" to setOf()))
        }
        test(dateTimes, LocalDateTime.Format {
            year()
            byUnicodePattern("MMddHHmmss")
        })
        test(dateTimes, LocalDateTime.Format {
            year()
            monthNumber()
            day()
            hour()
            minute()
            second()
        })
    }

    @Test
    fun testNoPadding() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("2008-7-5 0:0:0" to setOf()))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("2007-12-31 1:0:0" to setOf()))
            put(LocalDateTime(999, 12, 31, 23, 0, 0, 0), ("999-12-31 23:0:0" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("-1-1-2 0:1:0" to setOf()))
            put(LocalDateTime(9999, 12, 31, 12, 30, 0, 0), ("9999-12-31 12:30:0" to setOf()))
            put(LocalDateTime(-9999, 12, 31, 23, 59, 0, 0), ("-9999-12-31 23:59:0" to setOf()))
            put(LocalDateTime(10000, 1, 1, 0, 0, 1, 0), ("+10000-1-1 0:0:1" to setOf()))
            put(LocalDateTime(-10000, 1, 1, 0, 0, 59, 0), ("-10000-1-1 0:0:59" to setOf()))
            put(LocalDateTime(123456, 1, 1, 13, 44, 0, 0), ("+123456-1-1 13:44:0" to setOf()))
            put(LocalDateTime(-123456, 1, 1, 13, 44, 0, 0), ("-123456-1-1 13:44:0" to setOf()))
        }
        test(dateTimes, LocalDateTime.Format {
            year(Padding.NONE)
            byUnicodePattern("-M-d H:m:s")
        })
        test(dateTimes, LocalDateTime.Format {
            year(Padding.NONE)
            char('-')
            monthNumber(Padding.NONE)
            char('-')
            day(Padding.NONE)
            char(' ')
            hour(Padding.NONE)
            char(':')
            minute(Padding.NONE)
            char(':')
            second(Padding.NONE)
        })
    }


    @Test
    fun testSpacePadding() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("2008- 7- 5  0: 0: 0" to setOf()))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("2007-12-31  1: 0: 0" to setOf()))
            put(LocalDateTime(999, 12, 31, 23, 0, 0, 0), (" 999-12-31 23: 0: 0" to setOf()))
            put(LocalDateTime(1, 1, 2, 0, 1, 0, 0), ("   1- 1- 2  0: 1: 0" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("  -1- 1- 2  0: 1: 0" to setOf()))
            put(LocalDateTime(9999, 12, 31, 12, 30, 0, 0), ("9999-12-31 12:30: 0" to setOf()))
            put(LocalDateTime(-9999, 12, 31, 23, 59, 0, 0), ("-9999-12-31 23:59: 0" to setOf()))
            put(LocalDateTime(10000, 1, 1, 0, 0, 1, 0), ("+10000- 1- 1  0: 0: 1" to setOf()))
            put(LocalDateTime(-10000, 1, 1, 0, 0, 59, 0), ("-10000- 1- 1  0: 0:59" to setOf()))
            put(LocalDateTime(123456, 1, 1, 13, 44, 0, 0), ("+123456- 1- 1 13:44: 0" to setOf()))
            put(LocalDateTime(-123456, 1, 1, 13, 44, 0, 0), ("-123456- 1- 1 13:44: 0" to setOf()))
        }
        LocalDateTime.Format {
            year(Padding.SPACE)
            char('-')
            monthNumber(Padding.SPACE)
            char('-')
            day(Padding.SPACE)
            char(' ')
            hour(Padding.SPACE)
            char(':')
            minute(Padding.SPACE)
            char(':')
            second(Padding.SPACE)
        }.apply {
            test(dateTimes, this)
            parse(" 008- 7- 5  0: 0: 0")
            assertCanNotParse("  008- 7- 5  0: 0: 0")
            assertCanNotParse("  8- 7- 5  0: 0: 0")
            assertCanNotParse(" 008-  7- 5  0: 0: 0")
            assertCanNotParse(" 008-7- 5  0: 0: 0")
            assertCanNotParse("+008- 7- 5  0: 0: 0")
            assertCanNotParse("  -08- 7- 5  0: 0: 0")
            assertCanNotParse("   -08- 7- 5  0: 0: 0")
            assertCanNotParse("-8- 7- 5  0: 0: 0")
        }
    }

    @Test
    fun testIso() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("2008-07-05T00:00:00" to setOf("2008-07-05T00:00")))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("2007-12-31T01:00:00" to setOf("2007-12-31t01:00")))
            put(LocalDateTime(999, 11, 30, 23, 0, 0, 0), ("0999-11-30T23:00:00" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("-0001-01-02T00:01:00" to setOf()))
            put(LocalDateTime(9999, 10, 31, 12, 30, 0, 0), ("9999-10-31T12:30:00" to setOf()))
            put(LocalDateTime(-9999, 9, 30, 23, 59, 0, 0), ("-9999-09-30T23:59:00" to setOf()))
            put(LocalDateTime(10000, 8, 1, 0, 0, 1, 0), ("+10000-08-01T00:00:01" to setOf()))
            put(LocalDateTime(-10000, 7, 1, 0, 0, 59, 0), ("-10000-07-01T00:00:59" to setOf()))
            put(LocalDateTime(123456, 6, 1, 13, 44, 0, 0), ("+123456-06-01T13:44:00" to setOf()))
            put(LocalDateTime(-123456, 5, 1, 13, 44, 0, 0), ("-123456-05-01T13:44:00" to setOf()))
            put(LocalDateTime(123456, 6, 1, 0, 0, 0, 100000000), ("+123456-06-01T00:00:00.1" to setOf("+123456-06-01T00:00:00.10", "+123456-06-01T00:00:00.100")))
            put(LocalDateTime(-123456, 5, 1, 0, 0, 0, 10000000), ("-123456-05-01T00:00:00.01" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 1000000), ("2022-01-02T00:00:00.001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 100000), ("2022-01-02T00:00:00.0001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 10000), ("2022-01-02T00:00:00.00001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 1000), ("2022-01-02T00:00:00.000001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 100), ("2022-01-02T00:00:00.0000001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 10), ("2022-01-02T00:00:00.00000001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 1), ("2022-01-02T00:00:00.000000001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 999999999), ("2022-01-02T00:00:00.999999999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 99999999), ("2022-01-02T00:00:00.099999999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 9999999), ("2022-01-02T00:00:00.009999999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 999999), ("2022-01-02T00:00:00.000999999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 99999), ("2022-01-02T00:00:00.000099999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 9999), ("2022-01-02T00:00:00.000009999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 999), ("2022-01-02T00:00:00.000000999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 99), ("2022-01-02T00:00:00.000000099" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 9), ("2022-01-02T00:00:00.000000009" to setOf()))
        }
        test(dateTimes, LocalDateTime.Formats.ISO)
    }

    @Test
    fun testDoc() {
        val dateTime = LocalDateTime(2020, 8, 30, 18, 43, 13, 0)
        val format1 = LocalDateTime.Format { date(LocalDate.Formats.ISO); char(' ');  time(LocalTime.Formats.ISO) }
        assertEquals("2020-08-30 18:43:13", dateTime.format(format1))
        val format2 = LocalDateTime.Format {
          monthNumber(); char('/'); day()
          char(' ')
          hour(); char(':'); minute()
          optional { char(':'); second() }
        }
        assertEquals("08/30 18:43:13", dateTime.format(format2))
    }

    private fun test(strings: Map<LocalDateTime, Pair<String, Set<String>>>, format: DateTimeFormat<LocalDateTime>) {
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
