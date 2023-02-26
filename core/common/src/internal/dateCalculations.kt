/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal const val SECONDS_PER_HOUR = 60 * 60

internal const val SECONDS_PER_MINUTE = 60

internal const val MINUTES_PER_HOUR = 60

internal const val HOURS_PER_DAY = 24

internal const val SECONDS_PER_DAY: Int = SECONDS_PER_HOUR * HOURS_PER_DAY

internal const val NANOS_PER_ONE = 1_000_000_000
internal const val NANOS_PER_MILLI = 1_000_000
internal const val MILLIS_PER_ONE = 1_000

internal const val NANOS_PER_DAY: Long = NANOS_PER_ONE * SECONDS_PER_DAY.toLong()

internal const val NANOS_PER_MINUTE: Long = NANOS_PER_ONE * SECONDS_PER_MINUTE.toLong()

internal const val NANOS_PER_HOUR = NANOS_PER_ONE * SECONDS_PER_HOUR.toLong()

internal const val MILLIS_PER_DAY: Int = SECONDS_PER_DAY * MILLIS_PER_ONE

// org.threeten.bp.chrono.IsoChronology#isLeapYear
internal fun isLeapYear(year: Int): Boolean {
    val prolepticYear: Long = year.toLong()
    return prolepticYear and 3 == 0L && (prolepticYear % 100 != 0L || prolepticYear % 400 == 0L)
}

internal fun Int.monthLength(isLeapYear: Boolean): Int =
    when (this) {
        2 -> if (isLeapYear) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

// org.threeten.bp.LocalDate#toEpochDay
internal fun dateToEpochDays(year: Int, monthNumber: Int, dayOfMonth: Int): Int {
    val y = year
    val m = monthNumber
    var total = 0
    total += 365 * y
    if (y >= 0) {
        total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
    } else {
        total -= y / -4 - y / -100 + y / -400
    }
    total += ((367 * m - 362) / 12)
    total += dayOfMonth - 1
    if (m > 2) {
        total--
        if (!isLeapYear(year)) {
            total--
        }
    }
    return total - DAYS_0000_TO_1970
}

/**
 * The number of days in a 400 year cycle.
 */
internal const val DAYS_PER_CYCLE = 146097

/**
 * The number of days from year zero to year 1970.
 * There are five 400 year cycles from year zero to 2000.
 * There are 7 leap years from 1970 to 2000.
 */
internal const val DAYS_0000_TO_1970 = DAYS_PER_CYCLE * 5 - (30 * 365 + 7)

internal fun isoDayOfWeekOnDate(year: Int, monthNumber: Int, dayOfMonth: Int): Int =
    (dateToEpochDays(year, monthNumber, dayOfMonth) + 3).mod(7) + 1

// days in a 400-year cycle = 146097
// days in a 10,000-year cycle = 146097 * 25
// seconds per day = 86400
internal const val SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L
