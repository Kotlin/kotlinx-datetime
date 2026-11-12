/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.pinned

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*
import kotlin.time.Instant

/**
 * Oracles:
 * - Comparing j.u.t. fast-paths with actual parser
 * - Trivial idempotence tests
 * - Some formats only accept an input that can be output'ed
 * - format throws in accordance to its KDoc
 */
@Ignore
class FormatFuzzFindings {

    // ===============================================================================================================
    // F4 [HIGH] A year with 10 or more digits silently overflows Int
    //
    // What happens: the library's parser returns a wrong, valid value: "+8889908539-12-04" -> +299973947-12-04
    //               (8889908539 mod 2^32 == 299973947).
    // Root cause:   core/common/src/internal/format/parser/NumberConsumer.kt, parseAsciiIntOrNull: overflow is
    //               detected by `result < 0`, but the wrapped value is positive for half of the inputs.
    // Who is hit:   every custom format with `year()`, `X.Formats.ISO.parse`, `DateTimeComponents`. On the JVM the
    //               default `LocalDate.parse(s)` goes through java.time and rejects such strings; on the other
    //               platforms the default `parse` uses this very parser.
    // Found by:     slice pf.isoDiff; oracle: java.time fast path vs the library parser on mutated ISO strings.
    // Property:     FormatFuzzTest.jvmFastPathAgreesWithLibraryParserOnNearValidIsoStrings
    // ===============================================================================================================

    @Test
    fun f4_localDateYearOverflowIsSilentlyWrapped() {
        assertNull(LocalDate.Formats.ISO.parseOrNull("+8889908539-12-04"))
    }

    @Test
    fun f4_localDateBasicFormatYearOverflowIsSilentlyWrapped() {
        assertNull(LocalDate.Formats.ISO_BASIC.parseOrNull("+88899085391204"))
    }

    @Test
    fun f4_localDateTimeYearOverflowIsSilentlyWrapped() {
        assertNull(LocalDateTime.Formats.ISO.parseOrNull("-4781859834-04-17T23:59:59.999999999"))
    }

    @Test
    fun f4_yearMonthYearOverflowIsSilentlyWrapped() {
        assertNull(YearMonth.Formats.ISO.parseOrNull("-5082212582-08"))
    }

    @Test
    fun f4_instantYearOverflowIsSilentlyWrapped() {
        // returns +299973947-12-04T00:00:00Z
        assertNull(Instant.parseOrNull("+8889908539-12-04T00:00:00Z", DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET))
    }

    @Test
    fun f4_customFormatYearOverflowIsSilentlyWrapped() {
        val format = LocalDate.Format { year(); char('/'); monthNumber(); char('/'); day() }
        assertNull(format.parseOrNull("+8889908539/12/04"))
    }

    // ===============================================================================================================
    // F12 [MEDIUM] amPmHour accepts hours outside 1..12
    //
    // What happens: "13:00 AM" is 13:00, "00:30 PM" is 12:30, "23:59 AM" is 23:59 (and 23:59 formats as "11:59 PM").
    // Root cause:   core/common/src/format/LocalTimeFormat.kt, IncompleteLocalTime.toLocalTime():
    //               `(if (h == 12) 0 else h) + (if PM 12 else 0)` without checking `h in 1..12`. The field's
    //               FieldSpec declares min/max, but parsing does not apply them.
    // Found by:     slice pf.canon; oracle: for a format without alternatives and with fixed-width fields, an accepted
    //               string must be exactly what the formatter prints for the parsed value.
    // Property:     FormatFuzzTest.fixedWidthFormatsAcceptOnlyTheirOwnOutput
    // ===============================================================================================================

    private val amPmFormat = LocalTime.Format { amPmHour(); char(':'); minute(); char(' '); amPmMarker("AM", "PM") }

    @Test
    fun f12_amPmHourAbove12IsAccepted() {
        assertNull(amPmFormat.parseOrNull("13:00 AM"))
    }

    @Test
    fun f12_amPmHourZeroIsAccepted() {
        assertNull(amPmFormat.parseOrNull("00:30 PM"))
    }

    @Test
    fun f12_acceptedStringsAreNotTheFormattersOutput() {
        // for a format without alternatives and with fixed-width fields, a string is either rejected
        // or is exactly what the formatter prints for the parsed value
        val parsed = amPmFormat.parseOrNull("23:59 AM") ?: return
        assertEquals("23:59 AM", amPmFormat.format(parsed), "\"23:59 AM\" was parsed as $parsed")
    }

    // ===============================================================================================================
    // F5 [LOW] The JVM fast path and the library parser accept different strings
    //
    // What happens: `X.parse(s)` on the JVM is delegated to java.time, `X.Formats.ISO.parse(s)` (and `X.parse(s)` on
    //               every other platform) uses the library parser, so `parse` depends on the platform (cf. #369):
    //                 "20:38:46."            java.time: 20:38:46              library: error
    //                 "1909-08-25T03:22:28." java.time: 1909-08-25T03:22:28   library: error
    //                 "-0000-05-28"          java.time: error                 library: 0000-05-28
    //                 "+000000010074-07-01T22:16:56+12:18" (Instant) stdlib: error, library: +10074-07-01T09:58:56Z
    // Found by:     slices pf.isoDiff, di.deprecated; oracle: fast path vs generic path.
    // Property:     FormatFuzzTest.jvmFastPathAgreesWithLibraryParserOnNearValidIsoStrings
    //               (94 failures of this class in the census, next to 93 of F4)
    // ===============================================================================================================

    @Test
    fun f5_trailingDotIsAcceptedOnlyByTheJvmFastPath() {
        assertEquals(LocalTime.Formats.ISO.parseOrNull("20:38:46."), LocalTime.parseOrNull("20:38:46."))
    }

    @Test
    fun f5_trailingDotInLocalDateTimeIsAcceptedOnlyByTheJvmFastPath() {
        assertEquals(
            LocalDateTime.Formats.ISO.parseOrNull("1909-08-25T03:22:28."),
            LocalDateTime.parseOrNull("1909-08-25T03:22:28.")
        )
    }

    @Test
    fun f5_negativeZeroYearIsRejectedOnlyByTheJvmFastPath() {
        assertEquals(LocalDate.Formats.ISO.parseOrNull("-0000-05-28"), LocalDate.parseOrNull("-0000-05-28"))
    }

    // ===============================================================================================================
    // TODO Seva: disgaree, it's [HIGH]
    // F6 [LOW] parseOrNull throws DateTimeArithmeticException
    //
    // What happens: a day-of-year beyond the last supported date escapes as DateTimeArithmeticException, which is not
    //               an IllegalArgumentException, so it passes through both parse and parseOrNull.
    // Root cause:   core/common/src/format/LocalDateFormat.kt:132: the day of year is validated through
    //               `LocalDate(year, 1, 1).plus(dayOfYear - 1, DAY)`, which overflows on the maximum year
    //               before the check.
    // Found by:     slice pf.fmtSeparated (mutated output of random formats); oracle: documented exception types.
    // Property:     FormatFuzzTest.randomBuilderFormatsRoundTripAndFailCleanly
    // ===============================================================================================================

    @Test
    fun f6_parseOrNullThrowsForDayOfYearBeyondTheLastSupportedDate() {
        val format = LocalDate.Format { year(); char('-'); dayOfYear() }
        assertNull(format.parseOrNull("+999999999-366")) // +999999999 is not a leap year
    }

    // ===============================================================================================================
    // F18 [LOW] Formatting is not idempotent: an optional section with a truncated fraction
    //
    // What happens: the section is printed because the nanosecond is non-zero, but the printed digits are zeros;
    //               parsing the output and formatting again omits the section.
    // Found by:     slice pf.optional; oracle: format(parse(format(x))) == format(x).
    // ===============================================================================================================

    @Test
    fun f18_optionalSectionWithTruncatedFractionIsNotIdempotent() {
        val format = LocalTime.Format {
            hour(); char(':'); minute(); char(':'); second()
            optional { char('.'); secondFraction(1, 6) }
        }
        val formatted = format.format(LocalTime(10, 50, 43, 1)) // "10:50:43.0"; parsed and formatted again: "10:50:43"
        assertEquals(formatted, format.format(format.parse(formatted)))
    }

    // ===============================================================================================================
    // TODO Seva: disgaree, it's [HIGH]
    // F20 [MEDIUM] A month name for a month number outside 1..12 is "formatted" as an error message
    //
    // What happens: DateTimeComponents accepts monthNumber in 0..99; formatting it with `monthName` returns the text
    //               "The value 13 of monthNumber does not have a corresponding string representation" in place of
    //               the month instead of throwing.
    // Contract:     the KDoc of `DateTimeFormat<DateTimeComponents>.format { }` gives exactly this case as its example
    //               of IllegalStateException: "trying to format monthName using a monthNumber value of 20".
    // Root cause:   core/common/src/internal/format/FieldFormatDirective.kt, getStringValue:
    //               `values.getOrNull(it - field.minValue) ?: "The value ... string representation"` returns the
    //               message as the value.
    // Found by:     minimizing F19 (the same directive); oracle: the documented exception.
    // Property:     none in the repository.
    // ===============================================================================================================

    @Test
    fun f20_monthNameOfAnInvalidMonthNumberIsPrintedAsAnErrorMessage() {
        val format = DateTimeComponents.Format { monthName(MonthNames.ENGLISH_ABBREVIATED) }
        assertFailsWith<IllegalStateException> { format.format { monthNumber = 13 } }
    }

    @Test
    fun f20_rfc1123PrintsTheErrorMessageInsideTheDate() {
        // "Mon, 1 The value 13 of monthNumber does not have a corresponding string representation 2024 00:00:00 GMT"
        assertFailsWith<IllegalStateException> {
            DateTimeComponents.Formats.RFC_1123.format {
                setDateTime(LocalDateTime(2024, 1, 1, 0, 0)); setOffset(UtcOffset.ZERO); monthNumber = 13
            }
        }
    }

    // ===============================================================================================================
    // TODO Seva: disgaree, it's [HIGH]
    // F19 [LOW] parse and parseOrNull throw ArrayIndexOutOfBoundsException for a month number outside 1..12
    //
    // What happens: in a format with both the month number and the month name, the number is read first, and the
    //               name is then compared with `names[number - 1]` without a range check:
    //               "13/01/2024 Jan" -> ArrayIndexOutOfBoundsException: Index 12 out of bounds for length 12.
    // Contract:     parse throws IllegalArgumentException only; parseOrNull never throws.
    // Root cause:   core/common/src/internal/format/FieldFormatDirective.kt:99,
    //               NamedUnsignedIntFieldFormatDirective.AssignableString.trySetWithoutReassigning indexes `values`
    //               with the value that the field already has.
    // Found by:     Jazzer target FormatFuzzer (random format programs, repeated fields allowed);
    //               oracle: documented exception types.
    // Property:     none in the repository; the Jazzer targets are kept in fuzz/misc/jazzer.
    // ===============================================================================================================

    private val monthNumberAndName = LocalDate.Format {
        monthNumber(); char('/'); day(); char('/'); year(); char(' '); monthName(MonthNames.ENGLISH_ABBREVIATED)
    }

    @Test
    fun f19_parseThrowsArrayIndexOutOfBoundsForMonthNumberAboveTwelve() {
        assertFailsWith<IllegalArgumentException> { monthNumberAndName.parse("13/01/2024 Jan") }
    }

    @Test
    fun f19_parseOrNullThrowsForMonthNumberZero() {
        assertNull(monthNumberAndName.parseOrNull("00/01/2024 Jan"))
    }

    // ===============================================================================================================
    // TODO Seva: disgaree, it's [MEDIUM]
    // F22 [LOW] DateTimeFormat.format documents IllegalArgumentException, but throws IllegalStateException
    //
    // What happens: formatting DateTimeComponents with an unset field throws IllegalStateException("Field second is
    //               not set"). `DateTimeFormat.format(value)` and `formatTo` document IllegalArgumentException
    //               "if the value does not contain all the information required by the format"; only the extension
    //               `format { }` documents IllegalStateException.
    // Consequence:  a value parsed with an alternative left out cannot be printed with the same format: with
    //               `alternativeParsing({ }) { ... }` (and `[...]` of byUnicodePattern) "12:30" parses, and
    //               formatting the result throws. `optional { }` assigns the default and prints "12:30".
    // Found by:     Jazzer targets FormatFuzzer and UnicodeFuzzer; oracle: documented exception types.
    // Property:     none in the repository.
    // ===============================================================================================================

    @Test
    fun f22_formatThrowsIllegalStateExceptionForUnsetFields() {
        val format = DateTimeComponents.Format {
            hour(); char(':'); minute(); alternativeParsing({ }) { char(':'); second() }
        }
        val parsed = format.parse("12:30")
        assertFailsWith<IllegalArgumentException> { format.format(parsed) }
    }
}
