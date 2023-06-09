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
    }

    public object Format;
}

public fun UtcOffset.Format.build(block: UtcOffsetFormatBuilderFields.() -> Unit): Format<UtcOffset> =
    UtcOffsetFormat.build(block)

public fun UtcOffset.format(format: Format<UtcOffset>): String = format.format(this)

public fun UtcOffset.Companion.parse(input: String, format: Format<UtcOffset>): UtcOffset = format.parse(input)

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
