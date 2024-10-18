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
}