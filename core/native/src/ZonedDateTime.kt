/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

internal class ZonedDateTime(val dateTime: LocalDateTime, private val zone: TimeZone, val offset: UtcOffset) {
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plus(value: Int, unit: DateTimeUnit.DateBased): ZonedDateTime = dateTime.plus(value, unit).resolve()

    // Never throws in practice
    private fun LocalDateTime.resolve(): ZonedDateTime =
        // workaround for https://github.com/Kotlin/kotlinx-datetime/issues/51
        if (this@resolve.toInstant(offset).toLocalDateTime(zone) == this@resolve) {
            // this LocalDateTime is valid in these timezone and offset.
            ZonedDateTime(this, zone, offset)
        } else {
            // this LDT does need proper resolving, as the instant that it would map to given the preferred offset
            // is is mapped to another LDT.
            zone.atZone(this, offset)
        }

    override fun equals(other: Any?): Boolean =
        this === other || other is ZonedDateTime &&
            dateTime == other.dateTime && offset == other.offset && zone == other.zone

    override fun hashCode(): Int {
        return dateTime.hashCode() xor offset.hashCode() xor zone.hashCode().rotateLeft(3)
    }

    override fun toString(): String {
        var str = dateTime.toString() + offset.toString()
        if (zone !is FixedOffsetTimeZone || offset !== zone.offset) {
            str += "[$zone]"
        }
        return str
    }
}

internal fun ZonedDateTime.toInstant(): Instant =
    Instant(dateTime.toEpochSecond(offset), dateTime.nanosecond)


// org.threeten.bp.ZonedDateTime#until
// This version is simplified and to be used ONLY in case you know the timezones are equal!
/**
 * @throws ArithmeticException on arithmetic overflow
 * @throws DateTimeArithmeticException if setting [other] to the offset of [this] leads to exceeding boundaries of
 * [LocalDateTime].
 */

internal fun ZonedDateTime.until(other: ZonedDateTime, unit: DateTimeUnit): Long =
    when (unit) {
        // if the time unit is date-based, the offsets are disregarded and only the dates and times are compared.
        is DateTimeUnit.DateBased -> dateTime.until(other.dateTime, unit).toLong()
        // if the time unit is not date-based, we need to make sure that [other] is at the same offset as [this].
        is DateTimeUnit.TimeBased -> {
            val offsetDiff = offset.totalSeconds - other.offset.totalSeconds
            val otherLdtAdjusted = try {
                other.dateTime.plusSeconds(offsetDiff)
            } catch (e: IllegalArgumentException) {
                throw DateTimeArithmeticException(
                    "Unable to find difference between date-times, as one of them overflowed")
            }
            dateTime.until(otherLdtAdjusted, unit)
        }
    }
