/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TimeZoneKt")

package kotlinx.datetime

public expect open class TimeZone {
    /**
     * Returns the identifier string of the time zone.
     *
     * This identifier can be used later for finding this time zone with [TimeZone.of] function.
     */
    public val id: String

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
        public val UTC: TimeZone

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
     * Return a civil date/time value that this instant has in the time zone provided as an implicit receiver.
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

public expect class ZoneOffset : TimeZone {
    public val totalSeconds: Int
}

/**
 * Finds the offset from UTC this time zone has at the specified [instant] of physical time.
 *
 * @see Instant.toLocalDateTime
 * @see TimeZone.offsetAt
 */
public expect fun TimeZone.offsetAt(instant: Instant): ZoneOffset

/**
 * Return a civil date/time value that this instant has in the specified [timeZone].
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
 * Finds the offset from UTC the specified [timeZone] has at this instant of physical time.
 *
 * @see Instant.toLocalDateTime
 * @see TimeZone.offsetAt
 */
public fun Instant.offsetIn(timeZone: TimeZone): ZoneOffset =
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
 * Returns an instant that corresponds to the start of this date in the specified [timeZone].
 *
 * Note that it's not equivalent to `atTime(0, 0).toInstant(timeZone)`
 * because a day does not always start at the fixed time 0:00:00.
 */
public expect fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant
