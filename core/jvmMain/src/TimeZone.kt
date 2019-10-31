/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

actual class TimeZone private constructor(internal val zoneId: java.time.ZoneId) {
    public actual val id: String get() = zoneId.id


    // experimental member-extensions
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)
    public actual fun LocalDateTime.toInstant(): Instant = toInstant(this@TimeZone)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is TimeZone && this.zoneId == other.zoneId)

    override fun hashCode(): Int = zoneId.hashCode()

    override fun toString(): String = zoneId.toString()

    actual companion object {
        actual val SYSTEM: TimeZone = java.time.ZoneId.systemDefault().let(::TimeZone)
        actual val UTC: TimeZone = java.time.ZoneId.of("UTC").let(::TimeZone)
        actual fun of(zoneId: String): TimeZone = java.time.ZoneId.of(zoneId).let(::TimeZone)
    }
}