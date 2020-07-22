/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ChronoUnit
import kotlinx.datetime.internal.JSJoda.LocalDate as jtLocalDate

public actual class LocalDate internal constructor(internal val value: jtLocalDate) : Comparable<LocalDate> {
    actual companion object {
        public actual fun parse(isoString: String): LocalDate = try {
            jtLocalDate.parse(isoString).let(::LocalDate)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
            throw e
        }

        internal actual val MIN: LocalDate = LocalDate(jtLocalDate.MIN)
        internal actual val MAX: LocalDate = LocalDate(jtLocalDate.MAX)
    }

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int) :
            this(try {
                jtLocalDate.of(year, monthNumber, dayOfMonth)
            } catch (e: Throwable) {
                if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
                throw e
            })

    public actual val year: Int get() = value.year().toInt()
    public actual val monthNumber: Int get() = value.monthValue().toInt()
    public actual val month: Month get() = value.month().toMonth()
    public actual val dayOfMonth: Int get() = value.dayOfMonth().toInt()
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek().toDayOfWeek()
    public actual val dayOfYear: Int get() = value.dayOfYear().toInt()

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDate && this.value == other.value)

    override fun hashCode(): Int = value.hashCode().toInt()

    override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDate): Int = this.value.compareTo(other.value).toInt()
}


private fun LocalDate.plusNumber(value: Number, unit: CalendarUnit): LocalDate = try {

    when (unit) {
        CalendarUnit.YEAR -> this.value.plusYears(value)
        CalendarUnit.MONTH -> this.value.plusMonths(value)
        CalendarUnit.DAY -> this.value.plusDays(value)
        CalendarUnit.HOUR,
        CalendarUnit.MINUTE,
        CalendarUnit.SECOND,
        CalendarUnit.MILLISECOND,
        CalendarUnit.MICROSECOND,
        CalendarUnit.NANOSECOND -> throw IllegalArgumentException("Only date based units can be added to LocalDate")
    }.let(::LocalDate)
} catch (e: Throwable) {
    if (e.isJodaDateTimeException() || e.isJodaArithmeticException()) throw DateTimeArithmeticException(e)
    throw e
}

internal actual fun LocalDate.plus(value: Long, unit: CalendarUnit): LocalDate =
        plusNumber(value.toDouble(), unit)

internal actual fun LocalDate.plus(value: Int, unit: CalendarUnit): LocalDate =
        plusNumber(value, unit)



public actual operator fun LocalDate.plus(period: DatePeriod): LocalDate = try {
    with(period) {
        return@with value
                .run { if (years != 0 && months == 0) plusYears(years) else this }
                .run { if (months != 0) plusMonths(years.toDouble() * 12 + months) else this }
                .run { if (days != 0) plusDays(days) else this }

    }.let(::LocalDate)
} catch (e: Throwable) {
    if (e.isJodaDateTimeException() || e.isJodaArithmeticException()) throw DateTimeArithmeticException(e)
    throw e
}



public actual fun LocalDate.periodUntil(other: LocalDate): DatePeriod {
    var startD = this.value
    val endD = other.value
    val months = startD.until(endD, ChronoUnit.MONTHS).toInt(); startD = startD.plusMonths(months)
    val days = startD.until(endD, ChronoUnit.DAYS).toInt()

    return DatePeriod(months / 12, months % 12, days)
}

public actual fun LocalDate.daysUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.DAYS).toInt()

public actual fun LocalDate.monthsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.MONTHS).toInt()

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.YEARS).toInt()