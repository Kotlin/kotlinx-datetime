/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.Clock
import kotlin.test.*

class LocalTimeTest {



    @Test
    fun localTimeParsing() {
        fun checkParsedComponents(value: String, hour: Int, minute: Int, second: Int, nanosecond: Int) {
            checkComponents(value.toLocalTime(), hour, minute, second, nanosecond)
        }
        checkParsedComponents("18:43:15.100500", 18, 43, 15, 100500000)
        checkParsedComponents("18:43:15", 18, 43, 15, 0)
        checkParsedComponents("18:12", 18, 12, 0, 0)

        assertFailsWith<DateTimeFormatException> { LocalTime.parse("x") }
        assertFailsWith<DateTimeFormatException> { "+10000000004:00:00".toLocalDateTime() }

        /* Based on the ThreeTenBp project.
         * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
         */
        checkParsedComponents("02:01", 2, 1, 0, 0)
        checkParsedComponents("23:59:01", 23, 59, 1, 0)
        checkParsedComponents("23:59:59.990", 23, 59, 59, 990000000)
        checkParsedComponents("23:59:59.999990", 23, 59, 59, 999990000)
        checkParsedComponents("23:59:59.999999990", 23, 59, 59, 999999990)
    }

    /* Based on the ThreeTenBp project.
     * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
     */
    @Test
    fun strings() {
        assertEquals("02:01", LocalTime(2, 1, 0, 0).toString())
        assertEquals("23:59:01", LocalTime(23, 59, 1, 0).toString())
        assertEquals("23:59:59.990", LocalTime(23, 59, 59, 990000000).toString())
        assertEquals("23:59:59.999990", LocalTime(23, 59, 59, 999990000).toString())
        assertEquals("23:59:59.999999990", LocalTime(23, 59, 59, 999999990).toString())
    }

    @Test
    fun constructInvalidTime() {
        LocalTime(23, 59)
        assertFailsWith<IllegalArgumentException> { LocalTime(-1, 0) }
        assertFailsWith<IllegalArgumentException> { LocalTime(24, 0) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, -1) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 60) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 0, 60) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { LocalTime(0, 0, 0, 1_000_000_000) }
    }

}

fun checkComponents(value: LocalTime, hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0) {
    assertEquals(hour, value.hour, "hours")
    assertEquals(minute, value.minute, "minutes")
    assertEquals(second, value.second, "seconds")
    assertEquals(nanosecond, value.nanosecond, "nanoseconds")

    val fromComponents = LocalTime(hour, minute, second, nanosecond)
    checkEquals(fromComponents, value)
}

fun checkEquals(expected: LocalTime, actual: LocalTime) {
    assertEquals(expected, actual)
    assertEquals(expected.hashCode(), actual.hashCode())
    assertEquals(expected.toString(), actual.toString())
}
