/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.LocalTimeIso8601Serializer
import kotlinx.serialization.Serializable

/**
 * The time part of [LocalDateTime].
 *
 * This class represents time-of-day without a referencing a specific date.
 * To reconstruct a full [LocalDateTime], representing civil date and time, [LocalTime] needs to be
 * combined with [LocalDate] via [LocalDate.atTime] or [LocalTime.atDate].
 *
 * Also, [LocalTime] does not reference a particular time zone.
 * Therefore, even on the same date, [LocalTime] denotes different moments of time.
 * For example, `18:43` happens at different moments in Berlin and in Tokyo.
 *
 * The arithmetic on [LocalTime] values is not provided, since without accounting for the time zone
 * transitions it may give misleading results.
 */
@Serializable(LocalTimeIso8601Serializer::class)
public expect class LocalTime : Comparable<LocalTime> {
    public companion object {

        /**
         * Parses a string that represents a time value in ISO-8601 and returns the parsed [LocalTime] value.
         *
         * Examples of time in ISO-8601 format:
         * - `18:43`
         * - `18:43:00`
         * - `18:43:00.500`
         * - `18:43:00.123456789`
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalTime] are
         * exceeded.
         */
        public fun parse(isoString: String): LocalTime

        /**
         * Constructs a [LocalTime] that represents the specified number of seconds since the start of a calendar day.
         * The fractional parts of the second will be zero.
         *
         * @throws IllegalArgumentException if [secondOfDay] is outside the `0 until 86400` range,
         * with 86400 being the number of seconds in a calendar day.
         *
         * @see LocalTime.toSecondOfDay
         * @see LocalTime.fromMillisecondOfDay
         * @see LocalTime.fromNanosecondOfDay
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
         * @see LocalTime.fromSecondOfDay
         * @see LocalTime.toMillisecondOfDay
         * @see LocalTime.fromNanosecondOfDay
         */
        public fun fromMillisecondOfDay(millisecondOfDay: Int): LocalTime

        /**
         * Constructs a [LocalTime] that represents the specified number of nanoseconds since the start of
         * a calendar day.
         *
         * @throws IllegalArgumentException if [nanosecondOfDay] is outside the `0 until 86400 * 1_000_000_000` range,
         * with 86400 being the number of seconds in a calendar day.
         *
         * @see LocalTime.fromSecondOfDay
         * @see LocalTime.fromMillisecondOfDay
         * @see LocalTime.toNanosecondOfDay
         */
        public fun fromNanosecondOfDay(nanosecondOfDay: Long): LocalTime

        internal val MIN: LocalTime
        internal val MAX: LocalTime
    }

    public object Format;

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
     */
    public constructor(hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0)

    /** Returns the hour-of-day time component of this time value. */
    public val hour: Int
    /** Returns the minute-of-hour time component of this time value. */
    public val minute: Int
    /** Returns the second-of-minute time component of this time value. */
    public val second: Int
    /** Returns the nanosecond-of-second time component of this time value. */
    public val nanosecond: Int

    /** Returns the time as a second of a day, in `0 until 24 * 60 * 60`. */
    public fun toSecondOfDay(): Int

    /** Returns the time as a millisecond of a day, in `0 until 24 * 60 * 60 * 1_000`. */
    public fun toMillisecondOfDay(): Int

    /** Returns the time as a nanosecond of a day, in `0 until 24 * 60 * 60 * 1_000_000_000`. */
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
     */
    public override operator fun compareTo(other: LocalTime): Int

    /**
     * Converts this time value to the ISO-8601 string representation.
     *
     * @see LocalDateTime.parse
     */
    public override fun toString(): String
}

public fun LocalTime.Format.build(block: TimeFormatBuilderFields.() -> Unit): Format<LocalTime> =
    LocalTimeFormat.build(block)

/**
 * ISO-8601 extended format, used by [LocalTime.toString] and [LocalTime.parse].
 *
 * Examples: `12:34`, `12:34:56`, `12:34:56.789`.
 */
public val LocalTime.Format.ISO: Format<LocalTime> get() =
    LocalTimeFormat.ISO

public fun LocalTime.format(format: Format<LocalTime>): String = format.format(this)

public fun LocalTime.Companion.parse(input: String, format: Format<LocalTime>): LocalTime = format.parse(input)

/**
 * Converts this string representing a time value in ISO-8601 format to a [LocalTime] value.
 *
 * See [LocalTime.parse] for examples of time string representations.
 *
 * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [LocalTime] are exceeded.
 */
public fun String.toLocalTime(): LocalTime = LocalTime.parse(this)

/**
 * Combines this time's components with the specified date components into a [LocalDateTime] value.
 */
public fun LocalTime.atDate(year: Int, monthNumber: Int, dayOfMonth: Int = 0): LocalDateTime =
    LocalDateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)

/**
 * Combines this time's components with the specified date components into a [LocalDateTime] value.
 */
public fun LocalTime.atDate(year: Int, month: Month, dayOfMonth: Int = 0): LocalDateTime =
    LocalDateTime(year, month, dayOfMonth, hour, minute, second, nanosecond)

/**
 * Combines this time's components with the specified [LocalDate] components into a [LocalDateTime] value.
 */
public fun LocalTime.atDate(date: LocalDate): LocalDateTime = LocalDateTime(date, this)
