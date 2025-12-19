/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class LocalIsoWeekDateTest {
    @Test
    fun testWikipedia() {
        val table = listOf(
            "2005-01-01" to "2004-W53-6",
            "2005-01-02" to "2004-W53-7",
            "2005-12-31" to "2005-W52-6",
            "2006-01-01" to "2005-W52-7",
            "2006-01-02" to "2006-W01-1",
            "2006-12-31" to "2006-W52-7",
            "2007-01-01" to "2007-W01-1",
            "2007-12-30" to "2007-W52-7",
            "2007-12-31" to "2008-W01-1",
            "2008-01-01" to "2008-W01-2",
            "2008-12-28" to "2008-W52-7",
            "2008-12-29" to "2009-W01-1",
            "2008-12-30" to "2009-W01-2",
            "2008-12-31" to "2009-W01-3",
            "2009-01-01" to "2009-W01-4",
            "2009-12-31" to "2009-W53-4",
            "2010-01-01" to "2009-W53-5",
            "2010-01-02" to "2009-W53-6",
            "2010-01-03" to "2009-W53-7",
        )
        var lastWeekDate: LocalIsoWeekDate? = null
        for ((localDateString, weekDateString) in table) {
            val weekDate = LocalIsoWeekDate.parse(weekDateString)
            val date = LocalDate.parse(localDateString)
            assertEquals(date, weekDate.toLocalDate())
            assertEquals(weekDate, date.toLocalIsoWeekDate())
            assertEquals(weekDateString, weekDate.toString())
            assertEquals(weekDate, weekDate)
            assertEquals(weekDate, with(weekDate) { LocalIsoWeekDate(isoWeekYear, isoWeekNumber, dayOfWeek) })
            lastWeekDate?.let {
                assertTrue(weekDate > it)
                assertTrue(it < weekDate)
                assertTrue(weekDate != it)
            }
            lastWeekDate = weekDate
        }
    }

    @Test
    fun construction() {
        val good = listOf(
            Triple(YEAR_MAX, 52, 5), Triple(YEAR_MIN, 1, 1),
        )
        val bad = listOf(
            // out-of-range day-of-week
            listOf(Triple(2004, 1, 8), Triple(2004, 1, 9), Triple(2004, 1, 0), Triple(2004, 1, -1)),
            // out-of-range week-of-week-year
            listOf(Triple(2004, 0, 2), Triple(2004, -1, 2), Triple(2004, 54, 3), Triple(2004, 55, 6)),
            // week number 53 for non-leap week year
            listOf(Triple(2005, 53, 1), Triple(2008, 53, 2), Triple(YEAR_MAX, 53, 1)),
            // out-of-range year
            listOf(Triple(YEAR_MIN - 1, 1, 1)), listOf(Triple(YEAR_MAX + 1, 1, 1)),
            // out-of-range date for the max year
            listOf(Triple(YEAR_MAX, 52, 6), Triple(YEAR_MAX, 52, 7))
        ).flatten()
        for ((y, w, d) in good) {
            val date = LocalIsoWeekDate(y, w, d)
            assertEquals(y, date.isoWeekYear)
            assertEquals(w, date.isoWeekNumber)
            assertEquals(d, date.dayOfWeek.isoDayNumber)
        }
        for ((y, w, d) in bad) {
            assertFailsWith<IllegalArgumentException> { LocalIsoWeekDate(y, w, d) }
        }
    }

    @Test
    fun parsing() {
        fun checkComponents(weekDate: LocalIsoWeekDate, year: Int, week: Int, dayOfWeek: DayOfWeek) {
            assertEquals(year, weekDate.isoWeekYear)
            assertEquals(week, weekDate.isoWeekNumber)
            assertEquals(dayOfWeek, weekDate.dayOfWeek)
        }
        fun checkParsedComponents(value: String, year: Int, week: Int, dayOfWeek: DayOfWeek) {
            checkComponents(LocalIsoWeekDate.parse(value), year, week, dayOfWeek)
            assertEquals(value, LocalIsoWeekDate(year, week, dayOfWeek).toString())
        }
        assertInvalidFormat { LocalIsoWeekDate.parse("102017-W10-1") }
        assertInvalidFormat { LocalIsoWeekDate.parse("2017--W10-1") }
        assertInvalidFormat { LocalIsoWeekDate.parse("2017-W+10-1") }
        assertInvalidFormat { LocalIsoWeekDate.parse("2017-W10-+1") }
        assertInvalidFormat { LocalIsoWeekDate.parse("2017-W10--1") }
        assertInvalidFormat { LocalIsoWeekDate.parse("+1000000000-W10-1") }
        checkParsedComponents("0999-W12-2", 999, 12, DayOfWeek.TUESDAY)
        checkParsedComponents("-0001-W01-2", -1, 1, DayOfWeek.TUESDAY)
        checkParsedComponents("9999-W12-5", 9999, 12, DayOfWeek.FRIDAY)
        checkParsedComponents("-9999-W12-3", -9999, 12, DayOfWeek.WEDNESDAY)
        checkParsedComponents("+10000-W01-1", 10000, 1, DayOfWeek.MONDAY)
        checkParsedComponents("-10000-W01-6", -10000, 1, DayOfWeek.SATURDAY)
        checkParsedComponents("+123456-W01-4", 123456, 1, DayOfWeek.THURSDAY)
        checkParsedComponents("-123456-W01-7", -123456, 1, DayOfWeek.SUNDAY)
        for (i in 1..30) {
            checkComponents(LocalIsoWeekDate.parse("+${"0".repeat(i)}2024-W01-1"), 2024, 1, DayOfWeek.MONDAY)
            checkComponents(LocalIsoWeekDate.parse("-${"0".repeat(i)}2024-W01-1"), -2024, 1, DayOfWeek.MONDAY)
        }
    }
}
