/*
 * Copyright 2019-2026 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.zoneinfo.pinned

import kotlinx.datetime.*
import kotlinx.datetime.zoneinfo.BundledTimeZoneContext
import kotlin.test.*
import kotlin.time.Instant

class BundledTimeZoneFuzzFindings {

    // ===============================================================================================================
    // F16 [LOW] offsetAt throws at the edges of the Instant range
    //
    // What happens: a bundled rule-based zone throws DateTimeArithmeticException for valid instants whose local
    //               date-time is outside the LocalDateTime range; the system database returns the offset
    //               (on JDK 17+; see SystemTimeZoneFindings in core for JDK 8).
    // Contract:     `TimeZone.offsetAt` documents no exceptions.
    // Root cause:   core/common/src/internal/tz/TimeZoneRules.kt, RecurringZoneRules.infoAtInstant converts the
    //               instant to a LocalDateTime to learn its year.
    // Found by:     slice tz.extremeBundled; oracle: totality on extreme values, Bundled vs System.
    // Property:     BundledTimeZoneFuzzTest.everyInstantHasAnOffset
    // ===============================================================================================================

    @Test
    fun f16_offsetAtThrowsForTheMaximumInstant() {
        val max = Instant.fromEpochSeconds(Long.MAX_VALUE)
        val berlin = BundledTimeZoneContext.get("Europe/Berlin")
        assertFailsWith<Throwable> { berlin.offsetAt(max) }
    }

    @Test
    fun f16_offsetAtThrowsForTheLastInstantThatFitsLocalDateTimeInUtc() {
        val instant = LocalDateTime(999_999_999, 12, 31, 23, 59, 59).toInstant(UtcOffset.ZERO)
        val berlin = BundledTimeZoneContext.get("Europe/Berlin")
        assertFailsWith<Throwable> { berlin.offsetAt(instant) }
    }

    // ===============================================================================================================
    // F10 [LOW] `is FixedOffsetTimeZone` depends on the database
    //
    // What happens: region identifiers with constant rules (Etc/UTC, Etc/GMT+5, Etc/Greenwich) are
    //               FixedOffsetTimeZone in the system database on the JVM (`TimeZone.ofZone`), but RuleBasedTimeZone
    //               in the bundled one, so code that checks the type behaves differently depending on the context.
    //               Parsing of the fixed-offset identifiers themselves ("UTC+3", "GMT-01:30", "+0530") agreed
    //               on 2M cases.
    // Found by:     slice zi.ids; oracle: Bundled vs System.
    // Property:     BundledTimeZoneFuzzTest.identifiersMeanTheSameKindOfZoneInBothDatabases
    // ===============================================================================================================

    @Test
    fun f10_fixedRuleRegionsAreNotFixedOffsetTimeZones() {
        assertNotEquals(
            TimeZoneContext.System.get("Etc/UTC") is FixedOffsetTimeZone,
            BundledTimeZoneContext.get("Etc/UTC") is FixedOffsetTimeZone,
        )
    }
}
