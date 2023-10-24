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
                appendDayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            appendDayOfMonth(Padding.NONE)
            char(' ')
            appendMonthName(MonthNames("Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."))
            char(' ')
            appendYear()
            char(' ')
            appendHour()
            char(':')
            appendMinute()
            optional {
                char(':')
                appendSecond()
            }
            char(' ')
            alternativeParsing({
                chars("UT")
            }, {
                char('Z')
            }) {
                optional("GMT") {
                    appendOffset(UtcOffset.Formats.FOUR_DIGITS)
                }
            }
        }
        val kotlinCode = DateTimeFormat.formatAsKotlinBuilderDsl(almostRfc1123)
        assertEquals("""
            alternativeParsing({
            }) {
                appendDayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
                chars(", ")
            }
            appendDayOfMonth(Padding.NONE)
            char(' ')
            appendMonthName(MonthNames("Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."))
            char(' ')
            appendYear()
            char(' ')
            appendHour()
            char(':')
            appendMinute()
            optional {
                char(':')
                appendSecond()
            }
            char(' ')
            alternativeParsing({
                chars("UT")
            }, {
                char('Z')
            }) {
                optional("GMT") {
                    appendOffset(UtcOffset.Formats.FOUR_DIGITS)
                }
            }
        """.trimIndent(), kotlinCode)
    }
}
