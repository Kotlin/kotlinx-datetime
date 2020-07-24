/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime
import kotlin.random.*
import kotlin.test.*

class UtilTest {

    @Test
    fun testMultiplyAndDivideLarge() {
        val (d, r) = multiplyAndDivide(Long.MAX_VALUE - 1, Long.MAX_VALUE - 2, Long.MAX_VALUE)
        assertEquals(9223372036854775804L, d) // https://www.wolframalpha.com/input/?i=floor%28%282%5E63+-+2%29+*+%282%5E63+-+3%29+%2F+%282%5E63+-1%29%29
        assertEquals(2, r)
    }

    @Test
    fun testMultiplyAndDivideNoOverflow() {
        repeat(1000) {
            val a = Random.nextInt().toLong()
            val b = Random.nextInt().toLong()
            val c = Random.nextInt(1, Int.MAX_VALUE).toLong()
            val (d, r) = multiplyAndDivide(a, b, c)
            assertEquals(a * b / c, d)
            assertEquals(a * b % c, r)
        }
    }

    @Test
    fun testMultiplyAndDivideNearIntBoundary() {
        val (d, r) = multiplyAndDivide((1L shl 32) + 1693, (1L shl 32) - 3, (1L shl 33) - 1)
        assertEquals(2_147_484_493, d)
        assertEquals(2_147_479_414, r)
    }
}
