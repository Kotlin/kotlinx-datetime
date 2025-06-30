/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.internal.OffsetInfo
import kotlinx.datetime.internal.TimeZoneRulesFoundation
import kotlinx.datetime.internal.TzdbOnFilesystem
import kotlinx.datetime.internal.getAvailableZoneIds
import kotlinx.datetime.internal.getAvailableZoneIdsFoundation
import kotlinx.datetime.internal.timeZoneById
import kotlinx.datetime.internal.timeZoneByIdFoundation
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plusSeconds
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.datetime.test.TimeZoneNativeTest.OffsetInfoType.*

class TimeZoneNativeTest {

    // getAvailableZoneIdsFoundation() tests

    @Test
    fun getAvailableZoneIdsReturnsValidTimezoneSet() {
        assertReturnsNonEmptySetOfTimezoneStrings(getAvailableZoneIds())
    }

    @Test
    fun getAvailableZoneIdsFoundationReturnsValidTimezoneSet() {
        assertReturnsNonEmptySetOfTimezoneStrings(getAvailableZoneIdsFoundation())
    }

    @Test
    fun getAvailableZoneIdsContainsExpectedTimezoneIDs() {
        assertAvailableZoneIdsContainsExpectedTimezoneIDs(getAvailableZoneIds())
    }

    @Test
    fun getAvailableZoneIdsFoundationContainsExpectedTimezoneIDs() {
        assertAvailableZoneIdsContainsExpectedTimezoneIDs(getAvailableZoneIdsFoundation())
    }

    private fun assertReturnsNonEmptySetOfTimezoneStrings(zoneIds: Set<String>) {
        assertTrue(zoneIds.isNotEmpty(), "Zone IDs should not be empty")
        assertTrue(zoneIds.all { it.isNotBlank() }, "All zone IDs should be non-blank")
        assertTrue("UTC" in zoneIds || "GMT" in zoneIds, "Should contain UTC or GMT")
        assertTrue(zoneIds.any { it.contains("America") }, "Should contain America timezones")
        assertTrue(zoneIds.any { it.contains("Europe") }, "Should contain Europe timezones")
    }

    private val validTimeZones = listOf("America/New_York", "Europe/London", "Asia/Tokyo", "Australia/Sydney")

    private fun assertAvailableZoneIdsContainsExpectedTimezoneIDs(zoneIds: Set<String>) {
        assertTrue(validTimeZones.all { it in zoneIds }, "Should contain all common timezone")
    }

    // timeZone.offsetAt(Instant) tests

    @Test
    fun shouldHandleDstSpringForwardTransitionConsistentlyBetweenImplementations() {
        verifyDstTransitionConsistency(
            transitionTime = LocalDateTime(2025, 3, 9, 1, 59, 59),
            transitionName = "DST spring forward transition"
        )
    }

    @Test
    fun shouldHandleDstFallBackTransitionConsistentlyBetweenImplementations() {
        verifyDstTransitionConsistency(
            transitionTime = LocalDateTime(2025, 11, 2, 1, 59, 59),
            transitionName = "DST fall back transition"
        )
    }

    private fun verifyDstTransitionConsistency(transitionTime: LocalDateTime, transitionName: String) {
        val zoneId = "America/New_York"
        val regularTz = timeZoneById(zoneId)
        val foundationTz = timeZoneByIdFoundation(zoneId)

        val instantBefore = transitionTime.toInstant(regularTz)
        val instantAfter = transitionTime.plusSeconds(1).toInstant(regularTz)

        val offsetBefore = regularTz.offsetAt(instantBefore)
        val offsetAfter = regularTz.offsetAt(instantAfter)

        assertNotEquals(
            offsetBefore,
            offsetAfter,
            "Expected offset change during $transitionName"
        )

        assertEquals(
            offsetBefore,
            foundationTz.offsetAt(instantBefore),
            "Regular and Foundation implementations should have same offset before transition"
        )

        assertEquals(
            offsetAfter,
            foundationTz.offsetAt(instantAfter),
            "Regular and Foundation implementations should have same offset after transition"
        )
    }

    @Test
    fun shouldHandleNoTransitionPeriodConsistentlyBetweenImplementations() {
        val summerTime = LocalDateTime(2025, 7, 15, 12, 30, 45)
        val zoneId = "America/New_York"

        val regularTz = timeZoneById(zoneId)
        val foundationTz = timeZoneByIdFoundation(zoneId)

        val testPoints = listOf(
            summerTime,
            summerTime.plusSeconds(60 * 60),
            summerTime.plusSeconds(24 * 60 * 60),
            summerTime.plusSeconds(7 * 24 * 60 * 60)
        )

        for (dateTime in testPoints) {
            val instant = dateTime.toInstant(regularTz)
            val regularOffset = regularTz.offsetAt(instant)
            val foundationOffset = foundationTz.offsetAt(instant)

            assertEquals(
                regularOffset,
                foundationOffset,
                "Regular and Foundation implementations should have same offset during stable period at $dateTime"
            )
        }

        val firstOffset = regularTz.offsetAt(testPoints.first().toInstant(regularTz))
        for (dateTime in testPoints.drop(1)) {
            val offset = regularTz.offsetAt(dateTime.toInstant(regularTz))
            assertEquals(
                firstOffset,
                offset,
                "Offset should remain constant during stable summer period"
            )
        }
    }

    @Test
    fun shouldProduceConsistentUtcOffsetBetweenRegularAndFoundationTimeZones() {
        for ((zoneId, localDateTimes) in timeZoneRulesTestCases) {
            for ((ldt, _) in localDateTimes) {
                val regularTz = timeZoneById(zoneId)
                val foundationTz = timeZoneByIdFoundation(zoneId)
                val expected = regularTz.offsetAt(ldt.toInstant(regularTz))
                val actual = foundationTz.offsetAt(ldt.toInstant(foundationTz))
                assertEquals(expected, actual)
            }
        }
    }

    // TimeZoneRules.infoAtDatetime(LocalDateTime) tests

    enum class OffsetInfoType { REGULAR, GAP, OVERLAP }

    private data class TimeZoneRulesTestData(
        val zoneId: String,
        val localDateTimes: List<Pair<LocalDateTime, OffsetInfoType>>
    )

    private val timeZoneRulesTestCases = listOf(
        TimeZoneRulesTestData(
            "UTC",
            listOf(
                // UTC has no DST transitions
                LocalDateTime(2025, 1, 1, 0, 0, 0) to REGULAR,
                LocalDateTime(2025, 6, 15, 12, 0, 0) to REGULAR,
                LocalDateTime(2025, 12, 31, 23, 59, 59) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "America/New_York",
            listOf(
                // Before gap
                LocalDateTime(2025, 1, 1, 5, 0, 0) to REGULAR,
                // One day before gap
                LocalDateTime(2025, 3, 8, 2, 30, 0) to REGULAR,
                // At the beginning of the gap
                LocalDateTime(2025, 3, 9, 2, 0, 0) to GAP,
                // Inside gap
                LocalDateTime(2025, 3, 9, 2, 30, 0) to GAP,
                // At the end of the gap
                LocalDateTime(2025, 3, 9, 3, 0, 0) to REGULAR,
                // Between gap and overlap
                LocalDateTime(2025, 6, 30, 3, 0, 0) to REGULAR,
                // At the beginning of the overlap
                LocalDateTime(2025, 11, 2, 2, 0, 0) to REGULAR,
                // Inside overlap
                LocalDateTime(2025, 11, 2, 1, 30, 0) to OVERLAP,
                // At the end of the overlap
                LocalDateTime(2025, 11, 2, 1, 0, 0) to OVERLAP,
                // After overlap
                LocalDateTime(2025, 12, 31, 2, 0, 0) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "Europe/London",
            listOf(
                // Before gap
                LocalDateTime(2025, 3, 30, 0, 59, 59) to REGULAR,
                // At the beginning of the gap
                LocalDateTime(2025, 3, 30, 1, 0, 0) to GAP,
                // Inside gap
                LocalDateTime(2025, 3, 30, 1, 30, 0) to GAP,
                // At the end of the gap
                LocalDateTime(2025, 3, 30, 2, 0, 0) to REGULAR,
                // Between gap and overlap
                LocalDateTime(2025, 10, 26, 0, 59, 59) to REGULAR,
                // At the beginning of the overlap
                LocalDateTime(2025, 10, 26, 2, 0, 0) to REGULAR,
                // Inside overlap
                LocalDateTime(2025, 10, 26, 1, 30, 0) to OVERLAP,
                // At the end of the overlap
                LocalDateTime(2025, 10, 26, 1, 0, 0) to OVERLAP,
                // After overlap
                LocalDateTime(2025, 10, 26, 2, 0, 1) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "Australia/Sydney",
            listOf(
                // Before overlap
                LocalDateTime(2025, 4, 6, 2, 59, 59) to OVERLAP,
                // At the beginning of the overlap
                LocalDateTime(2025, 4, 6, 3, 0, 0) to REGULAR,
                // Inside overlap
                LocalDateTime(2025, 4, 6, 2, 30, 0) to OVERLAP,
                // At the end of the overlap
                LocalDateTime(2025, 4, 6, 2, 0, 0) to OVERLAP,
                // Before gap
                LocalDateTime(2025, 10, 5, 1, 59, 59) to REGULAR,
                // At the beginning of the gap
                LocalDateTime(2025, 10, 5, 2, 0, 0) to GAP,
                // Inside gap
                LocalDateTime(2025, 10, 5, 2, 30, 0) to GAP,
                // At the end of the gap
                LocalDateTime(2025, 10, 5, 3, 0, 0) to REGULAR,
                // After gap
                LocalDateTime(2025, 10, 5, 3, 0, 1) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "Asia/Kolkata",
            listOf(
                // India Standard Time - UTC+5:30, no DST
                LocalDateTime(2025, 1, 1, 0, 0, 0) to REGULAR,
                LocalDateTime(2025, 6, 15, 5, 30, 0) to REGULAR,
                LocalDateTime(2025, 12, 31, 23, 59, 59) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "America/Sao_Paulo",
            listOf(
                // Brazil - DST rules have changed multiple times
                // Currently no DST (as of 2019), but testing historical transitions
                LocalDateTime(2018, 2, 17, 23, 59, 59) to OVERLAP,
                LocalDateTime(2018, 2, 18, 0, 0, 0) to REGULAR,
                LocalDateTime(2018, 11, 3, 23, 59, 59) to REGULAR,
                LocalDateTime(2018, 11, 4, 0, 0, 0) to GAP,
                LocalDateTime(2025, 2, 17, 23, 59, 59) to REGULAR,
                LocalDateTime(2025, 2, 18, 0, 0, 0) to REGULAR,
                LocalDateTime(2025, 11, 3, 23, 59, 59) to REGULAR,
                LocalDateTime(2025, 11, 4, 0, 0, 0) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "Pacific/Chatham",
            listOf(
                // Chatham Islands - UTC+12:45/+13:45, unusual 45-minute offset
                // DST starts last Sunday in September at 2:45 AM
                LocalDateTime(2025, 9, 28, 2, 44, 59) to REGULAR,
                LocalDateTime(2025, 9, 28, 2, 45, 0) to GAP,
                LocalDateTime(2025, 9, 28, 3, 15, 0) to GAP,
                LocalDateTime(2025, 9, 28, 3, 45, 0) to REGULAR,
                // DST ends the first Sunday in April at 3:45 AM
                LocalDateTime(2025, 4, 6, 3, 44, 59) to OVERLAP,
                LocalDateTime(2025, 4, 6, 3, 45, 0) to REGULAR,
                LocalDateTime(2025, 4, 6, 3, 15, 0) to OVERLAP,
                LocalDateTime(2025, 4, 6, 2, 45, 0) to OVERLAP
            )
        ),
        TimeZoneRulesTestData(
            "Asia/Pyongyang",
            listOf(
                // North Korea changed from UTC+9 to UTC+8:30 on August 14, 2015
                LocalDateTime(2015, 8, 14, 23, 29, 59) to REGULAR,
                LocalDateTime(2015, 8, 14, 23, 30, 0) to OVERLAP,  // This would be in the overlap
                LocalDateTime(2015, 8, 14, 23, 59, 59) to OVERLAP,  // This would be in the overlap
                LocalDateTime(2015, 8, 15, 0, 0, 0) to REGULAR,
                // Changed back to UTC+9 on May 5, 2018
                LocalDateTime(2018, 5, 4, 23, 29, 59) to REGULAR,
                LocalDateTime(2018, 5, 4, 23, 30, 0) to GAP,  // This would be in the gap
                LocalDateTime(2018, 5, 5, 0, 0, 0) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "Pacific/Kwajalein",
            listOf(
                // Kwajalein skipped August 21, 1993 entirely
                // Moved from UTC-12 to UTC+12 (crossed International Date Line)
                LocalDateTime(1993, 8, 20, 23, 59, 59) to REGULAR,
                LocalDateTime(1993, 8, 21, 0, 0, 0) to GAP,  // This date doesn't exist, in the gap
                LocalDateTime(1993, 8, 21, 23, 59, 59) to GAP,  // This date doesn't exist, in the gap
                LocalDateTime(1993, 8, 22, 0, 0, 0) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "Pacific/Apia",
            listOf(
                // Apia is the capital of Samoa, Pacific/Samoa timezone is deprecated
                // Samoa skipped December 30, 2011 entirely
                // Moved from UTC-11 to UTC+13 (crossed International Date Line)
                LocalDateTime(2011, 12, 29, 23, 59, 59) to REGULAR,
                LocalDateTime(2011, 12, 30, 0, 0, 0) to GAP,  // This date doesn't exist, in the gap
                LocalDateTime(2011, 12, 30, 23, 59, 59) to GAP,  // This date doesn't exist, in the gap
                LocalDateTime(2011, 12, 31, 0, 0, 0) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "America/Caracas",
            listOf(
                // Based on this Wikipedia article: https://en.wikipedia.org/wiki/UTC%E2%88%9204:30
                // UTC−04:30 was used only in Venezuela from December 9, 2007, to May 1, 2016
                LocalDateTime(2007, 12, 9, 2, 29, 59) to REGULAR,
                LocalDateTime(2007, 12, 9, 2, 30, 0) to OVERLAP,  // This would be in the overlap
                LocalDateTime(2007, 12, 9, 2, 59, 59) to OVERLAP,  // This would be in the overlap
                LocalDateTime(2007, 12, 9, 3, 0, 0) to REGULAR,
                // Venezuela changed back from UTC-4:30 to UTC-4 on May 1, 2016
                LocalDateTime(2016, 5, 1, 2, 0, 0) to REGULAR,
                LocalDateTime(2016, 5, 1, 2, 30, 0) to GAP,  // This would be in the gap
                LocalDateTime(2016, 5, 1, 3, 0, 0) to REGULAR
            )
        ),
        TimeZoneRulesTestData(
            "Asia/Magadan",
            listOf(
                // Magadan changed from UTC+12 to UTC+10 on October 26, 2014
                // Creating a 2-hour overlap
                LocalDateTime(2014, 10, 25, 23, 59, 59) to REGULAR,
                LocalDateTime(2014, 10, 26, 0, 0, 0) to OVERLAP,  // Start of 2-hour overlap
                LocalDateTime(2014, 10, 26, 0, 30, 0) to OVERLAP,  // Inside overlap
                LocalDateTime(2014, 10, 26, 1, 0, 0) to OVERLAP,  // Still in overlap
                LocalDateTime(2014, 10, 26, 1, 59, 59) to OVERLAP,  // Still in overlap
                LocalDateTime(2014, 10, 26, 2, 0, 0) to REGULAR,  // End of overlap
                LocalDateTime(2014, 10, 26, 3, 0, 0) to REGULAR  // After overlap
            )
        )
    )

    private val tzdb = TzdbOnFilesystem()

    @Test
    fun shouldProduceConsistentOffsetInfoBetweenRegularAndFoundationTimeZoneRules() {
        for ((zoneId, localDateTimes) in timeZoneRulesTestCases) {
            val regularRules = tzdb.rulesForId(zoneId)
            val foundationRules = TimeZoneRulesFoundation(zoneId)
            println(formatHeader(zoneId))
            for ((ldt, offsetInfoType) in localDateTimes) {
                val expected = regularRules.infoAtDatetime(ldt)
                val actual = foundationRules.infoAtDatetime(ldt)
                println("expected: $expected, actual: $actual")
                assertEquals(expected, actual)
                val expectedType = when (offsetInfoType) {
                    REGULAR -> OffsetInfo.Regular::class
                    GAP -> OffsetInfo.Gap::class
                    OVERLAP -> OffsetInfo.Overlap::class
                }
                assertEquals(expectedType, actual::class)
            }
        }
    }

    private fun formatHeader(zoneId: String, preferredLength: Int = 64): String {
        val total = preferredLength - zoneId.length - 2
        val leftPart = "-".repeat(total / 2)
        val rightPart = "-".repeat(total / 2 + total % 2)
        return "$leftPart $zoneId $rightPart"
    }

    // timeZone.atZone(LocalDateTime) tests

    @Test
    fun shouldProduceConsistentZonedDateTimeBetweenRegularAndFoundationTimeZones() {
        for ((zoneId, localDateTimes) in timeZoneRulesTestCases) {
            for ((ldt, _) in localDateTimes) {
                val expected = timeZoneById(zoneId).atZone(ldt)
                val actual = timeZoneByIdFoundation(zoneId).atZone(ldt)
                assertEquals(expected, actual)
            }
        }
    }

    // timeZone.atStartOfDay(LocalDate) tests

    @Test
    fun shouldProduceConsistentInstanceBetweenRegularAndFoundationTimeZones() {
        for ((zoneId, localDateTimes) in timeZoneRulesTestCases) {
            for ((ldt, _) in localDateTimes) {
                val expected = timeZoneById(zoneId).atStartOfDay(ldt.date)
                val actual = timeZoneByIdFoundation(zoneId).atStartOfDay(ldt.date)
                assertEquals(expected, actual)
            }
        }
    }
}