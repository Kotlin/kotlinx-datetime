/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.serializers.*
import kotlinx.datetime.internal.JSJoda.ZoneId
import kotlinx.datetime.internal.JSJoda.ZoneOffset as jtZoneOffset
import kotlinx.serialization.Serializable

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val zoneId: ZoneId) {
    public actual val id: String get() = zoneId.id()


    // experimental member-extensions
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)
    public actual fun LocalDateTime.toInstant(): Instant = toInstant(this@TimeZone)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is TimeZone && this.zoneId == other.zoneId)

    override fun hashCode(): Int = zoneId.hashCode().toInt()

    override fun toString(): String = zoneId.toString()

    public actual companion object {
        public actual fun currentSystemDefault(): TimeZone = ofZone(ZoneId.systemDefault())
        public actual val UTC: FixedOffsetTimeZone = UtcOffset(jtZoneOffset.UTC).asTimeZone()

        public actual fun of(zoneId: String): TimeZone = try {
            ofZone(ZoneId.of(zoneId))
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw IllegalTimeZoneException(e)
            throw e
        }

        private fun ofZone(zoneId: ZoneId): TimeZone = when {
            zoneId is jtZoneOffset ->
                FixedOffsetTimeZone(UtcOffset(zoneId))
            zoneId.rules().isFixedOffset() ->
                FixedOffsetTimeZone(UtcOffset(zoneId.normalized() as jtZoneOffset), zoneId)
            else ->
                TimeZone(zoneId)
        }


        public actual val availableZoneIds: Set<String> get() = ZoneId.getAvailableZoneIds().toSet()
    }
}

@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone
internal constructor(public actual val offset: UtcOffset, zoneId: ZoneId): TimeZone(zoneId) {

    public actual constructor(offset: UtcOffset) : this(offset, offset.zoneOffset)

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds
}


public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime = try {
    kotlinx.datetime.internal.JSJoda.LocalDateTime.ofInstant(this.value, timeZone.zoneId).let(::LocalDateTime)
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    kotlinx.datetime.internal.JSJoda.LocalDateTime.ofInstant(this.value, offset.zoneOffset).let(::LocalDateTime)
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
