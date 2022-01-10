/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.UtcOffsetSerializer
import kotlinx.serialization.Serializable

/**
 * An offset from UTC.
 *
 * Examples of these values:
 */
@Serializable(with = UtcOffsetSerializer::class)
public expect class UtcOffset {
    /**
     * The number of seconds from UTC.
     *
     * The larger the value, the earlier some specific civil date/time happens with the offset.
     */
    public val totalSeconds: Int

    public companion object {
        /**
         * The zero offset from UTC.
         */
        public val ZERO: UtcOffset

        /**
         * Parses a string that represents an offset in an ISO-8601 time shift extended format, also supporting
         * specifying the number of seconds or not specifying the number of minutes.
         *
         * Examples of valid strings:
         * - `Z`, an offset of zero;
         * - `+05`, five hours;
         * - `-02`, minus two hours;
         * - `+03:30`, three hours and thirty minutes;
         * - `+01:23:45`, an hour, 23 minutes, and 45 seconds.
         */
        public fun parse(offsetString: String): UtcOffset
    }
}

/**
 * Constructs a [UtcOffset].
 *
 * All components must have the same sign.
 *
 * The bounds are checked: it is invalid to pass something other than `±[0; 59]` as the number of seconds or minutes.
 * For example, `UtcOffset(hours = 03, minutes = 61)` is invalid.
 *
 * However, if a component is the first non-null one, it can exceed these bounds.
 * So, for example, `UtcOffset(minutes = 241)` is valid.
 *
 * @throws IllegalArgumentException if some value in a component that is not the first non-null one exceeds its bounds.
 * @throws IllegalArgumentException if components have different signs.
 * @throws IllegalArgumentException if the value is outside of range `±18:00`.
 */
public expect fun UtcOffset(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset

@Deprecated("Use UtcOffset.ZERO instead", ReplaceWith("UtcOffset.ZERO"), DeprecationLevel.ERROR)
public fun UtcOffset(): UtcOffset = UtcOffset.ZERO

/**
 * The fixed-offset time zone with the given UTC offset.
 */
public fun UtcOffset.asTimeZone(): FixedOffsetTimeZone = FixedOffsetTimeZone(this)