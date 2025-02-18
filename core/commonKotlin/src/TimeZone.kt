/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor() {

    public actual companion object {

        public actual fun currentSystemDefault(): TimeZone {
            // TODO: probably check if currentSystemDefault name is parseable as FixedOffsetTimeZone?
            val (name, zone) = currentSystemDefaultZone()
            return zone ?: of(name)
        }

        public actual val UTC: FixedOffsetTimeZone = FixedOffsetTimeZone(UtcOffset.ZERO, "UTC")

        // org.threeten.bp.ZoneId#of(java.lang.String)
        public actual fun of(zoneId: String): TimeZone {
            // TODO: normalize aliases?
            if (zoneId == "UTC") {
                return UTC
            }
            if (zoneId == "Z" || zoneId == "z") {
                return UtcOffset.ZERO.asTimeZone()
            }
            if (zoneId == "SYSTEM") {
                return currentSystemDefault()
            }
            if (zoneId.length == 1) {
                throw IllegalTimeZoneException("Invalid zone ID: $zoneId")
            }
            try {
                if (zoneId.startsWith("+") || zoneId.startsWith("-")) {
                    return lenientOffsetFormat.parse(zoneId).asTimeZone()
                }
                if (zoneId == "UTC" || zoneId == "GMT" || zoneId == "UT") {
                    return FixedOffsetTimeZone(UtcOffset.ZERO, zoneId)
                }
                if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") ||
                    zoneId.startsWith("UTC-") || zoneId.startsWith("GMT-")
                ) {
                    val prefix = zoneId.take(3)
                    val offset = lenientOffsetFormat.parse(zoneId.substring(3))
                    return offset.asTimeZone(prefix)
                }
                if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
                    val offset = lenientOffsetFormat.parse(zoneId.substring(2))
                    return offset.asTimeZone("UT")
                }
            } catch (e: DateTimeFormatException) {
                throw IllegalTimeZoneException(e)
            }
            return try {
                timeZoneById(zoneId)
            } catch (e: Exception) {
                throw IllegalTimeZoneException("Invalid zone ID: $zoneId", e)
            }
        }

        public actual val availableZoneIds: Set<String>
            get() = getAvailableZoneIds()
    }

    public actual open val id: String
        get() = error("Should be overridden")

    public actual fun Instant.toLocalDateTime(): LocalDateTime = instantToLocalDateTime(this)

    @Suppress("DEPRECATION_ERROR")
    public actual fun LocalDateTime.toInstant(youShallNotPass: OverloadMarker): Instant =
        localDateTimeToInstant(this)

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

    internal open fun atStartOfDay(date: LocalDate): Instant = error("Should be overridden") //value.atStartOfDay(date)
    internal open fun offsetAtImpl(instant: Instant): UtcOffset = error("Should be overridden")

    internal open fun instantToLocalDateTime(instant: Instant): LocalDateTime = try {
        instant.toLocalDateTimeImpl(offsetAtImpl(instant))
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Instant $instant is not representable as LocalDateTime.", e)
    }

    internal open fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset? = null): Instant =
        error("Should be overridden")

    actual override fun equals(other: Any?): Boolean =
        this === other || other is TimeZone && this.id == other.id

    override fun hashCode(): Int = id.hashCode()

    actual override fun toString(): String = id
}

@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone internal constructor(public actual val offset: UtcOffset, override val id: String) : TimeZone() {

    public actual constructor(offset: UtcOffset) : this(offset, offset.toString())

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds

    override fun atStartOfDay(date: LocalDate): Instant =
        LocalDateTime(date, LocalTime.MIN).toInstant(offset)

    override fun offsetAtImpl(instant: Instant): UtcOffset = offset

    override fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset?): Instant =
        dateTime.toInstant(offset)

    override fun instantToLocalDateTime(instant: Instant): LocalDateTime = instant.toLocalDateTime(offset)
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

private val lenientOffsetFormat = UtcOffsetFormat.build {
    alternativeParsing(
        {
            offsetHours(Padding.NONE)
        },
        {
            isoOffset(
                zOnZero = false,
                useSeparator = false,
                outputMinute = WhenToOutput.IF_NONZERO,
                outputSecond = WhenToOutput.IF_NONZERO
            )
        }
    ) {
        isoOffset(
            zOnZero = true,
            useSeparator = true,
            outputMinute = WhenToOutput.ALWAYS,
            outputSecond = WhenToOutput.IF_NONZERO
        )
    }
}

internal actual fun localDateTimeToInstant(
    dateTime: LocalDateTime, timeZone: TimeZone, preferred: UtcOffset?
): Instant = timeZone.localDateTimeToInstant(dateTime, preferred)
