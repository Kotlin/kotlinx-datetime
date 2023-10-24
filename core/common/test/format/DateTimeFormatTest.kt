/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlinx.datetime.format.migration.*
import kotlin.test.*

class DateTimeFormatTest {

    @Test
    fun testErrorHandling() {
        val format = LocalDateTime.Formats.ISO
        assertEquals(LocalDateTime(2023, 2, 28, 15, 36), format.parse("2023-02-28T15:36"))
        val error = assertFailsWith<DateTimeFormatException> { format.parse("2023-02-40T15:36") }
        assertContains(error.message!!, "40")
        assertFailsWith<DateTimeFormatException> { format.parse("2023-02-XXT15:36") }
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
            appendYear()
            appendUnicodeFormatString("-MM-dd HH:mm:ss")
        })
        test(dateTimes, LocalDateTime.Format {
            appendYear()
            char('-')
            appendMonthNumber()
            char('-')
            appendDayOfMonth()
            char(' ')
            appendHour()
            char(':')
            appendMinute()
            char(':')
            appendSecond()
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
            appendYear()
            appendUnicodeFormatString("-MM-dd HH:mm")
        })
        test(dateTimes, LocalDateTime.Format {
            appendYear()
            char('-')
            appendMonthNumber()
            char('-')
            appendDayOfMonth()
            char(' ')
            appendHour()
            char(':')
            appendMinute()
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
            appendYear()
            appendUnicodeFormatString("MMddHHmmss")
        })
        test(dateTimes, LocalDateTime.Format {
            appendYear()
            appendMonthNumber()
            appendDayOfMonth()
            appendHour()
            appendMinute()
            appendSecond()
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
            appendYear(Padding.NONE)
            appendUnicodeFormatString("-M-d H:m:s")
        })
        test(dateTimes, LocalDateTime.Format {
            appendYear(Padding.NONE)
            char('-')
            appendMonthNumber(Padding.NONE)
            char('-')
            appendDayOfMonth(Padding.NONE)
            char(' ')
            appendHour(Padding.NONE)
            char(':')
            appendMinute(Padding.NONE)
            char(':')
            appendSecond(Padding.NONE)
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
        val format = LocalDateTime.Format {
            appendYear(Padding.SPACE)
            char('-')
            appendMonthNumber(Padding.SPACE)
            char('-')
            appendDayOfMonth(Padding.SPACE)
            char(' ')
            appendHour(Padding.SPACE)
            char(':')
            appendMinute(Padding.SPACE)
            char(':')
            appendSecond(Padding.SPACE)
        }
        test(dateTimes, format)
        format.parse(" 008- 7- 5  0: 0: 0")
        assertFailsWith<DateTimeFormatException> { format.parse("  008- 7- 5  0: 0: 0") }
        assertFailsWith<DateTimeFormatException> { format.parse("  8- 7- 5  0: 0: 0") }
        assertFailsWith<DateTimeFormatException> { format.parse(" 008-  7- 5  0: 0: 0") }
        assertFailsWith<DateTimeFormatException> { format.parse(" 008-7- 5  0: 0: 0") }
        assertFailsWith<DateTimeFormatException> { format.parse("+008- 7- 5  0: 0: 0") }
        assertFailsWith<DateTimeFormatException> { format.parse("  -08- 7- 5  0: 0: 0") }
        assertFailsWith<DateTimeFormatException> { format.parse("   -08- 7- 5  0: 0: 0") }
        assertFailsWith<DateTimeFormatException> {
            format.parse("-8- 7- 5  0: 0: 0")
        }
    }

    @Test
    fun testIso() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("2008-07-05T00:00" to setOf("2008-07-05T00:00:00")))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("2007-12-31T01:00" to setOf("2007-12-31t01:00")))
            put(LocalDateTime(999, 11, 30, 23, 0, 0, 0), ("0999-11-30T23:00" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("-0001-01-02T00:01" to setOf()))
            put(LocalDateTime(9999, 10, 31, 12, 30, 0, 0), ("9999-10-31T12:30" to setOf()))
            put(LocalDateTime(-9999, 9, 30, 23, 59, 0, 0), ("-9999-09-30T23:59" to setOf()))
            put(LocalDateTime(10000, 8, 1, 0, 0, 1, 0), ("+10000-08-01T00:00:01" to setOf()))
            put(LocalDateTime(-10000, 7, 1, 0, 0, 59, 0), ("-10000-07-01T00:00:59" to setOf()))
            put(LocalDateTime(123456, 6, 1, 13, 44, 0, 0), ("+123456-06-01T13:44" to setOf()))
            put(LocalDateTime(-123456, 5, 1, 13, 44, 0, 0), ("-123456-05-01T13:44" to setOf()))
            put(LocalDateTime(123456, 6, 1, 0, 0, 0, 100000000), ("+123456-06-01T00:00:00.100" to setOf("+123456-06-01T00:00:00.10", "+123456-06-01T00:00:00.1")))
            put(LocalDateTime(-123456, 5, 1, 0, 0, 0, 10000000), ("-123456-05-01T00:00:00.010" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 1000000), ("2022-01-02T00:00:00.001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 100000), ("2022-01-02T00:00:00.000100" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 10000), ("2022-01-02T00:00:00.000010" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 1000), ("2022-01-02T00:00:00.000001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 100), ("2022-01-02T00:00:00.000000100" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 10), ("2022-01-02T00:00:00.000000010" to setOf()))
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
    fun testBasicIso() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("20080705T0000" to setOf("20080705T000000")))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("20071231T0100" to setOf("20071231t0100")))
            put(LocalDateTime(999, 11, 30, 23, 0, 0, 0), ("09991130T2300" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("-00010102T0001" to setOf()))
            put(LocalDateTime(9999, 10, 31, 12, 30, 0, 0), ("99991031T1230" to setOf()))
            put(LocalDateTime(-9999, 9, 30, 23, 59, 0, 0), ("-99990930T2359" to setOf()))
            put(LocalDateTime(10000, 8, 1, 0, 0, 1, 0), ("+100000801T000001" to setOf()))
            put(LocalDateTime(-10000, 7, 1, 0, 0, 59, 0), ("-100000701T000059" to setOf()))
            put(LocalDateTime(123456, 6, 1, 13, 44, 0, 0), ("+1234560601T1344" to setOf()))
            put(LocalDateTime(-123456, 5, 1, 13, 44, 0, 0), ("-1234560501T1344" to setOf()))
            put(LocalDateTime(123456, 6, 1, 0, 0, 0, 100000000), ("+1234560601T000000.100" to setOf("+1234560601T000000.10", "+1234560601T000000.1")))
            put(LocalDateTime(-123456, 5, 1, 0, 0, 0, 10000000), ("-1234560501T000000.010" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 1000000), ("20220102T000000.001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 100000), ("20220102T000000.000100" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 10000), ("20220102T000000.000010" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 1000), ("20220102T000000.000001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 100), ("20220102T000000.000000100" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 10), ("20220102T000000.000000010" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 1), ("20220102T000000.000000001" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 999999999), ("20220102T000000.999999999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 99999999), ("20220102T000000.099999999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 9999999), ("20220102T000000.009999999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 999999), ("20220102T000000.000999999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 99999), ("20220102T000000.000099999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 9999), ("20220102T000000.000009999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 999), ("20220102T000000.000000999" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 99), ("20220102T000000.000000099" to setOf()))
            put(LocalDateTime(2022, 1, 2, 0, 0, 0, 9), ("20220102T000000.000000009" to setOf()))
        }
        test(dateTimes, LocalDateTime.Formats.ISO_BASIC)
    }

    @Test
    fun testDoc() {
        val dateTime = LocalDateTime(2020, 8, 30, 18, 43, 13, 0)
        val format1 = LocalDateTime.Format { appendDate(LocalDate.Formats.ISO); char(' ');  appendTime(LocalTime.Formats.ISO) }
        assertEquals("2020-08-30 18:43:13", dateTime.format(format1))
        val format2 = LocalDateTime.Format {
          appendMonthNumber(); char('/'); appendDayOfMonth()
          char(' ')
          appendHour(); char(':'); appendMinute()
          appendOptional { char(':'); appendSecond() }
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
