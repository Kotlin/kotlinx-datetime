/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.pinned

import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Instant

/*
 * Oracles (oooff, there were plenty of them):
 * - Basic PBT: `start.plus(start.until(end, unit), unit)` <=`end`, `start.plus(start.periodUntil(end)) == end`
 * - Monoid's identity: `plus(0)`, `until` to itself etc.
 * - Exception types
 * - BigInteger arithmetics + manual clamping
 */
@Ignore
class InstantArithmeticFuzzFindings {

    // ===============================================================================================================
    // F2 [HIGH] until() and periodUntil() are wrong around transitions that touch local midnight
    //
    // What happens: `until` returns a count that overshoots `other` when added back, and `periodUntil` returns
    //               a period with mixed signs (P1DT-20M, the symptom of the fixed issues #484/#488).
    // Picture:      America/Nuuk, 29 March 2025 (the figure of the report, physical time left to right):
    //
    //                 start                         23:00 -> 00:00      end        start.plus(1, DAY)
    //                   |------------ // ----------------|---------------|-----------------|
    //                 28.03 23:30 -02                 +23h30m      30.03 00:10 -01    30.03 00:30 -01
    //                 01:30Z                                           +23h40m             +24h
    //
    //               `until` returns 1 day although only 23h40m have passed, and `start + 1 day` lands on the
    //               non-existent 23:30, which resolves to 20 minutes after `end`.
    // Contract:     KDoc of periodUntil: "All components ... are positive or zero if this instant is earlier than
    //               the other"; KDoc of until: the sign follows the order of the instants.
    // Where:        every year in America/Nuuk (Godthab) and America/Scoresbysund (Saturday 23:00 -> Sunday 00:00);
    //               historical gaps at midnight (Berlin 1916-04-30, Pyongyang 2018-05-04, Dhaka 2009-06-19,
    //               Singapore 1981-12-31; Toronto 1919 is known to the maintainers from the #484 discussion);
    //               date-line jumps (Kiritimati 1994, Kwajalein 1993, Fakaofo 2011); 24-hour overlaps (Alaska 1867),
    //               where `until` even returns +1 for an EARLIER instant. About 1500 "class + zone + transition"
    //               buckets in 59M iterations.
    // Root cause:   core/common/src/Instant.kt, Instant.until / Instant.periodUntil: the "one day back" correction
    //               (`delta = -1`, "won't throw: thisLdt + days <= otherLdt") assumes that the previous date at the
    //               start's time-of-day resolves to a moment before `other`. If that local date-time is itself in a
    //               gap, it is pushed forward past `other`.
    // Found by:     slices ar.until, ar.periodUntil, ar.untilByZone; oracle: the documented laws
    //               ("start.plus(until) does not pass other", sign rules). java.time's ZonedDateTime.until gives the
    //               same number, but it computes a local-time difference and promises no consistency with plus.
    // Properties:   InstantArithmeticFuzzTest.untilHasTheSignOfTheDifferenceAndDoesNotOvershoot,
    //               InstantArithmeticFuzzTest.periodUntilRoundTripsAndHasConsistentSigns
    // ===============================================================================================================

    private val nuuk = TimeZoneContext.System.get("America/Nuuk")
    // 2025-03-29: the clocks in Nuuk jump from 23:00 to 00:00 of the next day.
    private val nuukStart = LocalDateTime(2025, 3, 28, 23, 30).toInstant(nuuk, TransitionHandler.REJECT_TRANSITIONS)
    private val nuukEnd = LocalDateTime(2025, 3, 30, 0, 10).toInstant(nuuk, TransitionHandler.REJECT_TRANSITIONS)

    @Test
    fun f2_untilOvershootsAcrossGapEndingAtMidnight() {
        // only 23h40m of physical time elapse, and adding one day to `start` lands after `end`
        val days = nuukStart.until(nuukEnd, DateTimeUnit.DAY, nuuk)
        val reached = nuukStart.plus(days, DateTimeUnit.DAY, nuuk)
        assertTrue(reached <= nuukEnd, "start.until(end, DAY) = $days, but start.plus($days, DAY) = $reached > end = $nuukEnd")
    }

    @Test
    fun f2_periodUntilHasMixedSignsAcrossGapEndingAtMidnight() {
        val period = nuukStart.periodUntil(nuukEnd, nuuk) // P1DT-20M
        assertTrue(period.minutes >= 0 && period.hours >= 0 && period.days >= 0, "mixed-sign period: $period")
    }

    @Test
    fun f2_untilOvershootsAcrossDateLineJump() {
        val kiritimati = TimeZoneContext.System.get("Pacific/Kiritimati") // skipped 1994-12-31 entirely
        val start = Instant.parse("1994-12-29T22:00:00Z") // 1994-12-29T12:00 local
        val end = Instant.parse("1994-12-31T10:00:00Z")   // 1995-01-01T00:00 local, the first moment after the jump
        val days = start.until(end, DateTimeUnit.DAY, kiritimati)
        val reached = start.plus(days, DateTimeUnit.DAY, kiritimati)
        assertTrue(reached <= end, "start.until(end, DAY) = $days, but start.plus($days, DAY) = $reached > end = $end")
    }

    @Test // Okay, this one is just hilarious
    fun f2_untilHasWrongSignAcross24HourOverlap() {
        val anchorage = TimeZoneContext.System.get("America/Anchorage") // 1867-10-18 was repeated (Alaska purchase)
        val start = Instant.parse("1867-10-19T01:00:00Z")
        val end = Instant.parse("1867-10-18T22:00:00Z") // three hours EARLIER
        val days = start.until(end, DateTimeUnit.DAY, anchorage)
        assertTrue(days <= 0, "start is later than end, but start.until(end, DAY) = $days")
    }

    @Test
    fun f2_periodUntilHasMixedSignsAcross24HourOverlap() {
        val anchorage = TimeZoneContext.System.get("America/Anchorage")
        val start = Instant.parse("1867-10-19T01:00:00Z")
        val end = Instant.parse("1867-10-18T22:00:00Z")
        val period = start.periodUntil(end, anchorage) // P1DT-27H
        assertTrue(period.days <= 0 && period.hours <= 0, "start is later than end, but the period is $period")
    }

    // ===============================================================================================================
    // F3 [MEDIUM] FIND_EARLIEST_VALID_TIME breaks the laws of arithmetic in overlaps
    //
    // What happens: the handler ignores the preferred offset, so any calendar operation, even "adding nothing",
    //               moves an instant from the second occurrence of an ambiguous local time to the first one.
    // Contract:     KDoc of until/periodUntil states the laws for "the same onTransition handler" without
    //               reservations: zero for equal instants, the sign follows the order.
    // Root cause:   core/common/src/TransitionHandler.kt, FIND_EARLIEST_VALID_TIME: overlaps always use offsetBefore.
    // Options:      honor `preferredOffset` in overlaps, or document that the laws only hold for handlers that do.
    // Found by:     slices ar.until, ar.periodUntil with a random handler; oracle: the zero laws.
    // Property:     InstantArithmeticFuzzTest.zeroLawsHoldForLenientHandlers
    // ===============================================================================================================

    private val berlin = TimeZoneContext.System.get("Europe/Berlin")
    // 2024-10-27T02:30 local, second occurrence (offset +01:00)
    private val inOverlap = Instant.parse("2024-10-27T01:30:00Z")

    @Test
    fun f3_plusZeroIsNotIdentityWithFindEarliestValidTime() {
        assertEquals(inOverlap, inOverlap.plus(0, DateTimeUnit.DAY, berlin, TransitionHandler.FIND_EARLIEST_VALID_TIME))
    }

    @Test
    fun f3_periodUntilItselfIsNotZeroWithFindEarliestValidTime() {
        // documented: "Exactly zero if this instant is equal to the other"; actual: PT1H
        assertEquals(DateTimePeriod(), inOverlap.periodUntil(inOverlap, berlin, TransitionHandler.FIND_EARLIEST_VALID_TIME))
    }

    @Test
    fun f3_untilHasWrongSignWithFindEarliestValidTime() {
        val oneMinuteEarlier = Instant.parse("2024-10-27T01:29:00Z")
        val days = inOverlap.until(oneMinuteEarlier, DateTimeUnit.DAY, berlin, TransitionHandler.FIND_EARLIEST_VALID_TIME)
        assertTrue(days <= 0, "other is earlier, but until(..., DAY) = $days")
    }

    // ===============================================================================================================
    // F1 [LOW] Instant.plus/minus(Long, TimeBased) clamps inside the valid range
    //
    // What happens: the result is Instant.MAX/MIN although the exact result is representable.
    // Contract:     "The return value is clamped to the boundaries of Instant if the result exceeds them."
    // Root cause:   core/common/src/Instant.kt, Instant.plus(Long, TimeBased): `plus(seconds.seconds)` goes through
    //               kotlin.time.Duration, which saturates to INFINITE above ~146 million years (4.6e15 s), while two
    //               valid Instants can be 2e9 years apart. `until` computes the same distances correctly.
    // Found by:     slice ar.timeMath; oracle: exact BigInteger arithmetic.
    // Property:     InstantArithmeticFuzzTest.plusTimeBasedUnitIsExactOrClamped
    // ===============================================================================================================

    @Test
    fun f1_plusTimeBasedUnitSaturatesInsideInstantRange() {
        val seconds = 5_000_000_000_000_000L // ~158 million years: far inside the Instant range
        assertEquals(
            Instant.fromEpochSeconds(seconds),
            Instant.fromEpochSeconds(0).plus(seconds, DateTimeUnit.SECOND)
        )
    }

    @Test
    fun f1_minusTimeBasedUnitSaturatesInsideInstantRange() {
        val seconds = 5_000_000_000_000_000L
        assertEquals(
            Instant.fromEpochSeconds(-seconds),
            Instant.fromEpochSeconds(0).minus(seconds, DateTimeUnit.SECOND)
        )
    }

    // ===============================================================================================================
    // F17 [LOW, documentation] Strict handlers make arithmetic throw IllegalArgumentException
    //
    // What happens: with REJECT_TRANSITIONS / REQUIRE_RESOLVED, plus/until/periodUntil throw the handler's
    //               IllegalArgumentException, even for a no-op on a valid Instant and for intermediate values
    //               of until/periodUntil that are neither an input nor a result.
    // Contract:     `@throws` of the arithmetic functions lists only DateTimeArithmeticException.
    // Found by:     slice ar.strict; oracle: documented exception types.
    // ===============================================================================================================

    @Test
    fun f17_plusZeroThrowsWithRejectTransitions() {
        assertEquals(inOverlap, inOverlap.plus(0, DateTimeUnit.DAY, berlin, TransitionHandler.REJECT_TRANSITIONS))
    }
}
