/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable

/**
 * The date part of [LocalDateTime].
 *
 * This class represents dates without a reference to a particular time zone.
 * As such, these objects may denote different spans of time in different time zones: for someone in Berlin,
 * `2020-08-30` started and ended at different moments from those for someone in Tokyo.
 *
 * The arithmetic on [LocalDate] values is defined independently of the time zone (so `2020-08-30` plus one day
 * is `2020-08-31` everywhere): see various [LocalDate.plus] and [LocalDate.minus] functions, as well
 * as [LocalDate.periodUntil] and various other [*until][LocalDate.daysUntil] functions.
 *
 * ### Arithmetic operations
 *
 * Operations with [DateTimeUnit.DateBased] and [DatePeriod] are provided for [LocalDate]:
 * - [LocalDate.plus] and [LocalDate.minus] allow expressing concepts like "two months later,"
 * - [LocalDate.until] and its shortcuts [LocalDate.daysUntil], [LocalDate.monthsUntil], and [LocalDate.yearsUntil]
 *   can be used to find the number of days, months, or years between two dates,
 * - [LocalDate.periodUntil] (and [LocalDate.minus] that accepts a [LocalDate])
 *   can be used to find the [DatePeriod] between two dates.
 *
 * ### Construction, serialization, and deserialization
 *
 * [LocalDate] can be constructed directly from its components, using the constructor.
 *
 * [fromEpochDays] can be used to obtain a [LocalDate] from the number of days since the epoch day `1970-01-01`;
 * [toEpochDays] is the inverse operation.
 *
 * [parse] and [toString] methods can be used to obtain a [LocalDate] from and convert it to a string in the
 * ISO 8601 extended format (for example, `2023-01-02`).
 *
 * [parse] and [LocalDate.format] both support custom formats created with [Format] or defined in [Formats].
 *
 * Additionally, there are several `kotlinx-serialization` serializers for [LocalDate]:
 * - [LocalDateIso8601Serializer] for the ISO 8601 extended format,
 * - [LocalDateComponentSerializer] for an object with components.
 */
@Serializable(with = LocalDateIso8601Serializer::class)
public expect class LocalDate : Comparable<LocalDate> {
    public companion object {
        /**
         * A shortcut for calling [DateTimeFormat.parse].
         *
         * Parses a string that represents a date and returns the parsed [LocalDate] value.
         *
         * If [format] is not specified, [Formats.ISO] is used.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDate] are exceeded.
         *
         * @see LocalDate.toString for formatting using the default format.
         * @see LocalDate.format for formatting using a custom format.
         */
        public fun parse(input: CharSequence, format: DateTimeFormat<LocalDate> = getIsoDateFormat()): LocalDate

        /**
         * Returns a [LocalDate] that is [epochDays] number of days from the epoch day `1970-01-01`.
         *
         * @throws IllegalArgumentException if the result exceeds the platform-specific boundaries of [LocalDate].
         *
         * @see LocalDate.toEpochDays
         */
        public fun fromEpochDays(epochDays: Int): LocalDate

        /**
         * Creates a new format for parsing and formatting [LocalDate] values.
         *
         * Example:
         * ```
         * // 2020 Jan 05
         * LocalDate.Format {
         *   year()
         *   char(' ')
         *   monthName(MonthNames.ENGLISH_ABBREVIATED)
         *   char(' ')
         *   dayOfMonth()
         * }
         * ```
         *
         * Only parsing and formatting of well-formed values is supported. If the input does not fit the boundaries
         * (for example, [dayOfMonth] is 31 for February), consider using [DateTimeComponents.Format] instead.
         *
         * There is a collection of predefined formats in [LocalDate.Formats].
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
         */
        public val ISO_BASIC: DateTimeFormat<LocalDate>
    }

    /**
     * Constructs a [LocalDate] instance from the given date components.
     *
     * The components [monthNumber] and [dayOfMonth] are 1-based.
     *
     * The supported ranges of components:
     * - [year] the range is platform-dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [monthNumber] `1..12`
     * - [dayOfMonth] `1..31`, the upper bound can be less, depending on the month
     *
     * @throws IllegalArgumentException if any parameter is out of range, or if [dayOfMonth] is invalid for the
     * given [monthNumber] and [year].
     */
    public constructor(year: Int, monthNumber: Int, dayOfMonth: Int)

    /**
     * Constructs a [LocalDate] instance from the given date components.
     *
     * The supported ranges of components:
     * - [year] the range is platform-dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [month] all values of the [Month] enum
     * - [dayOfMonth] `1..31`, the upper bound can be less, depending on the month
     *
     * @throws IllegalArgumentException if any parameter is out of range, or if [dayOfMonth] is invalid for the
     * given [month] and [year].
     */
    public constructor(year: Int, month: Month, dayOfMonth: Int)

    /** Returns the year component of the date. */
    public val year: Int

    /** Returns the number-of-the-month (1..12) component of the date. */
    public val monthNumber: Int

    /** Returns the month ([Month]) component of the date. */
    public val month: Month

    /** Returns the day-of-month component of the date. */
    public val dayOfMonth: Int

    /** Returns the day-of-week component of the date. */
    public val dayOfWeek: DayOfWeek

    /** Returns the day-of-year component of the date. */
    public val dayOfYear: Int

    /**
     * Returns the number of days since the epoch day `1970-01-01`.
     *
     * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
     *
     * @see LocalDate.fromEpochDays
     */
    public fun toEpochDays(): Int

    /**
     * Compares `this` date with the [other] date.
     * Returns zero if this date represents the same day as the other (i.e., equal to other),
     * a negative number if this date is earlier than the other,
     * and a positive number if this date is later than the other.
     */
    public override fun compareTo(other: LocalDate): Int

    /**
     * Converts this date to the extended ISO 8601 string representation.
     *
     * @see Formats.ISO for the format details.
     * @see parse for the dual operation: obtaining [LocalDate] from a string.
     * @see LocalDate.format for formatting using a custom format.
     */
    public override fun toString(): String
}

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
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
 * For finding an instant that corresponds to the start of a date in a particular time zone consider using
 * [LocalDate.atStartOfDayIn] function because a day does not always start at the fixed time 0:00:00.
 */
public fun LocalDate.atTime(hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0): LocalDateTime =
    LocalDateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)

/**
 * Combines this date's components with the specified [LocalTime] components into a [LocalDateTime] value.
 *
 * For finding an instant that corresponds to the start of a date in a particular time zone consider using
 * [LocalDate.atStartOfDayIn] function because a day does not always start at the fixed time 0:00:00.
 *
 * **Pitfall**: since [LocalDateTime] is not tied to a particular time zone, the resulting [LocalDateTime] may not
 * exist in the implicit time zone.
 * For example, `LocalDate(2021, 3, 28).atTime(LocalTime(2, 16, 20))` will successfully create a [LocalDateTime],
 * even though in Berlin, times between 2:00 and 3:00 do not exist on March 28, 2021 due to the transition to DST.
 */
public fun LocalDate.atTime(time: LocalTime): LocalDateTime = LocalDateTime(this, time)


/**
 * Returns a date that is the result of adding components of [DatePeriod] to this date. The components are
 * added in the order from the largest units to the smallest: first years and months, then days.
 *
 * @see LocalDate.periodUntil
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDate].
 */
public expect operator fun LocalDate.plus(period: DatePeriod): LocalDate

/**
 * Returns a date that is the result of subtracting components of [DatePeriod] from this date. The components are
 * subtracted in the order from the largest units to the smallest: first years and months, then days.
 *
 * @see LocalDate.periodUntil
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDate].
 */
public operator fun LocalDate.minus(period: DatePeriod): LocalDate =
    if (period.days != Int.MIN_VALUE && period.months != Int.MIN_VALUE) {
        plus(with(period) { DatePeriod(-years, -months, -days) })
    } else {
        // TODO: calendar operations are non-associative, check if subtracting years and months separately is correct
        minus(period.years, DateTimeUnit.YEAR).minus(period.months, DateTimeUnit.MONTH)
            .minus(period.days, DateTimeUnit.DAY)
    }

/**
 * Returns a [DatePeriod] representing the difference between `this` and [other] dates.
 *
 * The components of [DatePeriod] are calculated so that adding it to `this` date results in the [other] date.
 *
 * All components of the [DatePeriod] returned are:
 * - positive or zero if this date is earlier than the other,
 * - negative or zero if this date is later than the other,
 * - exactly zero if this date is equal to the other.
 *
 * @throws DateTimeArithmeticException if the number of months between the two dates exceeds an Int (JVM only).
 *
 * @see LocalDate.minus
 */
public expect fun LocalDate.periodUntil(other: LocalDate): DatePeriod

/**
 * Returns a [DatePeriod] representing the difference between [other] and `this` dates.
 *
 * The components of [DatePeriod] are calculated so that adding it back to the `other` date results in this date.
 *
 * All components of the [DatePeriod] returned are:
 * - negative or zero if this date is earlier than the other,
 * - positive or zero if this date is later than the other,
 * - exactly zero if this date is equal to the other.
 *
 * @throws DateTimeArithmeticException if the number of months between the two dates exceeds an Int (JVM only).
 *
 * @see LocalDate.periodUntil
 */
public operator fun LocalDate.minus(other: LocalDate): DatePeriod = other.periodUntil(this)

/**
 * Returns the whole number of the specified date [units][unit] between `this` and [other] dates.
 *
 * The value returned is:
 * - positive or zero if this date is earlier than the other,
 * - negative or zero if this date is later than the other,
 * - zero if this date is equal to the other.

 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see LocalDate.daysUntil
 * @see LocalDate.monthsUntil
 * @see LocalDate.yearsUntil
 *
 */
public expect fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Int

/**
 * Returns the number of whole days between two dates.
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see LocalDate.until
 */
public expect fun LocalDate.daysUntil(other: LocalDate): Int

/**
 * Returns the number of whole months between two dates.
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see LocalDate.until
 */
public expect fun LocalDate.monthsUntil(other: LocalDate): Int

/**
 * Returns the number of whole years between two dates.
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see LocalDate.until
 */
public expect fun LocalDate.yearsUntil(other: LocalDate): Int

/**
 * Returns a [LocalDate] that is the result of adding one [unit] to this date.
 *
 * The returned date is later than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public expect fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate

/**
 * Returns a [LocalDate] that is the result of subtracting one [unit] from this date.
 *
 * The returned date is earlier than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
@Deprecated("Use the minus overload with an explicit number of units", ReplaceWith("this.minus(1, unit)"))
public fun LocalDate.minus(unit: DateTimeUnit.DateBased): LocalDate = plus(-1, unit)

/**
 * Returns a [LocalDate] that is the result of adding the [value] number of the specified [unit] to this date.
 *
 * If the [value] is positive, the returned date is later than this date.
 * If the [value] is negative, the returned date is earlier than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
public expect fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate

/**
 * Returns a [LocalDate] that is the result of subtracting the [value] number of the specified [unit] from this date.
 *
 * If the [value] is positive, the returned date is earlier than this date.
 * If the [value] is negative, the returned date is later than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
public expect fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate

/**
 * Returns a [LocalDate] that is the result of adding the [value] number of the specified [unit] to this date.
 *
 * If the [value] is positive, the returned date is later than this date.
 * If the [value] is negative, the returned date is earlier than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
public expect fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate

/**
 * Returns a [LocalDate] that is the result of subtracting the [value] number of the specified [unit] from this date.
 *
 * If the [value] is positive, the returned date is earlier than this date.
 * If the [value] is negative, the returned date is later than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
public fun LocalDate.minus(value: Long, unit: DateTimeUnit.DateBased): LocalDate = plus(-value, unit)

// workaround for https://youtrack.jetbrains.com/issue/KT-65484
internal fun getIsoDateFormat() = LocalDate.Formats.ISO
