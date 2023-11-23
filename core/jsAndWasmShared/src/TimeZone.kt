/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.JodaTimeLocalDateTime
import kotlinx.datetime.internal.JodaTimeZoneId
import kotlinx.datetime.internal.JodaTimeZoneOffset
import kotlinx.datetime.internal.getAvailableZoneIdsSet
import kotlinx.datetime.serializers.*
import kotlinx.datetime.internal.JodaTimeZoneOffset as jtZoneOffset
import kotlinx.serialization.Serializable

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val zoneId: JodaTimeZoneId) {
    public actual val id: String get() = zoneId.id()

    // experimental member-extensions
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)
    public actual fun LocalDateTime.toInstant(): Instant = toInstant(this@TimeZone)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is TimeZone && this.zoneId == other.zoneId)

    override fun hashCode(): Int = zoneId.hashCode()

    override fun toString(): String = zoneId.toString()

    public actual companion object {
        public actual fun currentSystemDefault(): TimeZone = ofZone(JodaTimeZoneId.systemDefault())
        public actual val UTC: FixedOffsetTimeZone = UtcOffset(jtZoneOffset.UTC).asTimeZone()

        public actual fun of(zoneId: String): TimeZone = try {
            jsTry {
                ofZone(JodaTimeZoneId.of(zoneId))
            }
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw IllegalTimeZoneException(e)
            throw e
        }

        private fun ofZone(zoneId: JodaTimeZoneId): TimeZone {
            val zoneOffset = zoneId.toZoneOffset()
            return if (zoneOffset != null) {
                FixedOffsetTimeZone(UtcOffset(zoneOffset))
            } else if (zoneId.rules().isFixedOffset()) {
                FixedOffsetTimeZone(UtcOffset(zoneId.normalized().toZoneOffset()!!), zoneId)
            } else {
                TimeZone(zoneId)
            }
        }

        public actual val availableZoneIds: Set<String> get() = getAvailableZoneIdsSet()
    }
}

@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone
internal constructor(public actual val offset: UtcOffset, zoneId: JodaTimeZoneId): TimeZone(zoneId) {
    public actual constructor(offset: UtcOffset) : this(offset, offset.zoneOffset)

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds
}

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime = try {
    jsTry {
        JodaTimeLocalDateTime.ofInstant(this.value, timeZone.zoneId).let(::LocalDateTime)
    }
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    jsTry {
        JodaTimeLocalDateTime.ofInstant(this.value, offset.zoneOffset).let(::LocalDateTime)
    }
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}


public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
        zoneId.rules().offsetOfInstant(instant.value).let(::UtcOffset)

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        this.value.atZone(timeZone.zoneId).toInstant().let(::Instant)

public actual fun LocalDateTime.toInstant(offset: UtcOffset): Instant =
    this.value.toInstant(offset.zoneOffset).let(::Instant)

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
        this.value.atStartOfDay(timeZone.zoneId).toInstant().let(::Instant)
