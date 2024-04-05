/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class LocalDateFormatSamples {
    class MonthNamesSamples {
        @Test
        fun usage() {
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
            // constructing by passing 12 strings
            val myMonthNames = MonthNames(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            check(myMonthNames == MonthNames.ENGLISH_ABBREVIATED) // could just use the built-in one...
        }

        @Test
        fun constructionFromList() {
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
            check(MonthNames.ENGLISH_ABBREVIATED.names == listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            ))
        }

        @Test
        fun englishFull() {
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
            val format = LocalDate.Format {
                date(LocalDate.Formats.ISO)
                chars(", ")
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED) // "Mon", "Tue", ...
            }
            check(format.format(LocalDate(2021, 1, 13)) == "2021-01-13, Wed")
        }

        @Test
        fun constructionFromStrings() {
            // constructing by passing 7 strings
            val myMonthNames = DayOfWeekNames(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            )
            check(myMonthNames == DayOfWeekNames.ENGLISH_ABBREVIATED) // could just use the built-in one...
        }

        @Test
        fun constructionFromList() {
            val germanDayOfWeekNames = listOf(
                "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
            )
            // constructing by passing a list of 7 strings
            val myDayOfWeekNames = DayOfWeekNames(germanDayOfWeekNames)
            check(myDayOfWeekNames.names == germanDayOfWeekNames)
        }

        @Test
        fun names() {
            check(DayOfWeekNames.ENGLISH_ABBREVIATED.names == listOf(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            ))
        }

        @Test
        fun englishFull() {
            val format = LocalDate.Format {
                date(LocalDate.Formats.ISO)
                chars(", ")
                dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            }
            check(format.format(LocalDate(2021, 1, 13)) == "2021-01-13, Wednesday")
        }

        @Test
        fun englishAbbreviated() {
            val format = LocalDate.Format {
                date(LocalDate.Formats.ISO)
                chars(", ")
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
            }
            check(format.format(LocalDate(2021, 1, 13)) == "2021-01-13, Wed")
        }
    }
}
