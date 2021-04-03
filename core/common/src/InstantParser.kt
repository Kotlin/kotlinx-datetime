/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.math.min
import kotlin.math.pow

internal fun parseInstantCommon(string: String): Instant = parseIsoString(string)

/*
 * The algorithm for parsing time and zone offset was adapted from
 * https://github.com/square/moshi/blob/aea17e09bc6a3f9015d3de0e951923f1033d299e/adapters/src/main/java/com/squareup/moshi/adapters/Iso8601Utils.java
 */
private fun parseIsoString(isoString: String): Instant {
    try {
        val dateTimeSplit = isoString.split('T', ignoreCase = true)
        if (dateTimeSplit.size != 2) {
            throw DateTimeFormatException("ISO 8601 datetime must contain exactly one (T|t) delimiter.")
        }
        val localDate = LocalDate.parse(dateTimeSplit[0])

        // Iso8601Utils.parse
        val timePart = dateTimeSplit[1]
        var offset = 0
        val hour = parseInt(timePart, offset, offset + 2).also { offset += 2 }
        if (checkOffset(timePart, offset, ':')) {
            offset += 1
        }
        val minutes = parseInt(timePart, offset, offset + 2).also { offset += 2 }
        if (checkOffset(timePart, offset, ':')) {
            offset += 1
        }

        var seconds = 0
        var nanosecond = 0
        // seconds and fraction can be optional
        if (timePart.length > offset) {
            val c = timePart[offset]
            if (c != 'Z' && c != 'z' && c != '+' && c != '-') {
                seconds = parseInt(timePart, offset, offset + 2).also { offset += 2 }
                if (seconds > 59 && seconds < 63) { // https://github.com/Kotlin/kotlinx-datetime/issues/5
                    seconds = 59 // truncate up to 3 leap seconds
                }
                if (checkOffset(timePart, offset, '.')) {
                    offset += 1
                    val endOffset =
                        indexOfNonDigit(timePart, offset + 1) // assume at least one digit
                    val parseEndOffset =
                        min(endOffset, offset + 9) // parse up to 9 digits
                    val fraction = parseInt(timePart, offset, parseEndOffset)
                    nanosecond = (10.0.pow(9 - (parseEndOffset - offset)) * fraction).toInt()
                    offset = endOffset
                }
            }
        }

        // extract timezone
        if (timePart.length <= offset) {
            throw DateTimeFormatException("No time zone indicator in '$timePart'")
        }
        val timezone: TimeZone
        val timezoneIndicator = timePart[offset]
        if (timezoneIndicator == 'Z' || timezoneIndicator == 'z') {
            timezone = TimeZone.UTC
        } else if (timezoneIndicator == '+' || timezoneIndicator == '-') {
            val timezoneOffset = timePart.substring(offset)
            // 18-Jun-2015, tatu: Minor simplification, skip offset of "+0000"/"+00:00"
            if ("+0000" == timezoneOffset || "+00:00" == timezoneOffset) {
                timezone = TimeZone.UTC
            } else {
                val timezoneId = "UTC$timezoneOffset"
                timezone = TimeZone.of(timezoneId)
                val act = timezone.id
                if (act != timezoneId) {
                    /* 22-Jan-2015, tatu: Looks like canonical version has colons,
                     * but we may be given one without. If so, don't sweat.
                     * Yes, very inefficient. Hopefully not hit often.
                     * If it becomes a perf problem, add 'loose' comparison instead.
                     */
                    val cleaned = act.replace(":", "")
                    if (cleaned != timezoneId) {
                        throw IllegalTimeZoneException(
                            "Mismatching time zone indicator: "
                                    + timezoneId
                                    + " given, resolves to "
                                    + timezone.id
                        )
                    }
                }
            }
        } else {
            throw DateTimeFormatException("Invalid time zone indicator '$timezoneIndicator'")
        }
        return localDate.atTime(hour, minutes, seconds, nanosecond).toInstant(timezone)
    } catch (e: NumberFormatException) {
        throw DateTimeFormatException(e)
    }
}

/**
 * Check if the expected character exist at the given offset in the value.
 *
 * @param value the string to check at the specified offset
 * @param offset the offset to look for the expected character
 * @param expected the expected character
 * @return true if the expected character exist at the given offset
 */
private fun checkOffset(value: String, offset: Int, expected: Char): Boolean {
    return (offset < value.length) && (value[offset] == expected)
}

/**
 * Parse an integer located between 2 given offsets in a string
 *
 * @param value the string to parse
 * @param beginIndex the start index for the integer in the string
 * @param endIndex the end index for the integer in the string
 * @return the int
 * @throws NumberFormatException if the value is not a number
 */
@OptIn(ExperimentalStdlibApi::class)
private fun parseInt(value: String, beginIndex: Int, endIndex: Int): Int {
    if ((beginIndex < 0) || (endIndex > value.length) || (beginIndex > endIndex)) {
        throw NumberFormatException(value)
    }
    return value.substring(beginIndex, endIndex).toInt()
}

/**
 * Returns the index of the first character in the string that is not a digit, starting at offset.
 */
private fun indexOfNonDigit(string: String, offset: Int): Int {
    for (i in offset until string.length) {
        val c = string[i]
        if (c < '0' || c > '9') return i
    }
    return string.length
}
