/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public expect class LocalDate : Comparable<LocalDate> {
    companion object {
        public fun parse(isoString: String): LocalDate
    }
    public constructor(year: Int, monthNumber: Int, dayOfMonth: Int)

    public val year: Int
    public val monthNumber: Int
    public val month: Month
    public val dayOfMonth: Int
    public val dayOfWeek: DayOfWeek
    public val dayOfYear: Int

    public override fun compareTo(other: LocalDate): Int
}

public fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

expect fun LocalDate.plus(value: Long, unit: CalendarUnit): LocalDate
expect fun LocalDate.plus(value: Int, unit: CalendarUnit): LocalDate
expect operator fun LocalDate.plus(period: CalendarPeriod): LocalDate

expect fun LocalDate.periodUntil(other: LocalDate): CalendarPeriod
operator fun LocalDate.minus(other: LocalDate): CalendarPeriod = other.periodUntil(this)
