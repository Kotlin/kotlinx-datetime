/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneOffset as jtZoneOffset

@Serializable(with = TimeZoneSerializer::class)
actual open class TimeZone internal constructor(internal val zoneId: ZoneId) {
    public actual val id: String get() = zoneId.id


    // experimental member-extensions
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)
    public actual fun LocalDateTime.toInstant(): Instant = toInstant(this@TimeZone)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is TimeZone && this.zoneId == other.zoneId)

    override fun hashCode(): Int = zoneId.hashCode()

    override fun toString(): String = zoneId.toString()

    actual companion object {
        actual fun currentSystemDefault(): TimeZone = ZoneId.systemDefault().let(::TimeZone)
        actual val UTC: TimeZone = jtZoneOffset.UTC.let(::TimeZone)

        actual fun of(zoneId: String): TimeZone = try {
            val zone = ZoneId.of(zoneId)
            if (zone is jtZoneOffset) {
                ZoneOffset(zone)
            } else {
                TimeZone(zone)
            }
        } catch (e: Exception) {
            if (e is DateTimeException) throw IllegalTimeZoneException(e)
            throw e
        }

        actual val availableZoneIds: Set<String> get() = ZoneId.getAvailableZoneIds()
    }
}

@Serializable(with = ZoneOffsetSerializer::class)
public actual class ZoneOffset internal constructor(zoneOffset: jtZoneOffset): TimeZone(zoneOffset) {
    internal val zoneOffset get() = zoneId as jtZoneOffset

    actual val totalSeconds: Int get() = zoneOffset.totalSeconds
}

public actual fun TimeZone.offsetAt(instant: Instant): ZoneOffset =
        zoneId.rules.getOffset(instant.value).let(::ZoneOffset)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime = try {
    java.time.LocalDateTime.ofInstant(this.value, timeZone.zoneId).let(::LocalDateTime)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        this.value.atZone(timeZone.zoneId).toInstant().let(::Instant)

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
        this.value.atStartOfDay(timeZone.zoneId).toInstant().let(::Instant)
