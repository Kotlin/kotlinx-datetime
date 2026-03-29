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
    fun dayOfMonth() {
        // Using day-of-month with various paddings in a custom format
        val zeroPaddedDays = LocalDate.Format {
            day(); char('/'); monthNumber(); char('/'); year()
        }
        check(zeroPaddedDays.format(LocalDate(2021, 1, 6)) == "06/01/2021")
        check(zeroPaddedDays.format(LocalDate(2021, 1, 31)) == "31/01/2021")
        val spacePaddedDays = LocalDate.Format {
            day(padding = Padding.SPACE); char('/'); monthNumber(); char('/'); year()
        }
        check(spacePaddedDays.format(LocalDate(2021, 1, 6)) == " 6/01/2021")
        check(spacePaddedDays.format(LocalDate(2021, 1, 31)) == "31/01/2021")
    }

    @Test
    fun ordinalDay() {
        // Using ordinal day with the default English‑suffix formatter
        val defaultOrdinalDays = LocalDate.Format {
            dayOrdinal(DayOrdinalNames.ENGLISH); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED)
        }
        check(defaultOrdinalDays.format(LocalDate(2021, 1, 1)) == "1st Jan")
        check(defaultOrdinalDays.format(LocalDate(2021, 1, 2)) == "2nd Jan")
        check(defaultOrdinalDays.format(LocalDate(2021, 1, 3)) == "3rd Jan")
        check(defaultOrdinalDays.format(LocalDate(2021, 1, 4)) == "4th Jan")
        check(defaultOrdinalDays.format(LocalDate(2021, 1, 11)) == "11th Jan")
        check(defaultOrdinalDays.format(LocalDate(2021, 1, 21)) == "21st Jan")
        check(defaultOrdinalDays.format(LocalDate(2021, 1, 22)) == "22nd Jan")
        check(defaultOrdinalDays.format(LocalDate(2021, 1, 31)) == "31st Jan")
    }

    @Test
    fun dayOfWeek() {
        // Using strings for day-of-week names in a custom format
        val format = LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED); char(' '); day(); char('/'); monthNumber(); char('/'); year()
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
            val myDayOfWeekNames = DayOfWeekNames(
                "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
            )
            check(myDayOfWeekNames == DayOfWeekNames.ENGLISH_ABBREVIATED) // could just use the built-in one...
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

    class DayOrdinalNamesSamples {
        @Test
        fun usage() {
            // Using ordinal day with the default English‑suffix formatter
            val format = LocalDate.Format {
                dayOrdinal(DayOrdinalNames.ENGLISH); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED)
            }
            check(format.format(LocalDate(2021, 1, 13)) == "2021-01-13, Wed")
        }

        @Test
        fun customNames() {
            // Using ordinal day with a custom formatter that always falls back to "th"
            val customOrdinalDays = LocalDate.Format {
                dayOrdinal(
                    names = DayOrdinalNames(
                        List(31) {
                            val d = it + 1
                            when (d) {
                                1 -> "1st"
                                2 -> "2nd"
                                3 -> "3rd"
                                else -> "${d}th"
                            }
                        }
                    )); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED)
            }
            check(customOrdinalDays.format(LocalDate(2021, 1, 1)) == "1st Jan")
            check(customOrdinalDays.format(LocalDate(2021, 1, 2)) == "2nd Jan")
            check(customOrdinalDays.format(LocalDate(2021, 1, 3)) == "3rd Jan")
            check(customOrdinalDays.format(LocalDate(2021, 1, 22)) == "22th Jan")
            check(customOrdinalDays.format(LocalDate(2021, 1, 31)) == "31th Jan")
        }

        @Test
        fun names() {
            // Obtaining the list of day of week names
            check(
                DayOrdinalNames.ENGLISH.names == listOf(
                    "1st", "2nd", "3rd", "4th", "5th", "6th", "7th",
                    "8th", "9th", "10th", "11th", "12th", "13th",
                    "14th", "15th", "16th", "17th", "18th", "19th",
                    "20th", "21st", "22nd", "23rd", "24th", "25th",
                    "26th", "27th", "28th", "29th", "30th", "31st"
                )
            )
        }

        @Test
        fun invalidListSize() {
            // Attempting to create a DayOrdinalNames with an invalid list size
            try {
                DayOrdinalNames(listOf("1st", "2nd", "3rd")) // only 3 names, should throw
                check(false) // should not reach here
            } catch (e: Throwable) {
                check(e is IllegalArgumentException)
            }
        }
    }
}
