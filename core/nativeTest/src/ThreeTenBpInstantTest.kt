/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlin.test.*

class ThreeTenBpInstantTest {

    @Test
    fun instantComparisons() {
        val instants = arrayOf(
            Instant.ofEpochSecond(-2L, 0),
            Instant.ofEpochSecond(-2L, 999999998),
            Instant.ofEpochSecond(-2L, 999999999),
            Instant.ofEpochSecond(-1L, 0),
            Instant.ofEpochSecond(-1L, 1),
            Instant.ofEpochSecond(-1L, 999999998),
            Instant.ofEpochSecond(-1L, 999999999),
            Instant.ofEpochSecond(0L, 0),
            Instant.ofEpochSecond(0L, 1),
            Instant.ofEpochSecond(0L, 2),
            Instant.ofEpochSecond(0L, 999999999),
            Instant.ofEpochSecond(1L, 0),
            Instant.ofEpochSecond(2L, 0)
        )
        for (i in instants.indices) {
            val a = instants[i]
            for (j in instants.indices) {
                val b = instants[j]
                if (i < j) {
                    assertEquals(true, a < b, "$a <=> $b")
                    assertEquals(false, a == b, "$a <=> $b")
                } else if (i > j) {
                    assertEquals(true, a > b, "$a <=> $b")
                    assertEquals(false, a == b, "$a <=> $b")
                } else {
                    assertEquals(0, a.compareTo(b), "$a <=> $b")
                    assertEquals(true, a == b, "$a <=> $b")
                }
            }
        }
    }

    @Test
    fun instantEquals() {
        val test5a: Instant = Instant.ofEpochSecond(5L, 20)
        val test5b: Instant = Instant.ofEpochSecond(5L, 20)
        val test5n: Instant = Instant.ofEpochSecond(5L, 30)
        val test6: Instant = Instant.ofEpochSecond(6L, 20)
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
    fun toUnixMillis() {
        assertEquals(Instant.ofEpochSecond(1L, 1000000).toUnixMillis(), 1001L)
        assertEquals(Instant.ofEpochSecond(1L, 2000000).toUnixMillis(), 1002L)
        assertEquals(Instant.ofEpochSecond(1L, 567).toUnixMillis(), 1000L)
        assertEquals(Instant.ofEpochSecond(Long.MAX_VALUE / 1000).toUnixMillis(), Long.MAX_VALUE / 1000 * 1000)
        assertEquals(Instant.ofEpochSecond(Long.MIN_VALUE / 1000).toUnixMillis(), Long.MIN_VALUE / 1000 * 1000)
        assertEquals(Instant.ofEpochSecond(0L, -1000000).toUnixMillis(), -1L)
        assertEquals(Instant.ofEpochSecond(0L, 1000000).toUnixMillis(), 1)
        assertEquals(Instant.ofEpochSecond(0L, 999999).toUnixMillis(), 0)
        assertEquals(Instant.ofEpochSecond(0L, 1).toUnixMillis(), 0)
        assertEquals(Instant.ofEpochSecond(0L, 0).toUnixMillis(), 0)
        assertEquals(Instant.ofEpochSecond(0L, -1).toUnixMillis(), -1L)
        assertEquals(Instant.ofEpochSecond(0L, -999999).toUnixMillis(), -1L)
        assertEquals(Instant.ofEpochSecond(0L, -1000000).toUnixMillis(), -1L)
        assertEquals(Instant.ofEpochSecond(0L, -1000001).toUnixMillis(), -2L)
    }
}
