/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.datetime.internal.RuleBasedTimeZoneCalculations
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneOffset as jtZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val zoneId: ZoneIdLike) {
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
        public actual val UTC: FixedOffsetTimeZone =
            FixedOffsetTimeZone(UtcOffset.ZERO, ZoneId.of("UTC"))

        @Deprecated(
            "Use TimeZoneContext.System.currentTimeZone() instead",
            ReplaceWith("TimeZoneContext.System.currentTimeZone()")
        )
        public actual fun currentSystemDefault(): TimeZone =
            TimeZoneContext.System.currentTimeZone()

        @Deprecated(
            "Use TimeZoneContext.System.get() instead",
            ReplaceWith("TimeZoneContext.System.get(zoneId)")
        )
        public actual fun of(zoneId: String): TimeZone =
            TimeZoneContext.System.get(zoneId)

        @Deprecated(
            "Use TimeZoneContext.System.availableZoneIds() instead",
            ReplaceWith("TimeZoneContext.System.availableZoneIds()")
        )
        public actual val availableZoneIds: Set<String> get() =
            TimeZoneContext.System.availableZoneIds()

        internal fun ofZone(zoneId: ZoneId): TimeZone = when {
            zoneId is jtZoneOffset ->
                FixedOffsetTimeZone(UtcOffset(zoneId))
            zoneId.isFixedOffset ->
                FixedOffsetTimeZone(UtcOffset(zoneId.normalized() as jtZoneOffset), zoneId)
            else ->
                TimeZone(ZoneIdLike.ActualZoneId(zoneId))
        }

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
internal constructor(public actual val offset: UtcOffset, zoneId: ZoneId): TimeZone(ZoneIdLike.ActualZoneId(zoneId)) {

    public actual constructor(offset: UtcOffset) : this(offset, offset.zoneOffset)

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds
}

public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
    zoneId.offsetAt(instant)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    timeZone.zoneId.instantToLocalDateTime(this)

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    java.time.LocalDateTime.ofInstant(this.toJavaInstant(), offset.zoneOffset).let(::LocalDateTime)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(timeZone: TimeZone, youShallNotPass: OverloadMarker): Instant =
    localDateTimeToInstant(this, timeZone, null)

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(offset: UtcOffset, youShallNotPass: OverloadMarker): Instant =
    this.value.toInstant(offset.zoneOffset).toKotlinInstant()

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone, youShallNotPass: OverloadMarker): Instant =
    timeZone.zoneId.atStartOfDay(this)

internal actual fun localDateTimeToInstant(
    dateTime: LocalDateTime, timeZone: TimeZone, preferred: UtcOffset?
): Instant = timeZone.zoneId.localDateTimeToInstant(dateTime, preferred)

internal sealed interface ZoneIdLike {
    val id: String

    fun offsetAt(instant: Instant): UtcOffset

    fun instantToLocalDateTime(instant: Instant): LocalDateTime

    fun atStartOfDay(date: LocalDate): Instant

    fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset?): Instant

    class ActualZoneId(val actualZoneId: ZoneId) : ZoneIdLike {
        override val id: String
            get() = actualZoneId.id

        override fun offsetAt(instant: Instant): UtcOffset =
            actualZoneId.rules.getOffset(instant.toJavaInstant()).let(::UtcOffset)

        override fun instantToLocalDateTime(instant: Instant): LocalDateTime = try {
            java.time.LocalDateTime.ofInstant(instant.toJavaInstant(), actualZoneId).let(::LocalDateTime)
        } catch (e: DateTimeException) {
            throw DateTimeArithmeticException(e)
        }

        override fun atStartOfDay(date: LocalDate): Instant =
            date.value.atStartOfDay(actualZoneId).toInstant().toKotlinInstant()

        override fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset?): Instant =
            java.time.ZonedDateTime.ofLocal(
                dateTime.value, actualZoneId, preferred?.zoneOffset
            ).toInstant().toKotlinInstant()

        override fun equals(other: Any?): Boolean = other is ActualZoneId && actualZoneId == other.actualZoneId
        override fun hashCode(): Int = actualZoneId.hashCode()
        override fun toString(): String = actualZoneId.toString()
    }

    class RuleBasedZoneId(val zoneRules: RuleBasedTimeZoneCalculations): ZoneIdLike {
        override val id: String get() = zoneRules.id

        override fun offsetAt(instant: Instant): UtcOffset =
            zoneRules.offsetAtImpl(instant)

        override fun atStartOfDay(date: LocalDate): Instant =
            zoneRules.atStartOfDay(date)

        override fun localDateTimeToInstant(dateTime: LocalDateTime, preferred: UtcOffset?): Instant =
            zoneRules.localDateTimeToInstant(dateTime, preferred)

        override fun instantToLocalDateTime(instant: Instant): LocalDateTime =
            instant.toLocalDateTime(offsetAt(instant))

        override fun equals(other: Any?): Boolean = other is RuleBasedZoneId && zoneRules.id == other.zoneRules.id
        override fun hashCode(): Int = zoneRules.hashCode()
        override fun toString(): String = id
    }
}
