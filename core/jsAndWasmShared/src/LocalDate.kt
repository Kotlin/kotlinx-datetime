/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ChronoUnit
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.datetime.internal.JSJoda.LocalDate as jtLocalDate

@Serializable(with = LocalDateIso8601Serializer::class)
public actual class LocalDate internal constructor(internal val value: jtLocalDate) : Comparable<LocalDate> {
    public actual companion object {
        public actual fun parse(isoString: String): LocalDate = try {
            jsTry { jtLocalDate.parse(isoString) }.let(::LocalDate)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
            throw e
        }

        internal actual val MIN: LocalDate = LocalDate(jtLocalDate.MIN)
        internal actual val MAX: LocalDate = LocalDate(jtLocalDate.MAX)

        public actual fun fromEpochDays(epochDays: Int): LocalDate = try {
            jsTry {  LocalDate(jtLocalDate.ofEpochDay(epochDays)) }
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
            throw e
        }
    }

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int) :
            this(try {
                jsTry { jtLocalDate.of(year, monthNumber, dayOfMonth) }
            } catch (e: Throwable) {
                if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
                throw e
            })

    public actual constructor(year: Int, month: Month, dayOfMonth: Int) : this(year, month.number, dayOfMonth)

    public actual val year: Int get() = value.year().toInt()
    public actual val monthNumber: Int get() = value.monthValue().toInt()
    public actual val month: Month get() = value.month().toMonth()
    public actual val dayOfMonth: Int get() = value.dayOfMonth().toInt()
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek().toDayOfWeek()
    public actual val dayOfYear: Int get() = value.dayOfYear().toInt()

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is LocalDate && (this.value === other.value || this.value.equals(other.value)))

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDate): Int = this.value.compareTo(other.value)

    public actual fun toEpochDays(): Int = value.toEpochDay().toInt()
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public actual fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate = plusNumber(1, unit)
public actual fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plusNumber(value, unit)
public actual fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plusNumber(-value, unit)
public actual fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate = plusNumber(value, unit)

private fun LocalDate.plusNumber(value: Number, unit: DateTimeUnit.DateBased): LocalDate =
        try {
            jsTry {
                when (unit) {
                    is DateTimeUnit.DayBased -> this.value.plusDays(value.toDouble() * unit.days)
                    is DateTimeUnit.MonthBased -> this.value.plusMonths(value.toDouble() * unit.months)
                }.let(::LocalDate)
            }
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException() && !e.isJodaArithmeticException()) throw e
            throw DateTimeArithmeticException("The result of adding $value of $unit to $this is out of LocalDate range.", e)
        }


public actual operator fun LocalDate.plus(period: DatePeriod): LocalDate = try {
    jsTry {
        with(period) {
            return@with value
                .run { if (totalMonths != 0) plusMonths(totalMonths) else this }
                .run { if (days != 0) plusDays(days) else this }

        }.let(::LocalDate)
    }
} catch (e: Throwable) {
    if (e.isJodaDateTimeException() || e.isJodaArithmeticException()) throw DateTimeArithmeticException(e)
    throw e
}



public actual fun LocalDate.periodUntil(other: LocalDate): DatePeriod {
    var startD = this.value
    val endD = other.value
    val months = startD.until(endD, ChronoUnit.MONTHS).toInt(); startD = startD.plusMonths(months)
    val days = startD.until(endD, ChronoUnit.DAYS).toInt()

    return DatePeriod(totalMonths = months, days)
}

public actual fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Int = when(unit) {
    is DateTimeUnit.MonthBased -> monthsUntil(other) / unit.months
    is DateTimeUnit.DayBased -> daysUntil(other) / unit.days
}

public actual fun LocalDate.daysUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.DAYS).toInt()

public actual fun LocalDate.monthsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.MONTHS).toInt()

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.YEARS).toInt()
