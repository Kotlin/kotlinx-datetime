/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public expect enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;
}

public val DayOfWeek.number: Int get() = ordinal + 1

private val allDaysOfWeek = DayOfWeek.values().asList()
public fun DayOfWeek(number: Int): DayOfWeek {
    require(number in 1..7)
    return allDaysOfWeek[number - 1]
}
