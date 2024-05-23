/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateJvmKt")
package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.safeAdd
import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.LocalDate as jtLocalDate

@Serializable(with = LocalDateIso8601Serializer::class)
public actual class LocalDate internal constructor(
    internal val value: jtLocalDate
) : Comparable<LocalDate>, java.io.Serializable {
    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalDate>): LocalDate =
            if (format === Formats.ISO) {
                try {
                    jtLocalDate.parse(input).let(::LocalDate)
                } catch (e: DateTimeParseException) {
                    throw DateTimeFormatException(e)
                }
            } else {
                format.parse(input)
            }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): LocalDate = parse(input = isoString)

        internal actual val MIN: LocalDate = LocalDate(jtLocalDate.MIN)
        internal actual val MAX: LocalDate = LocalDate(jtLocalDate.MAX)

        public actual fun fromEpochDays(epochDays: Int): LocalDate =
            LocalDate(jtLocalDate.ofEpochDay(epochDays.toLong()))

        @Suppress("FunctionName")
        public actual fun Format(block: DateTimeFormatBuilder.WithDate.() -> Unit): DateTimeFormat<LocalDate> =
            LocalDateFormat.build(block)

        private const val serialVersionUID: Long = 1L
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalDate> get() = ISO_DATE

        public actual val ISO_BASIC: DateTimeFormat<LocalDate> = ISO_DATE_BASIC
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

    public actual fun toEpochDays(): Int = value.toEpochDay().clampToInt()

    private fun writeObject(oStream: java.io.ObjectOutputStream) {
        oStream.defaultWriteObject()
        oStream.writeObject(value.toString())
    }

    private fun readObject(iStream: java.io.ObjectInputStream) {
        iStream.defaultReadObject()
        val field = this::class.java.getDeclaredField(::value.name)
        field.isAccessible = true
        field.set(this, jtLocalDate.parse(iStream.readObject() as String))
    }

    private fun readObjectNoData() {
        throw java.io.InvalidObjectException("Stream data required")
    }
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public actual fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate =
        plus(1L, unit)

public actual fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate =
        plus(value.toLong(), unit)

public actual fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate =
        plus(-value.toLong(), unit)

public actual fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate =
        try {
            when (unit) {
                is DateTimeUnit.DayBased -> {
                    val addDays: Long = safeMultiply(value, unit.days.toLong())
                    ofEpochDayChecked(safeAdd(this.value.toEpochDay(), addDays))
                }
                is DateTimeUnit.MonthBased ->
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
    is DateTimeUnit.MonthBased -> (this.value.until(other.value, ChronoUnit.MONTHS) / unit.months).clampToInt()
    is DateTimeUnit.DayBased -> (this.value.until(other.value, ChronoUnit.DAYS) / unit.days).clampToInt()
}

public actual fun LocalDate.daysUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.DAYS).clampToInt()

public actual fun LocalDate.monthsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.MONTHS).clampToInt()

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.YEARS).clampToInt()
