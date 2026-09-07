/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlin.time.Instant

internal actual val UtcImpl: FixedOffsetTimeZone = FixedOffsetTimeZone(UtcOffset.ZERO, "UTC")

public actual class FixedOffsetTimeZone internal constructor(public actual val offset: UtcOffset, override val id: String) : TimeZone() {

    public actual constructor(offset: UtcOffset) : this(offset, offset.toString())

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds

    override fun offsetAt(instant: Instant): UtcOffset = offset

    override fun offsetInfoFor(dateTime: LocalDateTime): LocalDateTimeOffsetInfo =
        LocalDateTimeOffsetInfo.Regular(offset)

    override fun toString(): String = id

    override fun equals(other: Any?): Boolean =
        this === other || other is FixedOffsetTimeZone && this.id == other.id

    override fun hashCode(): Int = id.hashCode()

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
}

@PublishedApi
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
internal fun TimeZone.offsetAt(instant: Instant): UtcOffset = offsetAt(instant) // member shadows the extension

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    toLocalDateTimeImpl(offset)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Instant ${this@toLocalDateTime} is not representable as LocalDateTime", e)
}

internal fun Instant.toLocalDateTimeImpl(offset: UtcOffset): LocalDateTime {
    val localSecond: Long = epochSeconds + offset.totalSeconds // overflow caught later
    val localEpochDay = localSecond.floorDiv(SECONDS_PER_DAY.toLong())
    val secsOfDay = localSecond.mod(SECONDS_PER_DAY.toLong()).toInt()
    val date: LocalDate = LocalDate.fromEpochDays(localEpochDay) // may throw
    val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanosecondsOfSecond)
    return LocalDateTime(date, time)
}

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(timeZone: TimeZone, youShallNotPass: OverloadMarker): Instant =
    localDateTimeToInstantLenient(this, timeZone, TransitionHandler.USE_OFFSET_BEFORE, null)

@Suppress("DEPRECATION_ERROR")
public actual fun LocalDateTime.toInstant(offset: UtcOffset, youShallNotPass: OverloadMarker): Instant =
    Instant.fromEpochSeconds(this.toEpochSecond(offset), this.nanosecond)
