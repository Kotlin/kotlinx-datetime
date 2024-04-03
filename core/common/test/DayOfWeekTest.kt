/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class DayOfWeekTest {

    @Test
    fun testDayOfWeek() {
        for (i in 1..7) {
            assertEquals(i, DayOfWeek(i).isoDayNumber)
        }
        assertFailsWith<IllegalArgumentException> { DayOfWeek(-1) }
        assertFailsWith<IllegalArgumentException> { DayOfWeek(8) }
        assertFailsWith<IllegalArgumentException> { DayOfWeek(Int.MIN_VALUE) }
    }

    @Test
    fun testExhaustiveWhenDayOfWeek() {
        for (dayOfWeek in DayOfWeek.entries) {
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> assertEquals(1, dayOfWeek.isoDayNumber)
                DayOfWeek.TUESDAY -> assertEquals(2, dayOfWeek.isoDayNumber)
                DayOfWeek.WEDNESDAY -> assertEquals(3, dayOfWeek.isoDayNumber)
                DayOfWeek.THURSDAY -> assertEquals(4, dayOfWeek.isoDayNumber)
                DayOfWeek.FRIDAY -> assertEquals(5, dayOfWeek.isoDayNumber)
                DayOfWeek.SATURDAY -> assertEquals(6, dayOfWeek.isoDayNumber)
                DayOfWeek.SUNDAY -> assertEquals(7, dayOfWeek.isoDayNumber)
            }
        }
    }
}
