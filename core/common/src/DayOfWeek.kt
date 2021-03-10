/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.native.concurrent.*

public expect enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;
}

public val DayOfWeek.isoDayNumber: Int get() = ordinal + 1

@SharedImmutable
private val allDaysOfWeek = DayOfWeek.values().asList()
public fun DayOfWeek(isoDayNumber: Int): DayOfWeek {
    require(isoDayNumber in 1..7)
    return allDaysOfWeek[isoDayNumber - 1]
}
