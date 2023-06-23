/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.internal.*
import kotlin.test.*

class TimeZoneRulesTest {
    @Test
    fun ruleStrings() {
        val rules = readTzFile(EuropeBerlinTzFile2023c).toTimeZoneRules()
        // first, check that for the future, there are no explicitly defined transitions
        assertTrue(rules.transitionEpochSeconds.all {
            Instant.fromEpochSeconds(it) < LocalDateTime(2038, 1, 1, 0, 0).toInstant(UtcOffset.ZERO)
        })
        // Next, check that even after that, the rules behave correctly.
        // They are that the DST starts at 02:00 on the last Sunday in March and ends at 03:00
        // on the last Sunday in October.
        val dstStartTime = LocalDateTime(2040, 3, 25, 2, 0)
        val infoAtDstStart = rules.infoAtDatetime(dstStartTime)
        assertTrue(infoAtDstStart is OffsetInfo.Gap, "Expected Gap, got $infoAtDstStart")
        val dstEndTime = LocalDateTime(2040, 10, 28, 3, 0)
        val infoAtDstEnd = rules.infoAtDatetime(dstEndTime)
        assertTrue(infoAtDstEnd is OffsetInfo.Overlap, "Expected Overlap, got $infoAtDstEnd")
    }

    @Test
    fun ruleStringWithNonLastDayOfWeek() {
        val ruleString = "AST4ADT,M3.2.0,M11.1.0\n" // Atlantic/Bermuda
        val recurringRules =
            PosixTzString.readIfPresent(BinaryDataReader(ruleString.encodeToByteArray()))!!.toRecurringZoneRules()!!
        val rules = TimeZoneRules(UtcOffset(hours = -4), recurringRules)
        val dstStartTime = LocalDateTime(2020, 3, 8, 2, 1)
        val infoAtDstStart = rules.infoAtDatetime(dstStartTime)
        assertTrue(infoAtDstStart is OffsetInfo.Gap, "Expected Gap, got $infoAtDstStart")
        val dstEndTime = LocalDateTime(2020, 11, 1, 1, 1)
        val infoAtDstEnd = rules.infoAtDatetime(dstEndTime)
        assertTrue(infoAtDstEnd is OffsetInfo.Overlap, "Expected Overlap, got $infoAtDstEnd")
    }
}
