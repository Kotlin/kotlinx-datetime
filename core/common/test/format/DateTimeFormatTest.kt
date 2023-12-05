/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateTimeFormatTest {
    @Test
    fun testStringRepresentations() {
        // not exactly RFC 1123, because it would get recognized as the constant
        val almostRfc1123 = DateTimeComponents.Format {
            alternativeParsing({
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            dayOfMonth(Padding.NONE)
            char(' ')
            monthName(MonthNames("Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."))
            char(' ')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
            char(' ')
            alternativeParsing({
                chars("UT")
            }, {
                char('Z')
            }) {
                optional("GMT") {
                    offset(UtcOffset.Formats.FOUR_DIGITS)
                }
            }
        }
        val kotlinCode = DateTimeFormat.formatAsKotlinBuilderDsl(almostRfc1123)
        assertEquals("""
            alternativeParsing({
            }) {
                dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            dayOfMonth(Padding.NONE)
            char(' ')
            monthName(MonthNames("Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."))
            char(' ')
            year()
            char(' ')
            hour()
            char(':')
            minute()
            optional {
                char(':')
                second()
            }
            char(' ')
            alternativeParsing({
                chars("UT")
            }, {
                char('Z')
            }) {
                optional("GMT") {
                    offset(UtcOffset.Formats.FOUR_DIGITS)
                }
            }
        """.trimIndent(), kotlinCode)
    }

    @Test
    fun testParseStringWithNumbers() {
        val formats = listOf(
            "0123x0123",
            "0123x",
            "x0123",
            "0123",
            "x"
        )
        for (format in formats) {
            DateTimeComponents.Format { chars(format) }.parse(format)
        }
    }
}
