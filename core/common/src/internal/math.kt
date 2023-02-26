/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlin.native.concurrent.*

internal fun Long.clampToInt(): Int =
        when {
            this > Int.MAX_VALUE -> Int.MAX_VALUE
            this < Int.MIN_VALUE -> Int.MIN_VALUE
            else -> toInt()
        }

internal expect fun safeMultiply(a: Long, b: Long): Long
internal expect fun safeMultiply(a: Int, b: Int): Int
internal expect fun safeAdd(a: Long, b: Long): Long
internal expect fun safeAdd(a: Int, b: Int): Int

/** Multiplies two non-zero long values. */
internal fun safeMultiplyOrZero(a: Long, b: Long): Long {
    when (b) {
        -1L -> {
            if (a == Long.MIN_VALUE) {
                return 0L
            }
            return -a
        }
        1L -> return a
    }
    val total = a * b
    if (total / b != a) {
        return 0L
    }
    return total
}

/**
 * Calculates [a] * [b] / [c]. Returns a pair of the quotient and the remainder.
 * [c] must be greater than zero.
 *
 * @throws ArithmeticException if the result overflows a long
 */
internal fun multiplyAndDivide(a: Long, b: Long, c: Long): DivRemResult {
    if (a == 0L || b == 0L) return DivRemResult(0, 0)
    val ab = safeMultiplyOrZero(a, b)
    if (ab != 0L) return DivRemResult(ab / c, ab % c)

    /* Not just optimizations: this is needed for multiplyAndDivide(Long.MIN_VALUE, x, x) to work. */
    if (b == c) return DivRemResult(a, 0)
    if (a == c) return DivRemResult(b, 0)


    /* a * b = (ae * 2^64 + ah * 2^32 + al) * (be * 2^64 + bh * 2^32 + bl)
             = ae * be * 2^128 + (ae * bh + ah * be) * 2^96 + (ae * bl + ah * bh + al * be) * 2^64
               + (ah * bl + al * bh) * 2^32 + al * bl
             = 0 + w * 2^96 + x * 2^64 + y * 2^32 + z = xh * 2^96 + (xl + yh) * 2^64 + (yl + zh) * 2^32 + zl
             = r1 * 2^96 | r2 * 2^64 | r3 * 2^32 | r4
             = abh * 2^64 | abl */
    // a, b in [0; 2^64)

    // sign extensions to 128 bits:
    val ae = if (a >= 0) 0L else -1L // all ones or all zeros
    val be = if (b >= 0) 0L else -1L // all ones or all zeros

    val al = low(a) // [0; 2^32)
    val ah = high(a) // [0; 2^32)
    val bl = low(b) // [0; 2^32)
    val bh = high(b) // [0; 2^32)

    /* even though the language operates on signed Long values, we can add and multiply them as if they were unsigned
    due to the fact that they are encoded as 2's complement (hence the need to use sign extensions). The only operation
    here where sign matters is division. */
    val w = ae * bh + ah * be // we will only use the lower 32 bits of this value, so overflow is fine
    val x = ae * bl + ah * bh + al * be // may overflow, but overflow here goes beyond 128 bit
    val y1 = ah * bl
    val y2 = al * bh // y is split into y1 and y2 because y1 + y2 may overflow 2^64, which loses information
    val z = al * bl

    val r4 = low(z)
    val r3c = low(y1) + low(y2) + high(z)
    val r3 = low(r3c)
    val r2c = high(r3c) + low(x) + high(y1) + high(y2)
    val r2 = low(r2c)
    /* If r1 overflows 2^32 - 1, it's because of sign extension: we don't lose any significant bits because multiplying
    [0; 2^64) by [0; 2^64) may never exceed 2^128 - 1. */
    val r1 = high(r2c) + high(x) + low(w)

    var abl = (r3 shl 32) or r4 // low 64 bits of a * b
    var abh = (r1 shl 32) or r2 // high 64 bits of a * b


    val sign = if (indexBit(abh, 63) == 1L) -1 else 1

    if (sign == -1) {
        // negate, so that we operate on a positive number
        abl = abl.inv() + 1
        abh = abh.inv()
        if (abl == 0L) // abl overflowed
            abh += 1
    }

    /* The resulting quotient. This division is unsigned, so if the result doesn't fit in 63 bits, it means that
    overflow occurred. */
    var q = 0L
    // The remainder, always less than c and so fits in a Long.
    var r = 0L
    // Simple long division algorithm
    for (bitNo in 127 downTo 0) {
        // bit #bitNo of the numerator
        val nextBit = if (bitNo < 64) indexBit(abl, bitNo) else indexBit(abh, bitNo - 64)
        // left-shift R by one bit, setting the least significant bit to nextBit
        r = (r shl 1) or nextBit
        // if (R >= c). If R < 0, R >= 2^63 > Long.MAX_VALUE >= c
        if (r >= c || r < 0) {
            r -= c
            // set bit #bitNo of Q to 1
            if (bitNo < 63)
                q = q or (1L shl bitNo)
            else
                throw ArithmeticException("The result of a multiplication followed by division overflows a long")
        }
    }
    return DivRemResult(sign * q, sign * r)
}

internal class DivRemResult(val q: Long, val r: Long) {
    operator fun component1(): Long = q
    operator fun component2(): Long = r
}

@Suppress("NOTHING_TO_INLINE")
private inline fun low(x: Long) = x and 0xffffffff
@Suppress("NOTHING_TO_INLINE")
private inline fun high(x: Long) = (x shr 32) and 0xffffffff
/** For [bit] in [0; 63], return bit #[bit] of [value], counting from the least significant bit */
@Suppress("NOTHING_TO_INLINE")
private inline fun indexBit(value: Long, bit: Int): Long = (value shr bit and 1)


/**
 * Calculates ([d] * [n] + [r]) / [m], where [n], [m] > 0 and |[r]| <= [n].
 *
 * @throws ArithmeticException if the result overflows a long
 */
internal fun multiplyAddAndDivide(d: Long, n: Long, r: Long, m: Long): Long {
    var md = d
    var mr = r
    // make sure [md] and [mr] have the same sign
    if (d > 0 && r < 0) {
        md--
        mr += n
    } else if (d < 0 && r > 0) {
        md++
        mr -= n
    }
    if (md == 0L) {
        return mr / m
    }
    val (rd, rr) = multiplyAndDivide(md, n, m)
    return safeAdd(rd, safeAdd(mr / m, safeAdd(mr % m, rr) / m))
}

/**
 * Calculates [d] * [n] + [r], where [n] > 0 and |[r]| <= [n].
 *
 * @throws ArithmeticException if the result overflows a long
 */
internal fun multiplyAndAdd(d: Long, n: Long, r: Long): Long {
    var md = d
    var mr = r
    // make sure [md] and [mr] have the same sign
    if (d > 0 && r < 0) {
        md--
        mr += n
    } else if (d < 0 && r > 0) {
        md++
        mr -= n
    }
    return safeAdd(safeMultiply(md, n), mr)
}

@ThreadLocal
internal val POWERS_OF_TEN = intArrayOf(
    1,
    10,
    100,
    1000,
    10000,
    100000,
    1000000,
    10000000,
    100000000,
    1000000000
)

/**
 * The fraction [fractionalPart]/10^[digits].
 */
internal class DecimalFraction(
    /**
     * The numerator of the fraction.
     */
    val fractionalPart: Int,
    /**
     * The number of digits after the decimal point.
     */
    val digits: Int
): Comparable<DecimalFraction> {
    init {
        require(digits >= 0) { "Digits must be non-negative, but was $digits" }
    }

    /**
     * The integral numerator of the fraction, but with [newDigits] digits after the decimal point.
     */
    fun fractionalPartWithNDigits(newDigits: Int): Int = when {
        newDigits == digits -> fractionalPart
        newDigits > digits -> fractionalPart * POWERS_OF_TEN[newDigits - digits]
        else -> (fractionalPart / POWERS_OF_TEN[digits - newDigits - 1] + 5) / 10
    }

    override fun compareTo(other: DecimalFraction): Int =
        maxOf(digits, other.digits).let { maxPrecision ->
            fractionalPartWithNDigits(maxPrecision).compareTo(other.fractionalPartWithNDigits(maxPrecision))
        }

    override fun equals(other: Any?): Boolean = other is DecimalFraction && compareTo(other) == 0
}
