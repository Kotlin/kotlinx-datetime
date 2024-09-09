/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.*

class LocalDateRangeTest {
    val Dec_24_1900 = LocalDate(1900, 12, 24)
    val Dec_30_1999 = LocalDate(1999, 12, 30)
    val Jan_01_2000 = LocalDate(2000, 1, 1)
    val Jan_02_2000 = LocalDate(2000, 1, 2)
    val Jan_05_2000 = LocalDate(2000, 1, 5)
    val Jan_24_2000 = LocalDate(2000, 1, 24)
    val Dec_24_2000 = LocalDate(2000, 12, 24)

    @Test
    fun emptyRange() {
        assertTrue { (Jan_05_2000..Jan_01_2000).isEmpty() }
        assertTrue { (Jan_01_2000 downTo Jan_05_2000).isEmpty() }
        assertTrue { LocalDateRange.EMPTY.isEmpty() }
    }

    @Test
    fun forwardRange() {
        assertContentEquals(
            (1..5).map { LocalDate(2000, 1, it) },
            Jan_01_2000..Jan_05_2000
        )
        assertContentEquals(
            (1..<5).map { LocalDate(2000, 1, it) },
            Jan_01_2000..<Jan_05_2000
        )
        assertContentEquals(
            listOf(Jan_01_2000),
            Jan_01_2000..Jan_01_2000
        )
        assertContentEquals(
            listOf(
                LocalDate(1999, 12, 30),
                LocalDate(1999, 12, 31),
                LocalDate(2000, 1, 1),
                LocalDate(2000, 1, 2)
            ),
            Dec_30_1999..Jan_02_2000
        )
    }

    @Test
    fun backwardRange() {
        assertContentEquals(
            (5 downTo 1).map { LocalDate(2000, 1, it) },
            Jan_05_2000 downTo Jan_01_2000
        )
        assertContentEquals(
            (5 downTo 2).map { LocalDate(2000, 1, it) },
            Jan_05_2000 downUntil Jan_01_2000
        )
        assertContentEquals(
            listOf(Jan_01_2000),
            Jan_01_2000 downTo Jan_01_2000
        )
        assertContentEquals(
            listOf(
                LocalDate(2000, 1, 2),
                LocalDate(2000, 1, 1),
                LocalDate(1999, 12, 31),
                LocalDate(1999, 12, 30),
            ),
            Jan_02_2000 downTo Dec_30_1999
        )
    }

    @Test
    fun step() {
        assertContentEquals(
            (1..24 step 2).map { LocalDate(2000, 1, it) },
            (Jan_01_2000..Jan_24_2000).step(2, DateTimeUnit.DAY)
        )
        assertContentEquals(
            (24 downTo 1  step 2).map { LocalDate(2000, 1, it) },
            (Jan_24_2000 downTo Jan_01_2000).step(2, DateTimeUnit.DAY)
        )
    }

    @Test
    fun string() {
        assertEquals(
            "2000-01-01..2000-01-05",
            (Jan_01_2000..Jan_05_2000).toString()
        )
        assertEquals(
            "2000-01-05 downTo 2000-01-01 step -1D",
            (Jan_05_2000 downTo Jan_01_2000).toString()
        )
        assertEquals(
            "2000-01-01..2000-01-05 step 1D",
            LocalDateProgression.fromClosedRange(Jan_01_2000, Jan_05_2000, 1, DateTimeUnit.DAY).toString()
        )
        assertEquals(
            "2000-01-05 downTo 2000-01-01 step -1D",
            LocalDateProgression.fromClosedRange(Jan_05_2000, Jan_01_2000, -1, DateTimeUnit.DAY).toString()
        )
    }

    @Test
    fun random() {
        assertEquals(
            Jan_01_2000,
            (Jan_01_2000..Jan_01_2000).random()
        )

        assertEquals(
            Jan_01_2000,
            (Jan_01_2000 downTo Jan_01_2000).random()
        )

        assertFails {
            (Jan_02_2000..Jan_01_2000).random()
        }

        assertNull((Jan_02_2000..Jan_01_2000).randomOrNull())

        val seed = 123456
        val expectedRand = Random(seed)
        val actualRand = Random(seed)

        repeat(20) {
            assertEquals(
                expectedRand.nextInt(0..23).let { Jan_01_2000.plus(it, DateTimeUnit.DAY) },
                (Jan_01_2000..Jan_24_2000).random(actualRand)
            )
        }

        repeat(20) {
            assertEquals(
                expectedRand.nextInt(0..23).let { Jan_24_2000.minus(it, DateTimeUnit.DAY) },
                (Jan_24_2000 downTo Jan_01_2000).random(actualRand)
            )
        }

        listOf(1, 2, 5, 30).forEach { step ->
            repeat(20) {
                val range = (0..23 step step)
                assertEquals(
                    expectedRand.nextInt(0..range.last / step).let { Jan_01_2000.plus(it * step, DateTimeUnit.DAY) },
                    (Jan_01_2000..Jan_24_2000).step(step, DateTimeUnit.DAY).random(actualRand)
                )
            }

            repeat(20) {
                val range = (0..23 step step)
                assertEquals(
                    expectedRand.nextInt(0..range.last / step).let { Jan_24_2000.minus(it * step, DateTimeUnit.DAY) },
                    (Jan_24_2000 downTo Jan_01_2000).step(step, DateTimeUnit.DAY).random(actualRand)
                )
            }
        }
    }
}
