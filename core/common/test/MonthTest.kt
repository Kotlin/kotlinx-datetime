/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class MonthTest {
    @Test
    fun testExhaustiveWhen() {
        for (month in Month.entries) {
            when (month) {
                Month.JANUARY -> assertEquals(1, month.number)
                Month.FEBRUARY -> assertEquals(2, month.number)
                Month.MARCH -> assertEquals(3, month.number)
                Month.APRIL -> assertEquals(4, month.number)
                Month.MAY -> assertEquals(5, month.number)
                Month.JUNE -> assertEquals(6, month.number)
                Month.JULY -> assertEquals(7, month.number)
                Month.AUGUST -> assertEquals(8, month.number)
                Month.SEPTEMBER -> assertEquals(9, month.number)
                Month.OCTOBER -> assertEquals(10, month.number)
                Month.NOVEMBER -> assertEquals(11, month.number)
                Month.DECEMBER -> assertEquals(12, month.number)
            }
        }
    }
}
