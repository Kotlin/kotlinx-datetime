/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class LocalDateFormatSamples {

    @Test
    fun year() {
        // Using the year number in a custom format
        val format = LocalDate.Format {
            year(); char(' '); monthNumber(); char('/'); dayOfMonth()
        }
        check(format.format(LocalDate(2021, 1, 13)) == "2021 01/13")
        check(format.format(LocalDate(13, 1, 13)) == "0013 01/13")
        check(format.format(LocalDate(-2021, 1, 13)) == "-2021 01/13")
        check(format.format(LocalDate(12021, 1, 13)) == "+12021 01/13")
    }

    @Test
    fun yearTwoDigits() {
        // Using two-digit years in a custom format
        val format = LocalDate.Format {
            yearTwoDigits(baseYear = 1960); char(' '); monthNumber(); char('/'); dayOfMonth()
        }
        check(format.format(LocalDate(1960, 1, 13)) == "60 01/13")
        check(format.format(LocalDate(2000, 1, 13)) == "00 01/13")
        check(format.format(LocalDate(2021, 1, 13)) == "21 01/13")
        check(format.format(LocalDate(2059, 1, 13)) == "59 01/13")
        check(format.format(LocalDate(2060, 1, 13)) == "+2060 01/13")
        check(format.format(LocalDate(-13, 1, 13)) == "-13 01/13")
    }
    
    @Test
    fun monthNumber() {
        // Using month number with various paddings in a custom format
        val zeroPaddedMonths = LocalDate.Format {
            monthNumber(); char('/'); dayOfMonth(); char('/'); year()
        }
        check(zeroPaddedMonths.format(LocalDate(2021, 1, 13)) == "01/13/2021")
        check(zeroPaddedMonths.format(LocalDate(2021, 12, 13)) == "12/13/2021")
        val spacePaddedMonths = LocalDate.Format {
            monthNumber(padding = Padding.SPACE); char('/'); dayOfMonth(); char('/'); year()
        }
        check(spacePaddedMonths.format(LocalDate(2021, 1, 13)) == " 1/13/2021")
        check(spacePaddedMonths.format(LocalDate(2021, 12, 13)) == "12/13/2021")
    }

    @Test
    fun monthName() {
        // Using strings for month names in a custom format
        val format = LocalDate.Format {
            monthName(MonthNames.ENGLISH_FULL); char(' '); dayOfMonth(); char('/'); year()
        }
        check(format.format(LocalDate(2021, 1, 13)) == "January 13/2021")
        check(format.format(LocalDate(2021, 12, 13)) == "December 13/2021")
    }

    @Test
    fun dayOfMonth() {
        // Using day-of-month with various paddings in a custom format
        val zeroPaddedDays = LocalDate.Format {
            dayOfMonth(); char('/'); monthNumber(); char('/'); year()
        }
        check(zeroPaddedDays.format(LocalDate(2021, 1, 6)) == "06/01/2021")
        check(zeroPaddedDays.format(LocalDate(2021, 1, 31)) == "31/01/2021")
        val spacePaddedDays = LocalDate.Format {
            dayOfMonth(padding = Padding.SPACE); char('/'); monthNumber(); char('/'); year()
        }
        check(spacePaddedDays.format(LocalDate(2021, 1, 6)) == " 6/01/2021")
        check(spacePaddedDays.format(LocalDate(2021, 1, 31)) == "31/01/2021")
    }

    @Test
    fun dayOfWeek() {
        // Using strings for day-of-week names in a custom format
        val format = LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED); char(' '); dayOfMonth(); char('/'); monthNumber(); char('/'); year()
        }
        check(format.format(LocalDate(2021, 1, 13)) == "Wed 13/01/2021")
        check(format.format(LocalDate(2021, 12, 13)) == "Mon 13/12/2021")
    }

    @Test
    fun dayOfYear() {
        // Using day-of-year in a custom format
        val format = LocalDate.Format {
            year(); dayOfYear()
        }
        check(format.format(LocalDate(2021, 2, 13)) == "2021044")
        check(format.parse("2021044") == LocalDate(2021, 2, 13))
    }

    @Test
    fun date() {
        // Using a predefined format for a date in a larger custom format
        val format = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            alternativeParsing({ char('t') }) { char('T') }
            hour(); char(':'); minute()
        }
        check(format.format(LocalDateTime(2021, 1, 13, 14, 30)) == "2021-01-13T14:30")
    }

    class MonthNamesSamples {
        @Test
        fun usage() {
            // Using strings for month names in a custom format
            val format = LocalDate.Format {
                monthName(MonthNames.ENGLISH_ABBREVIATED) // "Jan", "Feb", ...
                char(' ')
                dayOfMonth()
                chars(", ")
                year()
            }
            check(format.format(LocalDate(2021, 1, 13)) == "Jan 13, 2021")
        }

        @Test
        fun constructionFromStrings() {
            // Constructing a custom set of month names for parsing and formatting by passing 12 strings
            val myMonthNames = MonthNames(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            check(myMonthNames == MonthNames.ENGLISH_ABBREVIATED) // could just use the built-in one...
        }

        @Test
        fun constructionFromList() {
            // Constructing a custom set of month names for parsing and formatting
            val germanMonthNames = listOf(
                "Januar", "Februar", "März", "April", "Mai", "Juni",
                "Juli", "August", "September", "Oktober", "November", "Dezember"
            )
            // constructing by passing a list of 12 strings
            val myMonthNamesFromList = MonthNames(germanMonthNames)
            check(myMonthNamesFromList.names == germanMonthNames)
        }

        @Test
        fun names() {
            // Obtaining the list of month names
            check(MonthNames.ENGLISH_ABBREVIATED.names == listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            ))
        }

        @Test
        fun englishFull() {
            // Using the built-in English month names in a custom format
            val format = LocalDate.Format {
                monthName(MonthNames.ENGLISH_FULL)
                char(' ')
                dayOfMonth()
                chars(", ")
                year()
            }
            check(format.format(LocalDate(2021, 1, 13)) == "January 13, 2021")
        }

        @Test
        fun englishAbbreviated() {
            // Using the built-in English abbreviated month names in a custom format
            val format = LocalDate.Format {
                monthName(MonthNames.ENGLISH_ABBREVIATED)
                char(' ')
                dayOfMonth()
                chars(", ")
                year()
            }
            check(format.format(LocalDate(2021, 1, 13)) == "Jan 13, 2021")
        }
    }

    class DayOfWeekNamesSamples {
        @Test
        fun usage() {
            // Using strings for day-of-week names in a custom format
            val format = LocalDate.Format {
                date(LocalDate.Formats.ISO)
                chars(", ")
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED) // "Mon", "Tue", ...
            }
            check(format.format(LocalDate(2021, 1, 13)) == "2021-01-13, Wed")
        }

        @Test
        fun constructionFromStrings() {
            // Constructing a custom set of day of week names for parsing and formatting by passing 7 strings
            val myMonthNames = DayOfWeekNames(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            )
            check(myMonthNames == DayOfWeekNames.ENGLISH_ABBREVIATED) // could just use the built-in one...
        }

        @Test
        fun constructionFromList() {
            // Constructing a custom set of day of week names for parsing and formatting
            val germanDayOfWeekNames = listOf(
                "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
            )
            // constructing by passing a list of 7 strings
            val myDayOfWeekNames = DayOfWeekNames(germanDayOfWeekNames)
            check(myDayOfWeekNames.names == germanDayOfWeekNames)
        }

        @Test
        fun names() {
            // Obtaining the list of day of week names
            check(DayOfWeekNames.ENGLISH_ABBREVIATED.names == listOf(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            ))
        }

        @Test
        fun englishFull() {
            // Using the built-in English day of week names in a custom format
            val format = LocalDate.Format {
                date(LocalDate.Formats.ISO)
                chars(", ")
                dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            }
            check(format.format(LocalDate(2021, 1, 13)) == "2021-01-13, Wednesday")
        }

        @Test
        fun englishAbbreviated() {
            // Using the built-in English abbreviated day of week names in a custom format
            val format = LocalDate.Format {
                date(LocalDate.Formats.ISO)
                chars(", ")
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
            }
            check(format.format(LocalDate(2021, 1, 13)) == "2021-01-13, Wed")
        }
    }
}
