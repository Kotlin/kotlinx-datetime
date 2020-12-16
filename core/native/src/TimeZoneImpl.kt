/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

internal interface TimeZoneImpl {
    val id: String
    fun atStartOfDay(date: LocalDate): Instant
    fun LocalDateTime.atZone(preferred: ZoneOffsetImpl?): ZonedDateTime
    fun offsetAt(instant: Instant): ZoneOffsetImpl
}

internal expect class PlatformTimeZoneImpl: TimeZoneImpl {
    companion object {
        fun of(zoneId: String): PlatformTimeZoneImpl
        fun currentSystemDefault(): PlatformTimeZoneImpl
        val availableZoneIds: Set<String>
    }
}

internal class ZoneOffsetImpl(val totalSeconds: Int, override val id: String): TimeZoneImpl {

    companion object {
        // org.threeten.bp.ZoneOffset#UTC
        val UTC = ZoneOffsetImpl(0, "Z")
    }

    override fun atStartOfDay(date: LocalDate): Instant =
        LocalDateTime(date, LocalTime.MIN).atZone(null).toInstant()

    override fun LocalDateTime.atZone(preferred: ZoneOffsetImpl?): ZonedDateTime {
        return ZonedDateTime(this@atZone, FixedOffsetTimeZone(this@ZoneOffsetImpl), this@ZoneOffsetImpl)
    }

    override fun offsetAt(instant: Instant): ZoneOffsetImpl = this

    // org.threeten.bp.ZoneOffset#toString
    override fun toString(): String = id

    // org.threeten.bp.ZoneOffset#hashCode
    override fun hashCode(): Int = totalSeconds

    // org.threeten.bp.ZoneOffset#equals
    override fun equals(other: Any?): Boolean =
        this === other || other is ZoneOffsetImpl && totalSeconds == other.totalSeconds
}