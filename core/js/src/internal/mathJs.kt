/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

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