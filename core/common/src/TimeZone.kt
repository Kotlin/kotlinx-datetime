/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable

/**
 * A time zone, provides the conversion between [Instant] and [LocalDateTime] values
 * using a collection of rules specifying which [LocalDateTime] value corresponds to each [Instant].
 */
@Serializable(with = TimeZoneSerializer::class)
public expect open class TimeZone {
    /**
     * Returns the identifier string of the time zone.
     *
     * This identifier can be used later for finding this time zone with [TimeZone.of] function.
     */
    public val id: String

    // TODO: Declare and document toString/equals/hashCode

    public companion object {
        /**
         * Queries the current system time zone.
         *
         * If the current system time zone changes, this function can reflect this change on the next invocation.
         */
        public fun currentSystemDefault(): TimeZone

        /**
         * Returns the time zone with the fixed UTC+0 offset.
         */
        public val UTC: FixedOffsetTimeZone

        /**
         * Returns the time zone identified by the provided [zoneId].
         *
         * The supported variants of time zone identifiers:
         * - `Z`, 'UTC', 'UT' or 'GMT' — identifies the fixed-offset time zone [TimeZone.UTC],
         * - a string starting with '+', '-', `UTC+`, `UTC-`, `UT+`, `UT-`, `GMT+`, `GMT-` — identifiers the time zone
         *   with the fixed offset specified after `+` or `-`,
         * - all other strings are treated as region-based zone identifiers.
         * In the IANA Time Zone Database (TZDB) which is used as the default source of time zones,
         * these ids are usually in the form `area/city`, for example, `Europe/Berlin` or `America/Los_Angeles`.
         *
         * @throws IllegalTimeZoneException if [zoneId] has an invalid format or a time-zone with the name [zoneId]
         * is not found.
         */
        public fun of(zoneId: String): TimeZone

        /**
         * Queries the set of identifiers of time zones available in the system.
         */
        public val availableZoneIds: Set<String>
    }

    /**
     * Return the civil date/time value that this instant has in the time zone provided as an implicit receiver.
     *
     * Note that while this conversion is unambiguous, the inverse ([LocalDateTime.toInstant])
     * is not necessary so.
     *
     * @see LocalDateTime.toInstant
     * @see Instant.offsetIn
     * @throws DateTimeArithmeticException if this value is too large to fit in [LocalDateTime].
     */
    public fun Instant.toLocalDateTime(): LocalDateTime

    /**
     * Returns an instant that corresponds to this civil date/time value in the time zone provided as an implicit receiver.
     *
     * Note that the conversion is not always unambiguous. There can be the following possible situations:
     * - There's only one instant that has this date/time value in the time zone. In this case
     * the conversion is unambiguous.
     * - There's no instant that has this date/time value in the time zone. Such situation appears when
     * the time zone experiences a transition from a lesser to a greater offset. In this case the conversion is performed with
     * the lesser offset.
     * - There are two possible instants that can have this date/time components in the time zone. In this case the earlier
     * instant is returned.
     *
     * @see Instant.toLocalDateTime
     */
    public fun LocalDateTime.toInstant(): Instant
}

/**
 * A time zone that is known to always have the same offset from UTC.
 */
@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public expect class FixedOffsetTimeZone : TimeZone {
    public constructor(offset: UtcOffset)

    /**
     * The constant offset from UTC that this time zone has.
     */
    public val offset: UtcOffset

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public val totalSeconds: Int
}

@Deprecated("Use FixedOffsetTimeZone of UtcOffset instead", ReplaceWith("FixedOffsetTimeZone"))
public typealias ZoneOffset = FixedOffsetTimeZone

/**
 * Finds the offset from UTC this time zone has at the specified [instant] of physical time.
 *
 * @see Instant.toLocalDateTime
 * @see TimeZone.offsetAt
 */
public expect fun TimeZone.offsetAt(instant: Instant): UtcOffset

/**
 * Returns a civil date/time value that this instant has in the specified [timeZone].
 *
 * Note that while this conversion is unambiguous, the inverse ([LocalDateTime.toInstant])
 * is not necessary so.
 *
 * @see LocalDateTime.toInstant
 * @see Instant.offsetIn
 * @throws DateTimeArithmeticException if this value is too large to fit in [LocalDateTime].
 */
public expect fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime

/**
 * Returns a civil date/time value that this instant has in the specified [UTC offset][offset].
 *
 * @see LocalDateTime.toInstant
 * @see Instant.offsetIn
 */
internal expect fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime

/**
 * Finds the offset from UTC the specified [timeZone] has at this instant of physical time.
 *
 * @see Instant.toLocalDateTime
 * @see TimeZone.offsetAt
 */
public fun Instant.offsetIn(timeZone: TimeZone): UtcOffset =
        timeZone.offsetAt(this)

/**
 * Returns an instant that corresponds to this civil date/time value in the specified [timeZone].
 *
 * Note that the conversion is not always unambiguous. There can be the following possible situations:
 * - There's only one instant that has this date/time value in the [timeZone]. In this case
 * the conversion is unambiguous.
 * - There's no instant that has this date/time value in the [timeZone]. Such situation appears when
 * the time zone experiences a transition from a lesser to a greater offset. In this case the conversion is performed with
 * the lesser offset.
 * - There are two possible instants that can have this date/time components in the [timeZone]. In this case the earlier
 * instant is returned.
 *
 * @see Instant.toLocalDateTime
 */
public expect fun LocalDateTime.toInstant(timeZone: TimeZone): Instant

/**
 * Returns an instant that corresponds to this civil date/time value that happens at the specified [UTC offset][offset].
 *
 * @see Instant.toLocalDateTime
 */
public expect fun LocalDateTime.toInstant(offset: UtcOffset): Instant

/**
 * Returns an instant that corresponds to the start of this date in the specified [timeZone].
 *
 * Note that it's not equivalent to `atTime(0, 0).toInstant(timeZone)`
 * because a day does not always start at the fixed time 0:00:00.
 * For example, if due do daylight saving time, clocks were shifted from 23:30
 * of one day directly to 00:30 of the next day, skipping the midnight, then
 * `atStartOfDayIn` would return the `Instant` corresponding to 00:30, whereas
 * `atTime(0, 0).toInstant(timeZone)` would return the `Instant` corresponding
 * to 01:00.
 */
public expect fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant
