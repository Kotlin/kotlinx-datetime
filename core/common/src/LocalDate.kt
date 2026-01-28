/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.internal.*

/**
 * The date part of [LocalDateTime].
 *
 * This class represents dates without a reference to a particular time zone.
 * As such, these objects may denote different time intervals in different time zones: for someone in Berlin,
 * `2020-08-30` started and ended at different moments from those for someone in Tokyo.
 *
 * The arithmetic on [LocalDate] values is defined independently of the time zone (so `2020-08-30` plus one day
 * is `2020-08-31` everywhere): see various [LocalDate.plus] and [LocalDate.minus] functions, as well
 * as [LocalDate.periodUntil] and various other [*until][LocalDate.daysUntil] functions.
 *
 * The range of supported years is at least enough to represent dates of all instants between
 * [Instant.DISTANT_PAST][kotlin.time.Instant.DISTANT_PAST]
 * and [Instant.DISTANT_FUTURE][kotlin.time.Instant.DISTANT_FUTURE].
 *
 * ### Arithmetic operations
 *
 * Operations with [DateTimeUnit.DateBased] and [DatePeriod] are provided for [LocalDate]:
 * - [LocalDate.plus] and [LocalDate.minus] allow expressing concepts like "two months later".
 * - [LocalDate.until] and its shortcuts [LocalDate.daysUntil], [LocalDate.monthsUntil], and [LocalDate.yearsUntil]
 *   can be used to find the number of days, months, or years between two dates.
 * - [LocalDate.periodUntil] (and [LocalDate.minus] that accepts a [LocalDate])
 *   can be used to find the [DatePeriod] between two dates.
 *
 * ### Platform specifics
 *
 * The range of supported years is unspecified, but at least is enough to represent dates of all instants between
 * [Instant.DISTANT_PAST][kotlin.time.Instant.DISTANT_PAST]
 * and [Instant.DISTANT_FUTURE][kotlin.time.Instant.DISTANT_FUTURE].
 *
 * On the JVM,
 * there are `LocalDate.toJavaLocalDate()` and `java.time.LocalDate.toKotlinLocalDate()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 * Similarly, on the Darwin platforms, there is a `LocalDate.toNSDateComponents()` extension function.
 *
 * ### Construction, serialization, and deserialization
 *
 * [LocalDate] can be constructed directly from its components using the constructor.
 * See sample 1.
 *
 * [fromEpochDays] can be used to obtain a [LocalDate] from the number of days since the epoch day `1970-01-01`;
 * [toEpochDays] is the inverse operation.
 * See sample 2.
 *
 * [parse] and [toString] methods can be used to obtain a [LocalDate] from and convert it to a string in the
 * ISO 8601 extended format.
 * See sample 3.
 *
 * [parse] and [LocalDate.format] both support custom formats created with [Format] or defined in [Formats].
 * See sample 4.
 *
 * Additionally, there are several `kotlinx-serialization` serializers for [LocalDate]:
 * - The default serializer, delegating to [toString] and [parse].
 * - [LocalDateIso8601Serializer] for the ISO 8601 extended format.
 * - [LocalDateComponentSerializer] for an object with components.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.constructorFunctionMonthNumber
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.fromAndToEpochDays
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.simpleParsingAndFormatting
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.customFormat
 */
@Serializable(with = LocalDateSerializer::class)
public expect class LocalDate : Comparable<LocalDate> {
    public companion object {
        /**
         * A shortcut for calling [DateTimeFormat.parse].
         *
         * Parses a string that represents a date and returns the parsed [LocalDate] value.
         *
         * If [format] is not specified, [Formats.ISO] is used.
         * `2023-01-02` is an example of a string in this format.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDate] are exceeded.
         *
         * @see LocalDate.toString for formatting using the default format.
         * @see LocalDate.format for formatting using a custom format.
         * @sample kotlinx.datetime.test.samples.LocalDateSamples.parsing
         */
        public fun parse(input: CharSequence, format: DateTimeFormat<LocalDate> = getIsoDateFormat()): LocalDate

        /**
         * Returns a [LocalDate] that is [epochDays] number of days from the epoch day `1970-01-01`.
         *
         * @throws IllegalArgumentException if the result exceeds the boundaries of [LocalDate].
         * @see LocalDate.toEpochDays
         * @sample kotlinx.datetime.test.samples.LocalDateSamples.fromAndToEpochDays
         */
        public fun fromEpochDays(epochDays: Long): LocalDate

        /**
         * Returns a [LocalDate] that is [epochDays] number of days from the epoch day `1970-01-01`.
         *
         * @see LocalDate.toEpochDays
         * @sample kotlinx.datetime.test.samples.LocalDateSamples.fromAndToEpochDays
         */
        public fun fromEpochDays(epochDays: Int): LocalDate

        /**
         * Creates a new format for parsing and formatting [LocalDate] values.
         *
         * Only parsing and formatting of well-formed values is supported. If the input does not fit the boundaries
         * (for example, [day] is 31 for February), consider using [DateTimeComponents.Format] instead.
         *
         * There is a collection of predefined formats in [LocalDate.Formats].
         *
         * @throws IllegalArgumentException if parsing using this format is ambiguous.
         * @sample kotlinx.datetime.test.samples.LocalDateSamples.customFormat
         */
        @Suppress("FunctionName")
        public fun Format(block: DateTimeFormatBuilder.WithDate.() -> Unit): DateTimeFormat<LocalDate>

        internal val MIN: LocalDate
        internal val MAX: LocalDate
    }

    /**
     * A collection of predefined formats for parsing and formatting [LocalDate] values.
     *
     * See [LocalDate.Formats.ISO] and [LocalDate.Formats.ISO_BASIC] for popular predefined formats.
     * [LocalDate.parse] and [LocalDate.toString] can be used as convenient shortcuts for the
     * [LocalDate.Formats.ISO] format.
     *
     * If predefined formats are not sufficient, use [LocalDate.Format] to create a custom
     * [kotlinx.datetime.format.DateTimeFormat] for [LocalDate] values.
     */
    public object Formats {
        /**
         * ISO 8601 extended format, which is the format used by [LocalDate.toString] and [LocalDate.parse].
         *
         * Examples of dates in ISO 8601 format:
         * - `2020-08-30`
         * - `+12020-08-30`
         * - `0000-08-30`
         * - `-0001-08-30`
         *
         * See ISO-8601-1:2019, 5.2.2.1b), using the "expanded calendar year" extension from 5.2.2.3a), generalized
         * to any number of digits in the year for years that fit in an [Int].
         *
         * @sample kotlinx.datetime.test.samples.LocalDateSamples.Formats.iso
         */
        public val ISO: DateTimeFormat<LocalDate>

        /**
         * ISO 8601 basic format.
         *
         * Examples of dates in ISO 8601 basic format:
         * - `20200830`
         * - `+120200830`
         * - `00000830`
         * - `-00010830`
         *
         * See ISO-8601-1:2019, 5.2.2.1a), using the "expanded calendar year" extension from 5.2.2.3a), generalized
         * to any number of digits in the year for years that fit in an [Int].
         *
         * @sample kotlinx.datetime.test.samples.LocalDateSamples.Formats.isoBasic
         */
        public val ISO_BASIC: DateTimeFormat<LocalDate>
    }

    /**
     * Constructs a [LocalDate] instance from the given date components.
     *
     * The components [month] and [day] are 1-based.
     *
     * The supported ranges of components:
     * - [year] the range is at least enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST][kotlin.time.Instant.DISTANT_PAST]
     *          and [Instant.DISTANT_FUTURE][kotlin.time.Instant.DISTANT_FUTURE]
     * - [month] `1..12`
     * - [day] `1..31`, the upper bound can be less, depending on the month
     *
     * @throws IllegalArgumentException if any parameter is out of range or if [day] is invalid for the
     * given [month] and [year].
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.constructorFunctionMonthNumber
     */
    public constructor(year: Int, month: Int, day: Int)

    /**
     * Constructs a [LocalDate] instance from the given date components.
     *
     * The supported ranges of components:
     * - [year] the range at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST][kotlin.time.Instant.DISTANT_PAST]
     *          and [Instant.DISTANT_FUTURE][kotlin.time.Instant.DISTANT_FUTURE]
     * - [month] all values of the [Month] enum
     * - [day] `1..31`, the upper bound can be less, depending on the month
     *
     * @throws IllegalArgumentException if any parameter is out of range or if [day] is invalid for the
     * given [month] and [year].
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.constructorFunction
     */
    public constructor(year: Int, month: Month, day: Int)

    /**
     * Returns the year component of the date.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.year
     */
    public val year: Int

    /** @suppress */
    @Deprecated("Use the 'month' property instead", ReplaceWith("this.month.number"), level = DeprecationLevel.WARNING)
    public val monthNumber: Int

    /**
     * Returns the month ([Month]) component of the date.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.month
     */
    public val month: Month

    /**
     * Returns the day-of-month (`1..31`) component of the date.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.day
     */
    public val day: Int

    /** @suppress */
    @Deprecated("Use the 'day' property instead", ReplaceWith("this.day"), level = DeprecationLevel.WARNING)
    public val dayOfMonth: Int

    /**
     * Returns the day-of-week component of the date.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.dayOfWeek
     */
    public val dayOfWeek: DayOfWeek

    /**
     * Returns the day-of-year (`1..366`) component of the date.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.dayOfYear
     */
    public val dayOfYear: Int

    /**
     * Returns the number of days since the epoch day `1970-01-01`.
     *
     * @see LocalDate.fromEpochDays
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.toEpochDays
     */
    public fun toEpochDays(): Long

    /**
     * Creates a [LocalDateRange] from `this` to [that], inclusive.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.simpleRangeCreation
     */
    public operator fun rangeTo(that: LocalDate): LocalDateRange

    /**
     * Creates a [LocalDateRange] from `this` to [that], exclusive, i.e., from this to (that - 1 day)
     *
     * @sample kotlinx.datetime.test.samples.LocalDateRangeSamples.simpleRangeCreation
     */
    public operator fun rangeUntil(that: LocalDate): LocalDateRange

    /**
     * Compares `this` date with the [other] date.
     * Returns zero if this date represents the same day as the other (meaning they are equal to one other),
     * a negative number if this date is earlier than the other,
     * and a positive number if this date is later than the other.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.compareToSample
     */
    public override fun compareTo(other: LocalDate): Int

    /**
     * Converts this date to the extended ISO 8601 string representation.
     *
     * @see Formats.ISO for the format details.
     * @see parse for the dual operation: obtaining [LocalDate] from a string.
     * @see LocalDate.format for formatting using a custom format.
     * @sample kotlinx.datetime.test.samples.LocalDateSamples.toStringSample
     */
    public override fun toString(): String
}

/**
 * @suppress
 */
@Deprecated(
    "Use the constructor that accepts a 'month' and a 'day'",
    ReplaceWith("LocalDate(year = year, month = monthNumber, day = dayOfMonth)"),
    level = DeprecationLevel.WARNING
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@LowPriorityInOverloadResolution
public fun LocalDate(year: Int, monthNumber: Int, dayOfMonth: Int): LocalDate =
    LocalDate(year = year, month = monthNumber, day = dayOfMonth)

/**
 * @suppress
 */
@Deprecated(
    "Use the constructor that accepts a 'day'",
    ReplaceWith("LocalDate(year = year, month = month, day = dayOfMonth)"),
    level = DeprecationLevel.WARNING
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@LowPriorityInOverloadResolution
public fun LocalDate(year: Int, month: Month, dayOfMonth: Int): LocalDate =
    LocalDate(year = year, month = month, day = dayOfMonth)

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.formatting
 */
public fun LocalDate.format(format: DateTimeFormat<LocalDate>): String = format.format(this)

/**
 * @suppress
 */
@Deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339", ReplaceWith("LocalDate.parse(this)"), DeprecationLevel.WARNING)
public fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

/**
 * Combines this date's components with the specified time components into a [LocalDateTime] value.
 *
 * For finding an instant that corresponds to the start of a date in a particular time zone, consider using
 * [LocalDate.atStartOfDayIn] function because a day does not always start at the fixed time 0:00:00.
 *
 * **Pitfall**: since [LocalDateTime] is not tied to a particular time zone, the resulting [LocalDateTime] may not
 * exist in the implicit time zone.
 * For example, `LocalDate(2021, 3, 28).atTime(2, 16, 20)` will successfully create a [LocalDateTime],
 * even though in Berlin, times between 2:00 and 3:00 do not exist on March 28, 2021 due to the transition to DST.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.atTimeInline
 */
public fun LocalDate.atTime(hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0): LocalDateTime =
    LocalDateTime(year, month, day, hour, minute, second, nanosecond)

/**
 * Combines this date's components with the specified [LocalTime] components into a [LocalDateTime] value.
 *
 * For finding an instant that corresponds to the start of a date in a particular time zone consider using
 * [LocalDate.atStartOfDayIn] function because a day does not always start at the fixed time 0:00:00.
 *
 * **Pitfall**: since [LocalDateTime] is not tied to a particular time zone, the resulting [LocalDateTime] may not
 * exist in the implicit time zone.
 * For example, `LocalDate(2021, 3, 28).atTime(LocalTime(2, 16, 20))` will successfully create a [LocalDateTime],
 * even though in Berlin, times between 2:00 and 3:00 do not exist on March 28, 2021, due to the transition to DST.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.atTime
 */
public fun LocalDate.atTime(time: LocalTime): LocalDateTime = LocalDateTime(this, time)


/**
 * Returns a date that results from adding components of [DatePeriod] to this date. The components are
 * added in the order from the largest units to the smallest: first years and months, then days.
 *
 * @see LocalDate.periodUntil
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDate].
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.plusPeriod
 */
public expect operator fun LocalDate.plus(period: DatePeriod): LocalDate

/**
 * Returns a date that results from subtracting components of [DatePeriod] from this date. The components are
 * subtracted in the order from the largest units to the smallest: first years and months, then days.
 *
 * @see LocalDate.periodUntil
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDate].
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.minusPeriod
 */
public operator fun LocalDate.minus(period: DatePeriod): LocalDate =
    if (period.days != Int.MIN_VALUE && period.months != Int.MIN_VALUE) {
        plus(with(period) { DatePeriod(-years, -months, -days) })
    } else {
        // TODO: calendar operations are non-associative; check if subtracting years and months separately is correct
        minus(period.years, DateTimeUnit.YEAR).minus(period.months, DateTimeUnit.MONTH)
            .minus(period.days, DateTimeUnit.DAY)
    }

/**
 * Returns a [DatePeriod] representing the difference between `this` and [other] dates.
 *
 * The components of [DatePeriod] are calculated so that adding it to `this` date results in the [other] date.
 *
 * All components of the [DatePeriod] returned are:
 * - Positive or zero if this date is earlier than the other.
 * - Negative or zero if this date is later than the other.
 * - Exactly zero if this date is equal to the other.
 *
 * @see LocalDate.minus for the same operation with the order of arguments reversed.
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.periodUntil
 */
public expect fun LocalDate.periodUntil(other: LocalDate): DatePeriod

/**
 * Returns a [DatePeriod] representing the difference between [other] and `this` dates.
 *
 * The components of [DatePeriod] are calculated so that adding it back to the `other` date results in this date.
 *
 * All components of the [DatePeriod] returned are:
 * - Negative or zero if this date is earlier than the other.
 * - Positive or zero if this date is later than the other.
 * - Exactly zero if this date is equal to the other.
 *
 * @see LocalDate.periodUntil for the same operation with the order of arguments reversed.
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.minusDate
 */
public operator fun LocalDate.minus(other: LocalDate): DatePeriod = other.periodUntil(this)

/**
 * Returns the whole number of the specified date [units][unit] between `this` and [other] dates.
 *
 * The value returned is:
 * - Positive or zero if this date is earlier than the other.
 * - Negative or zero if this date is later than the other.
 * - Zero if this date is equal to the other.
 *
 * The value is rounded toward zero.
 *
 * @see LocalDate.daysUntil
 * @see LocalDate.monthsUntil
 * @see LocalDate.yearsUntil
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.until
 */
public expect fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Long

/**
 * Returns the number of whole days between two dates.
 *
 * The value is rounded toward zero.
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see LocalDate.until
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.daysUntil
 */
public expect fun LocalDate.daysUntil(other: LocalDate): Int

/**
 * Returns the number of whole months between two dates.
 *
 * The value is rounded toward zero.
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see LocalDate.until
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.monthsUntil
 */
public expect fun LocalDate.monthsUntil(other: LocalDate): Int

/**
 * Returns the number of whole years between two dates.
 *
 * The value is rounded toward zero.
 *
 * @see LocalDate.until
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.yearsUntil
 */
public expect fun LocalDate.yearsUntil(other: LocalDate): Int

/**
 * Returns a [LocalDate] that results from adding one [unit] to this date.
 *
 * The value is rounded toward zero.
 *
 * The returned date is later than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public expect fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate

/**
 * Returns a [LocalDate] that results from subtracting one [unit] from this date.
 *
 * The value is rounded toward zero.
 *
 * The returned date is earlier than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
@Deprecated("Use the minus overload with an explicit number of units", ReplaceWith("this.minus(1, unit)"))
public fun LocalDate.minus(unit: DateTimeUnit.DateBased): LocalDate = plus(-1, unit)

/**
 * Returns a [LocalDate] that results from adding the [value] number of the specified [unit] to this date.
 *
 * If the [value] is positive, the returned date is later than this date.
 * If the [value] is negative, the returned date is earlier than this date.
 *
 * The value is rounded toward zero.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.plus
 */
public fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plus(value.toLong(), unit)

/**
 * Returns a [LocalDate] that results from subtracting the [value] number of the specified [unit] from this date.
 *
 * If the [value] is positive, the returned date is earlier than this date.
 * If the [value] is negative, the returned date is later than this date.
 *
 * The value is rounded toward zero.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.minus
 */
public fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate = plus(-(value.toLong()), unit)

/**
 * Returns a [LocalDate] that results from adding the [value] number of the specified [unit] to this date.
 *
 * If the [value] is positive, the returned date is later than this date.
 * If the [value] is negative, the returned date is earlier than this date.
 *
 * The value is rounded toward zero.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.plus
 */
public expect fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate

/**
 * Returns a [LocalDate] that results from subtracting the [value] number of the specified [unit] from this date.
 *
 * If the [value] is positive, the returned date is earlier than this date.
 * If the [value] is negative, the returned date is later than this date.
 *
 * The value is rounded toward zero.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 * @sample kotlinx.datetime.test.samples.LocalDateSamples.minus
 */
public fun LocalDate.minus(value: Long, unit: DateTimeUnit.DateBased): LocalDate = plus(-value, unit)

// A workaround for https://youtrack.jetbrains.com/issue/KT-65484
internal fun getIsoDateFormat() = LocalDate.Formats.ISO
