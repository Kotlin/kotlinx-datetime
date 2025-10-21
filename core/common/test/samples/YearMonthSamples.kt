/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlinx.datetime.onDay
import kotlin.test.*

class YearMonthSamples {

    @Test
    fun simpleParsingAndFormatting() {
        // Parsing and formatting YearMonth values
        check(YearMonth.parse("2023-01") == YearMonth(2023, Month.JANUARY))
        check(YearMonth(2023, Month.JANUARY).toString() == "2023-01")
    }

    @Test
    fun parsing() {
        // Parsing YearMonth values using predefined and custom formats
        check(YearMonth.parse("2024-04") == YearMonth(2024, Month.APRIL))
        val customFormat = YearMonth.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); chars(", "); year()
        }
        check(YearMonth.parse("Apr, 2024", customFormat) == YearMonth(2024, Month.APRIL))
    }

    @Test
    fun customFormat() {
        // Parsing and formatting YearMonth values using a custom format
        val customFormat = YearMonth.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); chars(", "); year()
        }
        val yearMonth = customFormat.parse("Apr, 2024")
        check(yearMonth == YearMonth(2024, Month.APRIL))
        val formatted = yearMonth.format(customFormat)
        check(formatted == "Apr, 2024")
    }

    @Test
    fun constructorFunctionMonthNumber() {
        // Constructing a YearMonth value using its constructor
        val yearMonth = YearMonth(2024, 4)
        check(yearMonth.year == 2024)
        check(yearMonth.month == Month.APRIL)
    }

    @Test
    fun constructorFunction() {
        // Constructing a YearMonth value using its constructor
        val yearMonth = YearMonth(2024, Month.APRIL)
        check(yearMonth.year == 2024)
        check(yearMonth.month == Month.APRIL)
    }

    @Test
    fun year() {
        // Getting the year
        check(YearMonth(2024, Month.APRIL).year == 2024)
        check(YearMonth(0, Month.APRIL).year == 0)
        check(YearMonth(-2024, Month.APRIL).year == -2024)
    }

    @Test
    fun month() {
        // Getting the month
        for (month in Month.entries) {
            check(YearMonth(2024, month).month == month)
        }
    }

    @Test
    fun days() {
        // Getting the range of days in a YearMonth
        val yearMonth = YearMonth(2024, Month.APRIL)
        // The range consists of all the days in the month:
        check(yearMonth.days.size == 30)
        check(yearMonth.days.first() == LocalDate(2024, Month.APRIL, 1))
        check(yearMonth.days.last() == LocalDate(2024, Month.APRIL, 30))
        check(yearMonth.days.contains(LocalDate(2024, Month.APRIL, 15)))
        // The range allows iterating over the days:
        for (day in yearMonth.days) {
            check(day.month == Month.APRIL)
            check(day.year == 2024)
        }
    }

    @Test
    fun compareToSample() {
        // Comparing YearMonth values
        check(YearMonth(2023, 4) < YearMonth(2024, 3))
        check(YearMonth(2023, 4) < YearMonth(2023, 5))
        check(YearMonth(-1000, 4) < YearMonth(0, 4))
    }

    @Test
    fun toStringSample() {
        // Converting YearMonth values to strings
        check(YearMonth(2024, 4).toString() == "2024-04")
        check(YearMonth(12024, 4).toString() == "+12024-04")
        check(YearMonth(-2024, 4).toString() == "-2024-04")
    }

    @Test
    fun formatting() {
        // Formatting a YearMonth value using predefined and custom formats
        check(YearMonth(2024, 4).toString() == "2024-04")
        check(YearMonth(2024, 4).format(YearMonth.Formats.ISO) == "2024-04")
        val customFormat = YearMonth.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); chars(", "); year()
        }
        check(YearMonth(2024, 4).format(customFormat) == "Apr, 2024")
    }

    @Test
    fun plusYear() {
        check(YearMonth(2023, Month.JANUARY).plusYear() == YearMonth(2024, Month.JANUARY))
    }

    @Test
    fun minusYear() {
        check(YearMonth(2023, Month.JANUARY).minusYear() == YearMonth(2022, Month.JANUARY))
    }

    @Test
    fun plusMonth() {
        check(YearMonth(2023, Month.JANUARY).plusMonth() == YearMonth(2023, Month.FEBRUARY))
    }

    @Test
    fun minusMonth() {
        check(YearMonth(2023, Month.JANUARY).minusMonth() == YearMonth(2022, Month.DECEMBER))
    }

    @Test
    fun yearMonth() {
        // Getting a YearMonth value from a LocalDate
        val localDate = LocalDate(2024, Month.APRIL, 13)
        check(localDate.yearMonth == YearMonth(2024, Month.APRIL))
    }

    @Test
    fun onDay() {
        // Getting a LocalDate value from a YearMonth
        val yearMonth = YearMonth(2024, Month.APRIL)
        check(yearMonth.onDay(13) == LocalDate(2024, Month.APRIL, 13))
    }

    @Test
    fun until() {
        // Measuring the difference between two year-months in terms of the given unit
        val startMonth = YearMonth(2023, Month.JANUARY)
        val endMonth = YearMonth(2024, Month.APRIL)
        val differenceInMonths = startMonth.until(endMonth, DateTimeUnit.MONTH)
        check(differenceInMonths == 15L)
        // one whole year and january, february, and march
    }

    @Test
    fun monthsUntil() {
        // Finding how many months have passed between two year-months
        val firstBillingMonth = YearMonth(2024, Month.MAY)
        val today = YearMonth(2024, Month.NOVEMBER)
        val billableMonths = firstBillingMonth.monthsUntil(today)
        check(billableMonths == 6)
    }

    @Test
    fun yearsUntil() {
        // Finding how many years have passed between two year-months
        val firstBillingMonth = YearMonth(2016, Month.JANUARY)
        val thisMonth = YearMonth(2024, Month.NOVEMBER)
        val billableYears = firstBillingMonth.yearsUntil(thisMonth)
        check(billableYears == 8)
    }

    @Test
    fun plus() {
        // Adding a number of months or years to a year-month
        val thisMonth = YearMonth(2024, Month.NOVEMBER)
        val halfYearLater = thisMonth.plus(6, DateTimeUnit.MONTH)
        check(halfYearLater == YearMonth(2025, Month.MAY))
        val twoMonthsLater = thisMonth.plus(2, DateTimeUnit.YEAR)
        check(twoMonthsLater == YearMonth(2026, Month.NOVEMBER))
    }

    @Test
    fun minus() {
        // Subtracting a number of months or years from a year-month
        val thisMonth = YearMonth(2024, Month.NOVEMBER)
        val halfYearAgo = thisMonth.minus(6, DateTimeUnit.MONTH)
        assertEquals(halfYearAgo, YearMonth(2024, Month.MAY))
        check(halfYearAgo == YearMonth(2024, Month.MAY))
        val twoYearsAgo = thisMonth.minus(2, DateTimeUnit.YEAR)
        check(twoYearsAgo == YearMonth(2022, Month.NOVEMBER))
    }


    @Test
    fun firstAndLastDay() {
        // Getting the first and last day of a year-month
        val yearMonth = YearMonth(2024, Month.FEBRUARY)
        check(yearMonth.firstDay == LocalDate(2024, Month.FEBRUARY, 1))
        check(yearMonth.lastDay == LocalDate(2024, Month.FEBRUARY, 29))
    }

    @Test
    fun numberOfDays() {
        // Determining the number of days in a year-month
        check(YearMonth(2024, Month.FEBRUARY).numberOfDays == 29)
        check(YearMonth(2023, Month.FEBRUARY).numberOfDays == 28)
        check(YearMonth(2024, Month.APRIL).numberOfDays == 30)
    }

    class Formats {
        @Test
        fun iso() {
            // Using the extended ISO format for parsing and formatting YearMonth values
            val yearMonth = YearMonth.Formats.ISO.parse("2024-04")
            check(yearMonth == YearMonth(2024, Month.APRIL))
            val formatted = YearMonth.Formats.ISO.format(yearMonth)
            check(formatted == "2024-04")
        }
    }
}
