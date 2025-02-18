/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneOffset as jtZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val zoneId: ZoneId) {
    public actual val id: String get() = zoneId.id


    // experimental member-extensions
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)

    @Suppress("DEPRECATION_ERROR")
    public actual fun LocalDateTime.toInstant(youShallNotPass: OverloadMarker): Instant = toInstant(this@TimeZone)

    @Suppress("DEPRECATION")
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().toLocalDateTime()")
    )
    public actual fun kotlinx.datetime.Instant.toLocalDateTime(): LocalDateTime =
        toStdlibInstant().toLocalDateTime()

    @PublishedApi
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "DEPRECATION")
    @kotlin.internal.LowPriorityInOverloadResolution
    internal actual fun LocalDateTime.toInstant(): kotlinx.datetime.Instant =
        toInstant(this@TimeZone).toDeprecatedInstant()

    actual override fun equals(other: Any?): Boolean =
            (this === other) || (other is TimeZone && this.zoneId == other.zoneId)

    override fun hashCode(): Int = zoneId.hashCode()

    actual override fun toString(): String = zoneId.toString()

    public actual companion object {
        public actual fun currentSystemDefault(): TimeZone = ofZone(ZoneId.systemDefault())
        public actual val UTC: FixedOffsetTimeZone =
            FixedOffsetTimeZone(UtcOffset.ZERO, ZoneId.of("UTC"))

        public actual fun of(zoneId: String): TimeZone = try {
            ofZone(ZoneId.of(if (zoneId == "z") "Z" else zoneId))
        } catch (e: Exception) {
            if (e is DateTimeException) throw IllegalTimeZoneException(e)
            throw e
        }

        internal fun ofZone(zoneId: ZoneId): TimeZone = when {
            zoneId is jtZoneOffset ->
                FixedOffsetTimeZone(UtcOffset(zoneId))
            zoneId.isFixedOffset ->
                FixedOffsetTimeZone(UtcOffset(zoneId.normalized() as jtZoneOffset), zoneId)
            else ->
                TimeZone(zoneId)
        }

        public actual val availableZoneIds: Set<String> get() = ZoneId.getAvailableZoneIds()
    }
}

// Workaround for https://issuetracker.google.com/issues/203956057
private val ZoneId.isFixedOffset: Boolean
    get() = try {
        // On older Android versions, this can throw even though it shouldn't
        rules.isFixedOffset
    } catch (e: ArrayIndexOutOfBoundsException) {
        false // Happens for America/Costa_Rica, Africa/Cairo, Egypt
    }

@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone
internal constructor(public actual val offset: UtcOffset, zoneId: ZoneId): TimeZone(zoneId) {

    public actual constructor(offset: UtcOffset) : this(offset, offset.zoneOffset)

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds
}

public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
        zoneId.rules.getOffset(instant.toJavaInstant()).let(::UtcOffset)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime = try {
    java.time.LocalDateTime.ofInstant(this.toJavaInstant(), timeZone.zoneId).let(::LocalDateTime)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    java.time.LocalDateTime.ofInstant(this.toJavaInstant(), offset.zoneOffset).let(::LocalDateTime)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}


@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(timeZone: TimeZone, youShallNotPass: OverloadMarker): Instant =
        this.value.atZone(timeZone.zoneId).toInstant().toKotlinInstant()

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(offset: UtcOffset, youShallNotPass: OverloadMarker): Instant =
        this.value.toInstant(offset.zoneOffset).toKotlinInstant()

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone, youShallNotPass: OverloadMarker): Instant =
        this.value.atStartOfDay(timeZone.zoneId).toInstant().toKotlinInstant()

internal actual fun localDateTimeToInstant(
    dateTime: LocalDateTime, timeZone: TimeZone, preferred: UtcOffset?
): Instant = java.time.ZonedDateTime.ofLocal(
    dateTime.value, timeZone.zoneId, preferred?.zoneOffset
).toInstant().toKotlinInstant()
