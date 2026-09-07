/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("TimeZoneKt")

package kotlinx.datetime

import java.time.DateTimeException
import java.time.ZoneId
import java.time.ZoneOffset as jtZoneOffset
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal fun TimeZone.Companion.ofZone(zoneId: ZoneId): TimeZone = when {
    zoneId is jtZoneOffset ->
        FixedOffsetTimeZone(UtcOffset(zoneId))
    zoneId.isFixedOffset ->
        FixedOffsetTimeZone(UtcOffset(zoneId.normalized() as jtZoneOffset), zoneId.toString())
    else ->
        JvmTimeZone(zoneId)
}

// Workaround for https://issuetracker.google.com/issues/203956057
private val ZoneId.isFixedOffset: Boolean
    get() = try {
        // On older Android versions, this can throw even though it shouldn't
        rules.isFixedOffset
    } catch (_: ArrayIndexOutOfBoundsException) {
        false // Happens for America/Costa_Rica, Africa/Cairo, Egypt
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
