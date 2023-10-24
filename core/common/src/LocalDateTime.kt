/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable

/**
 * The representation of a specific civil date and time without a reference to a particular time zone.
 *
 * This class does not describe specific *moments in time*, which are represented as [Instant] values.
 * Instead, its instances can be thought of as clock readings, something that an observer in a particular time zone
 * could witness.
 * For example, `2020-08-30T18:43` is not a *moment in time*, since someone in Berlin and someone in Tokyo would witness
 * this on their clocks at different times.
 *
 * The main purpose of this class is to provide human-readable representations of [Instant] values, or to transfer them
 * as data.
 *
 * The arithmetic on [LocalDateTime] values is not provided, since without accounting for the time zone transitions it may give misleading results.
 */
@Serializable(with = LocalDateTimeIso8601Serializer::class)
public expect class LocalDateTime : Comparable<LocalDateTime> {
    public companion object {

        /**
         * Parses a string that represents a date/time value in ISO-8601 format including date and time components
         * but without any time zone component and returns the parsed [LocalDateTime] value.
         *
         * Examples of date/time in ISO-8601 format:
         * - `2020-08-30T18:43`
         * - `2020-08-30T18:43:00`
         * - `2020-08-30T18:43:00.500`
         * - `2020-08-30T18:43:00.123456789`
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDateTime] are
         * exceeded.
         */
        public fun parse(isoString: String): LocalDateTime

        /**
         * Creates a new format for parsing and formatting [LocalDateTime] values.
         *
         * Examples:
         * ```
         * // `2020-08-30 18:43:13`, using predefined date and time formats
         * LocalDateTime.Format { appendDate(LocalDate.Format.ISO); char(' ');  appendTime(LocalTime.Format.ISO) }
         *
         * // `08/30 18:43:13`, using a custom format:
         * LocalDateTime.Format {
         *   appendMonthNumber(); char('/'); appendDayOfMonth()
         *   char(' ')
         *   appendHour(); char(':'); appendMinute()
         *   optional { char(':'); appendSecond() }
         * }
         * ```
         *
         * Only parsing and formatting of well-formed values is supported. If the input does not fit the boundaries
         * (for example, [dayOfMonth] is 31 for February), consider using [DateTimeComponents.Format] instead.
         *
         * There is a collection of predefined formats in [LocalDateTime.Formats].
         */
        @Suppress("FunctionName")
        public fun Format(builder: DateTimeFormatBuilder.WithDateTime.() -> Unit): DateTimeFormat<LocalDateTime>

        internal val MIN: LocalDateTime
        internal val MAX: LocalDateTime
    }

    /**
     * A collection of predefined formats for parsing and formatting [LocalDateTime] values.
     *
     * See [LocalDateTime.Formats.ISO] and [LocalDateTime.Formats.ISO_BASIC] for popular predefined formats.
     * [LocalDateTime.parse] and [LocalDateTime.toString] can be used as convenient shortcuts for the
     * [LocalDateTime.Formats.ISO] format.
     *
     * If predefined formats are not sufficient, use [LocalDateTime.Format] to create a custom
     * [kotlinx.datetime.format.DateTimeFormat] for [LocalDateTime] values.
     */
    public object Formats {
        /**
         * ISO 8601 extended format, which is the format used by [LocalDateTime.toString] and [LocalDateTime.parse].
         *
         * Examples of date/time in ISO 8601 format:
         * - `2020-08-30T18:43`
         * - `+12020-08-30T18:43:00`
         * - `0000-08-30T18:43:00.500`
         * - `-0001-08-30T18:43:00.123456789`
         */
        public val ISO: DateTimeFormat<LocalDateTime>

        /**
         * ISO 8601 basic format.
         *
         * Examples of date/time in ISO 8601 basic format:
         * - `20200830T1843`
         * - `+120200830T184300`
         * - `00000830T184300.500`
         * - `-00010830T184300.123456789`
         */
        public val ISO_BASIC: DateTimeFormat<LocalDateTime>
    }

    /**
     * Constructs a [LocalDateTime] instance from the given date and time components.
     *
     * The components [monthNumber] and [dayOfMonth] are 1-based.
     *
     * The supported ranges of components:
     * - [year] the range is platform dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [monthNumber] `1..12`
     * - [dayOfMonth] `1..31`, the upper bound can be less, depending on the month
     * - [hour] `0..23`
     * - [minute] `0..59`
     * - [second] `0..59`
     * - [nanosecond] `0..999_999_999`
     *
     * @throws IllegalArgumentException if any parameter is out of range, or if [dayOfMonth] is invalid for the given [monthNumber] and
     * [year].
     */
    public constructor(
        year: Int,
        monthNumber: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        nanosecond: Int = 0
    )

    /**
     * Constructs a [LocalDateTime] instance from the given date and time components.
     *
     * The supported ranges of components:
     * - [year] the range is platform dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [month] all values of the [Month] enum
     * - [dayOfMonth] `1..31`, the upper bound can be less, depending on the month
     * - [hour] `0..23`
     * - [minute] `0..59`
     * - [second] `0..59`
     * - [nanosecond] `0..999_999_999`
     *
     * @throws IllegalArgumentException if any parameter is out of range, or if [dayOfMonth] is invalid for the given [month] and
     * [year].
     */
    public constructor(
        year: Int,
        month: Month,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        nanosecond: Int = 0
    )

    /**
     * Constructs a [LocalDateTime] instance by combining the given [date] and [time] parts.
     */
    public constructor(date: LocalDate, time: LocalTime)

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

    /** Returns the hour-of-day time component of this date/time value. */
    public val hour: Int

    /** Returns the minute-of-hour time component of this date/time value. */
    public val minute: Int

    /** Returns the second-of-minute time component of this date/time value. */
    public val second: Int

    /** Returns the nanosecond-of-second time component of this date/time value. */
    public val nanosecond: Int

    /** Returns the date part of this date/time value. */
    public val date: LocalDate

    /** Returns the time part of this date/time value. */
    public val time: LocalTime

    /**
     * Compares `this` date/time value with the [other] date/time value.
     * Returns zero if this value is equal to the other,
     * a negative number if this value represents earlier civil time than the other,
     * and a positive number if this value represents later civil time than the other.
     */
    // TODO: add a note about pitfalls of comparing localdatetimes falling in the Autumn transition
    public override operator fun compareTo(other: LocalDateTime): Int

    /**
     * Converts this date/time value to the ISO-8601 string representation.
     *
     * @see LocalDateTime.parse
     */
    public override fun toString(): String
}

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
 */
public fun LocalDateTime.format(format: DateTimeFormat<LocalDateTime>): String = format.format(this)

/**
 * Parses a [LocalDateTime] value using the given [format].
 * Equivalent to calling [DateTimeFormat.parse] on [format] with [input].
 *
 * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDateTime] are exceeded.
 */
public fun LocalDateTime.Companion.parse(input: CharSequence, format: DateTimeFormat<LocalDateTime>): LocalDateTime =
    format.parse(input)

/**
 * Converts this string representing a date/time value in ISO-8601 format including date and time components
 * but without any time zone component to a [LocalDateTime] value.
 *
 * See [LocalDateTime.parse] for examples of date/time string representations.
 *
 * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDateTime] are exceeded.
 */
public fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)

