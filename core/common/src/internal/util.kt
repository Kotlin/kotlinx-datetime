/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

internal fun Char.isAsciiLetter(): Boolean = this in 'A'..'Z' || this in 'a'..'z'

internal fun Char.asciiDigitToInt(): Int = this - '0'

/** Working around the JSR-310 behavior of failing to parse long year numbers even when they start with leading zeros */
private fun removeLeadingZerosFromLongYearForm(input: String, minStringLengthAfterYear: Int): String {
    // the smallest string where the issue can occur is "+00000002024", its length is 12
    val failingYearStringLength = 12
    // happy path: the input is too short or the first character is not a sign, so the year is not in the long form
    if (input.length < failingYearStringLength + minStringLengthAfterYear || input[0] !in "+-") return input
    // the year is in the long form, so we need to remove the leading zeros
    // find the `-` that separates the year from the month
    val yearEnd = input.indexOf('-', 1)
    // if (yearEnd == -1) return input // implied by the next condition
    // if the year is too short, no need to remove the leading zeros, and if the string is malformed, just leave it
    if (yearEnd < failingYearStringLength) return input
    // how many leading zeroes are there?
    var leadingZeros = 0
    while (input[1 + leadingZeros] == '0') leadingZeros++ // no overflow, we know `-` is there
    // even if we removed all leading zeros, the year would still be too long
    if (yearEnd - leadingZeros >= failingYearStringLength) return input
    // we remove just enough leading zeros to make the year the right length
    // We need the resulting length to be `failYearStringLength - 1`, the current length is `yearEnd`.
    // The difference is `yearEnd - failingYearStringLength + 1` characters to remove.
    // Both the start index and the end index are shifted by 1 because of the sign.
    return input.removeRange(startIndex = 1, endIndex = yearEnd - failingYearStringLength + 2)
}

internal fun removeLeadingZerosFromLongYearFormLocalDate(input: String) =
    removeLeadingZerosFromLongYearForm(input.toString(), 6) // 6 = "-01-02".length

internal fun removeLeadingZerosFromLongYearFormLocalDateTime(input: String) =
    removeLeadingZerosFromLongYearForm(input.toString(), 12) // 12 = "-01-02T23:59".length

internal fun removeLeadingZerosFromLongYearFormYearMonth(input: String) =
    removeLeadingZerosFromLongYearForm(input.toString(), 3) // 3 = "-01".length
