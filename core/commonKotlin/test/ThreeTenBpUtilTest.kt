/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime.test

import kotlinx.datetime.internal.*
import kotlin.test.*

class ThreeTenBpUtilTest {
    @Test
    fun isLeap() {
        assertEquals(false, isLeapYear(1999))
        assertEquals(true, isLeapYear(2000))
        assertEquals(false, isLeapYear(2001))
        assertEquals(false, isLeapYear(2007))
        assertEquals(true, isLeapYear(2008))
        assertEquals(false, isLeapYear(2009))
        assertEquals(false, isLeapYear(2010))
        assertEquals(false, isLeapYear(2011))
        assertEquals(true, isLeapYear(2012))
        assertEquals(false, isLeapYear(2095))
        assertEquals(true, isLeapYear(2096))
        assertEquals(false, isLeapYear(2097))
        assertEquals(false, isLeapYear(2098))
        assertEquals(false, isLeapYear(2099))
        assertEquals(false, isLeapYear(2100))
        assertEquals(false, isLeapYear(2101))
        assertEquals(false, isLeapYear(2102))
        assertEquals(false, isLeapYear(2103))
        assertEquals(true, isLeapYear(2104))
        assertEquals(false, isLeapYear(2105))
        assertEquals(false, isLeapYear(-500))
        assertEquals(true, isLeapYear(-400))
        assertEquals(false, isLeapYear(-300))
        assertEquals(false, isLeapYear(-200))
        assertEquals(false, isLeapYear(-100))
        assertEquals(true, isLeapYear(0))
        assertEquals(false, isLeapYear(100))
        assertEquals(false, isLeapYear(200))
        assertEquals(false, isLeapYear(300))
        assertEquals(true, isLeapYear(400))
        assertEquals(false, isLeapYear(500))
    }
}
