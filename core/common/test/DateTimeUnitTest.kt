/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class DateTimeUnitTest {
    private val U = DateTimeUnit // alias

    @Test
    fun baseUnits() {
        val baseUnitProps = listOf(
                U::NANOSECOND, U::MICROSECOND, U::MILLISECOND, U::SECOND, U::MINUTE, U::HOUR,
                U::DAY, U::WEEK, U::MONTH, U::QUARTER, U::YEAR, U::CENTURY
        )
        for (unit in baseUnitProps) {
            assertEquals(unit.name, unit.get().toString())
        }

        val allUnits = baseUnitProps.map { it.get() }

        assertEquals(allUnits.size, allUnits.map { it.hashCode() }.distinct().size)

        for (unit in allUnits) {
            assertSame(unit, allUnits.singleOrNull { it == unit }) // should be no not same, but equal
        }
    }

    @Test
    fun productUnits() {
        ensureEquality(U.MICROSECOND, U.NANOSECOND * 1000, "MICROSECOND")
        ensureEquality(U.MICROSECOND * 2000, U.NANOSECOND * 2000_000, "2-MILLISECOND")

        val twoDays = U.DAY * 2
        assertEquals("2-DAY", twoDays.toString())

        val twoWeeks = U.WEEK * 2
        assertEquals("2-WEEK", twoWeeks.toString())
        assertNotEquals(twoDays, twoWeeks)

        val fortnight = U.DAY * 14
        ensureEquality(twoWeeks, fortnight, "2-WEEK")

        val fourQuarters = U.QUARTER * 4
        ensureEquality(U.YEAR, fourQuarters, "YEAR")

        val twoFourMonths = U.MONTH * 24
        val twoYears = U.YEAR * 2
        ensureEquality(twoYears, twoFourMonths, "2-YEAR")
    }

    private fun ensureEquality(v1: Any, v2: Any, expectToString: String) {
        assertEquals(v1, v2)
        assertEquals(v1.hashCode(), v2.hashCode())
        assertEquals(v1.toString(), v2.toString())
        assertEquals(expectToString, v2.toString())
    }

}