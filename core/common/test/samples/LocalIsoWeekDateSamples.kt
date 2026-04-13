/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.test.*

class LocalIsoWeekDateSamples {
    @Test
    fun constructorFunction() {
        // Constructing a LocalIsoWeekDate corresponding to the date 2005-01-01
        val isoWeekDate = LocalIsoWeekDate(2004, 53, DayOfWeek.SATURDAY)
        check(2004 == isoWeekDate.isoWeekYear) // note: not 2005!
        check(53 == isoWeekDate.isoWeekNumber)
        check(DayOfWeek.SATURDAY == isoWeekDate.dayOfWeek)
    }

    @Test
    fun localDateConversion() {
        // Converting a LocalDate to a LocalIsoWeekDate and back
        val date = LocalDate(2005, 1, 1)
        val isoWeekDate = LocalIsoWeekDate(2004, 53, DayOfWeek.SATURDAY)
        check(date == isoWeekDate.toLocalDate())
        check(isoWeekDate == date.toLocalIsoWeekDate())
    }

    @Test
    fun parsingAndFormatting() {
        // Parsing a LocalIsoWeekDate from a string corresponding to the date 2005-01-01
        val isoWeekDateString = "2004-W53-6"
        val parsed = LocalIsoWeekDate.parse(isoWeekDateString)
        check(parsed == LocalIsoWeekDate(2004, 53, DayOfWeek.SATURDAY))
        check(LocalDate(2005, 1, 1) == parsed.toLocalDate())
        check(isoWeekDateString == parsed.toString())
    }

    @Test
    fun weekYearProperty() {
        // The ISO week year of a LocalIsoWeekDate corresponding to the date 2005-01-01
        check(2004 == LocalDate(2005, 1, 1).toLocalIsoWeekDate().isoWeekYear)
    }

    @Test
    fun weekNumberProperty() {
        // The ISO week number of a LocalIsoWeekDate corresponding to the date 2005-01-01
        check(53 == LocalDate(2005, 1, 1).toLocalIsoWeekDate().isoWeekNumber)
    }

    @Test
    fun dayOfWeekProperty() {
        // The day-of-week of a LocalIsoWeekDate
        check(DayOfWeek.SATURDAY == LocalIsoWeekDate(2004, 53, DayOfWeek.SATURDAY).dayOfWeek)
    }

    @Test
    fun constructorFunctionDayOfWeekNumber() {
        // Constructing a LocalIsoWeekDate corresponding to the date 2005-01-01
        val isoWeekDate = LocalIsoWeekDate(2004, 53, 6)
        check(2004 == isoWeekDate.isoWeekYear) // note: not 2005!
        check(53 == isoWeekDate.isoWeekNumber)
        check(6 == isoWeekDate.dayOfWeek.isoDayNumber)
    }

    @Test
    fun orNull() {
        // Constructing a LocalIsoWeekDate or returning null if components are out of range
        check(LocalIsoWeekDate.orNull(2004, 53, DayOfWeek.SATURDAY) == LocalIsoWeekDate(2004, 53, DayOfWeek.SATURDAY))
        check(LocalIsoWeekDate.orNull(2004, 54, DayOfWeek.SATURDAY) == null)
    }

    @Test
    fun orNullDayOfWeekNumber() {
        // Constructing a LocalIsoWeekDate or returning null if components are out of range
        check(LocalIsoWeekDate.orNull(2004, 53, 6) == LocalIsoWeekDate(2004, 53, 6))
        check(LocalIsoWeekDate.orNull(2004, 53, 8) == null)
    }

    @Test
    fun parsing() {
        // Constructing a LocalIsoWeekDate from a string
        check(LocalDate(2005, 1, 2) == LocalIsoWeekDate.parse("2004-W53-7").toLocalDate())
        check(LocalDate(2008, 12, 29) == LocalIsoWeekDate.parse("2009-W01-1").toLocalDate())
    }

    @Test
    fun toStringSample() {
        // Converting a LocalIsoWeekDate to a string
        check("2004-W53-7" == LocalDate(2005, 1, 2).toLocalIsoWeekDate().toString())
        check("2009-W01-1" == LocalDate(2008, 12, 29).toLocalIsoWeekDate().toString())
    }

    @Test
    fun compareToSample() {
        // Comparing LocalIsoWeekDate values
        check(LocalIsoWeekDate(2004, 53, 1) < LocalIsoWeekDate(2004, 53, 2))
        check(LocalIsoWeekDate(2004, 52, 7) < LocalIsoWeekDate(2004, 53, 1))
        check(LocalIsoWeekDate(2004, 52, 7) < LocalIsoWeekDate(2005, 1, 1))
    }
}
