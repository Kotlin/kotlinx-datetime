/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmName("LocalDateTimeKt")
@file:JvmMultifileClass
package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.internal.*
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * The representation of a specific civil date and time without a reference to a particular time zone.
 *
 * This class does not describe specific *moments in time*. For that, use [Instant][kotlin.time.Instant] values instead.
 * Instead, you can think of its instances as clock readings, which can be observed in a particular time zone.
 * For example, `2020-08-30T18:43` is not a *moment in time* since someone in Berlin and Tokyo would witness
 * this on their clocks at different times, but it is a [LocalDateTime].
 *
 * The main purpose of this class is to provide human-readable representations of [Instant][kotlin.time.Instant] values,
 * to transfer them as data, or to define future planned events that will have the same local datetime
 * even if the time zone rules change.
 * In all other cases, when a specific time zone is known,
 * it is recommended to use [Instant][kotlin.time.Instant] instead.
 *
 * ### Arithmetic operations
 *
 * The arithmetic on [LocalDateTime] values is not provided since it may give misleading results
 * without accounting for time zone transitions.
 *
 * For example, in Berlin, naively adding one day to `2021-03-27T02:16:20` without accounting for the time zone would
 * result in `2021-03-28T02:16:20`.
 * However, the resulting local datetime cannot be observed in that time zone
 * because the clocks moved forward from `02:00` to `03:00` on that day.
 * This is known as a "time gap" or a "spring forward" transition.
 *
 * Similarly, the local datetime `2021-10-31T02:16:20` is ambiguous,
 * because the clocks moved back from `03:00` to `02:00`.
 * This is known as a "time overlap" or a "fall back" transition.
 *
 * For these reasons, using [LocalDateTime] as an input to arithmetic operations is discouraged.
 *
 * When only the date component is needed, without the time, use [LocalDate] instead.
 * It provides well-defined date arithmetic.
 *
 * If the time component must be taken into account, [LocalDateTime]
 * should be converted to [Instant][kotlin.time.Instant] using a specific time zone,
 * and the arithmetic on [Instant][kotlin.time.Instant] should be used.
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
 * ### Platform specifics
 *
 * The range of supported years is unspecified, but at least is enough to represent dates of all instants between
 * [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE].
 *
 * On the JVM, there are `LocalDateTime.toJavaLocalDateTime()` and `java.time.LocalDateTime.toKotlinLocalDateTime()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 * Similarly, on the Darwin platforms, there is a `LocalDateTime.toNSDateComponents()` extension function.
 *
 * ### Construction, serialization, and deserialization
 *
 * **Pitfall**: since [LocalDateTime] is always constructed without specifying the time zone, it cannot validate
 * whether the given date and time components are valid in the implied time zone.
 * For example, `2021-03-28T02:16:20` is invalid in Berlin, as it falls into a time gap, but nothing prevents one
 * from constructing such a [LocalDateTime].
 * Before using a [LocalDateTime] constructed using any API,
 * please ensure that the result is valid in the implied time zone.
 * The recommended pattern is to convert a [LocalDateTime] to [Instant][kotlin.time.Instant] as soon as possible (see
 * [LocalDateTime.toInstant]) and work with [Instant][kotlin.time.Instant] values instead.
 *
 * [LocalDateTime] can be constructed directly from its components, [LocalDate] and [LocalTime], using the constructor.
 * See sample 1.
 *
 * Some additional constructors that directly accept the values from date and time fields are provided for convenience.
 * See sample 2.
 *
 * [parse] and [toString] methods can be used to obtain a [LocalDateTime] from and convert it to a string in the
 * ISO 8601 extended format (for example, `2023-01-02T22:35:01`).
 * See sample 3.
 *
 * [parse] and [LocalDateTime.format] both support custom formats created with [Format] or defined in [Formats].
 * See sample 4.
 *
 * Additionally, there are several `kotlinx-serialization` serializers for [LocalDateTime]:
 * - The default serializer, delegating to [toString] and [parse].
 * - [LocalDateTimeIso8601Serializer] for the ISO 8601 extended format.
 * - [LocalDateTimeComponentSerializer] for an object with components.
 *
 * @see LocalDate for only the date part of the datetime value.
 * @see LocalTime for only the time part of the datetime value.
 * @see kotlin.time.Instant for the representation of a specific moment in time independent of a time zone.
 * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.fromDateAndTime
 * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.alternativeConstruction
 * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.simpleParsingAndFormatting
 * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.customFormat
 */
@Serializable(with = LocalDateTimeSerializer::class)
public expect class LocalDateTime : Comparable<LocalDateTime> {
    public companion object {

        /**
         * A shortcut for calling [DateTimeFormat.parse].
         *
         * Parses a string that represents a datetime value including date and time components
         * but without any time zone component and returns the parsed [LocalDateTime] value.
         *
         * If [format] is not specified, [Formats.ISO] is used.
         * `2023-01-02T23:40:57.120` is an example of a string in this format.
         *
         * See [Formats] and [Format] for predefined and custom formats.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalDateTime] are
         * exceeded.
         *
         * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.parsing
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
         *   monthNumber(); char('/'); day()
         *   char(' ')
         *   hour(); char(':'); minute()
         *   optional { char(':'); second() }
         * }
         * ```
         *
         * Only parsing and formatting of well-formed values is supported. If the input does not fit the boundaries
         * (for example, [day] is 31 for February), consider using [DateTimeComponents.Format] instead.
         *
         * There is a collection of predefined formats in [LocalDateTime.Formats].
         *
         * @throws IllegalArgumentException if parsing using this format is ambiguous.
         *
         * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.customFormat
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
         * Examples of datetime in ISO 8601 format:
         * - `2020-08-30T18:43`
         * - `+12020-08-30T18:43:00`
         * - `0000-08-30T18:43:00.5`
         * - `-0001-08-30T18:43:00.123456789`
         *
         * When formatting, seconds are always included, even if they are zero.
         * Fractional parts of the second are included if non-zero.
         *
         * Guaranteed to parse all strings that [LocalDateTime.toString] produces.
         *
         * See ISO-8601-1:2019, 5.4.2.1b), the version without the offset, together with
         * [LocalDate.Formats.ISO] and [LocalTime.Formats.ISO].
         *
         * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.Formats.iso
         */
        public val ISO: DateTimeFormat<LocalDateTime>
    }

    /**
     * Constructs a [LocalDateTime] instance from the given date and time components.
     *
     * The components [month] and [day] are 1-based.
     *
     * The supported ranges of components:
     * - [year] the range is platform-dependent, but at least is enough to represent dates of all instants between
     *          [Instant.DISTANT_PAST] and [Instant.DISTANT_FUTURE]
     * - [month] `1..12`
     * - [day] `1..31`, the upper bound can be less, depending on the month
     * - [hour] `0..23`
     * - [minute] `0..59`
     * - [second] `0..59`
     * - [nanosecond] `0..999_999_999`
     *
     * @throws IllegalArgumentException if any parameter is out of range
     * or if [day] is invalid for the given [monthNumber] and [year].
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.constructorFunctionWithMonthNumber
     */
    public constructor(
        year: Int,
        month: Int,
        day: Int,
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
     * - [day] `1..31`, the upper bound can be less, depending on the month
     * - [hour] `0..23`
     * - [minute] `0..59`
     * - [second] `0..59`
     * - [nanosecond] `0..999_999_999`
     *
     * @throws IllegalArgumentException if any parameter is out of range,
     * or if [day] is invalid for the given [month] and [year].
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.constructorFunction
     */
    public constructor(
        year: Int,
        month: Month,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        nanosecond: Int = 0
    )

    /**
     * Constructs a [LocalDateTime] instance by combining the given [date] and [time] parts.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.fromDateAndTime
     */
    public constructor(date: LocalDate, time: LocalTime)

    /**
     * Returns the year component of the [date]. Can be negative.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.dateComponents
     */
    public val year: Int

    /** @suppress */
    @Deprecated("Use the 'month' property instead", ReplaceWith("month.number"), level = DeprecationLevel.WARNING)
    public val monthNumber: Int

    /**
     * Returns the month ([Month]) component of the [date].
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.dateComponents
     */
    public val month: Month

    /**
     * Returns the day-of-month (`1..31`) component of the [date].
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.dateComponents
     */
    public val day: Int

    /** @suppress */
    @Deprecated("Use the 'day' property instead", ReplaceWith("day"), level = DeprecationLevel.WARNING)
    public val dayOfMonth: Int

    /**
     * Returns the day-of-week component of the [date].
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.dateComponents
     */
    public val dayOfWeek: DayOfWeek

    /**
     * Returns the 1-based day-of-year component of the [date].
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.dateComponents
     */
    public val dayOfYear: Int

    /**
     * Returns the hour-of-day (`0..23`) [time] component of this datetime value.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.timeComponents
     */
    public val hour: Int

    /**
     * Returns the minute-of-hour (`0..59`) [time] component of this datetime value.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.timeComponents
     */
    public val minute: Int

    /**
     * Returns the second-of-minute (`0..59`) [time] component of this datetime value.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.timeComponents
     */
    public val second: Int

    /**
     * Returns the nanosecond-of-second (`0..999_999_999`) [time] component of this datetime value.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.timeComponents
     */
    public val nanosecond: Int

    /**
     * Returns the date part of this datetime value.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.dateAndTime
     */
    public val date: LocalDate

    /**
     * Returns the time part of this datetime value.
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.dateAndTime
     */
    public val time: LocalTime

    /**
     * Compares `this` datetime value with the [other] datetime value.
     * Returns zero if this value is equal to the other,
     * a negative number if this value represents earlier civil time than the other,
     * and a positive number if this value represents later civil time than the other.
     *
     * **Pitfall**: comparing [LocalDateTime] values is less robust than comparing
     * [Instant][kotlin.time.Instant] values.
     * Consider the following situation, where a later moment in time corresponds to an earlier [LocalDateTime] value:
     * ```
     * val zone = TimeZone.of("Europe/Berlin")
     * val ldt1 = Clock.System.now().toLocalDateTime(zone) // 2021-10-31T02:16:20
     * // 45 minutes pass; clocks move back from 03:00 to 02:00 in the meantime
     * val ldt2 = Clock.System.now().toLocalDateTime(zone) // 2021-10-31T02:01:20
     * ldt2 > ldt1 // Returns `false`
     * ```
     *
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.compareToSample
     */
    public override operator fun compareTo(other: LocalDateTime): Int

    /**
     * Converts this datetime value to the ISO 8601 string representation.
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
     * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.toStringSample
     */
    public override fun toString(): String
}

/**
 * @suppress
 */
@Deprecated(
    "Use the constructor that accepts a 'month' and a 'day'",
    ReplaceWith("LocalDateTime(year = year, month = monthNumber, day = dayOfMonth, hour = hour, minute = minute, second = second, nanosecond = nanosecond)"),
    level = DeprecationLevel.WARNING
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@LowPriorityInOverloadResolution
public fun LocalDateTime(
    year: Int,
    monthNumber: Int,
    dayOfMonth: Int,
    hour: Int,
    minute: Int,
    second: Int = 0,
    nanosecond: Int = 0,
): LocalDateTime = LocalDateTime(
    year = year, month = monthNumber, day = dayOfMonth,
    hour = hour, minute = minute, second = second, nanosecond = nanosecond
)

/**
 * @suppress
 */
@Deprecated(
    "Use the constructor that accepts a 'day'",
    ReplaceWith("LocalDateTime(year = year, month = month, day = dayOfMonth, hour = hour, minute = minute, second = second, nanosecond = nanosecond)"),
    level = DeprecationLevel.WARNING
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@LowPriorityInOverloadResolution
public fun LocalDateTime(
    year: Int,
    month: Month,
    dayOfMonth: Int,
    hour: Int,
    minute: Int,
    second: Int = 0,
    nanosecond: Int = 0,
): LocalDateTime = LocalDateTime(
    year = year, month = month, day = dayOfMonth,
    hour = hour, minute = minute, second = second, nanosecond = nanosecond
)

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
 *
 * See [LocalDateTime.Formats] and [LocalDateTime.Format] for predefined and custom formats.
 *
 * @sample kotlinx.datetime.test.samples.LocalDateTimeSamples.formatting
 */
public fun LocalDateTime.format(format: DateTimeFormat<LocalDateTime>): String = format.format(this)

/**
 * @suppress
 */
@Deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339", ReplaceWith("LocalDateTime.parse(this)"), DeprecationLevel.WARNING)
public fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)

// A workaround for https://youtrack.jetbrains.com/issue/KT-65484
internal fun getIsoDateTimeFormat() = LocalDateTime.Formats.ISO
