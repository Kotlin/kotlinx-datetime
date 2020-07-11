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
    internal fun plusYears(years: Long): ZonedDateTime = dateTime.plusYears(years).resolve()
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusMonths(months: Long): ZonedDateTime = dateTime.plusMonths(months).resolve()
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusDays(days: Long): ZonedDateTime = dateTime.plusDays(days).resolve()

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
    val localEpochDay: Long = floorDiv(localSecond, SECONDS_PER_DAY.toLong())
    val secsOfDay: Long = floorMod(localSecond, SECONDS_PER_DAY.toLong())
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
internal fun ZonedDateTime.until(other: ZonedDateTime, unit: CalendarUnit): Long =
    when (unit) {
        // if the time unit is date-based, the offsets are disregarded and only the dates and times are compared.
        CalendarUnit.YEAR, CalendarUnit.MONTH, CalendarUnit.DAY -> {
            dateTime.until(other.dateTime, unit)
        }
        // if the time unit is not date-based, we need to make sure that [other] is at the same offset as [this].
        CalendarUnit.HOUR, CalendarUnit.MINUTE, CalendarUnit.SECOND, CalendarUnit.MILLISECOND, CalendarUnit.MICROSECOND, CalendarUnit.NANOSECOND -> {
            val offsetDiff = offset.totalSeconds - other.offset.totalSeconds
            val otherLdtAdjusted = try {
                other.dateTime.plusSeconds(offsetDiff.toLong())
            } catch (e: IllegalArgumentException) {
                throw DateTimeArithmeticException(
                    "Unable to find difference between date-times, as one of them overflowed")
            }
            dateTime.until(otherLdtAdjusted, unit)
        }
    }
