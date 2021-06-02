/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

internal interface TimeZoneImpl {
    val id: String
    fun atStartOfDay(date: LocalDate): Instant
    fun LocalDateTime.atZone(preferred: UtcOffset?): ZonedDateTime
    fun offsetAt(instant: Instant): UtcOffset
}

internal expect class PlatformTimeZoneImpl: TimeZoneImpl {
    companion object {
        fun of(zoneId: String): PlatformTimeZoneImpl
        fun currentSystemDefault(): PlatformTimeZoneImpl
        val availableZoneIds: Set<String>
    }
}

internal class ZoneOffsetImpl(val utcOffset: UtcOffset, override val id: String): TimeZoneImpl {

    override fun atStartOfDay(date: LocalDate): Instant =
        LocalDateTime(date, LocalTime.MIN).atZone(null).toInstant()

    override fun LocalDateTime.atZone(preferred: UtcOffset?): ZonedDateTime {
        return ZonedDateTime(this@atZone, utcOffset.asTimeZone(), utcOffset)
    }

    override fun offsetAt(instant: Instant): UtcOffset = utcOffset

    // org.threeten.bp.ZoneOffset#toString
    override fun toString(): String = id

    // org.threeten.bp.ZoneOffset#hashCode
    override fun hashCode(): Int = utcOffset.hashCode()

    // org.threeten.bp.ZoneOffset#equals
    override fun equals(other: Any?): Boolean =
        this === other || other is ZoneOffsetImpl && utcOffset == other.utcOffset
}