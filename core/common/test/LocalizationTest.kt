/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class LocalizationTest {

    @Test
    fun testDayOfWeekDisplayNameReturnsNonEmptyString() {
        for (day in DayOfWeek.entries) {
            val displayName = day.displayName()
            assertNotNull(displayName)
            assertTrue(displayName.isNotEmpty(), "Display name for $day should not be empty")
        }
    }

    @Test
    fun testMonthDisplayNameReturnsNonEmptyString() {
        for (month in Month.entries) {
            val displayName = month.displayName()
            assertNotNull(displayName)
            assertTrue(displayName.isNotEmpty(), "Display name for $month should not be empty")
        }
    }

    @Test
    fun testAllTextStylesWork() {
        val day = DayOfWeek.MONDAY
        val month = Month.JANUARY

        for (style in TextStyle.entries) {
            val dayName = day.displayName(textStyle = style)
            val monthName = month.displayName(textStyle = style)

            assertNotNull(dayName, "Day name should not be null for style $style")
            assertNotNull(monthName, "Month name should not be null for style $style")
            assertTrue(dayName.isNotEmpty(), "Day name should not be empty for style $style")
            assertTrue(monthName.isNotEmpty(), "Month name should not be empty for style $style")
        }
    }

    @Test
    fun testNarrowShorterThanShort() {
        val day = DayOfWeek.WEDNESDAY
        val month = Month.SEPTEMBER

        val dayNarrow = day.displayName(textStyle = TextStyle.NARROW)
        val dayShort = day.displayName(textStyle = TextStyle.SHORT)
        val dayFull = day.displayName(textStyle = TextStyle.FULL)

        val monthNarrow = month.displayName(textStyle = TextStyle.NARROW)
        val monthShort = month.displayName(textStyle = TextStyle.SHORT)
        val monthFull = month.displayName(textStyle = TextStyle.FULL)

        // Narrow should generally be shorter or equal to short
        assertTrue(dayNarrow.length <= dayShort.length,
            "Day narrow ($dayNarrow) should not be longer than short ($dayShort)")
        assertTrue(dayShort.length <= dayFull.length,
            "Day short ($dayShort) should not be longer than full ($dayFull)")

        assertTrue(monthNarrow.length <= monthShort.length,
            "Month narrow ($monthNarrow) should not be longer than short ($monthShort)")
        assertTrue(monthShort.length <= monthFull.length,
            "Month short ($monthShort) should not be longer than full ($monthFull)")
    }

    @Test
    fun testConsistentResults() {
        val day = DayOfWeek.FRIDAY
        val month = Month.MARCH

        // Multiple calls with same parameters should return same result
        val dayName1 = day.displayName()
        val dayName2 = day.displayName()
        assertEquals(dayName1, dayName2, "Multiple calls should return consistent results")

        val monthName1 = month.displayName()
        val monthName2 = month.displayName()
        assertEquals(monthName1, monthName2, "Multiple calls should return consistent results")
    }

    @Test
    fun testAllDaysHaveUniqueFullNames() {
        val dayNames = DayOfWeek.entries.map { it.displayName(textStyle = TextStyle.FULL) }
        val uniqueNames = dayNames.toSet()
        assertEquals(7, uniqueNames.size,
            "All 7 days should have unique full names. Found: $dayNames")
    }

    @Test
    fun testAllMonthsHaveUniqueFullNames() {
        val monthNames = Month.entries.map { it.displayName(textStyle = TextStyle.FULL) }
        val uniqueNames = monthNames.toSet()
        assertEquals(12, uniqueNames.size,
            "All 12 months should have unique full names. Found: $monthNames")
    }
}
