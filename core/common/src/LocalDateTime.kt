/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.datetime.serializers.LocalDateTimeComponentSerializer
import kotlinx.serialization.Serializable

/**
 * The representation of a specific civil date and time without a reference to a particular time zone.
 *
 * This class does not describe specific *moments in time*, which are represented as [Instant] values.
 * Instead, its instances can be thought of as clock readings, something that someone could observe in their time zone.
 * For example, `2020-08-30T18:43` is not a *moment in time*, since someone in Berlin and someone in Tokyo would witness
 * this on their clocks at different times, but it is a [LocalDateTime].
 *
 * The main purpose of this class is to provide human-readable representations of [Instant] values, or to transfer them
 * as data.
 * Instances of [LocalDateTime] should not be stored when a specific time zone is known: in this case, it is recommended
 * to use [Instant] instead.
 *
 * ### Arithmetic operations
 *
 * The arithmetic on [LocalDateTime] values is not provided, since without accounting for the time zone transitions it
 * may give misleading results.
 *
 * For example, in Berlin, naively adding one day to `2021-03-27T02:16:20` without accounting for the time zone would
 * result in `2021-03-28T02:16:20`.
 * However, this local date-time is invalid, because the clocks moved forward from `02:00` to `03:00` on that day.
 * This is known as a "time gap", or a "spring forward" transition.
 *
 * Similarly, the local date-time `2021-10-31T02:16:20` is ambiguous,
 * because the clocks moved back from `03:00` to `02:00`.
 * This is known as a "time overlap", or a "fall back" transition.
 *
 * For these reasons, using [LocalDateTime] as an input to arithmetic operations is discouraged.
 *
 * When only arithmetic on the date component is needed, without touching the time, use [LocalDate] instead,
 * as it provides well-defined date arithmetic.
 *
 * If the time component must be taken into account, [LocalDateTime]
 * should be converted to [Instant] using a specific time zone, and the arithmetic on [Instant] should be used.
 *
 * ```
 * val timeZone = TimeZone.of("Europe/Berlin")
 * val localDateTime = LocalDateTime(2021, 3, 27, 2, 16, 20)
 * val instant = localDateTime.toInstant(timeZone)
 *
 * val instantOneDayLater = instant.plus(1, DateTimeUnit.DAY, timeZone)
 * val localDateTimeOneDayLater = instantOneDayLater.toLocalDateTime(timeZone)
 * // 2021-03-28T03:16:20, as 02:16:20 that day is in a time gap
 *
 * val instantTwoDaysLater = instant.plus(2, DateTimeUnit.DAY, timeZone)
 * val localDateTimeTwoDaysLater = instantTwoDaysLater.toLocalDateTime(timeZone)
 * // 2021-03-29T02:16:20
 * ```
 *
 * ### Construction, serialization, and deserialization
 *
 * **Pitfall**: since [LocalDateTime] is always constructed without specifying the time zone, it cannot validate
 * whether the given date and time components are valid in the implied time zone.
 * For example, `2021-03-28T02:16:20` is invalid in Berlin, as it falls into a time gap, but nothing prevents one
 * from constructing such a [LocalDateTime].
 * Before constructing a [LocalDateTime] using any API, please ensure that the result is valid in the implied time zone.
 * The recommended pattern is to convert a [LocalDateTime] to [Instant] as soon as possible (see
 * [LocalDateTime.toInstant]) and work with [Instant] values instead.
 *
 * [LocalDateTime] can be constructed directly from its components, [LocalDate] and [LocalTime], using the constructor.
 * Some additional constructors that accept the date's and time's fields directly are provided for convenience.
 *
 * [parse] and [toString] methods can be used to obtain a [LocalDateTime] from and convert it to a string in the
 * ISO 8601 extended format (for example, `2023-01-02T22:35:01`).
 *
 * [parse] and [LocalDateTime.format] both support custom formats created with [Format] or defined in [Formats].
 *
 * Additionally, there are several `kotlinx-serialization` serializers for [LocalDateTime]:
 * - [LocalDateTimeIso8601Serializer] for the ISO 8601 extended format,
 * - [LocalDateTimeComponentSerializer] for an object with components.
 *
 * @see LocalDate for only the date part of the date/time value.
 * @see LocalTime for only the time part of the date/time value.
 * @see Instant for the representation of a specific moment in time independent of a time zone.
 */
@Serializable(with = LocalDateTimeIso8601Serializer::class)
public expect class LocalDateTime : Comparable<LocalDateTime> {
    public companion object {

        /**
         * A shortcut for calling [DateTimeFormat.parse].
         *
         * Parses a string that represents a date/time value including date and time components
         * but without any time zone component and returns the parsed [LocalDateTime] value.
         *
         * If [format] is not specified, [Formats.ISO] is used.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDateTime] are
         * exceeded.
         */
        public fun parse(input: CharSequence, format: DateTimeFormat<LocalDateTime> = getIsoDateTimeFormat()): LocalDateTime

        /**
         * Creates a new format for parsing and formatting [LocalDateTime] values.
         *
         * Examples:
         * ```
         * // `2020-08-30 18:43:13`, using predefined date and time formats
         * LocalDateTime.Format { date(LocalDate.Formats.ISO); char(' '); time(LocalTime.Formats.ISO) }
         *
         * // `08/30 18:43:13`, using a custom format:
         * LocalDateTime.Format {
         *   monthNumber(); char('/'); dayOfMonth()
         *   char(' ')
         *   hour(); char(':'); minute()
         *   optional { char(':'); second() }
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
     * [LocalDateTime.Formats.ISO] is a popular predefined format.
     *
     * If predefined formats are not sufficient, use [LocalDateTime.Format] to create a custom
     * [kotlinx.datetime.format.DateTimeFormat] for [LocalDateTime] values.
     */
    public object Formats {
        /**
         * ISO 8601 extended format.
         *
         * Examples of date/time in ISO 8601 format:
         * - `2020-08-30T18:43`
         * - `+12020-08-30T18:43:00`
         * - `0000-08-30T18:43:00.5`
         * - `-0001-08-30T18:43:00.123456789`
         *
         * When formatting, seconds are always included, even if they are zero.
         * Fractional parts of the second are included if non-zero.
         *
         * Guaranteed to parse all strings that [LocalDateTime.toString] produces.
         */
        public val ISO: DateTimeFormat<LocalDateTime>
    }

    /**
     * Constructs a [LocalDateTime] instance from the given date and time components.
     *
     * The components [monthNumber] and [dayOfMonth] are 1-based.
     *
     * The supported ranges of components:
     * - [year] the range is platform-dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [monthNumber] `1..12`
     * - [dayOfMonth] `1..31`, the upper bound can be less, depending on the month
     * - [hour] `0..23`
     * - [minute] `0..59`
     * - [second] `0..59`
     * - [nanosecond] `0..999_999_999`
     *
     * @throws IllegalArgumentException if any parameter is out of range,
     * or if [dayOfMonth] is invalid for the given [monthNumber] and [year].
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
     * - [year] the range is platform-dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [month] all values of the [Month] enum
     * - [dayOfMonth] `1..31`, the upper bound can be less, depending on the month
     * - [hour] `0..23`
     * - [minute] `0..59`
     * - [second] `0..59`
     * - [nanosecond] `0..999_999_999`
     *
     * @throws IllegalArgumentException if any parameter is out of range,
     * or if [dayOfMonth] is invalid for the given [month] and [year].
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

    /** Returns the number-of-the-month (1..12) component of the date. */
    public val monthNumber: Int

    /** Returns the month ([Month]) component of the date. */
    public val month: Month

    /** Returns the day-of-month component of the date. */
    public val dayOfMonth: Int

    /** Returns the day-of-week component of the date. */
    public val dayOfWeek: DayOfWeek

    /** Returns the 1-based day-of-year component of the date. */
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
     *
     * **Pitfall**: comparing [LocalDateTime] values is less robust than comparing [Instant] values.
     * Consider the following situation, where a later moment in time corresponds to an earlier [LocalDateTime] value:
     * ```
     * val zone = TimeZone.of("Europe/Berlin")
     * val ldt1 = Clock.System.now().toLocalDateTime(zone) // 2021-10-31T02:16:20
     * // 45 minutes pass; clocks move back from 03:00 to 02:00 in the meantime
     * val ldt2 = Clock.System.now().toLocalDateTime(zone) // 2021-10-31T02:01:20
     * ldt2 > ldt1 // returns `false`
     * ```
     */
    public override operator fun compareTo(other: LocalDateTime): Int

    /**
     * Converts this date/time value to the ISO 8601 string representation.
     *
     * For readability, if the time represents a round minute (without seconds or fractional seconds),
     * the string representation will not include seconds. Also, fractions of seconds will add trailing zeros to
     * the fractional part until its length is a multiple of three.
     *
     * Examples of output:
     * - `2020-08-30T18:43`
     * - `2020-08-30T18:43:00`
     * - `2020-08-30T18:43:00.500`
     * - `2020-08-30T18:43:00.123456789`
     *
     * @see LocalTime.toString for details of how the time part is formatted.
     * @see Formats.ISO for a very similar format. The difference is that [Formats.ISO] will always include seconds,
     * even if they are zero, and will not add trailing zeros to the fractional part of the second for readability.
     * @see parse for the dual operation: obtaining [LocalDateTime] from a string.
     * @see LocalDateTime.format for formatting using a custom format.
     */
    public override fun toString(): String
}

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
 */
public fun LocalDateTime.format(format: DateTimeFormat<LocalDateTime>): String = format.format(this)

/**
 * @suppress
 */
@Deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339", ReplaceWith("LocalDateTime.parse(this)"), DeprecationLevel.WARNING)
public fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)

// workaround for https://youtrack.jetbrains.com/issue/KT-65484
internal fun getIsoDateTimeFormat() = LocalDateTime.Formats.ISO
