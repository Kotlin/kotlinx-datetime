/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.clampToInt
import kotlinx.datetime.internal.format.AppendableFormatStructure

public interface UnresolvedZonedDateTime {
    public val rawLocalDateTime: LocalDateTime
    public val timeZone: TimeZone
    public val preferredOffset: UtcOffset?

    public fun resolve(): ZonedDateTime

    public object Formats {
        public val RFC_9557: DateTimeFormat<UnresolvedZonedDateTime> = Format {
            dateTime(LocalDateTime.Formats.ISO)
        }
    }

    public companion object {
        public fun parse(
            input: CharSequence,
            format: DateTimeFormat<UnresolvedZonedDateTime> = Formats.RFC_9557
        ): UnresolvedZonedDateTime {
            throw NotImplementedError("UnresolvedZonedDateTime.parse is not implemented yet")
        }

        public fun Format(
            block: DateTimeFormatBuilder.WithZonedDateTime.() -> Unit
        ): DateTimeFormat<UnresolvedZonedDateTime> {
            val builder = DateTimeComponentsFormat.Builder(AppendableFormatStructure())
            block(builder)
            return ZonedDateTimeFormat(builder.build())

        }
    }
}

private class UnresolvedZonedDateTimeImpl(
    override val rawLocalDateTime: LocalDateTime,
    override val timeZone: TimeZone,
    override val preferredOffset: UtcOffset? = null,
): UnresolvedZonedDateTime {
    override fun resolve(): ZonedDateTime =
        localDateTimeToInstant(rawLocalDateTime, timeZone, preferredOffset).atZone(timeZone)

    override fun equals(other: Any?): Boolean =
        this === other || other is UnresolvedZonedDateTime && rawLocalDateTime == other.rawLocalDateTime &&
                preferredOffset == other.preferredOffset && timeZone == other.timeZone

    override fun hashCode(): Int {
        var result = 5
        result = 31 * result + rawLocalDateTime.hashCode()
        result = 31 * result + (preferredOffset?.hashCode() ?: 0)
        result = 31 * result + timeZone.hashCode()
        return result
    }

    override fun toString(): String {
        return "UnresolvedZonedDateTime(localDateTime=$rawLocalDateTime, timeZone=$timeZone, utcOffset=$preferredOffset)"
    }
}

public fun UnresolvedZonedDateTime(
    localDateTime: LocalDateTime,
    timeZone: TimeZone,
    preferredUtcOffset: UtcOffset? = null,
): UnresolvedZonedDateTime = UnresolvedZonedDateTimeImpl(localDateTime, timeZone, preferredUtcOffset)

public class ZonedDateTime(
    public val localDateTime: LocalDateTime,
    public val utcOffset: UtcOffset,
    override val timeZone: TimeZone,
): UnresolvedZonedDateTime {
    public fun toInstant(): Instant = localDateTime.toInstant(utcOffset)

    override val rawLocalDateTime: LocalDateTime get() = localDateTime
    override val preferredOffset: UtcOffset get() = utcOffset

    override fun resolve(): ZonedDateTime = this

    override fun equals(other: Any?): Boolean =
        this === other || other is ZonedDateTime && localDateTime == other.localDateTime &&
                utcOffset == other.utcOffset && timeZone == other.timeZone

    override fun hashCode(): Int {
        var result = 3
        result = 31 * result + rawLocalDateTime.hashCode()
        result = 31 * result + utcOffset.hashCode()
        result = 31 * result + timeZone.hashCode()
        return result
    }

    override fun toString(): String {
        return "ZonedDateTime(localDateTime=$localDateTime, utcOffset=$utcOffset, timeZone=$timeZone)"
    }
}

private fun t() {
    val zdt = Clock.System.now().atZone(TimeZone.of("Europe/Berlin"))
    if (zdt == zdt.withDate { LocalDate(2024, 1, 1) }) {
        println("The date was the correct one already.")
    } else {
        println("The date was incorrect.")
    }
}




/// CONVERSION FUNCTIONS

public fun LocalDateTime.atZone(timeZone: TimeZone): UnresolvedZonedDateTime =
    UnresolvedZonedDateTime(this, timeZone)

public fun Instant.atZone(timeZone: TimeZone): ZonedDateTime {
    val offset = timeZone.offsetAt(this)
    return ZonedDateTime(
        localDateTime = toLocalDateTime(offset),
        utcOffset = offset,
        timeZone = timeZone
    )
}





/// CALENDAR-BASED MODIFICATION FUNCTIONS

// Modifies the date.
public fun UnresolvedZonedDateTime.withDate(
    action: (LocalDate) -> LocalDate
): UnresolvedZonedDateTime =
    UnresolvedZonedDateTime(action(rawLocalDateTime.date).atTime(rawLocalDateTime.time), timeZone, preferredOffset)

// Modifies the time.
public fun UnresolvedZonedDateTime.withTime(
    action: (LocalTime) -> LocalTime
): UnresolvedZonedDateTime =
    UnresolvedZonedDateTime(rawLocalDateTime.date.atTime(action(rawLocalDateTime.time)), timeZone, preferredOffset)

public fun UnresolvedZonedDateTime.plus(period: DatePeriod): UnresolvedZonedDateTime =
    withDate { it.plus(period) }

public fun UnresolvedZonedDateTime.minus(period: DatePeriod): UnresolvedZonedDateTime =
    withDate { it.minus(period) }

public fun UnresolvedZonedDateTime.plus(value: Long, unit: DateTimeUnit.DateBased): UnresolvedZonedDateTime =
    withDate { it.plus(value, unit) }

public fun UnresolvedZonedDateTime.minus(value: Long, unit: DateTimeUnit.DateBased): UnresolvedZonedDateTime =
    withDate { it.minus(value, unit) }

public fun UnresolvedZonedDateTime.plus(value: Int, unit: DateTimeUnit.DateBased): UnresolvedZonedDateTime =
    withDate { it.plus(value, unit) }

public fun UnresolvedZonedDateTime.minus(value: Int, unit: DateTimeUnit.DateBased): UnresolvedZonedDateTime =
    withDate { it.minus(value, unit) }




/// ELAPSED-TIME-BASED MODIFICATION FUNCTIONS

// Adds the real-world time to the instant.
public fun ZonedDateTime.plus(
    value: Long, unit: DateTimeUnit.TimeBased
): ZonedDateTime =
    toInstant().plus(value, unit).atZone(timeZone)

public fun ZonedDateTime.plus(value: Int, unit: DateTimeUnit.TimeBased): ZonedDateTime = plus(value.toLong(), unit)

public fun ZonedDateTime.minus(value: Int, unit: DateTimeUnit.TimeBased): ZonedDateTime = minus(value.toLong(), unit)

public fun ZonedDateTime.minus(value: Long, unit: DateTimeUnit.TimeBased): ZonedDateTime =
    if (value != Long.MIN_VALUE) {
        plus(-value, unit)
    } else {
        plus(-(value + 1), unit).plus(1, unit)
    }





/// COMBINED MODIFICATION FUNCTIONS

public fun ZonedDateTime.plus(value: Long, unit: DateTimeUnit): UnresolvedZonedDateTime = when (unit) {
    is DateTimeUnit.DateBased -> (this as UnresolvedZonedDateTime).plus(value, unit)
    is DateTimeUnit.TimeBased -> plus(value, unit)
}

public fun ZonedDateTime.minus(value: Long, unit: DateTimeUnit): UnresolvedZonedDateTime = when (unit) {
    is DateTimeUnit.DateBased -> (this as UnresolvedZonedDateTime).minus(value, unit)
    is DateTimeUnit.TimeBased -> minus(value, unit)
}

public fun ZonedDateTime.plus(value: Int, unit: DateTimeUnit): UnresolvedZonedDateTime =
    plus(value.toLong(), unit)

public fun ZonedDateTime.minus(value: Int, unit: DateTimeUnit): UnresolvedZonedDateTime = plus(-value.toLong(), unit)

public fun UnresolvedZonedDateTime.plus(period: DateTimePeriod): ZonedDateTime = try {
    plus(period.datePeriodPortion())
        .resolve().plus(period.totalNanoseconds, DateTimeUnit.NANOSECOND)
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding CalendarPeriod to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding CalendarPeriod", e)
}

public fun UnresolvedZonedDateTime.minus(period: DateTimePeriod): ZonedDateTime =
    /* An overflow can happen for any component, but we are only worried about nanoseconds, as having an overflow in
    any other component means that `plus` will throw due to the minimum value of the numeric type overflowing the
    `ZonedDateTime` limits. */
    if (period.totalNanoseconds != Long.MIN_VALUE) {
        val negatedPeriod = with(period) { buildDateTimePeriod(-totalMonths, -days, -totalNanoseconds) }
        plus(negatedPeriod)
    } else {
        val negatedPeriod = with(period) { buildDateTimePeriod(-totalMonths, -days, -(totalNanoseconds+1)) }
        plus(negatedPeriod).plus(1L, DateTimeUnit.NANOSECOND)
    }





/// TIME-BASED DISTANCE FUNCTIONS

public fun ZonedDateTime.until(other: ZonedDateTime, unit: DateTimeUnit.TimeBased): Long =
    toInstant().until(other.toInstant(), unit)

public fun ZonedDateTime.minus(other: ZonedDateTime, unit: DateTimeUnit.TimeBased): Long = other.until(this, unit)





/// DATE-BASED DISTANCE FUNCTIONS :(

// For date-based units:
// The 'ZonedDateTime' bound on the argument can not be relaxed into 'UnresolvedZonedDateTime' because of this case:
// this = 02:15 the day before DST change.
// other = 02:15 the day of DST change, and at 03:00 the clock is set back to 02:00.
// Depending on the resolver and on whether the 'other' is the moment before or after the DST change,
// the result of 'other.until(this, DateTimeUnit.DAY)' can be either 1 or 0 days.

public fun UnresolvedZonedDateTime.until(other: ZonedDateTime, unit: DateTimeUnit.DateBased): Long {
    require(this.timeZone == other.timeZone) {
        "ZonedDateTime.until: time zones must be the same, but was '${this.timeZone}' and '${other.timeZone}'"
    }
    val timeAfterAddingDate = withDate { other.rawLocalDateTime.date }.resolve().toInstant()
    val delta = when {
        other.rawLocalDateTime.date > rawLocalDateTime.date && timeAfterAddingDate > other.toInstant() -> -1 // addition won't throw: end date - date >= 1
        other.rawLocalDateTime.date < rawLocalDateTime.date && timeAfterAddingDate < other.toInstant() -> 1 // addition won't throw: date - end date >= 1
        else -> 0
    }
    return rawLocalDateTime.date.until(other.rawLocalDateTime.date.plus(delta, DateTimeUnit.DAY), unit)
}

public fun ZonedDateTime.minus(other: UnresolvedZonedDateTime, unit: DateTimeUnit.DateBased): Long = other.until(this, unit)

public fun UnresolvedZonedDateTime.daysUntil(other: ZonedDateTime): Int = until(other, DateTimeUnit.DAY).clampToInt()

public fun UnresolvedZonedDateTime.monthsUntil(other: ZonedDateTime): Int = until(other, DateTimeUnit.MONTH).clampToInt()

public fun UnresolvedZonedDateTime.yearsUntil(other: ZonedDateTime): Int = until(other, DateTimeUnit.YEAR).clampToInt()

public fun UnresolvedZonedDateTime.periodUntil(other: ZonedDateTime): DateTimePeriod {
    require(this.timeZone == other.timeZone) {
        "ZonedDateTime.periodUntil: time zones must be the same, but was '${this.timeZone}' and '${other.timeZone}'"
    }
    val otherInstant = other.toInstant()
    val timeAfterAddingDate = withDate { other.rawLocalDateTime.date }.resolve().toInstant()
    val delta = when {
        other.rawLocalDateTime.date > rawLocalDateTime.date && timeAfterAddingDate > otherInstant -> -1 // addition won't throw: end date - date >= 1
        other.rawLocalDateTime.date < rawLocalDateTime.date && timeAfterAddingDate < otherInstant -> 1 // addition won't throw: date - end date >= 1
        else -> 0
    }
    val endDate = other.rawLocalDateTime.date.plus(delta, DateTimeUnit.DAY) // `endDate` is guaranteed to be valid
    // won't throw: thisLdt + days <= otherLdt
    val nanoseconds = withDate { endDate }.resolve().until(other, DateTimeUnit.NANOSECOND) // |otherLdt - thisLdt| < 24h
    val datePeriod = endDate - rawLocalDateTime.date
    return buildDateTimePeriod(datePeriod.totalMonths, datePeriod.days, nanoseconds)
}

public fun ZonedDateTime.minus(other: ZonedDateTime): DateTimePeriod = other.periodUntil(this)




/// COMBINED DISTANCE FUNCTIONS

public fun ZonedDateTime.until(other: ZonedDateTime, unit: DateTimeUnit): Long = when (unit) {
    is DateTimeUnit.DateBased -> (this as UnresolvedZonedDateTime).until(other, unit)
    is DateTimeUnit.TimeBased -> until(other, unit)
}

public fun ZonedDateTime.minus(other: ZonedDateTime, unit: DateTimeUnit): Long = other.until(this, unit)
