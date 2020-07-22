/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public expect class LocalDate : Comparable<LocalDate> {
    companion object {
        /**
         * @throws DateTimeFormatException if the text cannot be parsed or the boundaries of [LocalDate] are exceeded.
         */
        public fun parse(isoString: String): LocalDate

        internal val MIN: LocalDate
        internal val MAX: LocalDate
    }

    /**
     * @throws IllegalArgumentException if any parameter is out of range, or if [dayOfMonth] is invalid for [month] and
     * [year].
     */
    public constructor(year: Int, monthNumber: Int, dayOfMonth: Int)

    public val year: Int
    public val monthNumber: Int
    public val month: Month
    public val dayOfMonth: Int
    public val dayOfWeek: DayOfWeek
    public val dayOfYear: Int

    public override fun compareTo(other: LocalDate): Int
}

/**
 * @throws DateTimeFormatException if the text cannot be parsed or the boundaries of [LocalDate] are exceeded.
 */
public fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

/**
 * @throws IllegalArgumentException if the calendar unit is not date-based.
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
internal expect fun LocalDate.plus(value: Long, unit: CalendarUnit): LocalDate

/**
 * @throws IllegalArgumentException if the calendar unit is not date-based.
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
internal expect fun LocalDate.plus(value: Int, unit: CalendarUnit): LocalDate

/**
 * @throws IllegalArgumentException if [period] has non-zero time (as opposed to date) components.
 * @throws DateTimeArithmeticException if arithmetic overflow occurs or the boundaries of [LocalDate] are exceeded at
 * any point in intermediate computations.
 */
expect operator fun LocalDate.plus(period: DatePeriod): LocalDate

/** */
expect fun LocalDate.periodUntil(other: LocalDate): DatePeriod

/** */
operator fun LocalDate.minus(other: LocalDate): DatePeriod = other.periodUntil(this)

public expect fun LocalDate.daysUntil(other: LocalDate): Int
public expect fun LocalDate.monthsUntil(other: LocalDate): Int
public expect fun LocalDate.yearsUntil(other: LocalDate): Int

public fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate =
        plus(unit.calendarScale, unit.calendarUnit)
public fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate =
        plus(value * unit.calendarScale, unit.calendarUnit)
public fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate =
        plus(value * unit.calendarScale, unit.calendarUnit)

public fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Int = when(unit) {
    is DateTimeUnit.DateBased.MonthBased -> (monthsUntil(other) / unit.months).toInt()
    is DateTimeUnit.DateBased.DayBased -> (daysUntil(other) / unit.days).toInt()
}
