/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ZoneId
import kotlinx.datetime.serializers.TimeZoneSerializer
import kotlinx.datetime.serializers.FixedOffsetTimeZoneSerializer
import kotlinx.serialization.Serializable
import kotlinx.datetime.internal.JSJoda.ZoneOffset as jtZoneOffset

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
        public actual fun currentSystemDefault(): TimeZone = ZoneId.systemDefault().let(::TimeZone)
        public actual val UTC: TimeZone = jtZoneOffset.UTC.let(::TimeZone)

        public actual fun of(zoneId: String): TimeZone = try {
            val zone = ZoneId.of(zoneId)
            if (zone is jtZoneOffset) {
                FixedOffsetTimeZone(UtcOffset(zone))
            } else {
                TimeZone(zone)
            }
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw IllegalTimeZoneException(e)
            throw e
        }

        public actual val availableZoneIds: Set<String> get() = ZoneId.getAvailableZoneIds().toSet()
    }
}

@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone actual constructor(public actual val utcOffset: UtcOffset): TimeZone(utcOffset.zoneOffset) {
    private val zoneOffset get() = zoneId as jtZoneOffset

    @Deprecated("Use utcOffset.totalSeconds", ReplaceWith("utcOffset.totalSeconds"))
    public actual val totalSeconds: Int get() = zoneOffset.totalSeconds().toInt()
}

public actual class UtcOffset(internal val zoneOffset: jtZoneOffset) {
    public actual val totalSeconds: Int get() = zoneOffset.totalSeconds().toInt()

    override fun hashCode(): Int = zoneOffset.hashCode().toInt()
    override fun equals(other: Any?): Boolean = other is UtcOffset && this.zoneOffset == other.zoneOffset
    override fun toString(): String = zoneOffset.toString()

    public actual companion object {
        public actual fun parse(offsetString: String): UtcOffset = try {
            jtZoneOffset.of(offsetString).let(::UtcOffset)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw DateTimeFormatException(e)
            throw e
        }
    }
}


public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime = try {
    kotlinx.datetime.internal.JSJoda.LocalDateTime.ofInstant(this.value, timeZone.zoneId).let(::LocalDateTime)
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}

public actual fun Instant.toLocalDateTime(utcOffset: UtcOffset): LocalDateTime = try {
    kotlinx.datetime.internal.JSJoda.LocalDateTime.ofInstant(this.value, utcOffset.zoneOffset).let(::LocalDateTime)
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}


public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
        zoneId.rules().offsetOfInstant(instant.value).let(::UtcOffset)

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        this.value.atZone(timeZone.zoneId).toInstant().let(::Instant)

public actual fun LocalDateTime.toInstant(utcOffset: UtcOffset): Instant =
    this.value.atZone(utcOffset.zoneOffset).toInstant().let(::Instant)

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
        this.value.atStartOfDay(timeZone.zoneId).toInstant().let(::Instant)
