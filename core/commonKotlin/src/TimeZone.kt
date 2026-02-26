/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.time.Instant

public actual open class TimeZone internal constructor() {

    public actual companion object {
        public actual val UTC: FixedOffsetTimeZone = FixedOffsetTimeZone(UtcOffset.ZERO, "UTC")

        @Deprecated(
            "Use TimeZoneContext.System.currentTimeZone() instead",
            ReplaceWith("TimeZoneContext.System.currentTimeZone()")
        )
        public actual fun currentSystemDefault(): TimeZone =
            TimeZoneContext.System.currentTimeZone()

        @Deprecated(
            "Use TimeZoneContext.System.get() instead",
            ReplaceWith("TimeZoneContext.System.get(zoneId)")
        )
        public actual fun of(zoneId: String): TimeZone = TimeZoneContext.System.get(zoneId)

        @Deprecated(
            "Use TimeZoneContext.System.availableZoneIds() instead",
            ReplaceWith("TimeZoneContext.System.availableZoneIds()")
        )
        public actual val availableZoneIds: Set<String>
            get() = TimeZoneContext.System.availableZoneIds()

        @Deprecated(
            "Serializing TimeZone is discouraged, " +
                    "as deserialization can fail depending on the configuration. " +
                    "Please serialize the string id instead.",
            level = DeprecationLevel.WARNING,
        )
        @Suppress("DEPRECATION")
        public actual fun serializer(): kotlinx.serialization.KSerializer<TimeZone> = TimeZoneSerializer
    }

    public actual open val id: String
        get() = error("Should be overridden")

    public actual fun Instant.toLocalDateTime(): LocalDateTime = instantToLocalDateTime(this)

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "Explicitly pass a TransitionHandler to `toInstant` calls",
        replaceWith = ReplaceWith("this.toInstant(TransitionHandler.USE_OFFSET_BEFORE)")
    )
    public actual fun LocalDateTime.toInstant(youShallNotPass: OverloadMarker): Instant =
        toInstant(TransitionHandler.USE_OFFSET_BEFORE)

    public actual fun LocalDateTime.toInstant(onTransition: TransitionHandler, utcOffset: UtcOffset?): Instant =
        this@toInstant.toInstant(this@TimeZone, onTransition, utcOffset)

    @Suppress("DEPRECATION")
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().toLocalDateTime()")
    )
    public actual fun kotlinx.datetime.Instant.toLocalDateTime(): LocalDateTime =
        toStdlibInstant().toLocalDateTime()

    @PublishedApi
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
    @kotlin.internal.LowPriorityInOverloadResolution
    internal actual fun LocalDateTime.toInstant(): kotlinx.datetime.Instant =
        toInstant(this@TimeZone).toDeprecatedInstant()

    internal open fun atStartOfDay(date: LocalDate): Instant = localDateTimeToInstantLenient(
        LocalDateTime(date, LocalTime.MIN), this, TransitionHandler.FIND_EARLIEST_VALID_TIME, preferred = null
    )

    internal open fun instantToLocalDateTime(instant: Instant): LocalDateTime = try {
        instant.toLocalDateTimeImpl(offsetAtImpl(instant))
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Instant $instant is not representable as LocalDateTime.", e)
    }

    internal open fun offsetAtImpl(instant: Instant): UtcOffset = error("Should be overridden")

    internal open fun offsetInfoForImpl(dateTime: LocalDateTime): LocalDateTimeOffsetInfo = error("Should be overridden")

    internal open fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset? = null): Instant =
        localDateTimeToInstantLenient(dateTime, this, TransitionHandler.USE_OFFSET_BEFORE, preferred)

    actual override fun equals(other: Any?): Boolean =
        error("Should be overridden")

    override fun hashCode(): Int =
        error("Should be overridden")

    actual override fun toString(): String = id
}

public actual class FixedOffsetTimeZone internal constructor(public actual val offset: UtcOffset, override val id: String) : TimeZone() {

    public actual constructor(offset: UtcOffset) : this(offset, offset.toString())

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds

    override fun atStartOfDay(date: LocalDate): Instant =
        LocalDateTime(date, LocalTime.MIN).toInstant(offset)

    override fun offsetAtImpl(instant: Instant): UtcOffset = offset

    override fun offsetInfoForImpl(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        LocalDateTimeOffsetInfo.Regular(offset)

    override fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset?): Instant =
        dateTime.toInstant(offset)

    override fun instantToLocalDateTime(instant: Instant): LocalDateTime = instant.toLocalDateTime(offset)

    override fun equals(other: Any?): Boolean =
        this === other || other is FixedOffsetTimeZone && this.id == other.id

    override fun hashCode(): Int = id.hashCode()

    /** @suppress */
    public actual companion object {
        /** @suppress */
        @Deprecated(
            "Serializing FixedOffsetTimeZone is discouraged, " +
                    "as deserialization can fail or return a non-fixed-offset zone depending on the configuration. " +
                    "Please serialize the string id instead.",
            level = DeprecationLevel.WARNING,
        )
        @Suppress("DEPRECATION")
        public actual fun serializer(): kotlinx.serialization.KSerializer<FixedOffsetTimeZone> =
            FixedOffsetTimeZoneSerializer
    }
}


public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
    offsetAtImpl(instant)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    timeZone.instantToLocalDateTime(this)

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    toLocalDateTimeImpl(offset)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Instant ${this@toLocalDateTime} is not representable as LocalDateTime", e)
}

internal fun Instant.toLocalDateTimeImpl(offset: UtcOffset): LocalDateTime {
    val localSecond: Long = epochSeconds + offset.totalSeconds // overflow caught later
    val localEpochDay = localSecond.floorDiv(SECONDS_PER_DAY.toLong())
    val secsOfDay = localSecond.mod(SECONDS_PER_DAY.toLong()).toInt()
    val date: LocalDate = LocalDate.fromEpochDays(localEpochDay) // may throw
    val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanosecondsOfSecond)
    return LocalDateTime(date, time)
}

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(timeZone: TimeZone, youShallNotPass: OverloadMarker): Instant =
    timeZone.localDateTimeToInstant(this)

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(offset: UtcOffset, youShallNotPass: OverloadMarker): Instant =
    Instant.fromEpochSeconds(this.toEpochSecond(offset), this.nanosecond)

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone, youShallNotPass: OverloadMarker): Instant =
    timeZone.atStartOfDay(this)

internal actual fun LocalDateTime.optimizedToInstantOffsetBefore(timeZone: TimeZone): Instant =
    timeZone.localDateTimeToInstant(this)

public actual fun TimeZone.offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
    offsetInfoForImpl(dateTime)

