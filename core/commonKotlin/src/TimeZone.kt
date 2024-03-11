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

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor() {

    public actual companion object {

        public actual fun currentSystemDefault(): TimeZone {
            // TODO: probably check if currentSystemDefault name is parseable as FixedOffsetTimeZone?
            val (name, rules) = currentSystemDefaultZone()
            return if (rules == null) {
                of(name)
            } else {
                RegionTimeZone(rules, name)
            }
        }

        public actual val UTC: FixedOffsetTimeZone = UtcOffset.ZERO.asTimeZone()

        // org.threeten.bp.ZoneId#of(java.lang.String)
        public actual fun of(zoneId: String): TimeZone {
            // TODO: normalize aliases?
            if (zoneId == "Z") {
                return UTC
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
                    return when (offset.totalSeconds) {
                        0 -> FixedOffsetTimeZone(offset, prefix)
                        else -> FixedOffsetTimeZone(offset, "$prefix$offset")
                    }
                }
                if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
                    val offset = lenientOffsetFormat.parse(zoneId.substring(2))
                    return when (offset.totalSeconds) {
                        0 -> FixedOffsetTimeZone(offset, "UT")
                        else -> FixedOffsetTimeZone(offset, "UT$offset")
                    }
                }
            } catch (e: DateTimeFormatException) {
                throw IllegalTimeZoneException(e)
            }
            return try {
                RegionTimeZone(systemTzdb.rulesForId(zoneId), zoneId)
            } catch (e: Exception) {
                throw IllegalTimeZoneException("Invalid zone ID: $zoneId", e)
            }
        }

        public actual val availableZoneIds: Set<String>
            get() = systemTzdb.availableTimeZoneIds()
    }

    public actual open val id: String
        get() = error("Should be overridden")

    public actual fun Instant.toLocalDateTime(): LocalDateTime = instantToLocalDateTime(this)
    public actual fun LocalDateTime.toInstant(): Instant = localDateTimeToInstant(this)

    internal open fun atStartOfDay(date: LocalDate): Instant = error("Should be overridden") //value.atStartOfDay(date)
    internal open fun offsetAtImpl(instant: Instant): UtcOffset = error("Should be overridden")

    internal open fun instantToLocalDateTime(instant: Instant): LocalDateTime = try {
        instant.toLocalDateTimeImpl(offsetAtImpl(instant))
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Instant $instant is not representable as LocalDateTime.", e)
    }

    internal open fun localDateTimeToInstant(dateTime: LocalDateTime): Instant =
        atZone(dateTime).toInstant()

    internal open fun atZone(dateTime: LocalDateTime, preferred: UtcOffset? = null): ZonedDateTime =
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

    override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime =
        ZonedDateTime(dateTime, this, offset)

    override fun instantToLocalDateTime(instant: Instant): LocalDateTime = instant.toLocalDateTime(offset)
    override fun localDateTimeToInstant(dateTime: LocalDateTime): Instant = dateTime.toInstant(offset)
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
    val localEpochDay = localSecond.floorDiv(SECONDS_PER_DAY.toLong()).toInt()
    val secsOfDay = localSecond.mod(SECONDS_PER_DAY.toLong()).toInt()
    val date: LocalDate = LocalDate.fromEpochDays(localEpochDay) // may throw
    val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanosecondsOfSecond)
    return LocalDateTime(date, time)
}

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
    timeZone.localDateTimeToInstant(this)

public actual fun LocalDateTime.toInstant(offset: UtcOffset): Instant =
    Instant(this.toEpochSecond(offset), this.nanosecond)

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
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
