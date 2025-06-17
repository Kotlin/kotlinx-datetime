/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.internal.getAvailableZoneIds
import kotlinx.datetime.internal.getAvailableZoneIdsFoundation
import kotlinx.datetime.internal.timeZoneById
import kotlinx.datetime.internal.timeZoneByIdFoundation
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plusSeconds
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun infoAtInstantViaOffsetAtTest() {
        val zoneId = "America/New_York"
        val timeZone = timeZoneById(zoneId)
        val timeZoneFoundation = timeZoneByIdFoundation(zoneId)

        val startLocalDateTime = LocalDateTime(2025, 3, 8, 0, 0, 0)
        val endLocalDateTime = LocalDateTime(2025, 3, 10, 0, 0, 0)
        var currentDate = startLocalDateTime

        while (currentDate <= endLocalDateTime) {
            val instant = currentDate.toInstant(timeZone)

            val offset = timeZone.offsetAt(instant)
            val offsetFoundation = timeZoneFoundation.offsetAt(instant)

            assertEquals(offset, offsetFoundation)

            currentDate = currentDate.plusSeconds(1)
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