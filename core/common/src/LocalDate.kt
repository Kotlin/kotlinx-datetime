/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public expect class LocalDate : Comparable<LocalDate> {
    public companion object {
        /**
         * Parses a string that represents a date in ISO-8601 format
         * and returns the parsed [LocalDate] value.
         *
         * An example of a local date in ISO-8601 format: `2020-08-30`.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDate] are exceeded.
         */
        public fun parse(isoString: String): LocalDate

        internal val MIN: LocalDate
        internal val MAX: LocalDate
    }

    /**
     * Constructs a [LocalDate] instance from the given date components.
     *
     * The components [monthNumber] and [dayOfMonth] are 1-based.
     *
     * The supported ranges of components:
     * - [year] the range is platform dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [monthNumber] `1..12`
     * - [dayOfMonth] `1..31`, the upper bound can be less, depending on the month
     *
     * @throws IllegalArgumentException if any parameter is out of range, or if [dayOfMonth] is invalid for the given [monthNumber] and
     * [year].
     */
    public constructor(year: Int, monthNumber: Int, dayOfMonth: Int)

    /**
     * Constructs a [LocalDate] instance from the given date components.
     *
     * The supported ranges of components:
     * - [year] the range is platform dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [month] all values of the [Month] enum
     * - [dayOfMonth] `1..31`, the upper bound can be less, depending on the month
     *
     * @throws IllegalArgumentException if any parameter is out of range, or if [dayOfMonth] is invalid for the given [month] and
     * [year].
     */
    public constructor(year: Int, month: Month, dayOfMonth: Int)

    /** Returns the year component of the date. */
    public val year: Int
    /** Returns the number-of-month (1..12) component of the date. */
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
     * Compares `this` date with the [other] date.
     * Returns zero if this date represent the same day as the other (i.e. equal to other),
     * a negative number if this date is earlier than the other,
     * and a positive number if this date is later than the other.
     */
    public override fun compareTo(other: LocalDate): Int


    /**
     * Converts this date to the ISO-8601 string representation.
     *
     * @see LocalDate.parse
     */
    public override fun toString(): String
}

/**
 * Converts this string representing a date in ISO-8601 format to a [LocalDate] value.
 *
 * See [LocalDate.parse] for examples of local date string representations.
 *
 * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDate] are exceeded.
 */
public fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

/**
 * Combines this date components with the specified time components into a [LocalDateTime] value.
 *
 * For finding an instant that corresponds to the start of a date in a particular time zone consider using
 * [LocalDate.atStartOfDayIn] function because a day does not always start at the fixed time 0:00:00.
 */
public fun LocalDate.atTime(hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0): LocalDateTime =
    LocalDateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)


/**
 * Returns a date that is the result of adding components of [DatePeriod] to this date. The components are
 * added in the order from the largest units to the smallest, i.e. from years to days.
 *
 * @see LocalDate.periodUntil
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDate].
 */
public expect operator fun LocalDate.plus(period: DatePeriod): LocalDate

/**
 * Returns a date that is the result of subtracting components of [DatePeriod] from this date. The components are
 * subtracted in the order from the largest units to the smallest, i.e. from years to days.
 *
 * @see LocalDate.periodUntil
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDate].
 */
public operator fun LocalDate.minus(period: DatePeriod): LocalDate =
    if (period.days != Int.MIN_VALUE && period.months != Int.MIN_VALUE) {
        plus(with(period) { DatePeriod(-years, -months, -days) })
    } else {
        minus(period.years, DateTimeUnit.YEAR).
        minus(period.months, DateTimeUnit.MONTH).
        minus(period.days, DateTimeUnit.DAY)
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
public expect fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate

/**
 * Returns a [LocalDate] that is the result of subtracting one [unit] from this date.
 *
 * The returned date is earlier than this date.
 *
 * @throws DateTimeArithmeticException if the result exceeds the boundaries of [LocalDate].
 */
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
