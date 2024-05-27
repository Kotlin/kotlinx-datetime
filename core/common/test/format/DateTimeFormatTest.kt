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
            day(Padding.NONE)
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
            day(Padding.NONE)
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

    /**
     * Tests printing of a format that embeds some constants.
     */
    @Test
    fun testStringRepresentationWithConstants() {
        val format = DateTimeComponents.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            time(LocalTime.Formats.ISO)
            optional {
                offset(UtcOffset.Formats.ISO)
            }
        }
        val kotlinCode = DateTimeFormat.formatAsKotlinBuilderDsl(format)
        assertEquals("""
            date(LocalDate.Formats.ISO)
            char(' ')
            time(LocalTime.Formats.ISO)
            optional {
                offset(UtcOffset.Formats.ISO)
            }
        """.trimIndent(), kotlinCode)
    }

    /**
     * Check that we mention [byUnicodePattern] in the string representation of the format when the conversion is
     * incorrect.
     */
    @OptIn(FormatStringsInDatetimeFormats::class)
    @Test
    fun testStringRepresentationAfterIncorrectConversion() {
        for (format in listOf("yyyy-MM-dd", "yy-MM-dd")) {
            assertContains(DateTimeFormat.formatAsKotlinBuilderDsl(
                DateTimeComponents.Format { byUnicodePattern(format) }
            ), "byUnicodePattern")
        }
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

    @Test
    fun testCreatingAmbiguousFormat() {
        assertFailsWith<IllegalArgumentException> {
            DateTimeComponents.Format {
                monthNumber(Padding.NONE)
                day(Padding.NONE)
            }
        }
    }

    @Test
    fun testOptionalBetweenConsecutiveNumbers() {
        val format = UtcOffset.Format {
            offsetHours(Padding.NONE)
            optional {
                optional { offsetSecondsOfMinute() }
                offsetMinutesOfHour()
            }
        }
        assertEquals(UtcOffset(-7, -30), format.parse("-730"))
    }
}

fun <T> DateTimeFormat<T>.assertCanNotParse(input: String) {
    val exception = assertFailsWith<DateTimeFormatException> { parse(input) }
    try {
        val message = exception.message ?: throw AssertionError("The parse exception didn't have a message")
        assertContains(message, input)
    } catch (e: AssertionError) {
        e.addSuppressed(exception)
        throw e
    }
}
