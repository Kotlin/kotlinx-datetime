/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.pinned

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.test.*

/**
 * Oracle: comparison with `java.time.format.DateTimeFormatter.ofPattern` taking into account that
 * `byUnicodePattern` is a subset and there are some documented divergences
 */
@Ignore
@OptIn(FormatStringsInDatetimeFormats::class)
class UnicodePatternFuzzFindings {

    // ===============================================================================================================
    // F13 [MEDIUM] An unterminated '[' silently drops everything in front of it
    //
    // What happens: the pattern is accepted, and the format loses fields both when formatting and when parsing:
    //               "HH:mm[:ss" formats 01:02:03 as ":03". java.time closes the section at the end of the pattern.
    // Found by:     slice up.unicode; oracle: java.time DateTimeFormatter output.
    // Property:     UnicodePatternFuzzTest.outputMatchesJavaTime
    // ===============================================================================================================

    @Test
    fun f13_unterminatedOptionalSectionDropsThePrefix() {
        val formatted = try {
            LocalTime.Format { byUnicodePattern("HH:mm[:ss") }.format(LocalTime(1, 2, 3))
        } catch (e: IllegalArgumentException) {
            return // rejecting the pattern would be fine as well
        }


        assertEquals("01:02:03", formatted)
    }

    @Test
    fun f13_nestedUnterminatedOptionalSectionDropsThePrefix() {
        val formatted = try {
            LocalTime.Format { byUnicodePattern("HH[:mm[:ss]") }.format(LocalTime(1, 2, 3))
        } catch (e: IllegalArgumentException) {
            return
        }
        assertEquals("01:02:03", formatted)
    }

    // ===============================================================================================================
    // F14 [LOW] A stray ']' throws NoSuchElementException("List is empty.")
    //
    // Contract:     byUnicodePattern documents IllegalArgumentException for invalid patterns.
    // Found by:     slice up.unicode; oracle: documented exception types.
    // Property:     UnicodePatternFuzzTest.patternErrorsAreDocumentedExceptions
    // ===============================================================================================================

    @Test
    fun f14_strayClosingBracketThrowsUndocumentedException() {
        assertFailsWith<IllegalArgumentException> { LocalTime.Format { byUnicodePattern("HH:mm]") } }
    }

    // ===============================================================================================================
    // F15 [LOW] An unterminated quoted literal is accepted
    //
    // What happens: "HH:mm'abc" formats as "01:02abc", "'HH" as "HH": a typo silently turns directives into a literal.
    //               java.time: "Pattern ends with an incomplete string literal".
    //               (The characters '{', '}', '#', reserved only by java.time, are accepted as literals; that
    //               difference is treated as deliberate.)
    // Found by:     slice up.unicode; oracle: java.time DateTimeFormatter pattern validation.
    // Property:     UnicodePatternFuzzTest.acceptedPatternsAreValidForJavaTime
    // ===============================================================================================================

    @Test
    fun f15_unterminatedQuoteIsAccepted() {
        assertFailsWith<IllegalArgumentException> { LocalTime.Format { byUnicodePattern("HH:mm'abc") } }
    }

    // ===============================================================================================================
    // F21 [LOW] A negative offset shorter than a minute is printed as "-00", "-0000", "-00:00"
    //
    // What happens: directives that do not print seconds still take the sign of the whole offset:
    //               UtcOffset(seconds = -30) is "-00:00" with `xxx` and "-0000" with UtcOffset.Formats.FOUR_DIGITS.
    //               java.time prints "+00:00" and "+0000": a value that is printed as zero has no minus.
    // Contract:     ISO 8601 has no negative zero offset, and in RFC 3339 / RFC 5322 "-00:00" and "-0000" mean
    //               "the offset is unknown". The text parses back as UtcOffset.ZERO.
    // Where:        only offsets in -00:00:59..-00:00:01. No zone of the IANA database has one (checked for the
    //               bundled 2026f and the JDK's 2026b), so this takes a hand-made UtcOffset.
    // Found by:     Jazzer target UnicodeFuzzer (DateTimeComponents with offsets); oracle: java.time DateTimeFormatter.
    // Property:     none in the repository.
    // ===============================================================================================================

    @Test
    fun f21_negativeSubMinuteOffsetIsPrintedAsMinusZero() {
        val expected = DateTimeFormatter.ofPattern("xxx").format(ZoneOffset.ofTotalSeconds(-30))
        assertEquals(expected, UtcOffset.Format { byUnicodePattern("xxx") }.format(UtcOffset(seconds = -30)))
    }

    @Test
    fun f21_fourDigitsFormatPrintsMinusZero() {
        assertEquals("+0000", UtcOffset.Formats.FOUR_DIGITS.format(UtcOffset(seconds = -1)))
    }
}
