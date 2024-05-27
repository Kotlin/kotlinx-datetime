/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.random.*
import kotlin.test.*

class LocalDateSamples {

    @Test
    fun simpleParsingAndFormatting() {
        // Parsing and formatting LocalDate values
        check(LocalDate.parse("2023-01-02") == LocalDate(2023, Month.JANUARY, 2))
        check(LocalDate(2023, Month.JANUARY, 2).toString() == "2023-01-02")
    }

    @Test
    fun parsing() {
        // Parsing LocalDate values using predefined and custom formats
        check(LocalDate.parse("2024-04-16") == LocalDate(2024, Month.APRIL, 16))
        val customFormat = LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); day(); chars(", "); year()
        }
        check(LocalDate.parse("Apr 16, 2024", customFormat) == LocalDate(2024, Month.APRIL, 16))
    }

    @Test
    fun fromAndToEpochDays() {
        // Converting LocalDate values to the number of days since 1970-01-01 and back
        check(LocalDate.fromEpochDays(0) == LocalDate(1970, Month.JANUARY, 1))
        val randomEpochDay = Random.nextInt(-50_000..50_000)
        val randomDate = LocalDate.fromEpochDays(randomEpochDay)
        check(randomDate.toEpochDays() == randomEpochDay)
    }

    @Test
    fun customFormat() {
        // Parsing and formatting LocalDate values using a custom format
        val customFormat = LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); day(); chars(", "); year()
        }
        val date = customFormat.parse("Apr 16, 2024")
        check(date == LocalDate(2024, Month.APRIL, 16))
        val formatted = date.format(customFormat)
        check(formatted == "Apr 16, 2024")
    }

    @Test
    fun constructorFunctionMonthNumber() {
        // Constructing a LocalDate value using its constructor
        val date = LocalDate(2024, 4, 16)
        check(date.year == 2024)
        check(date.month.number == 4)
        check(date.month == Month.APRIL)
        check(date.day == 16)
    }

    @Test
    fun constructorFunction() {
        // Constructing a LocalDate value using its constructor
        val date = LocalDate(2024, Month.APRIL, 16)
        check(date.year == 2024)
        check(date.month == Month.APRIL)
        check(date.day == 16)
    }

    @Test
    fun year() {
        // Getting the year
        check(LocalDate(2024, Month.APRIL, 16).year == 2024)
        check(LocalDate(0, Month.APRIL, 16).year == 0)
        check(LocalDate(-2024, Month.APRIL, 16).year == -2024)
    }

    @Test
    fun month() {
        // Getting the month
        for (month in Month.entries) {
            check(LocalDate(2024, month, 16).month == month)
        }
    }

    @Test
    fun day() {
        // Getting the day of the month
        for (day in 1..30) {
            check(LocalDate(2024, Month.APRIL, day).day == day)
        }
    }

    @Test
    fun dayOfWeek() {
        // Getting the day of the week
        check(LocalDate(2024, Month.APRIL, 16).dayOfWeek == DayOfWeek.TUESDAY)
        check(LocalDate(2024, Month.APRIL, 17).dayOfWeek == DayOfWeek.WEDNESDAY)
        check(LocalDate(2024, Month.APRIL, 18).dayOfWeek == DayOfWeek.THURSDAY)
    }

    @Test
    fun dayOfYear() {
        // Getting the 1-based day of the year
        check(LocalDate(2024, Month.APRIL, 16).dayOfYear == 107)
        check(LocalDate(2024, Month.JANUARY, 1).dayOfYear == 1)
        check(LocalDate(2024, Month.DECEMBER, 31).dayOfYear == 366)
    }

    @Test
    fun toEpochDays() {
        // Converting LocalDate values to the number of days since 1970-01-01
        check(LocalDate(2024, Month.APRIL, 16).toEpochDays() == 19829)
        check(LocalDate(1970, Month.JANUARY, 1).toEpochDays() == 0)
        check(LocalDate(1969, Month.DECEMBER, 25).toEpochDays() == -7)
    }

    @Test
    fun compareToSample() {
        // Comparing LocalDate values
        check(LocalDate(2023, 4, 16) < LocalDate(2024, 3, 15))
        check(LocalDate(2023, 4, 16) < LocalDate(2023, 5, 15))
        check(LocalDate(2023, 4, 16) < LocalDate(2023, 4, 17))
        check(LocalDate(-1000, 4, 16) < LocalDate(0, 4, 17))
    }

    @Test
    fun toStringSample() {
        // Converting LocalDate values to strings
        check(LocalDate(2024, 4, 16).toString() == "2024-04-16")
        check(LocalDate(12024, 4, 16).toString() == "+12024-04-16")
        check(LocalDate(-2024, 4, 16).toString() == "-2024-04-16")
    }

    @Test
    fun formatting() {
        // Formatting a LocalDate value using predefined and custom formats
        check(LocalDate(2024, 4, 16).toString() == "2024-04-16")
        check(LocalDate(2024, 4, 16).format(LocalDate.Formats.ISO) == "2024-04-16")
        val customFormat = LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); day(); chars(", "); year()
        }
        check(LocalDate(2024, 4, 16).format(customFormat) == "Apr 16, 2024")
    }

    @Test
    fun atTimeInline() {
        // Constructing a LocalDateTime value from a LocalDate and a LocalTime
        val date = LocalDate(2024, Month.APRIL, 16)
        val dateTime = date.atTime(13, 30)
        check(dateTime == LocalDateTime(2024, Month.APRIL, 16, 13, 30))
    }

    @Test
    fun atTime() {
        // Constructing a LocalDateTime value from a LocalDate and a LocalTime
        val date = LocalDate(2024, Month.APRIL, 16)
        val time = LocalTime(13, 30)
        val dateTime = date.atTime(time)
        check(dateTime == LocalDateTime(2024, Month.APRIL, 16, 13, 30))
    }

    @Test
    fun plusPeriod() {
        // Finding a date that's a given period after another date
        val startDate = LocalDate(2021, Month.OCTOBER, 30)
        check(startDate + DatePeriod(years = 1, months = 2, days = 3) == LocalDate(2023, Month.JANUARY, 2))
        // Step by step explanation:
        // 1. Months and years are added first as one step.
        val intermediateDate = LocalDate(2022, Month.DECEMBER, 30)
        check(startDate.plus(14, DateTimeUnit.MONTH) == intermediateDate)
        // 2. Days are added.
        check(intermediateDate.plus(3, DateTimeUnit.DAY) == LocalDate(2023, Month.JANUARY, 2))
    }

    @Test
    fun minusPeriod() {
        // Finding a date that's a given period before another date
        val startDate = LocalDate(2023, Month.JANUARY, 2)
        check(startDate - DatePeriod(years = 1, months = 2, days = 3) == LocalDate(2021, Month.OCTOBER, 30))
        // Step by step explanation:
        // 1. Months and years are subtracted first as one step.
        val intermediateDate = LocalDate(2021, Month.NOVEMBER, 2)
        check(startDate.minus(14, DateTimeUnit.MONTH) == intermediateDate)
        // 2. Days are subtracted.
        check(intermediateDate.minus(3, DateTimeUnit.DAY) == LocalDate(2021, Month.OCTOBER, 30))
    }

    @Test
    fun periodUntil() {
        // Finding the period between two dates
        val startDate = LocalDate(2023, Month.JANUARY, 2)
        val endDate = LocalDate(2024, Month.APRIL, 1)
        val period = startDate.periodUntil(endDate)
        check(period == DatePeriod(years = 1, months = 2, days = 30))
    }

    @Test
    fun minusDate() {
        // Finding the period between two dates
        val startDate = LocalDate(2023, Month.JANUARY, 2)
        val endDate = LocalDate(2024, Month.APRIL, 1)
        val period = endDate - startDate
        check(period == DatePeriod(years = 1, months = 2, days = 30))
    }

    @Test
    fun until() {
        // Measuring the difference between two dates in terms of the given unit
        val startDate = LocalDate(2023, Month.JANUARY, 2)
        val endDate = LocalDate(2024, Month.APRIL, 1)
        val differenceInMonths = startDate.until(endDate, DateTimeUnit.MONTH)
        check(differenceInMonths == 14)
        // one year, two months, and 30 days, rounded toward zero.
    }

    @Test
    fun daysUntil() {
        // Finding how many days have passed between two dates
        val dateOfConcert = LocalDate(2024, Month.SEPTEMBER, 26)
        val today = LocalDate(2024, Month.APRIL, 16)
        val daysUntilConcert = today.daysUntil(dateOfConcert)
        check(daysUntilConcert == 163)
    }

    @Test
    fun monthsUntil() {
        // Finding how many months have passed between two dates
        val babyDateOfBirth = LocalDate(2023, Month.DECEMBER, 14)
        val today = LocalDate(2024, Month.APRIL, 16)
        val ageInMonths = babyDateOfBirth.monthsUntil(today)
        check(ageInMonths == 4)
    }

    @Test
    fun yearsUntil() {
        // Finding how many years have passed between two dates
        val dateOfBirth = LocalDate(2016, Month.JANUARY, 14)
        val today = LocalDate(2024, Month.APRIL, 16)
        val age = dateOfBirth.yearsUntil(today)
        check(age == 8)
    }

    @Test
    fun plus() {
        // Adding a number of days or months to a date
        val today = LocalDate(2024, Month.APRIL, 16)
        val tenDaysLater = today.plus(10, DateTimeUnit.DAY)
        check(tenDaysLater == LocalDate(2024, Month.APRIL, 26))
        val twoMonthsLater = today.plus(2, DateTimeUnit.MONTH)
        check(twoMonthsLater == LocalDate(2024, Month.JUNE, 16))
    }

    @Test
    fun minus() {
        // Subtracting a number of days or months from a date
        val today = LocalDate(2024, Month.APRIL, 16)
        val tenDaysAgo = today.minus(10, DateTimeUnit.DAY)
        check(tenDaysAgo == LocalDate(2024, Month.APRIL, 6))
        val twoMonthsAgo = today.minus(2, DateTimeUnit.MONTH)
        check(twoMonthsAgo == LocalDate(2024, Month.FEBRUARY, 16))
    }

    class Formats {
        @Test
        fun iso() {
            // Using the extended ISO format for parsing and formatting LocalDate values
            val date = LocalDate.Formats.ISO.parse("2024-04-16")
            check(date == LocalDate(2024, Month.APRIL, 16))
            val formatted = LocalDate.Formats.ISO.format(date)
            check(formatted == "2024-04-16")
        }

        @Test
        fun isoBasic() {
            // Using the basic ISO format for parsing and formatting LocalDate values
            val date = LocalDate.Formats.ISO_BASIC.parse("20240416")
            check(date == LocalDate(2024, Month.APRIL, 16))
            val formatted = LocalDate.Formats.ISO_BASIC.format(date)
            check(formatted == "20240416")
        }
    }
}
