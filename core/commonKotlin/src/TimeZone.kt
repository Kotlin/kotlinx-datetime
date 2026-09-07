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
