/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateJvmKt")
package kotlinx.datetime

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.time.DateTimeException
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.LocalDate as jtLocalDate

actual object LocalDateLongSerializer: KSerializer<LocalDate> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): LocalDate =
        LocalDate(jtLocalDate.ofEpochDay(decoder.decodeLong()))

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeLong(value.value.toEpochDay())
    }

}

public actual class LocalDate internal constructor(internal val value: jtLocalDate) : Comparable<LocalDate> {
    actual companion object {
        public actual fun parse(isoString: String): LocalDate = try {
            jtLocalDate.parse(isoString).let(::LocalDate)
        } catch (e: DateTimeParseException) {
            throw DateTimeFormatException(e)
        }

        internal actual val MIN: LocalDate = LocalDate(jtLocalDate.MIN)
        internal actual val MAX: LocalDate = LocalDate(jtLocalDate.MAX)
    }

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int) :
            this(try {
                jtLocalDate.of(year, monthNumber, dayOfMonth)
            } catch (e: DateTimeException) {
                throw IllegalArgumentException(e)
            })

    public actual constructor(year: Int, month: Month, dayOfMonth: Int) : this(year, month.number, dayOfMonth)

    public actual val year: Int get() = value.year
    public actual val monthNumber: Int get() = value.monthValue
    public actual val month: Month get() = value.month
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek
    public actual val dayOfYear: Int get() = value.dayOfYear

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDate && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDate): Int = this.value.compareTo(other.value)
}

public actual fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate =
        plus(1L, unit)

public actual fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate =
        plus(value.toLong(), unit)

public actual fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate =
        plus(-value.toLong(), unit)

public actual fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate =
        try {
            when (unit) {
                is DateTimeUnit.DateBased.DayBased -> {
                    val addDays: Long = safeMultiply(value, unit.days.toLong())
                    ofEpochDayChecked(safeAdd(this.value.toEpochDay(), addDays))
                }
                is DateTimeUnit.DateBased.MonthBased ->
                    this.value.plusMonths(safeMultiply(value, unit.months.toLong()))
            }.let(::LocalDate)
        } catch (e: Exception) {
            if (e !is DateTimeException && e !is ArithmeticException) throw e
            throw DateTimeArithmeticException("The result of adding $value of $unit to $this is out of LocalDate range.", e)
        }

private val minEpochDay = java.time.LocalDate.MIN.toEpochDay()
private val maxEpochDay = java.time.LocalDate.MAX.toEpochDay()
private fun ofEpochDayChecked(epochDay: Long): java.time.LocalDate {
    // LocalDate.ofEpochDay doesn't actually check that the argument doesn't overflow year calculation
    if (epochDay !in minEpochDay..maxEpochDay)
        throw DateTimeException("The resulting day $epochDay is out of supported LocalDate range.")
    return java.time.LocalDate.ofEpochDay(epochDay)
}

public actual operator fun LocalDate.plus(period: DatePeriod): LocalDate = try {
    with(period) {
        return@with value
                .run { if (totalMonths != 0) plusMonths(totalMonths.toLong()) else this }
                .run { if (days != 0) plusDays(days.toLong()) else this }

    }.let(::LocalDate)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException("The result of adding $value to $this is out of LocalDate range.")
}


public actual fun LocalDate.periodUntil(other: LocalDate): DatePeriod {
    var startD = this.value
    val endD = other.value
    val months = startD.until(endD, ChronoUnit.MONTHS); startD = startD.plusMonths(months)
    val days = startD.until(endD, ChronoUnit.DAYS)

    if (months > Int.MAX_VALUE || months < Int.MIN_VALUE) {
        throw DateTimeArithmeticException("The number of months between $this and $other does not fit in an Int")
    }
    return DatePeriod(totalMonths = months.toInt(), days.toInt())
}

public actual fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Int = when(unit) {
    is DateTimeUnit.DateBased.MonthBased -> (this.value.until(other.value, ChronoUnit.MONTHS) / unit.months).clampToInt()
    is DateTimeUnit.DateBased.DayBased -> (this.value.until(other.value, ChronoUnit.DAYS) / unit.days).clampToInt()
}

public actual fun LocalDate.daysUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.DAYS).clampToInt()

public actual fun LocalDate.monthsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.MONTHS).clampToInt()

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.YEARS).clampToInt()
