/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A time zone, provides the conversion between [Instant] and [LocalDateTime] values
 * using a collection of rules specifying which [LocalDateTime] value corresponds to each [Instant].
 *
 * A time zone can be used in [Instant.toLocalDateTime] and [LocalDateTime.toInstant], and also in
 * those arithmetic operations on [Instant] that require knowing the calendar.
 *
 * To obtain a [TimeZone], typically, a [TimeZoneContext], such as [TimeZoneContext.System] is used.
 * - [TimeZoneContext.currentTimeZone] returns the currently chosen time zone.
 * - [TimeZoneContext.get] returns the time zone with the specified identifier, like
 *   `"Europe/Berlin"`, `"America/Los_Angeles"`, etc. For a list of such identifiers,
 *   see [TimeZoneContext.availableZoneIds].
 *
 * Also, the constant [TimeZone.UTC] is provided for the UTC time zone.
 *
 * On the JVM, there are `TimeZone.toJavaZoneId()` and `java.time.ZoneId.toKotlinTimeZone()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 * Similarly, on the Darwin platforms, there are `TimeZone.toNSTimeZone()` and `NSTimeZone.toKotlinTimeZone()` extension
 * functions.
 *
 * ### Serializing a [TimeZone]
 *
 * Special care must be taken to serialize and deserialize a [TimeZone].
 *
 * - A timezone identifier that is available on one system may be unavailable on another one.
 *   For example, in 2025, `America/Coyhaique` was introduced as a new timezone identifier.
 *   A system whose timezone database was updated before 2025 wouldn't be able to recognize that identifier.
 * - For a given timezone identifier, the behavior of the corresponding [TimeZone] object
 *   also depends on the system configuration.
 *   For example, in 2026, British Columbia decided to abolish daylight saving time transitions
 *   and stay on a fixed [offset][UtcOffset] permanently.
 *   Querying the [TimeZone] corresponding to the `America/Vancouver` identifier on an up-to-date system
 *   would recognize this change,
 *   but a system with an outdated timezone database would behave as if DST transitions were still practiced there.
 *
 * To sum it up, unless you control the timezone database, do not expect deserialization of valid timezone identifiers
 * to succeed, and even when it succeeds, expect to see different results between systems.
 * Consider sending [LocalDateTime] and [Instant] values between systems instead of a [TimeZone] identifier.
 *
 * To highlight these risks, [TimeZone] is intentionally not [Serializable], and no built-in serializer is provided
 * for [TimeZone].
 * Whenever it is necessary to serialize a [TimeZone], use its [TimeZone.id] instead.
 *
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.usage
 */
public expect open class TimeZone {
    /**
     * Returns the identifier string of the time zone.
     *
     * This identifier can be used later for finding this time zone with [TimeZoneContext.get] function.
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.id
     */
    public val id: String

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
    public fun offsetAt(instant: Instant): UtcOffset

    /**
     * Returns the [offset information][LocalDateTimeOffsetInfo] corresponding to the given [dateTime] in this time zone.
     *
     * See the [LocalDateTimeOffsetInfo] documentation for a detailed description.
     *
     * See [LocalDateTime.toInstant] together with [TransitionHandler] for a more streamlined way
     * to handle a subset of this function's use cases.
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.offsetInfoFor
     */
    public fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo

    /**
     * Equivalent to [id].
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.equalsSample
     */
    public override fun toString(): String

    /**
     * Compares this time zone to the other one.
     *
     * Time zones are equal if their identifier is the same, and they were obtained from the same
     * [timezone database][TimeZoneDatabase].
     *
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.equalsSample
     */
    public override fun equals(other: Any?): Boolean

    public companion object {
        /**
         * Returns the time zone with the fixed UTC+0 offset.
         *
         * The [id] of this time zone is `"UTC"`.
         *
         * @sample kotlinx.datetime.test.samples.TimeZoneSamples.utc
         */
        public val UTC: FixedOffsetTimeZone

        /**
         * Equivalent to [TimeZoneContext.System.currentTimeZone].
         */
        @Deprecated(
            "Use TimeZoneContext.System.currentTimeZone instead",
            ReplaceWith("TimeZoneContext.System.currentTimeZone()")
        )
        public fun currentSystemDefault(): TimeZone

        /**
         * Equivalent to [TimeZoneContext.System.get].
         */
        @Deprecated(
            "Use TimeZoneContext.System.get instead",
            ReplaceWith("TimeZoneContext.System.get(zoneId)")
        )
        public fun of(zoneId: String): TimeZone

        /**
         * Equivalent to [TimeZoneContext.System.availableZoneIds].
         */
        @Deprecated(
            "Use TimeZoneContext.System.availableZoneIds instead",
            ReplaceWith("TimeZoneContext.System.availableZoneIds()")
        )
        public val availableZoneIds: Set<String>

        /** @suppress */
        @Deprecated(
            "Serializing TimeZone is discouraged, " +
                    "as deserialization can fail depending on the configuration. " +
                    "Please serialize the string id instead.",
            level = DeprecationLevel.WARNING,
        )
        public fun serializer(): kotlinx.serialization.KSerializer<TimeZone>
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
    @Suppress("DEPRECATION")
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().toLocalDateTime()")
    )
    public fun kotlinx.datetime.Instant.toLocalDateTime(): LocalDateTime

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
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "Explicitly pass a TransitionHandler to `toInstant` calls",
        replaceWith = ReplaceWith("this.toInstant(TransitionHandler.USE_OFFSET_BEFORE)")
    )
    public fun LocalDateTime.toInstant(youShallNotPass: OverloadMarker = OverloadMarker.INSTANCE): Instant

    /**
     * Returns an instant that corresponds to this civil datetime value in the time zone provided as an implicit receiver.
     *
     * For example, in `Europe/Berlin`,
     * `2026-05-27T03:21` corresponds to the [Instant] with the Unix epoch second value of `1779844860`,
     * with the UTC offset `+02:00`.
     * This function can be used to obtain that [Instant].
     *
     * Because of the changes to the UTC offset over time in a given [TimeZone]
     * (for example, when clocks are moved to account for daylight saving time),
     * the conversion from [LocalDateTime] to [Instant] is not well-defined.
     * [onTransition] is invoked in that case.
     *
     * [utcOffset] may additionally be passed to validate the full [TimeZone]/[LocalDateTime]/[UtcOffset] triple
     * or to help handle scenarios where the conversion from [LocalDateTime] to [Instant] is not well-defined.
     * An [IllegalArgumentException] will be thrown if the [utcOffset] is provided but does not match
     * the expected UTC offset values for that [LocalDateTime].
     *
     * ### Behavior specifics
     *
     * - If only a single [Instant] has this [LocalDateTime] value in the time zone provided as an implicit receiver,
     *   the conversion is unambiguous.
     *   An [IllegalArgumentException] is thrown if [utcOffset] is not `null` and isn't equal to
     *   [TimeZone.offsetAt] for the resulting value.
     * - If a transition corresponds to this [LocalDateTime] in the time zone provided as an implicit receiver,
     *   meaning either a [gap][LocalDateTimeOffsetInfo.Gap] or an [overlap][LocalDateTimeOffsetInfo.Overlap]
     *   has occurred, [onTransition] is invoked.
     *   [utcOffset] is passed to [TransitionHandler.resolveDateTime] as the `preferredOffset`.
     *   A non-`null` [utcOffset] must be equal to [LocalDateTimeOffsetInfo.Transition.offsetBefore]
     *   or [LocalDateTimeOffsetInfo.Transition.offsetAfter], or an [IllegalArgumentException] will be thrown.
     *   Exceptions thrown from [onTransition] are rethrown untouched.
     *
     * @see Instant.toLocalDateTime
     * @sample kotlinx.datetime.test.samples.TimeZoneSamples.toInstantWithTwoReceivers
     */
    // Added after 0.8.0
    public fun LocalDateTime.toInstant(onTransition: TransitionHandler, utcOffset: UtcOffset? = null): Instant

    @PublishedApi
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
    @kotlin.internal.LowPriorityInOverloadResolution
    internal fun LocalDateTime.toInstant(): kotlinx.datetime.Instant
}

/**
 * A time zone that is known to always have the same offset from UTC.
 *
 * [TimeZoneContext.System.get] will return an instance of this class if the time zone rules are fixed.
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

    /** @suppress */
    public companion object {
        /** @suppress */
        @Deprecated(
            "Serializing FixedOffsetTimeZone is discouraged, " +
                    "as deserialization can fail or return a non-fixed-offset zone depending on the configuration. " +
                    "Please serialize the string id instead.",
            level = DeprecationLevel.WARNING,
        )
        public fun serializer(): kotlinx.serialization.KSerializer<FixedOffsetTimeZone>
    }
}

@Deprecated("Use FixedOffsetTimeZone or UtcOffset instead", ReplaceWith("FixedOffsetTimeZone"))
public typealias ZoneOffset = FixedOffsetTimeZone

@Suppress("DEPRECATION")
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.offsetAt(instant.toStdlibInstant())")
)
public fun TimeZone.offsetAt(instant: kotlinx.datetime.Instant): UtcOffset =
    offsetAt(instant.toStdlibInstant())

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

@Suppress("DEPRECATION")
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().toLocalDateTime(timeZone)")
)
public fun kotlinx.datetime.Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    toStdlibInstant().toLocalDateTime(timeZone)

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

@Suppress("DEPRECATION")
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().toLocalDateTime(offset)")
)
public fun kotlinx.datetime.Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime =
    toStdlibInstant().toLocalDateTime(offset)

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

@Suppress("DEPRECATION")
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().offsetIn(timeZone)")
)
public fun kotlinx.datetime.Instant.offsetIn(timeZone: TimeZone): UtcOffset =
    timeZone.offsetAt(toStdlibInstant())

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
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Explicitly pass a TransitionHandler to `toInstant` calls",
    replaceWith = ReplaceWith("this.toInstant(timeZone, TransitionHandler.USE_OFFSET_BEFORE)")
)
public expect fun LocalDateTime.toInstant(timeZone: TimeZone, youShallNotPass: OverloadMarker = OverloadMarker.INSTANCE): Instant

/**
 * Returns an instant that corresponds to this civil datetime value in the specified [timeZone].
 *
 * For example, in `Europe/Berlin`,
 * `2026-05-27T03:21` corresponds to the [Instant] with the Unix epoch second value of `1779844860`,
 * with the UTC offset `+02:00`.
 * This function can be used to obtain that [Instant].
 *
 * Because of the changes to the UTC offset over time in a given [TimeZone]
 * (for example, when clocks are moved to account for daylight saving time),
 * the conversion from [LocalDateTime] to [Instant] is not well-defined.
 * [onTransition] is invoked in that case.
 *
 * [utcOffset] may additionally be passed to validate the full [TimeZone]/[LocalDateTime]/[UtcOffset] triple
 * or to help handle scenarios where the conversion from [LocalDateTime] to [Instant] is not well-defined.
 * An [IllegalArgumentException] will be thrown if the [utcOffset] is provided but does not match
 * the expected UTC offset values for that [LocalDateTime].
 *
 * ### Behavior specifics
 *
 * - If only a single [Instant] has this [LocalDateTime] value in the given [timeZone],
 *   the conversion is unambiguous.
 *   An [IllegalArgumentException] is thrown if [utcOffset] is not `null` and isn't equal to
 *   [TimeZone.offsetAt] for the resulting value.
 * - If a transition corresponds to this [LocalDateTime] in the given [timeZone],
 *   meaning either a [gap][LocalDateTimeOffsetInfo.Gap] or an [overlap][LocalDateTimeOffsetInfo.Overlap]
 *   has occurred, [onTransition] is invoked.
 *   [utcOffset] is passed to [TransitionHandler.resolveDateTime] as the `preferredOffset`.
 *   A non-`null` [utcOffset] must be equal to [LocalDateTimeOffsetInfo.Transition.offsetBefore]
 *   or [LocalDateTimeOffsetInfo.Transition.offsetAfter], or an [IllegalArgumentException] will be thrown.
 *   Exceptions thrown from [onTransition] are rethrown untouched.
 *
 * @see Instant.toLocalDateTime
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.localDateTimeToInstantInZone
 */
public fun LocalDateTime.toInstant(
    timeZone: TimeZone, onTransition: TransitionHandler, utcOffset: UtcOffset? = null
): Instant = if (onTransition === TransitionHandler.USE_OFFSET_BEFORE && utcOffset === null) {
    optimizedToInstantOffsetBefore(timeZone)
} else {
    when (val offsetInfo = timeZone.offsetInfoFor(this)) {
        is LocalDateTimeOffsetInfo.Regular -> {
            require(utcOffset == null || utcOffset == offsetInfo.offset) {
                "The supplied UTC offset $utcOffset did not match the actual UTC offset ${offsetInfo.offset} " +
                    "at $this in the time zone $timeZone"
            }
            toInstant(offsetInfo.offset)
        }
        is LocalDateTimeOffsetInfo.Transition -> {
            require(utcOffset == null || utcOffset == offsetInfo.offsetBefore || utcOffset == offsetInfo.offsetAfter) {
                "The supplied UTC offset $utcOffset did not match any of the offset " +
                    "surrounding the transition $offsetInfo at $this in the time zone $timeZone"
            }
            onTransition.resolveDateTime(
                dateTime = this,
                transition = offsetInfo,
                preferredOffset = utcOffset,
            )
        }
    }
}

/**
 * Returns an instant that corresponds to this civil datetime value in the specified fixed-offset [timeZone].
 *
 * For example, in `Etc/UTC+02`,
 * `2026-05-27T03:21` corresponds to the [Instant] with the Unix epoch second value of `1779844860`.
 * This function can be used to obtain that [Instant].
 *
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.localDateTimeToInstantInFixedOffsetZone
 */
public fun LocalDateTime.toInstant(timeZone: FixedOffsetTimeZone): Instant = toInstant(timeZone.offset)

@PublishedApi
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
@kotlin.internal.LowPriorityInOverloadResolution
internal fun LocalDateTime.toInstant(timeZone: TimeZone): kotlinx.datetime.Instant =
    toInstant(timeZone).toDeprecatedInstant()

/**
 * Returns an instant that corresponds to this civil datetime value that happens at the specified [UTC offset][offset].
 *
 * @see Instant.toLocalDateTime
 * @sample kotlinx.datetime.test.samples.TimeZoneSamples.localDateTimeToInstantInOffset
 */
@Suppress("DEPRECATION_ERROR")
public expect fun LocalDateTime.toInstant(offset: UtcOffset, youShallNotPass: OverloadMarker = OverloadMarker.INSTANCE): Instant

@PublishedApi
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
@kotlin.internal.LowPriorityInOverloadResolution
internal fun LocalDateTime.toInstant(offset: UtcOffset): kotlinx.datetime.Instant =
    toInstant(offset).toDeprecatedInstant()

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
@Suppress("DEPRECATION_ERROR")
public expect fun LocalDate.atStartOfDayIn(timeZone: TimeZone, youShallNotPass: OverloadMarker = OverloadMarker.INSTANCE): Instant

@PublishedApi
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
@kotlin.internal.LowPriorityInOverloadResolution
internal fun LocalDate.atStartOfDayIn(timeZone: TimeZone): kotlinx.datetime.Instant =
    atStartOfDayIn(timeZone).toDeprecatedInstant()

// Convert dateTime to Instant in the given timeZone, use `preferred` on overlaps only, but it's okay for it to be invalid
internal fun localDateTimeToInstantLenient(
    dateTime: LocalDateTime, timeZone: TimeZone, handler: TransitionHandler, preferred: UtcOffset? = null
): Instant = localDateTimeToInstantLenient(dateTime, timeZone.offsetInfoFor(dateTime), handler, preferred)

internal fun localDateTimeToInstantLenient(
    dateTime: LocalDateTime,
    offsetInfo: LocalDateTimeOffsetInfo,
    handler: TransitionHandler,
    preferred: UtcOffset? = null
): Instant = when (offsetInfo) {
    is LocalDateTimeOffsetInfo.Regular -> dateTime.toInstant(offsetInfo.offset)
    is LocalDateTimeOffsetInfo.Transition -> {
        val actualPreferred = if (preferred == offsetInfo.offsetBefore || preferred == offsetInfo.offsetAfter) {
            preferred
        } else {
            null
        }
        handler.resolveDateTime(dateTime, offsetInfo, actualPreferred)
    }
}

internal expect fun LocalDateTime.optimizedToInstantOffsetBefore(timeZone: TimeZone): Instant