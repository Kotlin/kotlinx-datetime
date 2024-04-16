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
        check(LocalDate.parse("2023-01-02") == LocalDate(2023, Month.JANUARY, 2))
        check(LocalDate(2023, Month.JANUARY, 2).toString() == "2023-01-02")
    }

    @Test
    fun parsing() {
        check(LocalDate.parse("2024-04-16") == LocalDate(2024, Month.APRIL, 16))
        val customFormat = LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); dayOfMonth(); chars(", "); year()
        }
        check(LocalDate.parse("Apr 16, 2024", customFormat) == LocalDate(2024, Month.APRIL, 16))
    }

    @Test
    fun fromAndToEpochDays() {
        check(LocalDate.fromEpochDays(0) == LocalDate(1970, Month.JANUARY, 1))
        val randomEpochDay = Random.nextInt(-50_000..50_000)
        val randomDate = LocalDate.fromEpochDays(randomEpochDay)
        check(randomDate.toEpochDays() == randomEpochDay)
    }

    @Test
    fun customFormat() {
        val customFormat = LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); dayOfMonth(); chars(", "); year()
        }
        val date = customFormat.parse("Apr 16, 2024")
        check(date == LocalDate(2024, Month.APRIL, 16))
        val formatted = date.format(customFormat)
        check(formatted == "Apr 16, 2024")
    }

    @Test
    fun constructorFunctionMonthNumber() {
        val date = LocalDate(2024, 4, 16)
        check(date.year == 2024)
        check(date.monthNumber == 4)
        check(date.month == Month.APRIL)
        check(date.dayOfMonth == 16)
    }

    @Test
    fun constructorFunction() {
        val date = LocalDate(2024, Month.APRIL, 16)
        check(date.year == 2024)
        check(date.month == Month.APRIL)
        check(date.dayOfMonth == 16)
    }

    @Test
    fun year() {
        check(LocalDate(2024, Month.APRIL, 16).year == 2024)
        check(LocalDate(0, Month.APRIL, 16).year == 0)
        check(LocalDate(-2024, Month.APRIL, 16).year == -2024)
    }

    @Test
    fun month() {
        for (month in Month.entries) {
            check(LocalDate(2024, month, 16).month == month)
        }
    }

    @Test
    fun dayOfMonth() {
        repeat(30) {
            val dayOfMonth = it + 1
            check(LocalDate(2024, Month.APRIL, dayOfMonth).dayOfMonth == dayOfMonth)
        }
    }

    @Test
    fun dayOfWeek() {
        check(LocalDate(2024, Month.APRIL, 16).dayOfWeek == DayOfWeek.TUESDAY)
        check(LocalDate(2024, Month.APRIL, 17).dayOfWeek == DayOfWeek.WEDNESDAY)
        check(LocalDate(2024, Month.APRIL, 18).dayOfWeek == DayOfWeek.THURSDAY)
    }

    @Test
    fun dayOfYear() {
        check(LocalDate(2024, Month.APRIL, 16).dayOfYear == 107)
        check(LocalDate(2024, Month.JANUARY, 1).dayOfYear == 1)
        check(LocalDate(2024, Month.DECEMBER, 31).dayOfYear == 366)
    }

    @Test
    fun toEpochDays() {
        check(LocalDate(2024, Month.APRIL, 16).toEpochDays() == 19829)
        check(LocalDate(1970, Month.JANUARY, 1).toEpochDays() == 0)
        check(LocalDate(1969, Month.DECEMBER, 25).toEpochDays() == -7)
    }

    @Test
    fun compareToSample() {
        check(LocalDate(2023, 4, 16) < LocalDate(2024, 3, 15))
        check(LocalDate(2023, 4, 16) < LocalDate(2023, 5, 15))
        check(LocalDate(2023, 4, 16) < LocalDate(2023, 4, 17))
        check(LocalDate(-1000, 4, 16) < LocalDate(0, 4, 17))
    }

    @Test
    fun toStringSample() {
        check(LocalDate(2024, 4, 16).toString() == "2024-04-16")
        check(LocalDate(12024, 4, 16).toString() == "+12024-04-16")
        check(LocalDate(-2024, 4, 16).toString() == "-2024-04-16")
    }

    @Test
    fun formatting() {
        check(LocalDate(2024, 4, 16).format(LocalDate.Formats.ISO) == "2024-04-16")
        val customFormat = LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); dayOfMonth(); chars(", "); year()
        }
        check(LocalDate(2024, 4, 16).format(customFormat) == "Apr 16, 2024")
    }

    @Test
    fun atTimeInline() {
        val date = LocalDate(2024, Month.APRIL, 16)
        val dateTime = date.atTime(13, 30)
        check(dateTime == LocalDateTime(2024, Month.APRIL, 16, 13, 30))
    }

    @Test
    fun atTime() {
        val date = LocalDate(2024, Month.APRIL, 16)
        val time = LocalTime(13, 30)
        val dateTime = date.atTime(time)
        check(dateTime == LocalDateTime(2024, Month.APRIL, 16, 13, 30))
    }

    @Test
    fun plusPeriod() {
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
        val startDate = LocalDate(2023, Month.JANUARY, 2)
        val endDate = LocalDate(2024, Month.APRIL, 1)
        val period = startDate.periodUntil(endDate)
        check(period == DatePeriod(years = 1, months = 2, days = 30))
    }

    @Test
    fun minusDate() {
        val startDate = LocalDate(2023, Month.JANUARY, 2)
        val endDate = LocalDate(2024, Month.APRIL, 1)
        val period = endDate - startDate
        check(period == DatePeriod(years = 1, months = 2, days = 30))
    }

    @Test
    fun until() {
        val startDate = LocalDate(2023, Month.JANUARY, 2)
        val endDate = LocalDate(2024, Month.APRIL, 1)
        val differenceInMonths = startDate.until(endDate, DateTimeUnit.MONTH)
        check(differenceInMonths == 14)
        // one year, two months, and 30 days, rounded toward zero.
    }

    @Test
    fun daysUntil() {
        val dateOfConcert = LocalDate(2024, Month.SEPTEMBER, 26)
        val today = LocalDate(2024, Month.APRIL, 16)
        val daysUntilConcert = today.daysUntil(dateOfConcert)
        check(daysUntilConcert == 163)
    }

    @Test
    fun monthsUntil() {
        val babyDateOfBirth = LocalDate(2023, Month.DECEMBER, 14)
        val today = LocalDate(2024, Month.APRIL, 16)
        val ageInMonths = babyDateOfBirth.monthsUntil(today)
        check(ageInMonths == 4)
    }

    @Test
    fun yearsUntil() {
        val dateOfBirth = LocalDate(2016, Month.JANUARY, 14)
        val today = LocalDate(2024, Month.APRIL, 16)
        val age = dateOfBirth.yearsUntil(today)
        check(age == 8)
    }

    @Test
    fun plusInt() {
        val today = LocalDate(2024, Month.APRIL, 16)
        val tenDaysLater = today.plus(10, DateTimeUnit.DAY)
        check(tenDaysLater == LocalDate(2024, Month.APRIL, 26))
        val twoMonthsLater = today.plus(2, DateTimeUnit.MONTH)
        check(twoMonthsLater == LocalDate(2024, Month.JUNE, 16))
    }

    @Test
    fun minusInt() {
        val today = LocalDate(2024, Month.APRIL, 16)
        val tenDaysAgo = today.minus(10, DateTimeUnit.DAY)
        check(tenDaysAgo == LocalDate(2024, Month.APRIL, 6))
        val twoMonthsAgo = today.minus(2, DateTimeUnit.MONTH)
        check(twoMonthsAgo == LocalDate(2024, Month.FEBRUARY, 16))
    }

    @Test
    @Ignore // only the JVM has the range wide enough
    fun plusLong() {
        val today = LocalDate(2024, Month.APRIL, 16)
        val tenTrillionDaysLater = today.plus(10_000_000_000L, DateTimeUnit.DAY)
        assertEquals(LocalDate(2024, Month.APRIL, 16).plus(10_000_000_000L, DateTimeUnit.DAY), LocalDate(27_381_094, Month.MAY, 12))
        check(tenTrillionDaysLater == LocalDate(27_381_094, Month.MAY, 12))
    }

    @Test
    @Ignore // only the JVM has the range wide enough
    fun minusLong() {
        val today = LocalDate(2024, Month.APRIL, 16)
        val tenTrillionDaysAgo = today.minus(10_000_000_000L, DateTimeUnit.DAY)
        assertEquals(LocalDate(2024, Month.APRIL, 16).minus(10_000_000_000L, DateTimeUnit.DAY), LocalDate(-27_377_046, Month.MARCH, 22))
        check(tenTrillionDaysAgo == LocalDate(-27_377_046, Month.MARCH, 22))
    }

    class Formats {
        @Test
        fun iso() {
            val date = LocalDate.Formats.ISO.parse("2024-04-16")
            check(date == LocalDate(2024, Month.APRIL, 16))
            val formatted = LocalDate.Formats.ISO.format(date)
            check(formatted == "2024-04-16")
        }

        @Test
        fun isoBasic() {
            val date = LocalDate.Formats.ISO_BASIC.parse("20240416")
            check(date == LocalDate(2024, Month.APRIL, 16))
            val formatted = LocalDate.Formats.ISO_BASIC.format(date)
            check(formatted == "20240416")
        }
    }
}
