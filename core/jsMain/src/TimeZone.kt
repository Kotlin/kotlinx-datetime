/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ZoneId
import kotlinx.datetime.internal.JSJoda.ZoneOffset as jtZoneOffset

actual open class TimeZone internal constructor(internal val zoneId: ZoneId) {
    public actual val id: String get() = zoneId.id()


    // experimental member-extensions
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)
    public actual val Instant.offset: ZoneOffset get() = offsetAt(this@TimeZone)
    public actual fun LocalDateTime.toInstant(): Instant = toInstant(this@TimeZone)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is TimeZone && this.zoneId == other.zoneId)

    override fun hashCode(): Int = zoneId.hashCode().toInt()

    override fun toString(): String = zoneId.toString()

    actual companion object {
        actual val SYSTEM: TimeZone = ZoneId.systemDefault().let(::TimeZone)
        actual val UTC: TimeZone = jtZoneOffset.UTC.let(::TimeZone)
        actual fun of(zoneId: String): TimeZone = ZoneId.of(zoneId).let(::TimeZone)
        actual val availableZoneIds: Set<String> get() = ZoneId.getAvailableZoneIds().toSet()
    }
}

public actual class ZoneOffset internal constructor(zoneOffset: jtZoneOffset): TimeZone(zoneOffset) {
    internal val zoneOffset get() = zoneId as jtZoneOffset

    actual val totalSeconds: Int get() = zoneOffset.totalSeconds().toInt()
}