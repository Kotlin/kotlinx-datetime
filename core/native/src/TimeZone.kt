/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val value: TimeZoneImpl) {

    public actual companion object {

        public actual fun currentSystemDefault(): TimeZone = PlatformTimeZoneImpl.currentSystemDefault().let(::TimeZone)

        public actual val UTC: TimeZone = UtcOffset.ZERO.asTimeZone()

        // org.threeten.bp.ZoneId#of(java.lang.String)
        public actual fun of(zoneId: String): TimeZone {
            // TODO: normalize aliases?
            if (zoneId == "Z") {
                return UTC
            }
            if (zoneId.length == 1) {
                throw IllegalTimeZoneException("Invalid zone ID: $zoneId")
            }
            if (zoneId.startsWith("+") || zoneId.startsWith("-")) {
                return UtcOffset.parse(zoneId).asTimeZone()
            }
            if (zoneId == "UTC" || zoneId == "GMT" || zoneId == "UT") {
                return FixedOffsetTimeZone(UtcOffset(0), zoneId)
            }
            if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") ||
                zoneId.startsWith("UTC-") || zoneId.startsWith("GMT-")) {
                val prefix = zoneId.take(3)
                val offset = UtcOffset.parse(zoneId.substring(3))
                return when (offset.totalSeconds) {
                    0 -> FixedOffsetTimeZone(offset, prefix)
                    else -> FixedOffsetTimeZone(offset, "$prefix$offset")
                }
            }
            if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
                val offset = UtcOffset.parse(zoneId.substring(2))
                return when (offset.totalSeconds) {
                    0 -> FixedOffsetTimeZone(offset, "UT")
                    else -> FixedOffsetTimeZone(offset, "UT$offset")
                }
            }
            return TimeZone(PlatformTimeZoneImpl.of(zoneId))
        }

        public actual val availableZoneIds: Set<String>
            get() = PlatformTimeZoneImpl.availableZoneIds
    }

    public actual val id: String
        get() = value.id

    public actual fun Instant.toLocalDateTime(): LocalDateTime = try {
        toZonedLocalDateTime(this@TimeZone).dateTime
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Instant ${this@toLocalDateTime} is not representable as LocalDateTime", e)
    }

    public actual fun LocalDateTime.toInstant(): Instant = atZone().toInstant()

    internal open fun atStartOfDay(date: LocalDate): Instant = value.atStartOfDay(date)

    internal open fun LocalDateTime.atZone(preferred: UtcOffset? = null): ZonedDateTime =
        with(value) { atZone(preferred) }

    override fun equals(other: Any?): Boolean =
        this === other || other is TimeZone && this.value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()
}


@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone internal constructor(public actual val utcOffset: UtcOffset, id: String) : TimeZone(ZoneOffsetImpl(utcOffset, id)) {

    public actual constructor(utcOffset: UtcOffset) : this(utcOffset, utcOffset.toString())

    @Deprecated("Use utcOffset.totalSeconds", ReplaceWith("utcOffset.totalSeconds"))
    public actual val totalSeconds: Int get() = utcOffset.totalSeconds
}


public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
    value.offsetAt(instant)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
        with(timeZone) { toLocalDateTime() }

public actual fun Instant.toLocalDateTime(utcOffset: UtcOffset): LocalDateTime = try {
    toLocalDateTimeImpl(utcOffset)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Instant ${this@toLocalDateTime} is not representable as LocalDateTime", e)
}

internal fun Instant.toLocalDateTimeImpl(offset: UtcOffset): LocalDateTime {
    val localSecond: Long = epochSeconds + offset.totalSeconds // overflow caught later
    val localEpochDay = floorDiv(localSecond, SECONDS_PER_DAY.toLong()).toInt()
    val secsOfDay = floorMod(localSecond, SECONDS_PER_DAY.toLong()).toInt()
    val date: LocalDate = LocalDate.ofEpochDay(localEpochDay) // may throw
    val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanosecondsOfSecond)
    return LocalDateTime(date, time)
}

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        with(timeZone) { toInstant() }

public actual fun LocalDateTime.toInstant(utcOffset: UtcOffset): Instant =
    Instant(this.toEpochSecond(utcOffset), this.nanosecond)

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
        timeZone.atStartOfDay(this)
