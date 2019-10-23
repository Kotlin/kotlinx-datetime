/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class LocalDateTest {

    fun checkEquals(expected: LocalDate, actual: LocalDate) {
        assertEquals(expected, actual)
        assertEquals(expected.hashCode(), actual.hashCode())
        assertEquals(expected.toString(), actual.toString())
    }

    fun checkComponents(value: LocalDate, year: Int, month: Int, day: Int, dayOfWeek: Int? = null, dayOfYear: Int? = null) {
        assertEquals(year, value.year)
        assertEquals(month, value.monthNumber)
        assertEquals(Month(month), value.month)
        assertEquals(day, value.dayOfMonth)
        if (dayOfWeek != null) assertEquals(dayOfWeek, value.dayOfWeek.number)
        if (dayOfYear != null) assertEquals(dayOfYear, value.dayOfYear)

        val fromComponents = LocalDate(year, month, day)
        checkEquals(fromComponents, value)
    }

    fun checkLocalDateTimePart(date: LocalDate, datetime: LocalDateTime) {
        checkEquals(date, datetime.date)
        checkComponents(date, datetime.year, datetime.monthNumber, datetime.dayOfMonth, datetime.dayOfWeek.number, datetime.dayOfYear)
    }

    @Test
    fun localDateParsing() {
        fun checkParsedComponents(value: String, year: Int, month: Int, day: Int, dayOfWeek: Int, dayOfYear: Int) {
            checkComponents(LocalDate.parse(value), year, month, day, dayOfWeek, dayOfYear)
        }
        checkParsedComponents("2019-10-01", 2019, 10, 1, 2, 274)
        checkParsedComponents("2016-02-29", 2016, 2, 29, 1, 60)
        checkParsedComponents("2017-10-01", 2017, 10, 1,  7, 274)
    }

    @Test
    fun localDateTimePart() {
        val datetime = LocalDateTime.parse("2016-02-29T23:59")
        val date = LocalDate(2016, 2, 29)
        checkLocalDateTimePart(date, datetime)
    }
}