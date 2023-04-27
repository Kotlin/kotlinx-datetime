/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateTimeFormatTest {
    @Test
    fun testPythonDateTime() {
        val dateTimes = buildMap<LocalDateTime, Pair<String, Set<String>>> {
            put(LocalDateTime(2008, 7, 5, 0, 0, 0, 0), ("2008-07-05 00:00:00" to setOf()))
            put(LocalDateTime(2007, 12, 31, 1, 0, 0, 0), ("2007-12-31 01:00:00" to setOf()))
            put(LocalDateTime(999, 12, 31, 23, 0, 0, 0), ("0999-12-31 23:00:00" to setOf()))
            put(LocalDateTime(-1, 1, 2, 0, 1, 0, 0), ("-0001-01-02 00:01:00" to setOf()))
            put(LocalDateTime(9999, 12, 31, 12, 30, 0, 0), ("9999-12-31 12:30:00" to setOf()))
            put(LocalDateTime(-9999, 12, 31, 23, 59, 0, 0), ("-9999-12-31 23:59:00" to setOf()))
            put(LocalDateTime(10000, 1, 1, 0, 0, 1, 0), ("10000-01-01 00:00:01" to setOf()))
            put(LocalDateTime(-10000, 1, 1, 0, 0, 59, 0), ("-10000-01-01 00:00:59" to setOf()))
            put(LocalDateTime(123456, 1, 1, 13, 44, 0, 0), ("123456-01-01 13:44:00" to setOf()))
            put(LocalDateTime(-123456, 1, 1, 13, 44, 0, 0), ("-123456-01-01 13:44:00" to setOf()))
        }
        test(LocalDateTimeFormat.fromFormatString("ld<yyyy-mm-dd> lt<hh:mm:ss>"), dateTimes)
        test(LocalDateTimeFormat.build {
            appendYear(4)
            appendLiteral('-')
            appendMonthNumber(2)
            appendLiteral('-')
            appendDayOfMonth(2)
            appendLiteral(' ')
            appendHour(2)
            appendLiteral(':')
            appendMinute(2)
            appendLiteral(':')
            appendSecond(2)
        }, dateTimes)
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
            put(LocalDateTime(10000, 1, 1, 0, 0, 0, 0), ("10000-01-01 00:00" to setOf()))
            put(LocalDateTime(-10000, 1, 1, 0, 0, 0, 0), ("-10000-01-01 00:00" to setOf()))
            put(LocalDateTime(123456, 1, 1, 13, 44, 0, 0), ("123456-01-01 13:44" to setOf()))
            put(LocalDateTime(-123456, 1, 1, 13, 44, 0, 0), ("-123456-01-01 13:44" to setOf()))
        }
        test(LocalDateTimeFormat.fromFormatString("ld<yyyy-mm-dd> lt<hh:mm>"), dateTimes)
        test(LocalDateTimeFormat.build {
            appendYear(4)
            appendLiteral('-')
            appendMonthNumber(2)
            appendLiteral('-')
            appendDayOfMonth(2)
            appendLiteral(' ')
            appendHour(2)
            appendLiteral(':')
            appendMinute(2)
        }, dateTimes)
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
            put(LocalDateTime(10000, 1, 1, 0, 0, 1, 0), ("100000101000001" to setOf()))
            put(LocalDateTime(-10000, 1, 1, 0, 0, 59, 0), ("-100000101000059" to setOf()))
            put(LocalDateTime(123456, 1, 1, 13, 44, 0, 0), ("1234560101134400" to setOf()))
            put(LocalDateTime(-123456, 1, 1, 13, 44, 0, 0), ("-1234560101134400" to setOf()))
        }
        test(LocalDateTimeFormat.fromFormatString("ld<yyyymmdd>lt<hhmmss>"), dateTimes)
        test(LocalDateTimeFormat.build {
            appendYear(4)
            appendMonthNumber(2)
            appendDayOfMonth(2)
            appendHour(2)
            appendMinute(2)
            appendSecond(2)
        }, dateTimes)
    }

    private fun test(format: LocalDateTimeFormat, strings: Map<LocalDateTime, Pair<String, Set<String>>>) {
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
