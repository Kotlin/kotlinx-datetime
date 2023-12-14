/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

internal class ZonedDateTime(val dateTime: LocalDateTime, val offset: UtcOffset) {
    override fun equals(other: Any?): Boolean =
        this === other || other is ZonedDateTime &&
            dateTime == other.dateTime && offset == other.offset

    override fun hashCode(): Int {
        return dateTime.hashCode() xor offset.hashCode()
    }

    override fun toString(): String {
        val str = dateTime.toString() + offset.toString()
        return str
    }
}

internal fun ZonedDateTime.toInstant(): Instant =
    Instant(dateTime.toEpochSecond(offset), dateTime.nanosecond)
