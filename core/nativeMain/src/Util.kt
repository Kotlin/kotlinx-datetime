/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */
package kotlinx.datetime

import platform.posix.*

/**
 * Calculates [a] * [b] / [c]. Returns a pair of the quotient and the remainder.
 * [c] must be greater than zero.
 *
 * @throws ArithmeticException if the result overflows a long
 */
internal fun multiplyAndDivide(a: Long, b: Long, c: Long): Pair<Long, Long> {
    try {
        return safeMultiply(a, b).let { Pair(it / c, it % c) }
    } catch (e: ArithmeticException) {
        // this body is intentionally left blank
    }
    /* Not just optimizations: this is needed for multiplyAndDivide(Long.MIN_VALUE, x, x) to work. */
    if (b == c) return Pair(a, 0)
    if (a == c) return Pair(b, 0)

    inline fun low(x: Long) = x and 0xffffffff
    inline fun high(x: Long) = (x shr 32) and 0xffffffff

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

    /** For [bit] in [0; 63], return bit #[bit] of [value], counting from the least significant bit */
    inline fun indexBit(value: Long, bit: Int) = (value shr bit) and 1

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
    return Pair(sign * q, sign * r)
}

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
 * All code below was taken from various places of https://github.com/ThreeTen/threetenbp with few changes
 */

internal const val NANOS_PER_MILLI = 1_000_000
internal const val MILLIS_PER_ONE = 1_000
internal const val NANOS_PER_ONE = 1_000_000_000

/**
 * The number of seconds per hour.
 */
internal const val SECONDS_PER_HOUR = 60 * 60

/**
 * The number of seconds per minute.
 */
internal const val SECONDS_PER_MINUTE = 60

/**
 * The number of minutes per hour.
 */
internal const val MINUTES_PER_HOUR = 60

/**
 * The number of days in a 400 year cycle.
 */
internal const val DAYS_PER_CYCLE: Long = 146097

/**
 * The number of days from year zero to year 1970.
 * There are five 400 year cycles from year zero to 2000.
 * There are 7 leap years from 1970 to 2000.
 */
internal const val DAYS_0000_TO_1970: Long = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L)

// days in a 400 year cycle = 146097
// days in a 10,000 year cycle = 146097 * 25
// seconds per day = 86400
internal const val SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L

/**
 * Hours per day.
 */
internal const val HOURS_PER_DAY = 24

/**
 * Seconds per day.
 */
internal const val SECONDS_PER_DAY: Int = SECONDS_PER_HOUR * HOURS_PER_DAY

internal const val MINUTES_PER_DAY: Int = MINUTES_PER_HOUR * HOURS_PER_DAY

internal const val NANOS_PER_MINUTE: Long = NANOS_PER_ONE * SECONDS_PER_MINUTE.toLong()

internal const val NANOS_PER_HOUR = NANOS_PER_ONE * SECONDS_PER_HOUR.toLong()

/**
 * Nanos per day.
 */
internal const val NANOS_PER_DAY: Long = NANOS_PER_ONE * SECONDS_PER_DAY.toLong()

internal const val SECONDS_0000_TO_1970 = (146097L * 5L - (30L * 365L + 7L)) * 86400L

/**
 * Safely adds two long values.
 * throws [ArithmeticException] if the result overflows a long
 */
internal fun safeAdd(a: Long, b: Long): Long {
    val sum = a + b
    // check for a change of sign in the result when the inputs have the same sign
    if ((a xor sum) < 0 && (a xor b) >= 0) {
        throw ArithmeticException("Addition overflows a long: $a + $b")
    }
    return sum
}


/**
 * Safely subtracts one long from another.
 *
 * @param a  the first value
 * @param b  the second value to subtract from the first
 * @return the result
 * @throws ArithmeticException if the result overflows a long
 */
internal fun safeSubtract(a: Long, b: Long): Long {
    val result = a - b
    // check for a change of sign in the result when the inputs have the different signs
    if (a xor result < 0 && a xor b < 0) {
        throw ArithmeticException("Subtraction overflows a long: $a - $b")
    }
    return result
}

/**
 * Safely multiply a long by an int.
 *
 * @param a  the first value
 * @param b  the second value
 * @return the new total
 * @throws ArithmeticException if the result overflows a long
 */
internal fun safeMultiply(a: Long, b: Long): Long {
    when (b) {
        -1L -> {
            if (a == Long.MIN_VALUE) {
                throw ArithmeticException("Multiplication overflows a long: $a * $b")
            }
            return -a
        }
        0L -> return 0L
        1L -> return a
    }
    val total = a * b
    if (total / b != a) {
        throw ArithmeticException("Multiplication overflows a long: $a * $b")
    }
    return total
}

/**
 * Returns the floor division.
 * <p>
 * This returns {@code 0} for {@code floorDiv(0, 4)}.<br />
 * This returns {@code -1} for {@code floorDiv(-1, 4)}.<br />
 * This returns {@code -1} for {@code floorDiv(-2, 4)}.<br />
 * This returns {@code -1} for {@code floorDiv(-3, 4)}.<br />
 * This returns {@code -1} for {@code floorDiv(-4, 4)}.<br />
 * This returns {@code -2} for {@code floorDiv(-5, 4)}.<br />
 *
 * @param a  the dividend
 * @param b  the divisor
 * @return the floor division
 */
internal fun floorDiv(a: Long, b: Long): Long {
    return if (a >= 0) a / b else (a + 1) / b - 1
}

/**
 * Returns the floor modulus.
 *
 *
 * This returns `0` for `floorMod(0, 4)`.<br></br>
 * This returns `1` for `floorMod(-1, 4)`.<br></br>
 * This returns `2` for `floorMod(-2, 4)`.<br></br>
 * This returns `3` for `floorMod(-3, 4)`.<br></br>
 * This returns `0` for `floorMod(-4, 4)`.<br></br>
 *
 * @param a  the dividend
 * @param b  the divisor
 * @return the floor modulus (positive)
 */
internal fun floorMod(a: Long, b: Long): Long {
    return (a % b + b) % b
}

// org.threeten.bp.ZoneOffset#buildId
internal fun zoneIdByOffset(totalSeconds: Int): String {
    return if (totalSeconds == 0) {
        "Z"
    } else {
        val absTotalSeconds: Int = abs(totalSeconds)
        val buf = StringBuilder()
        val absHours: Int = absTotalSeconds / SECONDS_PER_HOUR
        val absMinutes: Int = absTotalSeconds / SECONDS_PER_MINUTE % MINUTES_PER_HOUR
        buf.append(if (totalSeconds < 0) "-" else "+")
            .append(if (absHours < 10) "0" else "").append(absHours)
            .append(if (absMinutes < 10) ":0" else ":").append(absMinutes)
        val absSeconds: Int = absTotalSeconds % SECONDS_PER_MINUTE
        if (absSeconds != 0) {
            buf.append(if (absSeconds < 10) ":0" else ":").append(absSeconds)
        }
        buf.toString()
    }
}

// org.threeten.bp.chrono.IsoChronology#isLeapYear
internal fun isLeapYear(year: Int): Boolean {
    val prolepticYear: Long = year.toLong()
    return prolepticYear and 3 == 0L && (prolepticYear % 100 != 0L || prolepticYear % 400 == 0L)
}
