/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.Month
import kotlinx.datetime.length
import kotlinx.datetime.maxLength
import kotlinx.datetime.minLength
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthTest {

    @Test
    fun length() {
        assertEquals(31, Month.JANUARY.length(false))
        assertEquals(31, Month.MARCH.length(false))
        assertEquals(30, Month.APRIL.length(false))
        assertEquals(31, Month.MAY.length(false))
        assertEquals(30, Month.JUNE.length(false))
        assertEquals(31, Month.JULY.length(false))
        assertEquals(31, Month.AUGUST.length(false))
        assertEquals(30, Month.SEPTEMBER.length(false))
        assertEquals(31, Month.OCTOBER.length(false))
        assertEquals(30, Month.NOVEMBER.length(false))
        assertEquals(31, Month.DECEMBER.length(false))

        assertEquals(28, Month.FEBRUARY.length(false))
        assertEquals(29, Month.FEBRUARY.length(true))
    }

    @Test
    fun minLength() {
        assertEquals(31, Month.JANUARY.minLength())
        assertEquals(28, Month.FEBRUARY.minLength())
        assertEquals(31, Month.MARCH.minLength())
        assertEquals(30, Month.APRIL.minLength())
        assertEquals(31, Month.MAY.minLength())
        assertEquals(30, Month.JUNE.minLength())
        assertEquals(31, Month.JULY.minLength())
        assertEquals(31, Month.AUGUST.minLength())
        assertEquals(30, Month.SEPTEMBER.minLength())
        assertEquals(31, Month.OCTOBER.minLength())
        assertEquals(30, Month.NOVEMBER.minLength())
        assertEquals(31, Month.DECEMBER.minLength())
    }

    @Test
    fun maxLength() {
        assertEquals(31, Month.JANUARY.maxLength())
        assertEquals(29, Month.FEBRUARY.maxLength())
        assertEquals(31, Month.MARCH.maxLength())
        assertEquals(30, Month.APRIL.maxLength())
        assertEquals(31, Month.MAY.maxLength())
        assertEquals(30, Month.JUNE.maxLength())
        assertEquals(31, Month.JULY.maxLength())
        assertEquals(31, Month.AUGUST.maxLength())
        assertEquals(30, Month.SEPTEMBER.maxLength())
        assertEquals(31, Month.OCTOBER.maxLength())
        assertEquals(30, Month.NOVEMBER.maxLength())
        assertEquals(31, Month.DECEMBER.maxLength())
    }

}
