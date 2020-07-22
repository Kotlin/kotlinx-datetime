/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*

class ThreeTenBpInstantTest {

    @Test
    fun instantComparisons() {
        val instants = arrayOf(
            Instant.fromEpochSeconds(-2L, 0),
            Instant.fromEpochSeconds(-2L, 999999998),
            Instant.fromEpochSeconds(-2L, 999999999),
            Instant.fromEpochSeconds(-1L, 0),
            Instant.fromEpochSeconds(-1L, 1),
            Instant.fromEpochSeconds(-1L, 999999998),
            Instant.fromEpochSeconds(-1L, 999999999),
            Instant.fromEpochSeconds(0L, 0),
            Instant.fromEpochSeconds(0L, 1),
            Instant.fromEpochSeconds(0L, 2),
            Instant.fromEpochSeconds(0L, 999999999),
            Instant.fromEpochSeconds(1L, 0),
            Instant.fromEpochSeconds(2L, 0)
        )
        for (i in instants.indices) {
            val a = instants[i]
            for (j in instants.indices) {
                val b = instants[j]
                when {
                    i < j -> {
                        assertTrue(a < b, "$a <=> $b")
                        assertNotEquals(a, b, "$a <=> $b")
                    }
                    i > j -> {
                        assertTrue(a > b, "$a <=> $b")
                        assertNotEquals(a, b, "$a <=> $b")
                    }
                    else -> {
                        assertEquals(0, a.compareTo(b), "$a <=> $b")
                        assertEquals(a, b, "$a <=> $b")
                    }
                }
            }
        }
    }

    @Test
    fun instantEquals() {
        val test5a: Instant = Instant.fromEpochSeconds(5L, 20)
        val test5b: Instant = Instant.fromEpochSeconds(5L, 20)
        val test5n: Instant = Instant.fromEpochSeconds(5L, 30)
        val test6: Instant = Instant.fromEpochSeconds(6L, 20)
        assertEquals(true, test5a == test5a)
        assertEquals(true, test5a == test5b)
        assertEquals(false, test5a == test5n)
        assertEquals(false, test5a == test6)
        assertEquals(true, test5b == test5a)
        assertEquals(true, test5b == test5b)
        assertEquals(false, test5b == test5n)
        assertEquals(false, test5b == test6)
        assertEquals(false, test5n == test5a)
        assertEquals(false, test5n == test5b)
        assertEquals(true, test5n == test5n)
        assertEquals(false, test5n == test6)
        assertEquals(false, test6 == test5a)
        assertEquals(false, test6 == test5b)
        assertEquals(false, test6 == test5n)
        assertEquals(true, test6 == test6)
    }

    @Test
    fun toEpochMilliseconds() {
        assertEquals(Instant.fromEpochSeconds(1L, 1000000).toEpochMilliseconds(), 1001L)
        assertEquals(Instant.fromEpochSeconds(1L, 2000000).toEpochMilliseconds(), 1002L)
        assertEquals(Instant.fromEpochSeconds(1L, 567).toEpochMilliseconds(), 1000L)
        assertEquals(Instant.fromEpochSeconds(Long.MAX_VALUE / 1_000_000).toEpochMilliseconds(), Long.MAX_VALUE / 1_000_000 * 1000)
        assertEquals(Instant.fromEpochSeconds(Long.MIN_VALUE / 1_000_000).toEpochMilliseconds(), Long.MIN_VALUE / 1_000_000 * 1000)
        assertEquals(Instant.fromEpochSeconds(0L, -1000000).toEpochMilliseconds(), -1L)
        assertEquals(Instant.fromEpochSeconds(0L, 1000000).toEpochMilliseconds(), 1)
        assertEquals(Instant.fromEpochSeconds(0L, 999999).toEpochMilliseconds(), 0)
        assertEquals(Instant.fromEpochSeconds(0L, 1).toEpochMilliseconds(), 0)
        assertEquals(Instant.fromEpochSeconds(0L, 0).toEpochMilliseconds(), 0)
        assertEquals(Instant.fromEpochSeconds(0L, -1).toEpochMilliseconds(), -1L)
        assertEquals(Instant.fromEpochSeconds(0L, -999999).toEpochMilliseconds(), -1L)
        assertEquals(Instant.fromEpochSeconds(0L, -1000000).toEpochMilliseconds(), -1L)
        assertEquals(Instant.fromEpochSeconds(0L, -1000001).toEpochMilliseconds(), -2L)
    }
}
