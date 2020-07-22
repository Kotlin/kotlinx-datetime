/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

internal class ZonedDateTime(val dateTime: LocalDateTime, private val zone: TimeZone, val offset: ZoneOffset) {
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plus(value: Int, unit: DateTimeUnit.DateBased): ZonedDateTime = dateTime.plus(value, unit).resolve()

    // Never throws in practice
    private fun LocalDateTime.resolve(): ZonedDateTime = with(zone) { atZone(offset) }

    override fun equals(other: Any?): Boolean =
        this === other || other is ZonedDateTime &&
            dateTime == other.dateTime && offset == other.offset && zone == other.zone

    @OptIn(ExperimentalStdlibApi::class)
    override fun hashCode(): Int {
        return dateTime.hashCode() xor offset.hashCode() xor zone.hashCode().rotateLeft(3)
    }

    override fun toString(): String {
        var str = dateTime.toString() + offset.toString()
        if (offset !== zone) {
            str += "[$zone]"
        }
        return str
    }
}

internal fun ZonedDateTime.toInstant(): Instant =
    Instant(dateTime.toEpochSecond(offset), dateTime.nanosecond)

// org.threeten.bp.LocalDateTime#ofEpochSecond + org.threeten.bp.ZonedDateTime#create
/**
 * @throws IllegalArgumentException if the [Instant] exceeds the boundaries of [LocalDateTime]
 */
internal fun Instant.toZonedLocalDateTime(zone: TimeZone): ZonedDateTime {
    val currentOffset = with (zone) { offset }
    val localSecond: Long = epochSeconds + currentOffset.totalSeconds // overflow caught later
    val localEpochDay = floorDiv(localSecond, SECONDS_PER_DAY.toLong()).toInt()
    val secsOfDay = floorMod(localSecond, SECONDS_PER_DAY.toLong()).toInt()
    val date: LocalDate = LocalDate.ofEpochDay(localEpochDay) // may throw
    val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanosecondsOfSecond)
    return ZonedDateTime(LocalDateTime(date, time), zone, currentOffset)
}

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
