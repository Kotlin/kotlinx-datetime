/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class UtcOffsetSamples {

    @Test
    fun construction() {
        // Constructing a UtcOffset using the constructor function
        val offset = UtcOffset(hours = 3, minutes = 30)
        check(offset.totalSeconds == 12600)
        // UtcOffset.ZERO is a constant representing the zero offset
        check(UtcOffset(seconds = 0) == UtcOffset.ZERO)
    }

    @Test
    fun simpleParsingAndFormatting() {
        // Parsing a UtcOffset from a string
        val offset = UtcOffset.parse("+01:30")
        check(offset.totalSeconds == 90 * 60)
        // Formatting a UtcOffset to a string
        val formatted = offset.toString()
        check(formatted == "+01:30")
    }

    @Test
    fun customFormat() {
        // Parsing a UtcOffset using a custom format
        val customFormat = UtcOffset.Format {
            optional("GMT") {
                offsetHours(Padding.NONE); char(':'); offsetMinutesOfHour()
                optional { char(':'); offsetSecondsOfMinute() }
            }
        }
        val offset = customFormat.parse("+01:30:15")
        // Formatting a UtcOffset using both a custom format and a predefined one
        check(offset.format(customFormat) == "+1:30:15")
        check(offset.format(UtcOffset.Formats.FOUR_DIGITS) == "+0130")
    }

    @Test
    fun equalsSample() {
        // Comparing UtcOffset values for equality
        val offset1 = UtcOffset.parse("+01:30")
        val offset2 = UtcOffset(minutes = 90)
        check(offset1 == offset2)
        val offset3 = UtcOffset(hours = 1)
        check(offset1 != offset3)
    }

    @Test
    fun parsing() {
        // Parsing a UtcOffset from a string using predefined and custom formats
        check(UtcOffset.parse("+01:30").totalSeconds == 5400)
        check(UtcOffset.parse("+0130", UtcOffset.Formats.FOUR_DIGITS).totalSeconds == 5400)
        val customFormat = UtcOffset.Format { offsetHours(Padding.NONE); offsetMinutesOfHour() }
        check(UtcOffset.parse("+130", customFormat).totalSeconds == 5400)
    }

    @Test
    fun toStringSample() {
        // Converting a UtcOffset to a string
        check(UtcOffset.parse("+01:30:00").toString() == "+01:30")
        check(UtcOffset(hours = 1, minutes = 30).toString() == "+01:30")
        check(UtcOffset(seconds = 5400).toString() == "+01:30")
    }

    @Test
    fun formatting() {
        // Formatting a UtcOffset to a string using predefined and custom formats
        check(UtcOffset(hours = 1, minutes = 30).format(UtcOffset.Formats.FOUR_DIGITS) == "+0130")
        val customFormat = UtcOffset.Format { offsetHours(Padding.NONE); offsetMinutesOfHour() }
        check(UtcOffset(hours = 1, minutes = 30).format(customFormat) == "+130")
    }

    @Test
    fun constructorFunction() {
        // Using the constructor function to create UtcOffset values
        check(UtcOffset(hours = 3, minutes = 30).totalSeconds == 12600)
        check(UtcOffset(seconds = -3600) == UtcOffset(hours = -1))
        try {
            UtcOffset(hours = 1, minutes = 60)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Since `hours` is non-zero, `minutes` must be in the range of 0..59
        }
        try {
            UtcOffset(hours = -1, minutes = 30)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Since `hours` is negative, `minutes` must also be negative
        }
    }

    @Test
    fun asFixedOffsetTimeZone() {
        // Converting a UtcOffset to a fixed-offset TimeZone
        UtcOffset(hours = 3, minutes = 30).asTimeZone().let { timeZone ->
            check(timeZone.id == "+03:30")
            check(timeZone.offset == UtcOffset(hours = 3, minutes = 30))
        }
    }

    class Formats {
        @Test
        fun isoBasic() {
            // Using the basic ISO format for parsing and formatting UtcOffset values
            val offset = UtcOffset.Formats.ISO_BASIC.parse("+103622")
            check(offset == UtcOffset(hours = 10, minutes = 36, seconds = 22))
            val formatted = UtcOffset.Formats.ISO_BASIC.format(offset)
            check(formatted == "+103622")
        }

        @Test
        fun iso() {
            // Using the extended ISO format for parsing and formatting UtcOffset values
            val offset = UtcOffset.Formats.ISO.parse("+10:36:22")
            check(offset == UtcOffset(hours = 10, minutes = 36, seconds = 22))
            val formatted = UtcOffset.Formats.ISO.format(offset)
            check(formatted == "+10:36:22")
        }

        @Test
        fun fourDigits() {
            // Using the constant-width compact format for parsing and formatting UtcOffset values
            val offset = UtcOffset.Formats.FOUR_DIGITS.parse("+1036")
            check(offset == UtcOffset(hours = 10, minutes = 36))
            val offsetWithSeconds = UtcOffset(hours = 10, minutes = 36, seconds = 59)
            val formattedOffsetWithSeconds = UtcOffset.Formats.FOUR_DIGITS.format(offsetWithSeconds)
            check(formattedOffsetWithSeconds == "+1036")
        }
    }
}
