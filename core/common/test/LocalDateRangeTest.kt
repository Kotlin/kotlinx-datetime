/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.random.nextLong
import kotlin.test.*

class LocalDateRangeTest {
    val Dec_24_1900 = LocalDate(1900, 12, 24)
    val Dec_30_1999 = LocalDate(1999, 12, 30)
    val Jan_01_2000 = LocalDate(2000, 1, 1)
    val Jan_02_2000 = LocalDate(2000, 1, 2)
    val Jan_05_2000 = LocalDate(2000, 1, 5)
    val Jan_06_2000 = LocalDate(2000, 1, 6)
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
        assertTrue {
            (LocalDate.MIN..<LocalDate.MIN).isEmpty()
        }
        assertTrue {
            (LocalDate.MIN..<LocalDate.MAX).isNotEmpty()
        }
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

        assertFailsWith<IllegalArgumentException> {
            (Jan_02_2000..Jan_01_2000).random()
        }

        assertNull((Jan_02_2000..Jan_01_2000).randomOrNull())

        val seed = 123456
        val expectedRand = Random(seed)
        val actualRand = Random(seed)

        repeat(20) {
            assertEquals(
                expectedRand.nextLong(0L..23L).let { Jan_01_2000.plus(it, DateTimeUnit.DAY) },
                (Jan_01_2000..Jan_24_2000).random(actualRand)
            )
        }

        repeat(20) {
            assertEquals(
                expectedRand.nextLong(0L..23L).let { Jan_24_2000.minus(it, DateTimeUnit.DAY) },
                (Jan_24_2000 downTo Jan_01_2000).random(actualRand)
            )
        }

        listOf(1L, 2L, 5L, 30L).forEach { step ->
            repeat(20) {
                val range = (0L..23L step step)
                assertEquals(
                    expectedRand.nextLong(0L..range.last / step).let { Jan_01_2000.plus(it * step, DateTimeUnit.DAY) },
                    (Jan_01_2000..Jan_24_2000).step(step, DateTimeUnit.DAY).random(actualRand)
                )
            }

            repeat(20) {
                val range = (0L..23L step step)
                assertEquals(
                    expectedRand.nextLong(0..range.last / step).let { Jan_24_2000.minus(it * step, DateTimeUnit.DAY) },
                    (Jan_24_2000 downTo Jan_01_2000).step(step, DateTimeUnit.DAY).random(actualRand)
                )
            }
        }
        repeat(20) {
            (Jan_01_2000..Jan_24_2000).step(5, DateTimeUnit.DAY).let { assertContains(it, it.random()) }
        }
    }

    @Test
    fun first() {
        assertEquals((Jan_01_2000..Jan_01_2000).first(), Jan_01_2000)
        assertEquals((Jan_01_2000 downTo Jan_01_2000).first(), Jan_01_2000)
        assertEquals((Jan_01_2000..Jan_05_2000).first(), Jan_01_2000)
        assertEquals((Jan_05_2000 downTo Jan_01_2000).first(), Jan_05_2000)
        assertFailsWith<NoSuchElementException> { (Jan_02_2000..Jan_01_2000).first() }
        assertFailsWith<NoSuchElementException> { (Jan_01_2000 downTo Jan_02_2000).first() }
    }

    @Test
    fun last() {
        assertEquals((Jan_01_2000..Jan_01_2000).last(), Jan_01_2000)
        assertEquals((Jan_01_2000 downTo Jan_01_2000).last(), Jan_01_2000)
        assertEquals((Jan_01_2000..Jan_05_2000).last(), Jan_05_2000)
        assertEquals((Jan_05_2000 downTo Jan_01_2000).last(), Jan_01_2000)
        assertEquals((Jan_01_2000..Jan_06_2000).step(2, DateTimeUnit.DAY).last(), Jan_05_2000)
        assertEquals((Jan_06_2000 downTo Jan_01_2000).step(2, DateTimeUnit.DAY).last(), Jan_02_2000)
        assertFailsWith<NoSuchElementException> { (Jan_02_2000..Jan_01_2000).last() }
        assertFailsWith<NoSuchElementException> { (Jan_01_2000 downTo Jan_02_2000).last() }
    }

    @Test
    fun firstOrNull() {
        assertEquals((Jan_01_2000..Jan_01_2000).firstOrNull(), Jan_01_2000)
        assertEquals((Jan_01_2000 downTo Jan_01_2000).firstOrNull(), Jan_01_2000)
        assertEquals((Jan_01_2000..Jan_05_2000).firstOrNull(), Jan_01_2000)
        assertEquals((Jan_05_2000 downTo Jan_01_2000).firstOrNull(), Jan_05_2000)
        assertNull( (Jan_02_2000..Jan_01_2000).firstOrNull() )
        assertNull( (Jan_01_2000 downTo Jan_02_2000).firstOrNull() )
    }

    @Test
    fun lastOrNull() {
        assertEquals((Jan_01_2000..Jan_01_2000).lastOrNull(), Jan_01_2000)
        assertEquals((Jan_01_2000 downTo Jan_01_2000).lastOrNull(), Jan_01_2000)
        assertEquals((Jan_01_2000..Jan_05_2000).lastOrNull(), Jan_05_2000)
        assertEquals((Jan_05_2000 downTo Jan_01_2000).lastOrNull(), Jan_01_2000)
        assertNull( (Jan_02_2000..Jan_01_2000).lastOrNull() )
        assertNull( (Jan_01_2000 downTo Jan_02_2000).lastOrNull() )
    }

    @Test
    fun reversed() {
        assertContentEquals(
            Jan_05_2000 downTo Jan_01_2000,
            (Jan_01_2000..Jan_05_2000).reversed()
        )
        assertContentEquals(
            Jan_01_2000..Jan_05_2000,
            (Jan_05_2000 downTo Jan_01_2000).reversed()
        )
        assertContentEquals(
            Jan_01_2000..Jan_01_2000,
            (Jan_01_2000..Jan_01_2000).reversed()
        )

    }

    @Test
    fun contains() {
        assertTrue { Jan_01_2000 in Jan_01_2000..Jan_01_2000 }
        assertTrue { Jan_02_2000 in Jan_01_2000..Jan_05_2000 }
        assertTrue { Jan_01_2000 in Jan_01_2000 downTo Jan_01_2000 }
        assertTrue { Jan_02_2000 in Jan_05_2000 downTo Jan_01_2000 }

        assertFalse { Jan_01_2000 in Jan_02_2000..Jan_02_2000 }
        assertFalse { Jan_05_2000 in Jan_02_2000..Jan_02_2000 }
        assertFalse { Jan_01_2000 in Jan_02_2000..Jan_05_2000 }
        assertFalse { Jan_24_2000 in Jan_02_2000..Jan_02_2000 }
        assertFalse { Jan_01_2000 in Jan_02_2000 downTo Jan_02_2000 }
        assertFalse { Jan_05_2000 in Jan_02_2000 downTo Jan_02_2000 }
        assertFalse { Jan_01_2000 in Jan_02_2000 downTo Jan_05_2000 }
        assertFalse { Jan_24_2000 in Jan_05_2000 downTo Jan_02_2000 }

        assertTrue { (Jan_01_2000..Jan_01_2000).containsAll(listOf(Jan_01_2000)) }
        assertTrue { (Jan_01_2000..Jan_05_2000).containsAll(listOf(Jan_01_2000, Jan_02_2000, Jan_05_2000)) }

        assertFalse { (Jan_01_2000..Jan_01_2000).containsAll(listOf(Jan_01_2000, Jan_02_2000)) }
        assertFalse { (Jan_01_2000..Jan_05_2000).containsAll(listOf(Jan_01_2000, Jan_02_2000, Jan_05_2000, Jan_24_2000)) }

    }

    @Test
    fun getSize() {
        assertEquals(1, (Jan_01_2000..Jan_01_2000).size)
        assertEquals(1, (Jan_01_2000 downTo Jan_01_2000).size)
        assertEquals(2, (Jan_01_2000..Jan_02_2000).size)
        assertEquals(2, (Jan_02_2000 downTo Jan_01_2000).size)
        assertEquals(5, (Jan_01_2000..Jan_05_2000).size)
        assertEquals(5, (Jan_05_2000 downTo Jan_01_2000).size)
        assertEquals(4, (Jan_01_2000..<Jan_05_2000).size)

        assertEquals(0, (Jan_02_2000..Jan_01_2000).size)
        assertEquals (0, (Jan_01_2000 downTo Jan_02_2000).size)
        assertEquals(Int.MAX_VALUE, (LocalDate.MIN..LocalDate.MAX).size)
        assertEquals(Int.MAX_VALUE, (LocalDate.MAX downTo LocalDate.MIN).size)

        assertEquals(1, (Jan_01_2000..Jan_02_2000).step(2, DateTimeUnit.DAY).size)
        assertEquals(3, (Jan_01_2000..Jan_05_2000).step(2, DateTimeUnit.DAY).size)
        assertEquals(2, (Jan_02_2000..Jan_05_2000).step(2, DateTimeUnit.DAY).size)
        assertEquals(1, (Jan_02_2000 downTo Jan_01_2000).step(2, DateTimeUnit.DAY).size)
        assertEquals(3, (Jan_05_2000 downTo Jan_01_2000).step(2, DateTimeUnit.DAY).size)
        assertEquals(2, (Jan_05_2000 downTo Jan_02_2000).step(2, DateTimeUnit.DAY).size)
    }
}
