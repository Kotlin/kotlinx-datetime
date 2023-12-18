/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlin.test.*

class ThreeTenBpLocalDateTest {

    @Test
    fun dayOfWeek() {
        var dow = DayOfWeek.MONDAY
        for (month in 1..12) {
            val length = month.monthLength(false)
            for (i in 1..length) {
                val d = LocalDate(2007, month, i)
                assertSame(d.dayOfWeek, dow)
                dow = DayOfWeek(dow.isoDayNumber % 7 + 1)
            }
        }
    }

    @Test
    fun getDayOfYear() {
        val dates = arrayOf(
            Triple(2008, 7, 5),
            Triple(2007, 7, 5),
            Triple(2006, 7, 5),
            Triple(2005, 7, 5),
            Triple(2004, 1, 1),
            Triple(-1, 1, 2))
        for ((y, m, d) in dates) {
            var total = 0
            for (i in 1 until m) {
                total += i.monthLength(isLeapYear(y))
            }
            val doy = total + d
            assertEquals(LocalDate(y, m, d).dayOfYear, doy)
        }
    }

    @Test
    fun plusMonths() {
        val date = LocalDate(2007, 7, 15)
        assertEquals(LocalDate(2007, 8, 15), date.plusMonths(1))
        assertEquals(LocalDate(2009, 8, 15), date.plusMonths(25))
        assertEquals(LocalDate(2007, 6, 15), date.plusMonths(-1))
        assertEquals(LocalDate(2006, 12, 15), date.plusMonths(-7))
        assertEquals(LocalDate(2004, 12, 15), date.plusMonths(-31))
        assertEquals(LocalDate(2009, 2, 28), LocalDate(2008, 2, 29).plusMonths(12))
        assertEquals(LocalDate(2007, 4, 30), LocalDate(2007, 3, 31).plusMonths(1))
    }

    @Test
    fun plusDays() {
        val date = LocalDate(2007, 7, 15)
        assertEquals(LocalDate(2007, 7, 16), date.plusDays(1))
        assertEquals(LocalDate(2007, 9, 15), date.plusDays(62))
        assertEquals(date, LocalDate(2006, 7, 14).plusDays(366))
        assertEquals(LocalDate(2008, 7, 15), date.plusMonths(-12).plusDays(365 + 366))
        assertEquals(LocalDate(2007, 7, 14), date.plusDays(-1))
        assertEquals(LocalDate(2006, 12, 31), date.plusDays(-196))
        assertEquals(LocalDate(2005, 7, 15), date.plusDays(-730))
    }
}
