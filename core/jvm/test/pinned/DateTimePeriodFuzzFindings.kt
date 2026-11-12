/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.pinned

import kotlinx.datetime.*
import kotlin.test.*

/*
 * Results of pediod fuzzing and pb-testing that I suspect are bugs.
 *
 * Oracles used:
 * - `parseOrNull` ~ `parse`, `parseOrNull` should not fail
 * - `parse` should not throw unspecified exceptions
 * - `parse(dtp.toString())` == `dtp`
 * - Identical behaviour for reference grammar implementation
 *     - Reference grammar -- generated from KDoc
 */
@Ignore
class DateTimePeriodFuzzFindings {

    // Regex grammar duct-taped from KDoc. Not used in tests, only to denote the oracle used
    private val grammar = Regex(
        "^([+-])?P(?:([+-]?\\d+)Y)?(?:([+-]?\\d+)M)?(?:([+-]?\\d+)W)?(?:([+-]?\\d+)D)?" +
                "(?:T(?:([+-]?\\d+)H)?(?:([+-]?\\d+)M)?(?:([+-]?\\d+)(?:\\.(\\d{1,9}))?S)?)?$"
    )

    // ===============================================================================================================
    // F7 [MEDIUM] parse() and parseOrNull() throw StringIndexOutOfBoundsException
    //
    // What happens: truncated input crashes instead of IllegalArgumentException / null.
    // Root cause:   core/common/src/DateTimePeriod.kt, parseImpl: `text[i]` is read without checking the end of the
    //               string after a fraction; after a trailing sign the out-of-bounds read happens while building the
    //               error message ("A number expected after '${text[i]}'"), so parseOrNull fails as well.
    // Found by:     slice pd.parse; oracle: documented exception types, parseOrNull never throws.
    // Property:     DateTimePeriodFuzzTest.parseFailsOnlyWithTheDocumentedException
    // ===============================================================================================================

    @Test
    fun f7_parseOrNullThrowsOnFractionWithoutDesignator() {
        assertNull(DateTimePeriod.parseOrNull("PT1.5"))
    }

    @Test
    fun f7_parseOrNullThrowsOnTrailingSign() {
        assertNull(DateTimePeriod.parseOrNull("P-"))
    }

    @Test
    fun f7_parseThrowsUndocumentedExceptionOnTruncatedInput() {
        for (input in listOf("PT1.5", "P-", "P+", "-P-", "P1Y-")) {
            assertFailsWith<IllegalArgumentException>("parse(\"$input\")") { DateTimePeriod.parse(input) }
        }
    }

    // ===============================================================================================================
    // F9 [MEDIUM] toString() of Int.MIN_VALUE components
    //
    // What happens: `days * sign` overflows, the minus stays both in the prefix and at the number:
    //               DatePeriod(days = Int.MIN_VALUE).toString() == "-P-2147483648D". By the documented grammar this
    //               string means PLUS 2^31 days, and it does not parse back.
    // Consequence:  the ISO 8601 serializers of DateTimePeriod/DatePeriod do not round-trip such values
    //               (found by the serialization slice; the component serializers are fine).
    // Found by:     slices pd.toString, sr.roundTrip; oracle: round trip + the reference grammar.
    // Property:     DateTimePeriodFuzzTest.toStringDenotesTheSameValueAndParsesBack
    // ===============================================================================================================

    @Test
    fun f9_toStringOfMinDaysDoesNotRoundTrip() {
        val period = DatePeriod(days = Int.MIN_VALUE)
        assertEquals(period, DateTimePeriod.parse(period.toString()), "toString() = \"$period\"")
    }

    @Test
    fun f9_toStringOfMinYearsDoesNotRoundTrip() {
        val period = DateTimePeriod(years = Int.MIN_VALUE)
        assertEquals(period, DateTimePeriod.parse(period.toString()), "toString() = \"$period\"")
    }

    // ===============================================================================================================
    // F8 [LOW] Strings outside the documented grammar are accepted
    //
    // What happens: "PY", "PD", "PTS", "P1YM" (a designator without a number) -> P0D / P1Y;
    //               "PT1.S" (empty fraction) -> PT1S; "PT.5S" (no integer part) -> PT0.5S;
    //               "PT1,5S" -> PT1.5S (the KDoc names only '.', though ISO 8601 allows a comma);
    //               "P1y" and "P1DT1h" are accepted while "p1Y" and "P1Dt1H" are rejected.
    // Contract:     KDoc of DateTimePeriod.parse: "the number of years, followed by Y", ...,
    //               "The fractional part is separated with a `.`".
    // Found by:     slice pd.parse; oracle: the reference grammar.
    // Property:     DateTimePeriodFuzzTest.parseImplementsTheDocumentedGrammar
    // ===============================================================================================================

    @Test
    fun f8_designatorWithoutNumberIsAccepted() {
        for (input in listOf("PY", "PD", "PTS", "PTH", "P1YM")) {
            assertNull(DateTimePeriod.parseOrNull(input), "\"$input\" has a designator without a number")
        }
    }

    @Test
    fun f8_emptyOrIntegerlessFractionIsAccepted() {
        for (input in listOf("PT1.S", "PT.5S")) {
            assertNull(DateTimePeriod.parseOrNull(input), "\"$input\"")
        }
    }

    @Test
    fun f8_caseSensitivityIsInconsistent() {
        assertEquals(
            DateTimePeriod.parseOrNull("P1DT1h") != null,
            DateTimePeriod.parseOrNull("P1Dt1H") != null,
            "P1DT1h is accepted, but P1Dt1H is rejected"
        )
    }
}
