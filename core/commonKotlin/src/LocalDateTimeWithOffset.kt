/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

internal data class LocalDateTimeWithOffset(val dateTime: LocalDateTime, val offset: UtcOffset)

internal fun LocalDateTimeWithOffset.toInstant(): Instant =
    Instant(dateTime.toEpochSecond(offset), dateTime.nanosecond)
