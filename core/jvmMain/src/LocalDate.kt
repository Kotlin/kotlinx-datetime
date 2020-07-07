/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateJvmKt")
package kotlinx.datetime

import java.time.temporal.ChronoUnit
import java.time.LocalDate as jtLocalDate


public actual class LocalDate internal constructor(internal val value: jtLocalDate) : Comparable<LocalDate> {
    actual companion object {
        public actual fun parse(isoString: String): LocalDate {
            return jtLocalDate.parse(isoString).let(::LocalDate)
        }
    }

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int) :
            this(jtLocalDate.of(year, monthNumber, dayOfMonth))

    public actual val year: Int get() = value.year
    public actual val monthNumber: Int get() = value.monthValue
    public actual val month: Month get() = value.month
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek
    public actual val dayOfYear: Int get() = value.dayOfYear

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDate && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDate): Int = this.value.compareTo(other.value)
}


public actual fun LocalDate.plus(value: Long, unit: CalendarUnit): LocalDate =
        when (unit) {
            CalendarUnit.YEAR -> this.value.plusYears(value)
            CalendarUnit.MONTH -> this.value.plusMonths(value)
            CalendarUnit.WEEK -> this.value.plusWeeks(value)
            CalendarUnit.DAY -> this.value.plusDays(value)
            CalendarUnit.HOUR,
            CalendarUnit.MINUTE,
            CalendarUnit.SECOND,
            CalendarUnit.NANOSECOND -> throw IllegalArgumentException("Only date based units can be added to LocalDate")
        }.let(::LocalDate)

public actual fun LocalDate.plus(value: Int, unit: CalendarUnit): LocalDate =
        plus(value.toLong(), unit)

public actual operator fun LocalDate.plus(period: DatePeriod): LocalDate =
        with(period) {
            return@with value
                    .run { if (years != 0 && months == 0) plusYears(years.toLong()) else this }
                    .run { if (months != 0) plusMonths(years * 12L + months.toLong()) else this }
                    .run { if (days != 0) plusDays(days.toLong()) else this }

        }.let(::LocalDate)


public actual fun LocalDate.periodUntil(other: LocalDate): DatePeriod {
    var startD = this.value
    val endD = other.value
    val months = startD.until(endD, ChronoUnit.MONTHS); startD = startD.plusMonths(months)
    val days = startD.until(endD, ChronoUnit.DAYS)

    return DatePeriod((months / 12).toInt(), (months % 12).toInt(), days.toInt())
}

public actual fun LocalDate.daysUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.DAYS).clampToInt()

public actual fun LocalDate.monthsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.MONTHS).clampToInt()

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.YEARS).clampToInt()