/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
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

/**
 * Safely adds two int values.
 * throws [ArithmeticException] if the result overflows an int
 */
internal actual fun safeAdd(a: Int, b: Int): Int {
    val sum = a + b
    // check for a change of sign in the result when the inputs have the same sign
    if ((a xor sum) < 0 && (a xor b) >= 0) {
        throw ArithmeticException("Addition overflows an int: $a + $b")
    }
    return sum
}

/**
 * Safely multiply a long by a long.
 *
 * @param a  the first value
 * @param b  the second value
 * @return the new total
 * @throws ArithmeticException if the result overflows a long
 */
internal actual fun safeMultiply(a: Long, b: Long): Long {
    if (b == 1L) {
        return a
    }
    if (a == 1L) {
        return b
    }
    if (a == 0L || b == 0L) {
        return 0
    }
    val total = a * b
    if (total / b != a || a == Long.MIN_VALUE && b == -1L || b == Long.MIN_VALUE && a == -1L) {
        throw ArithmeticException("Multiplication overflows a long: $a * $b")
    }
    return total
}

/**
 * Safely multiply an int by an int.
 *
 * @param a  the first value
 * @param b  the second value
 * @return the new total
 * @throws ArithmeticException if the result overflows an int
 */
internal actual fun safeMultiply(a: Int, b: Int): Int {
    val total = a.toLong() * b.toLong()
    if (total < Int.MIN_VALUE || total > Int.MAX_VALUE) {
        throw ArithmeticException("Multiplication overflows an int: $a * $b")
    }
    return total.toInt()
}