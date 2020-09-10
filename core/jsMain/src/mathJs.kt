/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * Safely adds two long values.
 * throws [ArithmeticException] if the result overflows a long
 */
internal actual fun safeAdd(a: Long, b: Long): Long {
    val sum = a + b
    // check for a change of sign in the result when the inputs have the same sign
    if ((a xor sum) < 0 && (a xor b) >= 0) {
        throw ArithmeticException("Addition overflows a long: $a + $b")
    }
    return sum
}

internal actual fun safeAdd(a: Int, b: Int): Int {
    val sum = a + b
    // check for a change of sign in the result when the inputs have the same sign
    if ((a xor sum) < 0 && (a xor b) >= 0) {
        throw ArithmeticException("Addition overflows Int range: $a + $b")
    }
    return sum
}

/**
 * Safely multiply a long by an int.
 *
 * @param a  the first value
 * @param b  the second value
 * @return the new total
 * @throws ArithmeticException if the result overflows a long
 */
internal actual fun safeMultiply(a: Long, b: Long): Long {
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

internal actual fun safeMultiply(a: Int, b: Int): Int {
    val result = a.toLong() * b
    if (result > Int.MAX_VALUE || result < Int.MIN_VALUE) throw ArithmeticException("Multiplication overflows Int range: $a * $b.")
    return result.toInt()
}

/**
 * Returns the floor division.
 *
 * This returns `0` for `floorDiv(0, 4)`.
 * This returns `-1` for `floorDiv(-1, 4)`.
 * This returns `-1` for `floorDiv(-2, 4)`.
 * This returns `-1` for `floorDiv(-3, 4)`.
 * This returns `-1` for `floorDiv(-4, 4)`.
 * This returns `-2` for `floorDiv(-5, 4)`.
 *
 * @param a  the dividend
 * @param b  the divisor
 * @return the floor division
 */
internal fun floorDiv(a: Long, b: Long): Long = if (a >= 0) a / b else (a + 1) / b - 1

/**
 * Returns the floor modulus.
 *
 * This returns `0` for `floorMod(0, 4)`.
 * This returns `1` for `floorMod(-1, 4)`.
 * This returns `2` for `floorMod(-2, 4)`.
 * This returns `3` for `floorMod(-3, 4)`.
 * This returns `0` for `floorMod(-4, 4)`.
 *
 * @param a  the dividend
 * @param b  the divisor
 * @return the floor modulus (positive)
 */
internal fun floorMod(a: Long, b: Long): Long = (a % b + b) % b