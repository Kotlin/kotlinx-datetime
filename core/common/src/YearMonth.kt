/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable

/**
 * The year-month part of [LocalDate], without a day-of-month.
 *
 * This class represents years and months without a reference to a particular time zone.
 * As such, these objects may denote different time intervals in different time zones: for someone in Berlin,
 * `2020-08` started and ended at different moments from those for someone in Tokyo.
 *
 * ### Arithmetic operations
 *
 * The arithmetic on [YearMonth] values is defined independently of the time zone (so `2020-08` plus one month
 * is `2020-09` everywhere).
 *
 * Operations with [DateTimeUnit.MonthBased] are provided for [YearMonth]:
 * - [YearMonth.plus] and [YearMonth.minus] allow expressing concepts like "two months later".
 * - [YearMonth.until] and its shortcuts [YearMonth.monthsUntil] and [YearMonth.yearsUntil]
 *   can be used to find the number of months or years between two dates.
 *
 * ### Platform specifics
 *
 * The range of supported years is unspecified, but at least is enough to represent year-months of all instants
 * between [Instant.DISTANT_PAST][kotlin.time.Instant.DISTANT_PAST]
 * and [Instant.DISTANT_FUTURE][kotlin.time.Instant.DISTANT_FUTURE] in any time zone.
 *
 * On the JVM,
 * there are `YearMonth.toJavaYearMonth()` and `java.time.YearMonth.toKotlinYearMonth()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 * Similarly, on the Darwin platforms, there is a `YearMonth.toNSDateComponents()` extension function.
 *
 * ### Construction, serialization, and deserialization
 *
 * [YearMonth] can be constructed directly from its components using the constructor.
 * See sample 1.
 *
 * [parse] and [toString] methods can be used to obtain a [YearMonth] from and convert it to a string in the
 * ISO 8601 extended format.
 * See sample 2.
 *
 * [parse] and [YearMonth.format] both support custom formats created with [Format] or defined in [Formats].
 * See sample 3.
 *
 * Additionally, there are several `kotlinx-serialization` serializers for [YearMonth]:
 * - The default serializer, delegating to [toString] and [parse].
 * - [YearMonthIso8601Serializer] for the ISO 8601 extended format.
 * - [YearMonthComponentSerializer] for an object with components.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.constructorFunctionMonthNumber
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.simpleParsingAndFormatting
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.customFormat
 */
@Serializable(with = YearMonthSerializer::class)
public expect class YearMonth
/**
 * Constructs a [YearMonth] instance from the given year-month components.
 *
 * The [month] component is 1-based.
 *
 * The supported ranges of components:
 * - [year] the range is unspecified, but at least is enough to represent year-months of all instants between
 *          [Instant.DISTANT_PAST][kotlin.time.Instant.DISTANT_PAST]
 *          and [Instant.DISTANT_FUTURE][kotlin.time.Instant.DISTANT_FUTURE] in any time zone.
 * - [month] `1..12`
 *
 * @throws IllegalArgumentException if any parameter is out of range.
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.constructorFunctionMonthNumber
 */
public constructor(year: Int, month: Int) : Comparable<YearMonth> {
    /**
     * Returns the year component of the year-month.
     *
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.year
     */
    public val year: Int

    /**
     * Returns the month ([Month]) component of the year-month.
     *
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.month
     */
    public val month: Month

    /**
     * Returns the first day of the year-month.
     *
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.firstAndLastDay
     */
    public val firstDay: LocalDate

    /**
     * Returns the last day of the year-month.
     *
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.firstAndLastDay
     */
    public val lastDay: LocalDate

    /**
     * Returns the number of days in the year-month.
     *
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.numberOfDays
     */
    public val numberOfDays: Int

    internal val monthNumber: Int

    /**
     * Returns the range of days in the year-month.
     *
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.days
     */
    public val days: LocalDateRange

    /**
     * Constructs a [YearMonth] instance from the given year-month components.
     *
     * The range for [year] is unspecified, but at least is enough to represent year-months of all instants
     * between [Instant.DISTANT_PAST][kotlin.time.Instant.DISTANT_PAST]
     * and [Instant.DISTANT_FUTURE][kotlin.time.Instant.DISTANT_FUTURE] in any time zone.
     *
     * @throws IllegalArgumentException if [year] is out of range.
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.constructorFunction
     */
    public constructor(year: Int, month: Month)

    public companion object {
        /**
         * A shortcut for calling [DateTimeFormat.parse].
         *
         * Parses a string that represents a date and returns the parsed [YearMonth] value.
         *
         * If [format] is not specified, [Formats.ISO] is used.
         * `2023-01` is an example of a string in this format.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [YearMonth] are exceeded.
         *
         * @see YearMonth.toString for formatting using the default format.
         * @see YearMonth.format for formatting using a custom format.
         * @sample kotlinx.datetime.test.samples.YearMonthSamples.parsing
         */
        public fun parse(input: CharSequence, format: DateTimeFormat<YearMonth> = Formats.ISO): YearMonth

        /**
         * Creates a new format for parsing and formatting [YearMonth] values.
         *
         * There is a collection of predefined formats in [YearMonth.Formats].
         *
         * @throws IllegalArgumentException if parsing using this format is ambiguous.
         * @sample kotlinx.datetime.test.samples.YearMonthSamples.customFormat
         */
        @Suppress("FunctionName")
        public fun Format(block: DateTimeFormatBuilder.WithYearMonth.() -> Unit): DateTimeFormat<YearMonth>
    }

    /**
     * A collection of predefined formats for parsing and formatting [YearMonth] values.
     *
     * [YearMonth.Formats.ISO] is a popular predefined format.
     * [YearMonth.parse] and [YearMonth.toString] can be used as convenient shortcuts for it.
     *
     * Use [YearMonth.Format] to create a custom [kotlinx.datetime.format.DateTimeFormat] for [YearMonth] values.
     */
    public object Formats {
        /**
         * ISO 8601 extended format, which is the format used by [YearMonth.toString] and [YearMonth.parse].
         *
         * Examples of year-months in ISO 8601 format:
         * - `2020-08`
         * - `+12020-08`
         * - `0000-08`
         * - `-0001-08`
         *
         * See ISO-8601-1:2019, 5.2.2.2a), using the "expanded calendar year" extension from 5.2.2.3b), generalized
         * to any number of digits in the year for years that fit in an [Int].
         *
         * @sample kotlinx.datetime.test.samples.YearMonthSamples.Formats.iso
         */
        public val ISO: DateTimeFormat<YearMonth>
    }

    /**
     * Creates a [YearMonthRange] from `this` to [that], inclusive.
     *
     * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.simpleRangeCreation
     */
    public operator fun rangeTo(that: YearMonth): YearMonthRange

    /**
     * Creates a [YearMonthRange] from `this` to [that], exclusive, i.e., from this to (that - 1 month)
     *
     * @sample kotlinx.datetime.test.samples.YearMonthRangeSamples.simpleRangeCreation
     */
    public operator fun rangeUntil(that: YearMonth): YearMonthRange

    /**
     * Compares `this` date with the [other] year-month.
     * Returns zero if this year-month represents the same month as the other (meaning they are equal to one other),
     * a negative number if this year-month is earlier than the other,
     * and a positive number if this year-month is later than the other.
     *
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.compareToSample
     */
    override fun compareTo(other: YearMonth): Int

    /**
     * Converts this year-month to the extended ISO 8601 string representation.
     *
     * @see Formats.ISO for the format details.
     * @see parse for the dual operation: obtaining [YearMonth] from a string.
     * @see YearMonth.format for formatting using a custom format.
     * @sample kotlinx.datetime.test.samples.YearMonthSamples.toStringSample
     */
    override fun toString(): String
}

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.formatting
 */
public fun YearMonth.format(format: DateTimeFormat<YearMonth>): String = format.format(this)

/**
 * Returns the year-month part of this date.
 *
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.yearMonth
 */
public val LocalDate.yearMonth: YearMonth get() = YearMonth(year, month)

/**
 * Combines this year-month with the specified day-of-month into a [LocalDate] value.
 *
 * @throw IllegalArgumentException if the [day] is out of range for this year-month.
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.onDay
 */
public fun YearMonth.onDay(day: Int): LocalDate = LocalDate(year, month, day)

/**
 * Returns the number of whole years between two year-months.
 *
 * The value is rounded toward zero.
 *
 * @see YearMonth.until
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.yearsUntil
 */
public fun YearMonth.yearsUntil(other: YearMonth): Int =
    ((other.prolepticMonth - prolepticMonth) / 12L).toInt()

/**
 * Returns the number of whole months between two year-months.
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result
 * or [Int.MIN_VALUE] for a negative result.
 *
 * @see YearMonth.until
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.monthsUntil
 */
public fun YearMonth.monthsUntil(other: YearMonth): Int =
    (other.prolepticMonth - prolepticMonth).clampToInt()

/**
 * Returns the whole number of the specified month-based [units][unit] between `this` and [other] year-months.
 *
 * The value returned is:
 * - Positive or zero if this year-month is earlier than the other.
 * - Negative or zero if this year-month is later than the other.
 * - Zero if this date is equal to the other.
 *
 * The value is rounded toward zero.
 *
 * @see YearMonth.monthsUntil
 * @see YearMonth.yearsUntil
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.until
 */
public fun YearMonth.until(other: YearMonth, unit: DateTimeUnit.MonthBased): Long =
    (other.prolepticMonth - prolepticMonth) / unit.months

/**
 * The [YearMonth] 12 months later.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [YearMonth].
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.plusYear
 */
public fun YearMonth.plusYear(): YearMonth = plus(1, DateTimeUnit.YEAR)

/**
 * The [YearMonth] 12 months earlier.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [YearMonth].
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.minusYear
 */
public fun YearMonth.minusYear(): YearMonth = minus(1, DateTimeUnit.YEAR)

/**
 * The [YearMonth] one month later.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [YearMonth].
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.plusMonth
 */
public fun YearMonth.plusMonth(): YearMonth = plus(1, DateTimeUnit.MONTH)

/**
 * The [YearMonth] one month earlier.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [YearMonth].
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.minusMonth
 */
public fun YearMonth.minusMonth(): YearMonth = minus(1, DateTimeUnit.MONTH)

/**
 * Returns a [YearMonth] that results from adding the [value] number of the specified [unit] to this year-month.
 *
 * If the [value] is positive, the returned year-month is later than this year-month.
 * If the [value] is negative, the returned year-month is earlier than this year-month.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [YearMonth].
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.plus
 */
public fun YearMonth.plus(value: Int, unit: DateTimeUnit.MonthBased): YearMonth =
    plus(value.toLong(), unit)

/**
 * Returns a [YearMonth] that results from subtracting the [value] number of the specified [unit] from this year-month.
 *
 * If the [value] is positive, the returned year-month is earlier than this year-month.
 * If the [value] is negative, the returned year-month is later than this year-month.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [YearMonth].
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.minus
 */
public fun YearMonth.minus(value: Int, unit: DateTimeUnit.MonthBased): YearMonth =
    minus(value.toLong(), unit)

/**
 * Returns a [YearMonth] that results from adding the [value] number of the specified [unit] to this year-month.
 *
 * If the [value] is positive, the returned year-month is later than this year-month.
 * If the [value] is negative, the returned year-month is earlier than this year-month.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [YearMonth].
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.plus
 */
public fun YearMonth.plus(value: Long, unit: DateTimeUnit.MonthBased): YearMonth = try {
    safeMultiply(value, unit.months.toLong()).let { monthsToAdd ->
        if (monthsToAdd == 0L) {
            this
        } else {
            YearMonth.fromProlepticMonth(safeAdd(prolepticMonth, monthsToAdd))
        }
    }
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding $value of $unit to $this", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of YearMonth exceeded when adding $value of $unit to $this", e)
}

/**
 * Returns a [YearMonth] that results from subtracting the [value] number of the specified [unit] from this year-month.
 *
 * If the [value] is positive, the returned year-month is earlier than this year-month.
 * If the [value] is negative, the returned year-month is later than this year-month.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [YearMonth].
 * @sample kotlinx.datetime.test.samples.YearMonthSamples.minus
 */
public fun YearMonth.minus(value: Long, unit: DateTimeUnit.MonthBased): YearMonth =
    if (value != Long.MIN_VALUE) plus(-value, unit) else plus(Long.MAX_VALUE, unit).plus(1, unit)

internal val YearMonth.prolepticMonth: Long get() = year * 12L + (monthNumber - 1)

internal fun YearMonth.Companion.fromProlepticMonth(prolepticMonth: Long): YearMonth {
    val year = prolepticMonth.floorDiv(12)
    require(year in LocalDate.MIN.year..LocalDate.MAX.year) {
        "Year $year is out of range: ${LocalDate.MIN.year}..${LocalDate.MAX.year}"
    }
    val month = prolepticMonth.mod(12) + 1
    return YearMonth(year.toInt(), month)
}

internal val YearMonth.Companion.MAX get() = LocalDate.MAX.yearMonth
internal val YearMonth.Companion.MIN get() = LocalDate.MIN.yearMonth
