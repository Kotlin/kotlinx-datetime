/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.internal.Path
import kotlinx.datetime.internal.TimeZoneRulesFoundation
import kotlinx.datetime.internal.TzdbOnFilesystem
import kotlinx.datetime.internal.defaultTzdbPath
import kotlinx.datetime.internal.getAvailableZoneIds
import kotlinx.datetime.internal.getAvailableZoneIdsFoundation
import kotlinx.datetime.internal.timeZoneById
import kotlinx.datetime.internal.timeZoneByIdFoundation
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plusSeconds
import kotlinx.datetime.toInstant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

        testPoints.forEach { dateTime ->
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
        testPoints.drop(1).forEach { dateTime ->
            val offset = regularTz.offsetAt(dateTime.toInstant(regularTz))
            assertEquals(
                firstOffset,
                offset,
                "Offset should remain constant during stable summer period"
            )
        }
    }

    // TimeZoneRules.infoAtDatetime(LocalDateTime) tests

    private data class TimeZoneRulesTestData(val zodeId: String, val localDateTimes: List<LocalDateTime>)

    private lateinit var timeZoneRulesTestCases: List<TimeZoneRulesTestData>

    @BeforeTest
    fun setupTimeZoneRulesTestCases() {
        timeZoneRulesTestCases = listOf(
            TimeZoneRulesTestData(
                "UTC",
                listOf(
                    // UTC has no DST transitions
                    LocalDateTime(2025, 1, 1, 0, 0, 0),
                    LocalDateTime(2025, 6, 15, 12, 0, 0),
                    LocalDateTime(2025, 12, 31, 23, 59, 59)
                )
            ),
            TimeZoneRulesTestData(
                "America/New_York",
                listOf(
                    // Before gap
                    LocalDateTime(2025, 1, 1, 5, 0, 0),
                    // At the beginning of the gap
                    LocalDateTime(2025, 3, 9, 2, 0, 0),
                    // Inside gap
                    LocalDateTime(2025, 3, 9, 2, 30, 0),
                    // At the end of the gap
                    LocalDateTime(2025, 3, 9, 3, 0, 0),
                    // Between gap and overlap
                    LocalDateTime(2025, 6, 30, 3, 0, 0),
                    // At the beginning of the overlap
                    LocalDateTime(2025, 11, 2, 2, 0, 0),
                    // Inside overlap
                    LocalDateTime(2025, 11, 2, 1, 30, 0),
                    // At the end of the overlap
                    LocalDateTime(2025, 11, 2, 1, 0, 0),
                    // After overlap
                    LocalDateTime(2025, 12, 31, 2, 0, 0)
                )
            ),
            TimeZoneRulesTestData(
                "Europe/London",
                listOf(
                    // Before gap
                    LocalDateTime(2025, 3, 30, 0, 59, 59),
                    // At the beginning of the gap
                    LocalDateTime(2025, 3, 30, 1, 0, 0),
                    // Inside gap
                    LocalDateTime(2025, 3, 30, 1, 30, 0),
                    // At the end of the gap
                    LocalDateTime(2025, 3, 30, 2, 0, 0),
                    // Between gap and overlap
                    LocalDateTime(2025, 10, 26, 0, 59, 59),
                    // At the beginning of the overlap
                    LocalDateTime(2025, 10, 26, 1, 0, 0),
                    // Inside overlap
                    LocalDateTime(2025, 10, 26, 1, 30, 0),
                    // At the end of the overlap
                    LocalDateTime(2025, 10, 26, 2, 0, 0),
                    // After overlap
                    LocalDateTime(2025, 10, 26, 2, 0, 1)
                )
            )
        )
    }

    private val tzdb = runCatching { TzdbOnFilesystem(Path.fromString(defaultTzdbPath())) }

    @Test
    fun shouldProduceConsistentOffsetInfoBetweenRegularAndFoundationTimeZoneRules() {
        for ((zoneId, localDateTimes) in timeZoneRulesTestCases) {
            val regularRules = tzdb.getOrThrow().rulesForId(zoneId)
            val foundationRules = TimeZoneRulesFoundation(zoneId)
            for (ldt in localDateTimes) {
                val expected = regularRules.infoAtDatetime(ldt)
                val actual = foundationRules.infoAtDatetime(ldt)
                assertEquals(expected, actual)
            }
        }
    }

    // timeZone.atZone(LocalDateTime) tests

    @Test
    fun testTimeZoneByIdFoundationAlwaysReturnsTimeZone() {
        val ldt = LocalDateTime(2025, 1, 1, 0, 0, 0)
        for (zoneId in validTimeZones) {
            val expected = timeZoneById(zoneId).atZone(ldt)
            val actual = timeZoneByIdFoundation(zoneId).atZone(ldt)
            assertEquals(expected, actual)
        }
    }
}