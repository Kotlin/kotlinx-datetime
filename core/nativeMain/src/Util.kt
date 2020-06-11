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
 * All code from here was taken from various places of https://github.com/ThreeTen/threetenbp with few changes
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
