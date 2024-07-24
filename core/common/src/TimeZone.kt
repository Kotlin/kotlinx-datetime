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
 *
 * A time zone can be used in [Instant.toLocalDateTime] and [LocalDateTime.toInstant], and also in
 * those arithmetic operations on [Instant] that require knowing the calendar.
 *
 * A [TimeZone] can be constructed using the [TimeZone.of] function, which accepts the string identifier, like
 * `"Europe/Berlin"`, `"America/Los_Angeles"`, etc. For a list of such identifiers, see [TimeZone.availableZoneIds].
 * Also, the constant [TimeZone.UTC] is provided for the UTC time zone.
 *
 * For interaction with `kotlinx-serialization`, [TimeZoneSerializer] is provided that serializes the time zone as its
 * identifier.
 *
 * On the JVM, there are `TimeZone.toJavaZoneId()` and `java.time.ZoneId.toKotlinTimeZone()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 * Similarly, on the Darwin platforms, there are `TimeZone.toNSTimeZone()` and `NSTimeZone.toKotlinTimeZone()` extension
 * functions.
 *
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.usage
 */
@Serializable(with = TimeZoneSerializer::class)
public expect open class TimeZone {
    /**
     * Returns the identifier string of the time zone.
     *
     * This identifier can be used later for finding this time zone with [TimeZone.of] function.
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.id
     */
    public val id: String

    /**
     * Equivalent to [id].
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.equalsSample
     */
    public override fun toString(): String

    /**
     * Compares this time zone to the other one. Time zones are equal if their identifier is the same.
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.equalsSample
     */
    public override fun equals(other: Any?): Boolean

    public companion object {
        /**
         * Queries the current system time zone.
         *
         * If the current system time zone changes, this function can reflect this change on the next invocation.
         *
         * @sample kotlinx.datetime.test.samples.TimeZoneSamples.currentSystemDefault
         */
        public fun currentSystemDefault(): TimeZone

        /**
         * Returns the time zone with the fixed UTC+0 offset.
         *
         * The [id] of this time zone is `"UTC"`.
         *
         * @sample kotlinx.datetime.test.samples.TimeZoneSamples.utc
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
         * It is guaranteed that passing any value from [availableZoneIds] to this function will return
         * a valid time zone.
         *
         * @throws IllegalTimeZoneException if [zoneId] has an invalid format or a time-zone with the name [zoneId]
         * is not found.
         * @sample kotlinx.datetime.test.samples.TimeZoneSamples.constructorFunction
         */
        public fun of(zoneId: String): TimeZone

        /**
         * Queries the set of identifiers of time zones available in the system.
         *
         * @sample kotlinx.datetime.test.samples.TimeZoneSamples.availableZoneIds
         */
        public val availableZoneIds: Set<String>
    }

    /**
     * Return the civil datetime value that this instant has in the time zone provided as an implicit receiver.
     *
     * Note that while this conversion is unambiguous, the inverse ([LocalDateTime.toInstant])
     * is not necessarily so.
     *
     * @see LocalDateTime.toInstant
     * @see Instant.offsetIn
     * @throws DateTimeArithmeticException if this value is too large to fit in [LocalDateTime].
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.toLocalDateTimeWithTwoReceivers
     */
    public fun Instant.toLocalDateTime(): LocalDateTime

    /**
     * Returns an instant that corresponds to this civil datetime value in the time zone provided as an implicit receiver.
     *
     * Note that the conversion is not always well-defined. There can be the following possible situations:
     * - Only one instant has this datetime value in the time zone.
     *   In this case, the conversion is unambiguous.
     * - No instant has this datetime value in the time zone.
     *   Such a situation appears when the time zone experiences a transition from a lesser to a greater offset.
     *   In this case, the conversion is performed with the lesser (earlier) offset, as if the time gap didn't occur yet.
     * - Two possible instants can have these datetime components in the time zone.
     *   In this case, the earlier instant is returned.
     *
     * @see Instant.toLocalDateTime
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.toInstantWithTwoReceivers
     */
    public fun LocalDateTime.toInstant(): Instant
}

/**
 * A time zone that is known to always have the same offset from UTC.
 *
 * [TimeZone.of] will return an instance of this class if the time zone rules are fixed.
 *
 * Time zones that are [FixedOffsetTimeZone] at some point in time can become non-fixed in the future due to
 * changes in legislation or other reasons.
 *
 * On the JVM, there are `FixedOffsetTimeZone.toJavaZoneOffset()` and
 * `java.time.ZoneOffset.toKotlinFixedOffsetTimeZone()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 * Note also the functions available for [TimeZone] in general.
 *
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.FixedOffsetTimeZoneSamples.casting
 */
@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public expect class FixedOffsetTimeZone : TimeZone {
    /**
     * Constructs a time zone with the fixed [offset] from UTC.
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.FixedOffsetTimeZoneSamples.constructorFunction
     */
    public constructor(offset: UtcOffset)

    /**
     * The constant offset from UTC that this time zone has.
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.FixedOffsetTimeZoneSamples.offset
     */
    public val offset: UtcOffset

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public val totalSeconds: Int
}

@Deprecated("Use FixedOffsetTimeZone or UtcOffset instead", ReplaceWith("FixedOffsetTimeZone"))
public typealias ZoneOffset = FixedOffsetTimeZone

/**
 * Finds the offset from UTC this time zone has at the specified [instant] of physical time.
 *
 * **Pitfall**: the offset returned from this function should typically not be used for datetime arithmetics
 * because the offset can change over time due to daylight-saving-time transitions and other reasons.
 * Use [TimeZone] directly with arithmetic operations instead.
 *
 * @see Instant.toLocalDateTime
 * @see TimeZone.offsetAt
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.offsetAt
 */
public expect fun TimeZone.offsetAt(instant: Instant): UtcOffset

/**
 * Returns a civil datetime value that this instant has in the specified [timeZone].
 *
 * Note that while this conversion is unambiguous, the inverse ([LocalDateTime.toInstant])
 * is not necessarily so.
 *
 * @see LocalDateTime.toInstant
 * @see Instant.offsetIn
 * @throws DateTimeArithmeticException if this value is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.instantToLocalDateTime
 */
public expect fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime

/**
 * Returns a civil datetime value that this instant has in the specified [UTC offset][offset].
 *
 * **Pitfall**: it is typically more robust to use [TimeZone] directly because the offset can change over time due to
 * daylight-saving-time transitions and other reasons, so [this] instant may actually correspond to a different offset
 * in the implied time zone.
 *
 * @see LocalDateTime.toInstant
 * @see Instant.offsetIn
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.instantToLocalDateTimeInOffset
 */
internal expect fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime

/**
 * Finds the offset from UTC the specified [timeZone] has at this instant of physical time.
 *
 * **Pitfall**: the offset returned from this function should typically not be used for datetime arithmetics
 * because the offset can change over time due to daylight-saving-time transitions and other reasons.
 * Use [TimeZone] directly with arithmetic operations instead.
 *
 * @see Instant.toLocalDateTime
 * @see TimeZone.offsetAt
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.offsetIn
 */
public fun Instant.offsetIn(timeZone: TimeZone): UtcOffset =
        timeZone.offsetAt(this)

/**
 * Returns an instant that corresponds to this civil datetime value in the specified [timeZone].
 *
 * Note that the conversion is not always well-defined. There can be the following possible situations:
 * - Only one instant has this datetime value in the [timeZone].
 *   In this case, the conversion is unambiguous.
 * - No instant has this datetime value in the [timeZone].
 *   Such a situation appears when the time zone experiences a transition from a lesser to a greater offset.
 *   In this case, the conversion is performed with the lesser (earlier) offset, as if the time gap didn't occur yet.
 * - Two possible instants can have these datetime components in the [timeZone].
 *   In this case, the earlier instant is returned.
 *
 * @see Instant.toLocalDateTime
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.localDateTimeToInstantInZone
 */
public expect fun LocalDateTime.toInstant(timeZone: TimeZone): Instant

/**
 * Returns an instant that corresponds to this civil datetime value that happens at the specified [UTC offset][offset].
 *
 * @see Instant.toLocalDateTime
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.localDateTimeToInstantInOffset
 */
public expect fun LocalDateTime.toInstant(offset: UtcOffset): Instant

/**
 * Returns an instant that corresponds to the start of this date in the specified [timeZone].
 *
 * Note that it's not equivalent to `atTime(0, 0).toInstant(timeZone)`
 * because a day does not always start at a fixed time 00:00:00.
 * For example, if, due to daylight saving time, clocks were shifted from 23:30
 * of one day directly to 00:30 of the next day, skipping the midnight, then
 * `atStartOfDayIn` would return the `Instant` corresponding to 00:30, whereas
 * `atTime(0, 0).toInstant(timeZone)` would return the `Instant` corresponding
 * to 01:00.
 *
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.atStartOfDayIn
 */
public expect fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant
