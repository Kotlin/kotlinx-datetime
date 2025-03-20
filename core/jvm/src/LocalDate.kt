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
import kotlin.internal.*

@Serializable(with = LocalDateIso8601Serializer::class)
public actual class LocalDate internal constructor(
    internal val value: jtLocalDate
) : Comparable<LocalDate>, java.io.Serializable {
    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalDate>): LocalDate =
            if (format === Formats.ISO) {
                try {
                    val sanitizedInput = removeLeadingZerosFromLongYearFormLocalDate(input.toString())
                    jtLocalDate.parse(sanitizedInput).let(::LocalDate)
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

        public actual fun fromEpochDays(epochDays: Long): LocalDate =
            try {
                LocalDate(jtLocalDate.ofEpochDay(epochDays))
            } catch (e: DateTimeException) {
                throw IllegalArgumentException(e)
            }

        public actual fun fromEpochDays(epochDays: Int): LocalDate = fromEpochDays(epochDays.toLong())

        @Suppress("FunctionName")
        public actual fun Format(block: DateTimeFormatBuilder.WithDate.() -> Unit): DateTimeFormat<LocalDate> =
            LocalDateFormat.build(block)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalDate> get() = ISO_DATE

        public actual val ISO_BASIC: DateTimeFormat<LocalDate> = ISO_DATE_BASIC
    }

    public actual constructor(year: Int, month: Int, day: Int) :
            this(try {
                jtLocalDate.of(year, month, day)
            } catch (e: DateTimeException) {
                throw IllegalArgumentException(e)
            })

    public actual constructor(year: Int, month: Month, day: Int) : this(year, month.number, day)

    @Deprecated(
        "Use kotlinx.datetime.Month",
        ReplaceWith("LocalDate(year, month.toKotlinMonth(), dayOfMonth)")
    )
    public constructor(year: Int, month: java.time.Month, dayOfMonth: Int) :
            this(year, month.toKotlinMonth(), dayOfMonth)

    public actual val year: Int get() = value.year
    @Deprecated("Use the 'month' property instead", ReplaceWith("this.month.number"), level = DeprecationLevel.WARNING)
    public actual val monthNumber: Int get() = value.monthValue
    public actual val month: Month get() = value.month.toKotlinMonth()
    @PublishedApi internal fun getMonth(): java.time.Month = value.month
    @Deprecated("Use the 'day' property instead", ReplaceWith("this.day"), level = DeprecationLevel.WARNING)
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val day: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek.toKotlinDayOfWeek()
    @PublishedApi internal fun getDayOfWeek(): java.time.DayOfWeek = value.dayOfWeek
    public actual val dayOfYear: Int get() = value.dayOfYear

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDate && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDate): Int = this.value.compareTo(other.value)

    public actual fun toEpochDays(): Long = value.toEpochDay()

    @PublishedApi
    @JvmName("toEpochDays")
    internal fun toEpochDaysJvm(): Int = value.toEpochDay().clampToInt()

    private fun writeReplace(): Any = Ser(Ser.DATE_TAG, this)
}

/**
 * @suppress
 */
@Deprecated(
    "Use the constructor that accepts a 'day'",
    ReplaceWith("LocalDate(year = year, month = month.toKotlinMonth(), day = dayOfMonth)"),
    level = DeprecationLevel.WARNING
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@LowPriorityInOverloadResolution
public fun LocalDate(year: Int, month: java.time.Month, dayOfMonth: Int): LocalDate =
    LocalDate(year = year, month = month.toKotlinMonth(), day = dayOfMonth)

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public actual fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate =
        plus(1L, unit)

@PublishedApi
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@LowPriorityInOverloadResolution
internal fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plus(value.toLong(), unit)

@PublishedApi
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@LowPriorityInOverloadResolution
internal fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plus(-value.toLong(), unit)

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
                .run { if (totalMonths != 0L) plusMonths(totalMonths) else this }
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

    return DatePeriod(totalMonths = months, days.toInt())
}

public actual fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Long = when(unit) {
    is DateTimeUnit.MonthBased -> (this.value.until(other.value, ChronoUnit.MONTHS) / unit.months)
    is DateTimeUnit.DayBased -> (this.value.until(other.value, ChronoUnit.DAYS) / unit.days)
}

@PublishedApi
@JvmName("until")
internal fun LocalDate.untilJvm(other: LocalDate, unit: DateTimeUnit.DateBased): Int =
    until(other, unit).clampToInt()

public actual fun LocalDate.daysUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.DAYS).clampToInt()

public actual fun LocalDate.monthsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.MONTHS).clampToInt()

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
        this.value.until(other.value, ChronoUnit.YEARS).toInt()
