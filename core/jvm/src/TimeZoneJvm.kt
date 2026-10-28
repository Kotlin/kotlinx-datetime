/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.datetime.internal.TimeZoneRules
import kotlinx.datetime.serializers.*
import kotlinx.datetime.toInstant
import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneOffset as jtZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

public actual open class TimeZone internal constructor() {
    public actual open val id: String get() =
        error("Should be overridden")

    public actual open fun offsetAt(instant: Instant): UtcOffset =
        error("Should be overridden")

    public actual open fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        error("Should be overridden")

    // experimental member-extensions
    @Deprecated(
        "Pass the time zone as a context parameter using the `context(timeZone) { }` syntax",
        level = DeprecationLevel.HIDDEN,
    )
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        "Explicitly pass a TransitionHandler to `toInstant` calls " +
                "and pass the time zone as a context parameter using the `context(timeZone) { }` syntax",
        level = DeprecationLevel.HIDDEN,
        replaceWith = ReplaceWith("this.toInstant(TransitionHandler.USE_OFFSET_BEFORE)")
    )
    public actual fun LocalDateTime.toInstant(youShallNotPass: OverloadMarker): Instant =
        toInstant(this@TimeZone, TransitionHandler.USE_OFFSET_BEFORE)

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
        error("Should be overridden")

    override fun hashCode(): Int =
        error("Should be overridden")

    actual override fun toString(): String =
        error("Should be overridden")

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
                JvmTimeZone(zoneId)
        }

        @Deprecated(
            "Serializing TimeZone is discouraged, " +
                    "as deserialization can fail depending on the configuration. " +
                    "Please serialize the string id instead.",
            level = DeprecationLevel.WARNING,
        )
        @Suppress("DEPRECATION")
        public actual fun serializer(): kotlinx.serialization.KSerializer<TimeZone> = TimeZoneSerializer
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

public actual class FixedOffsetTimeZone
internal constructor(public actual val offset: UtcOffset, internal val actualZoneId: ZoneId): TimeZone() {
    public actual constructor(offset: UtcOffset) : this(offset, offset.zoneOffset)

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds

    /** @suppress */
    public actual companion object {
        /** @suppress */
        @Deprecated(
            "Serializing FixedOffsetTimeZone is discouraged, " +
                    "as deserialization can fail or return a non-fixed-offset zone depending on the configuration. " +
                    "Please serialize the string id instead.",
            level = DeprecationLevel.WARNING,
        )
        @Suppress("DEPRECATION")
        public actual fun serializer(): kotlinx.serialization.KSerializer<FixedOffsetTimeZone> =
            FixedOffsetTimeZoneSerializer
    }

    override val id: String
        get() = actualZoneId.id

    override fun offsetAt(instant: Instant): UtcOffset = offset

    override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        LocalDateTimeOffsetInfo.Regular(offset)

    override fun equals(other: Any?): Boolean =
        other is FixedOffsetTimeZone && actualZoneId == other.actualZoneId

    override fun hashCode(): Int = actualZoneId.hashCode()

    override fun toString(): String = actualZoneId.toString()
}

// compatibility with 0.8.0
@PublishedApi
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
internal fun TimeZone.offsetAt(instant: Instant): UtcOffset = offsetAt(instant)

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    LocalDateTime(java.time.LocalDateTime.ofEpochSecond(epochSeconds, nanosecondsOfSecond, offset.zoneOffset))
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Explicitly pass a TransitionHandler to `toInstant` calls",
    replaceWith = ReplaceWith("this.toInstant(timeZone, TransitionHandler.USE_OFFSET_BEFORE)")
)
public actual fun LocalDateTime.toInstant(timeZone: TimeZone, youShallNotPass: OverloadMarker): Instant =
    toInstant(timeZone, TransitionHandler.USE_OFFSET_BEFORE)

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(offset: UtcOffset, youShallNotPass: OverloadMarker): Instant =
    Instant.fromEpochSeconds(this.value.toEpochSecond(offset.zoneOffset), this.nanosecond)

internal class JvmTimeZone(val actualZoneId: ZoneId) : TimeZone() {
    override val id: String
        get() = actualZoneId.id

    override fun offsetAt(instant: Instant): UtcOffset =
        actualZoneId.rules.getOffset(instant.toJavaInstant()).let(::UtcOffset)

    override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo {
        val rules = actualZoneId.rules
        val validOffsets = rules.getValidOffsets(dateTime.value)
        validOffsets.singleOrNull()?.let { offset ->
            // fast path for the common case of only a single offset: we're making only one call to the Java API
            return LocalDateTimeOffsetInfo.Regular(UtcOffset(offset))
        }
        val transition = rules.getTransition(dateTime.value)
        check(transition != null) { "Inconsistent reading: no transition at $dateTime, offsets: $validOffsets" }
        return LocalDateTimeOffsetInfo.Transition(
            transition.instant.toKotlinInstant(),
            transition.offsetBefore.let(::UtcOffset),
            transition.offsetAfter.let(::UtcOffset),
        )
    }

    override fun equals(other: Any?): Boolean =
        other is JvmTimeZone && actualZoneId == other.actualZoneId

    override fun hashCode(): Int = actualZoneId.hashCode()

    override fun toString(): String = actualZoneId.toString()
}

internal class RuleBasedTimeZone(
    private val tzid: TimeZoneRules, override val id: String, val origin: Any?, overloadResolver: Unit
): TimeZone() {
    override fun offsetAt(instant: Instant): UtcOffset = tzid.infoAtInstant(instant)

    override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        tzid.infoAtDatetime(dateTime)

    override fun equals(other: Any?): Boolean =
        other is RuleBasedTimeZone && id == other.id && origin == other.origin

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id
}
