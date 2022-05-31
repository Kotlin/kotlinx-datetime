/*
 * Copyright 2019-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test
import kotlin.random.*
import kotlin.test.*
import kotlinx.datetime.*

class MultiplyAndDivideTest {

    private fun mulDiv(a: Long, b: Long, m: Long): Pair<Long, Long> =
            multiplyAndDivide(a, b, m).run { q to r }

    @Test
    fun small() {
        assertEquals(4L to 3L, mulDiv(5L, 15L, 18L))
        assertEquals(15L to 0L, mulDiv(5L, 12L, 4L))
    }

    @Test
    fun smallNegative() {
        assertEquals(4L to 3L, mulDiv(-5L, -15L, 18L))
        assertEquals(597308323L to 475144067L, mulDiv(-1057588554, -1095571653, 1939808965))
    }

    @Test
    fun large() {
        val l = Long.MAX_VALUE
        val result = mulDiv(l - 1, l - 2, l)
        assertEquals(9223372036854775804L to 2L, result) // https://www.wolframalpha.com/input/?i=floor%28%282%5E63+-+2%29+*+%282%5E63+-+3%29+%2F+%282%5E63+-1%29%29
    }

    @Test
    fun largeNegative() {
        val r1 = mulDiv(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)
        val r2 = mulDiv(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE)
        assertEquals(Long.MIN_VALUE to 0L, r1)
        assertEquals(r1, r2)

        val r3 = mulDiv(Long.MIN_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE)
        val r4 = mulDiv(Long.MAX_VALUE - 1, Long.MIN_VALUE, Long.MAX_VALUE)

        assertEquals(-9223372036854775806 to -9223372036854775806, r3)
        assertEquals(r3, r4)
    }

    @Test
    fun halfLarge() {
        val l = Long.MAX_VALUE / 2 + 1
        val result = mulDiv(l - 2, l - 3, l)
        assertEquals(4611686018427387899L to 6L, result)
    }

    @Test
    fun randomProductFitsInLong() {
        repeat(STRESS_TEST_ITERATIONS) {
            val a = Random.nextInt().toLong()
            val b = Random.nextInt().toLong()
            val m = Random.nextInt(1, Int.MAX_VALUE).toLong()
//            println("$a, $b, $c: ${a * b / c}, ${a * b % c}")
            val (q, r) = mulDiv(a, b, m)
//            println("$d, $r")
            assertEquals(a * b / m, q)
            assertEquals(a * b % m, r)
        }
    }


    @Test
    fun nearIntBoundary() {
        val (d, r) = mulDiv((1L shl 32) + 1693, (1L shl 32) - 3, (1L shl 33) - 1)
        assertEquals(2_147_484_493, d)
        assertEquals(2_147_479_414, r)
    }

    @Test
    fun largeOverflows() {
        assertFailsWith<ArithmeticException> { mulDiv(Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE) }
        assertFailsWith<ArithmeticException> { mulDiv(Long.MAX_VALUE, 4, 3) }
    }
}
