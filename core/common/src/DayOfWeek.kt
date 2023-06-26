/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.native.concurrent.*

/**
 * The enumeration class representing the days of the week.
 */
public expect enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;
}

/**
 * The ISO-8601 number of the given day of the week. Monday is 1, Sunday is 7.
 */
public val DayOfWeek.isoDayNumber: Int get() = ordinal + 1

private val allDaysOfWeek = DayOfWeek.values().asList()

/**
 * Returns the [DayOfWeek] instance for the given ISO-8601 week day number. Monday is 1, Sunday is 7.
 */
public fun DayOfWeek(isoDayNumber: Int): DayOfWeek {
    require(isoDayNumber in 1..7)
    return allDaysOfWeek[isoDayNumber - 1]
}
