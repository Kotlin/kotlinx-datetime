/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.internal.clampToInt
import kotlin.random.Random
import kotlin.random.nextLong
import kotlin.test.*

class YearMonthRangeTest {
    val Dec_1999 = YearMonth(1999, 12)
    val Jan_2000 = YearMonth(2000, 1)
    val Feb_2000 = YearMonth(2000, 2)
    val May_2000 = YearMonth(2000, 5)
    val Jun_2000 = YearMonth(2000, 6)
    val Dec_2000 = YearMonth(2000, 12)

    @Test
    fun emptyRange() {
        assertTrue { (May_2000..Jan_2000).isEmpty() }
        assertTrue { (Jan_2000 downTo May_2000).isEmpty() }
        assertTrue { YearMonthRange.EMPTY.isEmpty() }
    }

    @Test
    fun forwardRange() {
        assertContentEquals(
            (1..5).map { YearMonth(2000, it) },
            Jan_2000..May_2000
        )
        assertContentEquals(
            (1..<5).map { YearMonth(2000, it) },
            Jan_2000..<May_2000
        )
        assertTrue {
            (YearMonth.MIN..<YearMonth.MIN).isEmpty()
        }
        assertTrue {
            (YearMonth.MIN..<YearMonth.MAX).isNotEmpty()
        }
        assertContentEquals(
            listOf(Jan_2000),
            Jan_2000..Jan_2000
        )
        assertContentEquals(
            listOf(
                YearMonth(1999, 12),
                YearMonth(2000, 1),
                YearMonth(2000, 2),
            ),
            Dec_1999..Feb_2000
        )
    }

    @Test
    fun backwardRange() {
        assertContentEquals(
            (5 downTo 1).map { YearMonth(2000, it) },
            May_2000 downTo Jan_2000
        )
        assertContentEquals(
            listOf(Jan_2000),
            Jan_2000 downTo Jan_2000
        )
        assertContentEquals(
            listOf(
                YearMonth(2000, 2),
                YearMonth(2000, 1),
                YearMonth(1999, 12),
            ),
            Feb_2000 downTo Dec_1999
        )
    }

    @Test
    fun step() {
        assertContentEquals(
            (1..12 step 2).map { YearMonth(2000, it) },
            (Jan_2000..Dec_2000).step(2, DateTimeUnit.MONTH)
        )
        assertContentEquals(
            (12 downTo 1  step 2).map { YearMonth(2000, it) },
            (Dec_2000 downTo Jan_2000).step(2, DateTimeUnit.MONTH)
        )
        assertContentEquals(
            (Jan_2000..Dec_2000).step(Long.MAX_VALUE / 2, DateTimeUnit.YEAR),
            listOf(Jan_2000)
        )
    }

    @Test
    fun string() {
        assertEquals(
            "2000-01..2000-05",
            (Jan_2000..May_2000).toString()
        )
        assertEquals(
            "2000-05 downTo 2000-01 step -1M",
            (May_2000 downTo Jan_2000).toString()
        )
        assertEquals(
            "2000-01..2000-05 step 1M",
            YearMonthProgression.fromClosedRange(Jan_2000, May_2000, 1, DateTimeUnit.MONTH).toString()
        )
        assertEquals(
            "2000-05 downTo 2000-01 step -1M",
            YearMonthProgression.fromClosedRange(May_2000, Jan_2000, -1, DateTimeUnit.MONTH).toString()
        )
    }

    @Test
    fun random() {
        assertEquals(
            Jan_2000,
            (Jan_2000..Jan_2000).random()
        )

        assertEquals(
            Jan_2000,
            (Jan_2000 downTo Jan_2000).random()
        )

        assertFailsWith<NoSuchElementException> {
            (Feb_2000..Jan_2000).random()
        }

        assertNull((Feb_2000..Jan_2000).randomOrNull())

        val seed = 123456
        val expectedRand = Random(seed)
        val actualRand = Random(seed)

        repeat(20) {
            assertEquals(
                expectedRand.nextLong(0L..11L).let { Jan_2000.plus(it, DateTimeUnit.MONTH) },
                (Jan_2000..Dec_2000).random(actualRand)
            )
        }

        repeat(20) {
            assertEquals(
                expectedRand.nextLong(0L..11L).let { Dec_2000.minus(it, DateTimeUnit.MONTH) },
                (Dec_2000 downTo Jan_2000).random(actualRand)
            )
        }

        listOf(1L, 2L, 5L, 30L).forEach { step ->
            repeat(20) {
                val range = (0L..11L step step)
                assertEquals(
                    expectedRand.nextLong(0L..range.last / step).let { Jan_2000.plus(it * step, DateTimeUnit.MONTH) },
                    (Jan_2000..Dec_2000).step(step, DateTimeUnit.MONTH).random(actualRand)
                )
            }

            repeat(20) {
                val range = (0L..11L step step)
                assertEquals(
                    expectedRand.nextLong(0..range.last / step).let { Dec_2000.minus(it * step, DateTimeUnit.MONTH) },
                    (Dec_2000 downTo Jan_2000).step(step, DateTimeUnit.MONTH).random(actualRand)
                )
            }
        }
        repeat(20) {
            (Jan_2000..Dec_2000).step(5, DateTimeUnit.MONTH).let { assertContains(it, it.random()) }
        }
    }

    @Test
    fun first() {
        assertEquals((Jan_2000..Jan_2000).first(), Jan_2000)
        assertEquals((Jan_2000 downTo Jan_2000).first(), Jan_2000)
        assertEquals((Jan_2000..May_2000).first(), Jan_2000)
        assertEquals((May_2000 downTo Jan_2000).first(), May_2000)
        assertFailsWith<NoSuchElementException> { (Feb_2000..Jan_2000).first() }
        assertFailsWith<NoSuchElementException> { (Jan_2000 downTo Feb_2000).first() }
    }

    @Test
    fun last() {
        assertEquals((Jan_2000..Jan_2000).last(), Jan_2000)
        assertEquals((Jan_2000 downTo Jan_2000).last(), Jan_2000)
        assertEquals((Jan_2000..May_2000).last(), May_2000)
        assertEquals((May_2000 downTo Jan_2000).last(), Jan_2000)
        assertEquals((Jan_2000..Jun_2000).step(2, DateTimeUnit.MONTH).last(), May_2000)
        assertEquals((Jun_2000 downTo Jan_2000).step(2, DateTimeUnit.MONTH).last(), Feb_2000)
        assertFailsWith<NoSuchElementException> { (Feb_2000..Jan_2000).last() }
        assertFailsWith<NoSuchElementException> { (Jan_2000 downTo Feb_2000).last() }
    }

    @Test
    fun firstOrNull() {
        assertEquals((Jan_2000..Jan_2000).firstOrNull(), Jan_2000)
        assertEquals((Jan_2000 downTo Jan_2000).firstOrNull(), Jan_2000)
        assertEquals((Jan_2000..May_2000).firstOrNull(), Jan_2000)
        assertEquals((May_2000 downTo Jan_2000).firstOrNull(), May_2000)
        assertNull( (Feb_2000..Jan_2000).firstOrNull() )
        assertNull( (Jan_2000 downTo Feb_2000).firstOrNull() )
    }

    @Test
    fun lastOrNull() {
        assertEquals((Jan_2000..Jan_2000).lastOrNull(), Jan_2000)
        assertEquals((Jan_2000 downTo Jan_2000).lastOrNull(), Jan_2000)
        assertEquals((Jan_2000..May_2000).lastOrNull(), May_2000)
        assertEquals((May_2000 downTo Jan_2000).lastOrNull(), Jan_2000)
        assertNull( (Feb_2000..Jan_2000).lastOrNull() )
        assertNull( (Jan_2000 downTo Feb_2000).lastOrNull() )
    }

    @Test
    fun reversed() {
        assertEquals(
            May_2000 downTo Jan_2000,
            (Jan_2000..May_2000).reversed()
        )
        assertEquals(
            Jan_2000..May_2000,
            (May_2000 downTo Jan_2000).reversed()
        )
        assertEquals(
            Jan_2000 downTo Jan_2000,
            (Jan_2000..Jan_2000).reversed()
        )
    }

    @Test
    fun contains() {
        assertTrue { Jan_2000 in Jan_2000..Jan_2000 }
        assertTrue { Feb_2000 in Jan_2000..May_2000 }
        assertTrue { Jan_2000 in Jan_2000 downTo Jan_2000 }
        assertTrue { Feb_2000 in May_2000 downTo Jan_2000 }

        assertFalse { Jan_2000 in Feb_2000..Feb_2000 }
        assertFalse { May_2000 in Feb_2000..Feb_2000 }
        assertFalse { Jan_2000 in Feb_2000..May_2000 }
        assertFalse { Dec_2000 in Feb_2000..Feb_2000 }
        assertFalse { Jan_2000 in Feb_2000 downTo Feb_2000 }
        assertFalse { May_2000 in Feb_2000 downTo Feb_2000 }
        assertFalse { Jan_2000 in Feb_2000 downTo May_2000 }
        assertFalse { Dec_2000 in May_2000 downTo Feb_2000 }

        assertFalse { (Jan_2000..May_2000).contains(Any()) }

        assertTrue { (Jan_2000..Jan_2000).containsAll(listOf(Jan_2000)) }
        assertTrue { (Jan_2000..May_2000).containsAll(listOf(Jan_2000, Feb_2000, May_2000)) }

        assertFalse { (Jan_2000..Jan_2000).containsAll(listOf(Jan_2000, Feb_2000)) }
        assertFalse { (Jan_2000..May_2000).containsAll(listOf(Jan_2000, Feb_2000, May_2000, Dec_2000)) }

        assertFalse { ((Jan_2000..May_2000) as Collection<*>).containsAll(listOf(Any())) }

    }

    @Test
    fun getSize() {
        assertEquals(1, (Jan_2000..Jan_2000).size)
        assertEquals(1, (Jan_2000 downTo Jan_2000).size)
        assertEquals(2, (Jan_2000..Feb_2000).size)
        assertEquals(2, (Feb_2000 downTo Jan_2000).size)
        assertEquals(5, (Jan_2000..May_2000).size)
        assertEquals(5, (May_2000 downTo Jan_2000).size)
        assertEquals(4, (Jan_2000..<May_2000).size)

        assertEquals(0, (Feb_2000..Jan_2000).size)
        assertEquals (0, (Jan_2000 downTo Feb_2000).size)

        val maxSizeOfRange = (YearMonth.MAX.prolepticMonth - YearMonth.MIN.prolepticMonth + 1L).clampToInt()
        assertEquals(maxSizeOfRange, (YearMonth.MIN..YearMonth.MAX).size)
        assertEquals(maxSizeOfRange, (YearMonth.MAX downTo YearMonth.MIN).size)

        assertEquals(1, (Jan_2000..Feb_2000).step(2, DateTimeUnit.MONTH).size)
        assertEquals(3, (Jan_2000..May_2000).step(2, DateTimeUnit.MONTH).size)
        assertEquals(2, (Feb_2000..May_2000).step(2, DateTimeUnit.MONTH).size)
        assertEquals(1, (Feb_2000 downTo Jan_2000).step(2, DateTimeUnit.MONTH).size)
        assertEquals(3, (May_2000 downTo Jan_2000).step(2, DateTimeUnit.MONTH).size)
        assertEquals(2, (May_2000 downTo Feb_2000).step(2, DateTimeUnit.MONTH).size)
    }

    @Test
    fun equalityAndHashCode() {
        assertEquals(
            Jan_2000..May_2000,
            (Jan_2000..May_2000).step(1, DateTimeUnit.MONTH)
        )
        assertEquals(
            (Jan_2000..May_2000).hashCode(),
            ((Jan_2000..May_2000).step(1, DateTimeUnit.MONTH)).hashCode()
        )

        assertEquals(
            (Jan_2000..May_2000).step(12, DateTimeUnit.MONTH),
            (Jan_2000..May_2000).step(1, DateTimeUnit.YEAR)
        )
        assertEquals(
            (Jan_2000..May_2000).step(12, DateTimeUnit.MONTH).hashCode(),
            (Jan_2000..May_2000).step(1, DateTimeUnit.YEAR).hashCode()
        )

        assertEquals(
            (May_2000 downTo Jan_2000).step(12, DateTimeUnit.MONTH),
            (May_2000 downTo Jan_2000).step(1, DateTimeUnit.YEAR)
        )
        assertEquals(
            (May_2000 downTo Jan_2000).step(12, DateTimeUnit.MONTH).hashCode(),
            (May_2000 downTo Jan_2000).step(1, DateTimeUnit.YEAR).hashCode()
        )
    }
}
