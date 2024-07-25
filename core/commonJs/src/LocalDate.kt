/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlinx.datetime.internal.JSJoda.LocalDate as jtLocalDate
import kotlinx.datetime.internal.JSJoda.ChronoUnit as jtChronoUnit

@Serializable(with = LocalDateSerializer::class)
public actual class LocalDate internal constructor(internal val value: jtLocalDate) : Comparable<LocalDate> {
    public actual companion object {

        public actual fun parse(
            input: CharSequence,
            format: DateTimeFormat<LocalDate>
        ): LocalDate = if (format === Formats.ISO) {
            try {
                jsTry { jtLocalDate.parse(input.toString()) }.let(::LocalDate)
            } catch (e: Throwable) {
                if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
                throw e
            }
        } else {
            format.parse(input)
        }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): LocalDate = parse(input = isoString)

        internal actual val MIN: LocalDate = LocalDate(jtLocalDate.MIN)
        internal actual val MAX: LocalDate = LocalDate(jtLocalDate.MAX)

        public actual fun fromEpochDays(epochDays: Int): LocalDate = try {
            LocalDate(jsTry { jtLocalDate.ofEpochDay(epochDays) })
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
            throw e
        }

        @Suppress("FunctionName")
        public actual fun Format(block: DateTimeFormatBuilder.WithDate.() -> Unit): DateTimeFormat<LocalDate> =
            LocalDateFormat.build(block)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalDate> get() = ISO_DATE

        public actual val ISO_BASIC: DateTimeFormat<LocalDate> = ISO_DATE_BASIC
    }

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int) :
            this(try {
                jsTry { jtLocalDate.of(year, monthNumber, dayOfMonth) }
            } catch (e: Throwable) {
                if (e.isJodaDateTimeException()) throw IllegalArgumentException(e)
                throw e
            })

    public actual constructor(year: Int, month: Month, dayOfMonth: Int) : this(year, month.number, dayOfMonth)

    public actual val year: Int get() = value.year()
    public actual val monthNumber: Int get() = value.monthValue()
    public actual val month: Month get() = value.month().toMonth()
    public actual val dayOfMonth: Int get() = value.dayOfMonth()
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek().toDayOfWeek()
    public actual val dayOfYear: Int get() = value.dayOfYear()

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
            when (unit) {
                is DateTimeUnit.DayBased -> jsTry { this.value.plusDays((value.toDouble() * unit.days).toInt()) }
                is DateTimeUnit.MonthBased -> jsTry { this.value.plusMonths((value.toDouble() * unit.months).toInt()) }
            }.let(::LocalDate)
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException() && !e.isJodaArithmeticException()) throw e
            throw DateTimeArithmeticException("The result of adding $value of $unit to $this is out of LocalDate range.", e)
        }


public actual operator fun LocalDate.plus(period: DatePeriod): LocalDate = try {
    with(period) {
        return@with value
                .run { if (totalMonths != 0) jsTry { plusMonths(totalMonths) } else this }
                .run { if (days != 0) jsTry { plusDays(days) } else this }

    }.let(::LocalDate)
} catch (e: Throwable) {
    if (e.isJodaDateTimeException() || e.isJodaArithmeticException()) throw DateTimeArithmeticException(e)
    throw e
}



public actual fun LocalDate.periodUntil(other: LocalDate): DatePeriod {
    var startD = this.value
    val endD = other.value
    val months = startD.until(endD, jtChronoUnit.MONTHS).toInt(); startD = jsTry { startD.plusMonths(months) }
    val days = startD.until(endD, jtChronoUnit.DAYS).toInt()

    return DatePeriod(totalMonths = months, days)
}

public actual fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Int = when(unit) {
    is DateTimeUnit.MonthBased -> monthsUntil(other) / unit.months
    is DateTimeUnit.DayBased -> daysUntil(other) / unit.days
}

public actual fun LocalDate.daysUntil(other: LocalDate): Int =
        this.value.until(other.value, jtChronoUnit.DAYS).toInt()

public actual fun LocalDate.monthsUntil(other: LocalDate): Int =
        this.value.until(other.value, jtChronoUnit.MONTHS).toInt()

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
        this.value.until(other.value, jtChronoUnit.YEARS).toInt()
