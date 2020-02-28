/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import platform.posix.*

/**
 * Most code here was taken with few changes from https://github.com/ThreeTen/threetenbp
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
 * Safely multiply a long by an int.
 *
 * @param a  the first value
 * @param b  the second value
 * @return the new total
 * @throws ArithmeticException if the result overflows a long
 */
fun safeMultiply(a: Long, b: Int): Long {
    when (b) {
        -1 -> {
            if (a == Long.MIN_VALUE) {
                throw ArithmeticException("Multiplication overflows a long: $a * $b")
            }
            return -a
        }
        0 -> return 0L
        1 -> return a
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
fun floorDiv(a: Long, b: Long): Long {
    return if (a >= 0) a / b else (a + 1) / b - 1
}
fun floorDiv(a: Int, b: Int): Int {
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
fun floorMod(a: Long, b: Long): Long {
    return (a % b + b) % b
}
fun floorMod(a: Int, b: Int): Int {
    return (a % b + b) % b
}


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

internal fun isLeapYear(year: Int): Boolean {
    val prolepticYear: Long = year.toLong()
    return prolepticYear and 3 == 0L && (prolepticYear % 100 != 0L || prolepticYear % 400 == 0L)
}

fun ofEpochDay(epochDay: Long): LocalDate {
    var zeroDay: Long = epochDay + DAYS_0000_TO_1970
    // find the march-based year
    zeroDay -= 60 // adjust to 0000-03-01 so leap day is at end of four year cycle
    var adjust: Long = 0
    if (zeroDay < 0) { // adjust negative years to positive for calculation
        val adjustCycles: Long = (zeroDay + 1) / DAYS_PER_CYCLE - 1
        adjust = adjustCycles * 400
        zeroDay += -adjustCycles * DAYS_PER_CYCLE
    }
    var yearEst: Long = (400 * zeroDay + 591) / DAYS_PER_CYCLE
    var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
    if (doyEst < 0) { // fix estimate
        yearEst--
        doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
    }
    yearEst += adjust // reset any negative year
    val marchDoy0 = doyEst.toInt()
    // convert march-based values back to january-based
    val marchMonth0 = (marchDoy0 * 5 + 2) / 153
    val month0 = (marchMonth0 + 2) % 12
    val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
    yearEst += marchMonth0 / 10.toLong()
    return LocalDate(yearEst.toInt(), Month(month0 + 1), dom)
}
