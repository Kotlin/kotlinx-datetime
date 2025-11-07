/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.ENGLISH_NARROW
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlin.test.Test

class YearMonthFormatSamples {
    @Test
    fun year() {
        // Using the year number in a custom format
        val format = LocalDate.Format {
            year(); char(' '); monthNumber(); char('/'); day()
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
            yearTwoDigits(baseYear = 1960); char(' '); monthNumber(); char('/'); day()
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
            monthNumber(); char('/'); day(); char('/'); year()
        }
        check(zeroPaddedMonths.format(LocalDate(2021, 1, 13)) == "01/13/2021")
        check(zeroPaddedMonths.format(LocalDate(2021, 12, 13)) == "12/13/2021")
        val spacePaddedMonths = LocalDate.Format {
            monthNumber(padding = Padding.SPACE); char('/'); day(); char('/'); year()
        }
        check(spacePaddedMonths.format(LocalDate(2021, 1, 13)) == " 1/13/2021")
        check(spacePaddedMonths.format(LocalDate(2021, 12, 13)) == "12/13/2021")
    }

    @Test
    fun monthName() {
        // Using strings for month names in a custom format
        val format = LocalDate.Format {
            monthName(MonthNames.ENGLISH_FULL); char(' '); day(); char('/'); year()
        }
        check(format.format(LocalDate(2021, 1, 13)) == "January 13/2021")
        check(format.format(LocalDate(2021, 12, 13)) == "December 13/2021")
    }

    @Test
    fun yearMonth() {
        // Using a predefined format for a year-month in a larger custom format
        val format = LocalDate.Format {
            yearMonth(YearMonth.Formats.ISO)
            chars(", ")
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
            char(' ')
            day()
        }
        check(format.format(LocalDate(2021, 1, 13)) == "2021-01, Wed 13")
    }

    class MonthNamesSamples {
        @Test
        fun usage() {
            // Using strings for month names in a custom format
            val format = LocalDate.Format {
                monthName(MonthNames.ENGLISH_ABBREVIATED) // "Jan", "Feb", ...
                char(' ')
                day()
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
                day()
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
                day()
                chars(", ")
                year()
            }
            check(format.format(LocalDate(2021, 1, 13)) == "Jan 13, 2021")
        }

        @Test
        fun englishNarrow() {
            // Using the built-in English narrow month names in formatting
            // Note: Narrow names contain duplicates (e.g., "J" for January/June/July) and cannot be parsed unambiguously
            check(MonthNames.ENGLISH_NARROW == listOf(
                "J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"
            ))
            // They are useful for compact display where context makes the meaning clear
            check(Month.JANUARY.toString() == "JANUARY")
            val narrowName = MonthNames.ENGLISH_NARROW[Month.MARCH.ordinal]
            check(narrowName == "M")
        }
    }
}
