/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
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

class TimeZoneNativeTest {

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

    @Test
    fun foo() {
        val zoneId = "America/New_York"

        val regularTz = timeZoneById(zoneId)
        debugOffsetsInIntervals(regularTz)

        val foundationTz = timeZoneByIdFoundation(zoneId)
        debugOffsetsInIntervals(foundationTz)
    }

    private fun debugOffsetsInIntervals(timeZone: TimeZone) {
        val beforeGap = LocalDateTime(2025, 1, 1, 5, 0, 0)
        val insideGap = LocalDateTime(2025, 3, 9, 2, 30, 0)
        val betweenGapAndOverlap = LocalDateTime(2025, 6, 30, 3, 0, 0)
        val insideOverlap = LocalDateTime(2025, 11, 2, 1, 30, 0)
        val afterOverlap = LocalDateTime(2025, 12, 31, 2, 0, 0)

        println("-----------------------------------------------------------------------------------------")
        val zdtBeforeGap = timeZone.atZone(beforeGap)  // -05:00
        val zdtInsideGap = timeZone.atZone(insideGap)  // -04:00
        val zdtBetweenGapAndOverlap = timeZone.atZone(betweenGapAndOverlap)  // -04:00
        val zdtInsideOverlap = timeZone.atZone(insideOverlap)  // -04:00
        val zdtAfterOverlap = timeZone.atZone(afterOverlap)  // -05:00
        println("-----------------------------------------------------------------------------------------")

        println("beforeGap: $zdtBeforeGap")
        println("insideGap: $zdtInsideGap")
        println("betweenGapAndOverlap: $zdtBetweenGapAndOverlap")
        println("insideOverlap: $zdtInsideOverlap")
        println("afterOverlap: $zdtAfterOverlap")
    }

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

    @Test
    fun testTimeZoneByIdFoundationAlwaysReturnsTimeZone() {
        val ldt = LocalDateTime(2025, 1, 1, 0, 0, 0)
        for (zoneId in validTimeZones) {
            val expected = timeZoneById(zoneId).atZone(ldt)
            val actual = timeZoneByIdFoundation(zoneId).atZone(ldt)
            assertEquals(expected, actual)
        }
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
}