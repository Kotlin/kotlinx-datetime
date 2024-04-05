/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateTimeFormatSamples {

    @Test
    fun format() {
        check(LocalDate.Formats.ISO.format(LocalDate(2021, 2, 7)) == "2021-02-07")
    }

    @Test
    fun formatTo() {
        val sb = StringBuilder()
        sb.append("Today is ")
        LocalDate.Formats.ISO.formatTo(sb, LocalDate(2024, 4, 5))
        check(sb.toString() == "Today is 2024-04-05")
    }

    @Test
    fun parse() {
        check(LocalDate.Formats.ISO.parse("2021-02-07") == LocalDate(2021, 2, 7))
        try {
            LocalDate.Formats.ISO.parse("2021-02-07T")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // the input string is not in the expected format
        }
        try {
            LocalDate.Formats.ISO.parse("2021-02-40")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // the input string is in the expected format, but the value is invalid
        }
        // to parse strings that have valid formats but invalid values, use `DateTimeComponents`:
        check(DateTimeComponents.Format { date(LocalDate.Formats.ISO) }.parse("2021-02-40").dayOfMonth == 40)
    }

    @Test
    fun parseOrNull() {
        check(LocalDate.Formats.ISO.parseOrNull("2021-02-07") == LocalDate(2021, 2, 7))
        check(LocalDate.Formats.ISO.parseOrNull("2021-02-07T") == null)
        check(LocalDate.Formats.ISO.parseOrNull("2021-02-40") == null)
        check(LocalDate.Formats.ISO.parseOrNull("2021-02-40") == null)
        // to parse strings that have valid formats but invalid values, use `DateTimeComponents`:
        val dateTimeComponentsFormat = DateTimeComponents.Format { date(LocalDate.Formats.ISO) }
        check(dateTimeComponentsFormat.parseOrNull("2021-02-40")?.dayOfMonth == 40)
    }

    @Test
    fun formatAsKotlinBuilderDsl() {
        val customFormat = LocalDate.Format {
            @OptIn(FormatStringsInDatetimeFormats::class)
            byUnicodePattern("MM/dd uuuu")
        }
        val customFormatAsKotlinCode = DateTimeFormat.formatAsKotlinBuilderDsl(customFormat)
        check(
            customFormatAsKotlinCode == """
                monthNumber()
                char('/')
                dayOfMonth()
                char(' ')
                year()
            """.trimIndent()
        )
    }

    class PaddingSamples {
        @Test
        fun usage() {
            val format = LocalDate.Format {
                year(Padding.SPACE)
                chars(", ")
                monthNumber(Padding.NONE)
                char('/')
                dayOfMonth(Padding.ZERO)
            }
            val leoFirstReignStart = LocalDate(457, 2, 7)
            check(leoFirstReignStart.format(format) == " 457, 2/07")
        }

        @Test
        fun zero() {
            val format = LocalDate.Format {
                monthNumber(Padding.ZERO) // padding with zeros is the default, but can be explicitly specified
                char('/')
                dayOfMonth()
                char(' ')
                year()
            }
            val leoFirstReignStart = LocalDate(457, 2, 7)
            check(leoFirstReignStart.format(format) == "02/07 0457")
            check(LocalDate.parse("02/07 0457", format) == leoFirstReignStart)
            try {
                LocalDate.parse("02/7 0457", format)
                fail("Expected IllegalArgumentException")
            } catch (e: IllegalArgumentException) {
                // parsing without padding is not allowed, and the day-of-month was not padded
            }
        }

        @Test
        fun none() {
            val format = LocalDate.Format {
                monthNumber(Padding.NONE)
                char('/')
                dayOfMonth()
                char(' ')
                year()
            }
            val leoFirstReignStart = LocalDate(457, 2, 7)
            check(leoFirstReignStart.format(format) == "2/07 0457")
            // providing leading zeros on parsing is not required, but allowed:
            check(LocalDate.parse("2/07 0457", format) == leoFirstReignStart)
            check(LocalDate.parse("02/07 0457", format) == leoFirstReignStart)
        }

        @Test
        fun spaces() {
            val format = LocalDate.Format {
                monthNumber(Padding.SPACE)
                char('/')
                dayOfMonth()
                char(' ')
                year()
            }
            val leoFirstReignStart = LocalDate(457, 2, 7)
            check(leoFirstReignStart.format(format) == " 2/07 0457")
            // providing leading zeros on parsing instead of spaces is allowed:
            check(LocalDate.parse(" 2/07 0457", format) == leoFirstReignStart)
            check(LocalDate.parse("02/07 0457", format) == leoFirstReignStart)
        }
    }
}
