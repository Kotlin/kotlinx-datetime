/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateTimeFormatBuilderSamples {

    @Test
    fun chars() {
        // Defining a custom format that includes verbatim strings
        val format = LocalDate.Format {
            monthNumber()
            char('/')
            day()
            chars(", ")
            year()
        }
        check(LocalDate(2020, 1, 13).format(format) == "01/13, 2020")
    }

    @Test
    fun alternativeParsing() {
        // Defining a custom format that allows parsing one of several alternatives
        val format = DateTimeComponents.Format {
            // optionally, date:
            alternativeParsing({
            }) {
                date(LocalDate.Formats.ISO)
            }
            // optionally, time:
            alternativeParsing({
            }) {
                // either the `T` or the `t` character:
                alternativeParsing({ char('t') }) { char('T') }
                time(LocalTime.Formats.ISO)
            }
        }
        val date = LocalDate(2020, 1, 13)
        val time = LocalTime(12, 30, 16)
        check(format.parse("2020-01-13t12:30:16").toLocalDateTime() == date.atTime(time))
        check(format.parse("2020-01-13").toLocalDate() == date)
        check(format.parse("T12:30:16").toLocalTime() == time)
        check(format.format { setDate(date); setTime(time) } == "2020-01-13T12:30:16")
    }

    @Test
    fun optional() {
        // Defining a custom format that includes parts that will be omitted if they are zero
        val format = UtcOffset.Format {
            optional(ifZero = "Z") {
                offsetHours()
                optional {
                    char(':')
                    offsetMinutesOfHour()
                    optional {
                        char(':')
                        offsetSecondsOfMinute()
                    }
                }
            }
        }
        // During parsing, the optional parts can be omitted:
        check(format.parse("Z") == UtcOffset.ZERO)
        check(format.parse("-05") == UtcOffset(hours = -5))
        check(format.parse("-05:30") == UtcOffset(hours = -5, minutes = -30))
        check(format.parse("-05:15:05") == UtcOffset(hours = -5, minutes = -15, seconds = -5))
        // ... but they can also be present:
        check(format.parse("-05:00") == UtcOffset(hours = -5))
        check(format.parse("-05:00:00") == UtcOffset(hours = -5))
        // During formatting, the optional parts are only included if they are non-zero:
        check(UtcOffset.ZERO.format(format) == "Z")
        check(UtcOffset(hours = -5).format(format) == "-05")
        check(UtcOffset(hours = -5, minutes = -30).format(format) == "-05:30")
        check(UtcOffset(hours = -5, minutes = -15, seconds = -5).format(format) == "-05:15:05")

        try {
            LocalDate.Format { optional { year() }}
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Since `year` has no optional component, it is an error to put it inside `optional`.
            // Use `alternativeParsing` for parsing-only optional components.
        }
    }

    @Test
    fun char() {
        // Defining a custom format that includes a verbatim character
        val format = LocalDate.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
        }
        check(LocalDate(2020, 1, 1).format(format) == "2020-01-01")
    }
}
