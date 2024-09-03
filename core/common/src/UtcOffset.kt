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
 *
 * **Pitfall**: the offset is not a time zone.
 * It does not contain any information about the time zone's rules, such as daylight-saving-time transitions.
 * It is just a fixed offset from UTC, something that may be in effect in a given location today but will change
 * tomorrow.
 * Even if the offset is fixed currently, it may change in the future due to political decisions.
 *
 * See [TimeZone] for a type that represents a time zone.
 *
 * ### Platform specifics
 *
 * On the JVM, there are `UtcOffset.toJavaZoneOffset()` and `java.time.ZoneOffset.toKotlinUtcOffset()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 *
 * ### Construction, serialization, and deserialization
 *
 * To construct a [UtcOffset] value, use the [UtcOffset] constructor function.
 * [totalSeconds] returns the number of seconds from UTC.
 * See sample 1.
 *
 * There is also a [ZERO] constant for the offset of zero.
 *
 * [parse] and [toString] methods can be used to obtain a [UtcOffset] from and convert it to a string in the
 * ISO 8601 extended format (for example, `+01:30`).
 * See sample 2.
 *
 * [parse] and [UtcOffset.format] both support custom formats created with [Format] or defined in [Formats].
 * See sample 3.
 *
 * To serialize and deserialize [UtcOffset] values with `kotlinx-serialization`, use the [UtcOffsetSerializer].
 *
 * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.construction
 * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.simpleParsingAndFormatting
 * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.customFormat
 */
@Serializable(with = UtcOffsetSerializer::class)
public expect class UtcOffset {
    /**
     * The number of seconds from UTC.
     *
     * The larger the value, the earlier some specific civil datetime happens with the offset.
     */
    public val totalSeconds: Int

    /**
     * Returns `true` if [other] is a [UtcOffset] with the same [totalSeconds].
     *
     * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.equalsSample
     */
    public override fun equals(other: Any?): Boolean

    public companion object {
        /**
         * The zero offset from UTC, `Z`.
         */
        public val ZERO: UtcOffset

        /**
         * A shortcut for calling [DateTimeFormat.parse].
         *
         * Parses a string that represents a UTC offset and returns the parsed [UtcOffset] value.
         *
         * If [format] is not specified, [Formats.ISO] is used.
         * `Z` or `+01:30` are examples of valid input.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [UtcOffset] are
         * exceeded.
         * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.parsing
         */
        public fun parse(input: CharSequence, format: DateTimeFormat<UtcOffset> = getIsoUtcOffsetFormat()): UtcOffset

        /**
         * Creates a new format for parsing and formatting [UtcOffset] values.
         *
         * Since [UtcOffset] values are rarely formatted and parsed on their own,
         * instances of [DateTimeFormat] obtained here will likely need to be passed to
         * [DateTimeFormatBuilder.WithUtcOffset.offset] in a format builder for a larger data structure.
         *
         * There is a collection of predefined formats in [UtcOffset.Formats].
         *
         * @throws IllegalArgumentException if parsing using this format is ambiguous.
         * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.customFormat
         */
        @Suppress("FunctionName")
        public fun Format(block: DateTimeFormatBuilder.WithUtcOffset.() -> Unit): DateTimeFormat<UtcOffset>
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
         * ISO 8601 extended format, which is the format used by [UtcOffset.parse] and [UtcOffset.toString].
         *
         * An extension of ISO 8601 that allows parsing and formatting seconds.
         *
         * When formatting, seconds are omitted if they are zero. If the whole offset is zero, the letter `Z` is output.
         *
         * Examples of UTC offsets in ISO 8601 format:
         * - `Z` or `+00:00`, an offset of zero;
         * - `+05:00`, five hours;
         * - `-02:00`, minus two hours;
         * - `-17:16`
         * - `+10:36:30`
         *
         * See ISO-8601-1:2019, 4.3.13c), extended to support seconds-of-minute when they are non-zero.
         *
         * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.Formats.iso
         */
        public val ISO: DateTimeFormat<UtcOffset>

        /**
         * ISO 8601 basic format.
         *
         * An extension of ISO 8601 that allows parsing and formatting seconds.
         *
         * When formatting, seconds are omitted if they are zero. If the whole offset is zero, the letter `Z` is output.
         *
         * Examples of UTC offsets in ISO 8601 basic format:
         * - `Z`
         * - `+05`
         * - `-1716`
         * - `+103630`
         *
         * See ISO-8601-1:2019, 4.3.13a) and b), extended to support seconds-of-minute when they are non-zero.
         *
         * @see UtcOffset.Formats.FOUR_DIGITS
         * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.Formats.isoBasic
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
         * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.Formats.fourDigits
         */
        public val FOUR_DIGITS: DateTimeFormat<UtcOffset>
    }

    /**
     * Converts this UTC offset to the extended ISO 8601 string representation, for example, `+02:30` or `Z`.
     *
     * @see Formats.ISO for the format details.
     * @see parse for the dual operation: obtaining [UtcOffset] from a string.
     * @see UtcOffset.format for formatting using a custom format.
     *
     * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.toStringSample
     */
    public override fun toString(): String
}

/**
 * Formats this value using the given [format].
 * Equivalent to calling [DateTimeFormat.format] on [format] with `this`.
 *
 * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.formatting
 */
public fun UtcOffset.format(format: DateTimeFormat<UtcOffset>): String = format.format(this)

/**
 * Constructs a [UtcOffset] from hours, minutes, and seconds components.
 *
 * All components must have the same sign.
 * Otherwise, [IllegalArgumentException] will be thrown.
 *
 * The bounds are checked: it is invalid to pass something other than `±[0; 59]` as the number of seconds or minutes;
 * [IllegalArgumentException] will be thrown if this rule is violated.
 * For example, `UtcOffset(hours = 3, minutes = 61)` is invalid.
 *
 * However, the non-null component of the highest order can exceed these bounds,
 * for example, `UtcOffset(minutes = 241)` and `UtcOffset(seconds = -3600)` are both valid.
 *
 * @throws IllegalArgumentException if a component exceeds its bounds when a higher order component is specified.
 * @throws IllegalArgumentException if components have different signs.
 * @throws IllegalArgumentException if the resulting `UtcOffset` value is outside of range `±18:00`.
 * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.constructorFunction
 */
public expect fun UtcOffset(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset

@Deprecated("Use UtcOffset.ZERO instead", ReplaceWith("UtcOffset.ZERO"), DeprecationLevel.ERROR)
public fun UtcOffset(): UtcOffset = UtcOffset.ZERO

/**
 * Returns the fixed-offset time zone with the given UTC offset.
 *
 * **Pitfall**: UTC offsets are static values and do not change with time.
 * If the logic requires that the offset changes with time, for example, to account for daylight-saving-time
 * transitions, use [TimeZone.of] with an IANA time zone name to obtain a time zone that can handle
 * changes in the offset.
 *
 * @sample kotlinx.datetime.test.samples.UtcOffsetSamples.asFixedOffsetTimeZone
 */
public fun UtcOffset.asTimeZone(): FixedOffsetTimeZone = FixedOffsetTimeZone(this)

// workaround for https://youtrack.jetbrains.com/issue/KT-65484
internal fun getIsoUtcOffsetFormat() = UtcOffset.Formats.ISO
