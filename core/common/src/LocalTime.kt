/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalTimeKt")
@file:JvmMultifileClass

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.internal.*
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName


/**
 * The time part of [LocalDateTime].
 *
 * This class represents time-of-day without referencing a specific date.
 * To reconstruct a full [LocalDateTime], representing civil date and time, [LocalTime] needs to be
 * combined with [LocalDate] via [LocalDate.atTime] or [LocalTime.atDate].
 *
 * Also, [LocalTime] does not reference a particular time zone.
 * Therefore, even on the same date, [LocalTime] denotes different moments of time.
 * For example, `18:43` happens at different moments in Berlin and in Tokyo.
 * It may not even exist or be ambiguous on days when clocks are adjusted.
 *
 * The arithmetic on [LocalTime] values is not provided, since without accounting for the time zone
 * transitions it may give misleading results.
 *
 * ### Arithmetic operations
 *
 * Arithmetic operations on [LocalTime] are not provided because they are not well-defined without a date and
 * a time zone.
 * See [LocalDateTime] for an explanation of why not accounting for time zone transitions may lead to incorrect results.
 * To perform arithmetic operations on time values, first, obtain an [Instant][kotlin.time.Instant].
 *
 * ```
 * val time = LocalTime(13, 30)
 * val date = Clock.System.todayAt(TimeZone.currentSystemDefault())
 * val instant = time.atDate(date).toInstant(TimeZone.currentSystemDefault())
 * val instantThreeHoursLater = instant.plus(3.hours)
 * val timeThreeHoursLater = instantThreeHoursLater.toLocalDateTime(TimeZone.currentSystemDefault()).time
 * ```
 *
 * Since this pattern is extremely verbose and difficult to get right, it is recommended to work exclusively
 * with [Instant][kotlin.time.Instant] and only obtain a [LocalTime] when displaying the time to the user.
 *
 * ### Platform specifics
 *
 * On the JVM,
 * there are `LocalTime.toJavaLocalTime()` and `java.time.LocalTime.toKotlinLocalTime()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 *
 * ### Construction, serialization, and deserialization
 *
 * [LocalTime] can be constructed directly from its components using the constructor. See sample 1.
 *
 * [fromSecondOfDay], [fromMillisecondOfDay], and [fromNanosecondOfDay] can be used to obtain a [LocalTime] from the
 * number of seconds, milliseconds, or nanoseconds since the start of the day, assuming that the offset from the UTC
 * does not change during the day.
 * [toSecondOfDay], [toMillisecondOfDay], and [toNanosecondOfDay] are the inverse operations.
 * See sample 2.
 *
 * [parse] and [toString] methods can be used to obtain a [LocalTime] from and convert it to a string in the
 * ISO 8601 extended format. See sample 3.
 *
 * [parse] and [LocalTime.format] both support custom formats created with [Format] or defined in [Formats].
 * See sample 4.
 *
 * Additionally, there are several `kotlinx-serialization` serializers for [LocalTime]:
 * - The default serializer, delegating to [toString] and [parse].
 * - [LocalTimeIso8601Serializer] for the ISO 8601 extended format,
 * - [LocalTimeComponentSerializer] for an object with components.
 *
 * @sample kotlinx.datetime.test.samples.LocalTimeSamples.construction
 * @sample kotlinx.datetime.test.samples.LocalTimeSamples.representingAsNumbers
 * @sample kotlinx.datetime.test.samples.LocalTimeSamples.simpleParsingAndFormatting
 * @sample kotlinx.datetime.test.samples.LocalTimeSamples.customFormat
 */
@Serializable(LocalTimeSerializer::class)
public expect class LocalTime : Comparable<LocalTime> {
    public companion object {

        /**
         * A shortcut for calling [DateTimeFormat.parse].
         *
         * Parses a string that represents time-of-day and returns the parsed [LocalTime] value.
         *
         * If [format] is not specified, [Formats.ISO] is used.
         * `23:40:57.120` is an example of a string in this format.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalTime] are
         * exceeded.
         *
         * @see LocalTime.toString for formatting using the default format.
         * @see LocalTime.format for formatting using a custom format.
         * @sample kotlinx.datetime.test.samples.LocalTimeSamples.parsing
         */
        public fun parse(input: CharSequence, format: DateTimeFormat<LocalTime> = getIsoTimeFormat()): LocalTime

        /**
         * Constructs a [LocalTime] that represents the specified number of seconds since the start of a calendar day.
         * The fractional parts of the second will be zero.
         *
         * @throws IllegalArgumentException if [secondOfDay] is outside the `0 until 86400` range,
         * with 86400 being the number of seconds in a calendar day.
         *
         * It is incorrect to pass to this function
         * the number of seconds that have physically elapsed since the start of the day.
         * The reason is that, due to daylight-saving-time transitions, the number of seconds since the start
         * of the day is not a constant value: clocks could be shifted by an hour or more on some dates.
         * Use [Instant][kotlin.time.Instant] to perform reliable time arithmetic.
         *
         * @see LocalTime.toSecondOfDay
         * @see LocalTime.fromMillisecondOfDay
         * @see LocalTime.fromNanosecondOfDay
         * @sample kotlinx.datetime.test.samples.LocalTimeSamples.fromAndToSecondOfDay
         */
        public fun fromSecondOfDay(secondOfDay: Int): LocalTime

        /**
         * Constructs a [LocalTime] that represents the specified number of milliseconds since the start of
         * a calendar day.
         * The sub-millisecond parts of the `LocalTime` will be zero.
         *
         * @throws IllegalArgumentException if [millisecondOfDay] is outside the `0 until 86400 * 1_000` range,
         * with 86400 being the number of seconds in a calendar day.
         *
         * It is incorrect to pass to this function
         * the number of milliseconds that have physically elapsed since the start of the day.
         * The reason is that, due to daylight-saving-time transitions, the number of milliseconds since the start
         * of the day is not a constant value: clocks could be shifted by an hour or more on some dates.
         * Use [Instant][kotlin.time.Instant] to perform reliable time arithmetic.
         *
         * @see LocalTime.fromSecondOfDay
         * @see LocalTime.toMillisecondOfDay
         * @see LocalTime.fromNanosecondOfDay
         * @sample kotlinx.datetime.test.samples.LocalTimeSamples.fromAndToMillisecondOfDay
         */
        public fun fromMillisecondOfDay(millisecondOfDay: Int): LocalTime

        /**
         * Constructs a [LocalTime] that represents the specified number of nanoseconds since the start of
         * a calendar day.
         *
         * @throws IllegalArgumentException if [nanosecondOfDay] is outside the `0 until 86400 * 1_000_000_000` range,
         * with 86400 being the number of seconds in a calendar day.
         *
         * It is incorrect to pass to this function
         * the number of nanoseconds that have physically elapsed since the start of the day.
         * The reason is that, due to daylight-saving-time transitions, the number of nanoseconds since the start
         * of the day is not a constant value: clocks could be shifted by an hour or more on some dates.
         * Use [Instant][kotlin.time.Instant] to perform reliable time arithmetic.
         *
         * @see LocalTime.fromSecondOfDay
         * @see LocalTime.fromMillisecondOfDay
         * @see LocalTime.toNanosecondOfDay
         * @sample kotlinx.datetime.test.samples.LocalTimeSamples.fromAndToNanosecondOfDay
         */
        public fun fromNanosecondOfDay(nanosecondOfDay: Long): LocalTime

        /**
         * Creates a new format for parsing and formatting [LocalTime] values.
         *
         * Example:
         * ```
         * LocalTime.Format {
         *   hour(); char(':'); minute(); char(':'); second()
         *   optional { char('.'); secondFraction() }
         * }
         * ```
         *
         * Only parsing and formatting of well-formed values is supported. If the input does not fit the boundaries
         * (for example, [second] is 60), consider using [DateTimeComponents.Format] instead.
         *
         * There is a collection of predefined formats in [LocalTime.Formats].
         *
         * @throws IllegalArgumentException if parsing using this format is ambiguous.
         * @sample kotlinx.datetime.test.samples.LocalTimeSamples.customFormat
         */
        @Suppress("FunctionName")
        public fun Format(builder: DateTimeFormatBuilder.WithTime.() -> Unit): DateTimeFormat<LocalTime>

        internal val MIN: LocalTime
        internal val MAX: LocalTime
    }

    /**
     * A collection of predefined formats for parsing and formatting [LocalDateTime] values.
     *
     * [LocalTime.Formats.ISO] is a popular predefined format.
     *
     * If predefined formats are not sufficient, use [LocalTime.Format] to create a custom
     * [kotlinx.datetime.format.DateTimeFormat] for [LocalTime] values.
     */
    public object Formats {
        /**
         * ISO 8601 extended format.
         *
         * Examples: `12:34`, `12:34:56`, `12:34:56.789`, `12:34:56.1234`.
         *
         * When formatting, seconds are always included, even if they are zero.
         * Fractional parts of the second are included if non-zero.
         *
         * Guaranteed to parse all strings that [LocalTime.toString] produces.
         *
         * See ISO-8601-1:2019.
         * Any of the extended formats in 5.3.1.2b), 5.3.1.4a), and 5.3.1.3a) can be used, depending on whether
         * seconds and fractional seconds are non-zero.
         * The length of the fractional part is flexible between one and nine digits.
         * The only allowed separator between seconds and fractional seconds is the dot `.`.
         * We *forbid* using the time designator `T` to allow for a predictable composition of formats:
         * see the note at the end of rule 5.3.5.
         *
         * @sample kotlinx.datetime.test.samples.LocalTimeSamples.Formats.iso
         */
        public val ISO: DateTimeFormat<LocalTime>
    }

    /**
     * Constructs a [LocalTime] instance from the given time components.
     *
     * The supported ranges of components:
     * - [hour] `0..23`
     * - [minute] `0..59`
     * - [second] `0..59`
     * - [nanosecond] `0..999_999_999`
     *
     * @throws IllegalArgumentException if any parameter is out of range.
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.constructorFunction
     */
    public constructor(hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0)

    /**
     * Returns the hour-of-day (0..23) time component of this time value.
     *
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.hour
     */
    public val hour: Int

    /**
     * Returns the minute-of-hour (0..59) time component of this time value.
     *
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.minute
     */
    public val minute: Int

    /**
     * Returns the second-of-minute (0..59) time component of this time value.
     *
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.second
     */
    public val second: Int

    /**
     * Returns the nanosecond-of-second (0..999_999_999) time component of this time value.
     *
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.nanosecond
     */
    public val nanosecond: Int

    /**
     * Returns the time as a second of a day, in `0 until 24 * 60 * 60`.
     *
     * Note that this is *not* the number of seconds since the start of the day!
     * For example, `LocalTime(4, 0).toMillisecondOfDay()` will return `4 * 60 * 60`, the four hours
     * worth of seconds, but because of DST transitions, when clocks show 4:00, in fact, three, four, five, or
     * some other number of hours could have passed since the day started.
     * Use [Instant][kotlin.time.Instant] to perform reliable time arithmetic.
     *
     * @see toMillisecondOfDay
     * @see toNanosecondOfDay
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.toSecondOfDay
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.fromAndToSecondOfDay
     */
    public fun toSecondOfDay(): Int

    /**
     * Returns the time as a millisecond of a day, in `0 until 24 * 60 * 60 * 1_000`.
     *
     * Note that this is *not* the number of milliseconds since the start of the day!
     * For example, `LocalTime(4, 0).toMillisecondOfDay()` will return `4 * 60 * 60 * 1_000`, the four hours
     * worth of milliseconds, but because of DST transitions, when clocks show 4:00, in fact, three, four, five, or
     * some other number of hours could have passed since the day started.
     * Use [Instant][kotlin.time.Instant] to perform reliable time arithmetic.
     *
     * @see toSecondOfDay
     * @see toNanosecondOfDay
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.toMillisecondOfDay
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.fromAndToMillisecondOfDay
     */
    public fun toMillisecondOfDay(): Int

    /**
     * Returns the time as a nanosecond of a day, in `0 until 24 * 60 * 60 * 1_000_000_000`.
     *
     * Note that this is *not* the number of nanoseconds since the start of the day!
     * For example, `LocalTime(4, 0).toMillisecondOfDay()` will return `4 * 60 * 60 * 1_000_000_000`, the four hours
     * worth of nanoseconds, but because of DST transitions, when clocks show 4:00, in fact, three, four, five, or
     * some other number of hours could have passed since the day started.
     * Use [Instant][kotlin.time.Instant] to perform reliable time arithmetic.
     *
     * @see toMillisecondOfDay
     * @see toNanosecondOfDay
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.toNanosecondOfDay
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.fromAndToNanosecondOfDay
     */
    public fun toNanosecondOfDay(): Long

    /**
     * Compares `this` time value with the [other] time value.
     * Returns zero if this value is equal to the other, a negative number if this value occurs earlier
     * in the course of a typical day than the other, and a positive number if this value occurs
     * later in the course of a typical day than the other.
     *
     * Note that, on days when there is a time overlap (for example, due to the daylight saving time
     * transitions in autumn), a "lesser" wall-clock reading can, in fact, happen later than the
     * "greater" one.
     *
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.compareTo
     */
    public override operator fun compareTo(other: LocalTime): Int

    /**
     * Converts this time value to the extended ISO 8601 string representation.
     *
     * For readability, if the time represents a round minute (without seconds or fractional seconds),
     * the string representation will not include seconds. Also, fractions of seconds will add trailing zeros to
     * the fractional part until the number of digits after the dot is a multiple of three.
     *
     * Examples of output:
     * - `18:43`
     * - `18:43:00`
     * - `18:43:00.500`
     * - `18:43:00.123456789`
     *
     * @see Formats.ISO for a very similar format. The difference is that [Formats.ISO] will always include seconds,
     * even if they are zero, and will not add trailing zeros to the fractional part of the second for readability.
     * @see parse for the dual operation: obtaining [LocalTime] from a string.
     * @see LocalTime.format for formatting using a custom format.
     * @sample kotlinx.datetime.test.samples.LocalTimeSamples.toStringSample
     */
    public override fun toString(): String
}

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
 *
 * @sample kotlinx.datetime.test.samples.LocalTimeSamples.formatting
 */
public fun LocalTime.format(format: DateTimeFormat<LocalTime>): String = format.format(this)

/**
 * @suppress
 */
@Deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339", ReplaceWith("LocalTime.parse(this)"), DeprecationLevel.WARNING)
public fun String.toLocalTime(): LocalTime = LocalTime.parse(this)

/**
 * Combines this time's components with the specified date components into a [LocalDateTime] value.
 *
 * There is no check of whether the time is valid on the specified date because that depends on the time zone, which
 * this method does not accept.
 *
 * @sample kotlinx.datetime.test.samples.LocalTimeSamples.atDateComponentWiseMonthNumber
 */
public fun LocalTime.atDate(year: Int, month: Int, day: Int): LocalDateTime =
    LocalDateTime(year, month, day, hour, minute, second, nanosecond)

/** @suppress */
@Deprecated(
    "Use the overload that accepts a 'month' and a 'day' instead",
    ReplaceWith("this.atDate(year = year, month = monthNumber, day = dayOfMonth)"),
    DeprecationLevel.WARNING
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@LowPriorityInOverloadResolution
public fun LocalTime.atDate(year: Int, monthNumber: Int, dayOfMonth: Int, fakeArgument: Unit = Unit): LocalDateTime =
    fakeArgument.let {
        LocalDateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
    }

/**
 * Combines this time's components with the specified date components into a [LocalDateTime] value.
 *
 * There is no check of whether the time is valid on the specified date because that depends on the time zone, which
 * this method does not accept.
 *
 * @sample kotlinx.datetime.test.samples.LocalTimeSamples.atDateComponentWise
 */
public fun LocalTime.atDate(year: Int, month: Month, day: Int): LocalDateTime =
    LocalDateTime(year, month, day, hour, minute, second, nanosecond)

/** @suppress */
@Deprecated(
    "Use the overload that accepts a 'month' and a 'day' instead",
    ReplaceWith("this.atDate(year = year, month = month, day = dayOfMonth)"),
    DeprecationLevel.WARNING
)
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@LowPriorityInOverloadResolution
public fun LocalTime.atDate(year: Int, month: Month, dayOfMonth: Int, fakeArgument: Unit = Unit): LocalDateTime =
    fakeArgument.let {
        LocalDateTime(year, month, dayOfMonth, hour, minute, second, nanosecond)
    }

/**
 * Combines this time's components with the specified [LocalDate] components into a [LocalDateTime] value.
 *
 * There is no check of whether the time is valid on the specified date because that depends on the time zone, which
 * this method does not accept.
 *
 * @sample kotlinx.datetime.test.samples.LocalTimeSamples.atDate
 */
public fun LocalTime.atDate(date: LocalDate): LocalDateTime = LocalDateTime(date, this)

// workaround for https://youtrack.jetbrains.com/issue/KT-65484
internal fun getIsoTimeFormat() = LocalTime.Formats.ISO
