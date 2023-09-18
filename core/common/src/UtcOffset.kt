/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.UtcOffsetSerializer
import kotlinx.serialization.Serializable

/**
 * An offset from UTC.
 *
 * Examples of these values:
 * - `Z`, an offset of zero;
 * - `+05`, plus five hours;
 * - `-02`, minus two hours;
 * - `+03:30`, plus three hours and thirty minutes;
 * - `+01:23:45`, plus one hour, 23 minutes, and 45 seconds.
 */
@Serializable(with = UtcOffsetSerializer::class)
public expect class UtcOffset {
    /**
     * The number of seconds from UTC.
     *
     * The larger the value, the earlier some specific civil date/time happens with the offset.
     */
    public val totalSeconds: Int

    // TODO: Declare and document toString/equals/hashCode

    public companion object {
        /**
         * The zero offset from UTC, `Z`.
         */
        public val ZERO: UtcOffset

        /**
         * Parses a string that represents an offset in an ISO-8601 time shift extended format, also supporting
         * specifying the number of seconds or not specifying the number of minutes.
         *
         * Examples of valid strings:
         * - `Z` or `+00:00`, an offset of zero;
         * - `+05`, five hours;
         * - `-02`, minus two hours;
         * - `+03:30`, three hours and thirty minutes;
         * - `+01:23:45`, an hour, 23 minutes, and 45 seconds.
         */
        public fun parse(offsetString: String): UtcOffset

        /**
         * Creates a new format for parsing and formatting [UtcOffset] values.
         *
         * Example:
         * ```
         * // `GMT` on zero, `+4:30:15`, using a custom format:
         * UtcOffset.Format {
         *   appendOptional("GMT") {
         *     appendOffsetTotalHours(Padding.NONE)
         *     appendLiteral(':')
         *     appendOffsetMinutesOfHour()
         *     appendOptional {
         *       appendLiteral(':')
         *       appendOffsetSecondsOfMinute()
         *     }
         *   }
         * }
         * ```
         *
         * Since [UtcOffset] values are rarely formatted and parsed on their own,
         * instances of [DateTimeFormat] obtained here will likely need to be passed to
         * [UtcOffsetFormatBuilderFields.appendOffset] in a format builder for a larger data structure.
         *
         * There is a collection of predefined formats in [UtcOffset.Formats].
         */
        @Suppress("FunctionName")
        public fun Format(block: UtcOffsetFormatBuilderFields.() -> Unit): DateTimeFormat<UtcOffset>
    }

    /**
     * A collection of predefined formats for parsing and formatting [UtcOffset] values.
     *
     * See [UtcOffset.Formats.ISO], [UtcOffset.Formats.ISO_BASIC], and [UtcOffset.Formats.FOUR_DIGITS]
     * for popular predefined formats.
     *
     * If predefined formats are not sufficient, use [UtcOffset.Format] to create a custom
     * [kotlinx.datetime.format.DateTimeFormat] for [UtcOffset] values.
     */
    public object Formats {
        /**
         * ISO 8601 extended format, which is the format used by [UtcOffset.toString].
         *
         * Examples of UTC offsets in ISO 8601 format:
         * - `Z`
         * - `+05:00`
         * - `-17:16`
         * - `+10:36:30`
         */
        public val ISO: DateTimeFormat<UtcOffset>

        /**
         * ISO 8601 basic format.
         *
         * Examples of UTC offsets in ISO 8601 basic format:
         * - `Z`
         * - `+05`
         * - `-1716`
         * - `+103630`
         *
         * @see UtcOffset.Formats.FOUR_DIGITS
         */
        public val ISO_BASIC: DateTimeFormat<UtcOffset>

        /**
         * A subset of the ISO 8601 basic format that always outputs and parses exactly a numeric sign and four digits:
         * two digits for the hour and two digits for the minute. If the offset has a non-zero number of seconds,
         * they are truncated.
         *
         * Examples of UTC offsets in this format:
         * - `+0000`
         * - `+0500`
         * - `-1716`
         * - `+1036`
         *
         * @see UtcOffset.Formats.ISO_BASIC
         */
        public val FOUR_DIGITS: DateTimeFormat<UtcOffset>
    }
}

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
 */
public fun UtcOffset.format(format: DateTimeFormat<UtcOffset>): String = format.format(this)

/**
 * Parses a [UtcOffset] value using the given [format].
 * Equivalent to calling [DateTimeFormat.parse] on [format] with [input].
 *
 * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [UtcOffset] are exceeded.
 */
public fun UtcOffset.Companion.parse(input: CharSequence, format: DateTimeFormat<UtcOffset>): UtcOffset =
    format.parse(input)

/**
 * Constructs a [UtcOffset] from hours, minutes, and seconds components.
 *
 * All components must have the same sign.
 *
 * The bounds are checked: it is invalid to pass something other than `±[0; 59]` as the number of seconds or minutes.
 * For example, `UtcOffset(hours = 3, minutes = 61)` is invalid.
 *
 * However, the non-null component of the highest order can exceed these bounds,
 * for example, `UtcOffset(minutes = 241)` is valid.
 *
 * @throws IllegalArgumentException if a component exceeds its bounds when a higher order component is specified.
 * @throws IllegalArgumentException if components have different signs.
 * @throws IllegalArgumentException if the resulting `UtcOffset` value is outside of range `±18:00`.
 */
public expect fun UtcOffset(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset

@Deprecated("Use UtcOffset.ZERO instead", ReplaceWith("UtcOffset.ZERO"), DeprecationLevel.ERROR)
public fun UtcOffset(): UtcOffset = UtcOffset.ZERO

/**
 * Returns the fixed-offset time zone with the given UTC offset.
 */
public fun UtcOffset.asTimeZone(): FixedOffsetTimeZone = FixedOffsetTimeZone(this)
