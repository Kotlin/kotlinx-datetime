/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.clampToInt
import kotlinx.datetime.internal.format.AppendableFormatStructure

public class UnresolvedZonedDateTime(
    public val rawLocalDateTime: LocalDateTime,
    public val timeZone: TimeZone,
    public val preferredOffset: UtcOffset? = null,
) {
    public fun resolve(resolver: TransitionResolver): Instant =
        localDateTimeToInstant(rawLocalDateTime, timeZone, preferredOffset)

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

    public object Formats {
        public val RFC_9557: DateTimeFormat<UnresolvedZonedDateTime> = Format {
            dateTime(LocalDateTime.Formats.ISO)
        }
    }

    public companion object {
        public fun parse(
            input: CharSequence,
            format: DateTimeFormat<UnresolvedZonedDateTime> = Formats.RFC_9557
        ): UnresolvedZonedDateTime = format.parse(input)

        public fun Format(
            block: DateTimeFormatBuilder.WithZonedDateTime.() -> Unit
        ): DateTimeFormat<UnresolvedZonedDateTime> {
            val builder = DateTimeComponentsFormat.Builder(AppendableFormatStructure())
            block(builder)
            return ZonedDateTimeFormat(builder.build())

        }
    }
}

private fun t() {
    val timeZone = TimeZone.of("Europe/Berlin")
    val zdt = Clock.System.now().unresolve(timeZone)
    if (zdt == zdt.withDate { LocalDate(2024, 1, 1) }) {
        println("The date was the correct one already.")
    } else {
        println("The date was incorrect.")
    }
}


/// CONVERSION FUNCTIONS

public fun LocalDateTime.atZone(timeZone: TimeZone): UnresolvedZonedDateTime =
    UnresolvedZonedDateTime(this, timeZone)

public fun Instant.unresolve(timeZone: TimeZone) : UnresolvedZonedDateTime =
    UnresolvedZonedDateTime(toLocalDateTime(timeZone), timeZone, preferredOffset = offsetIn(timeZone))

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

// public fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant
// public fun Instant.plus(value: Int, unit: DateTimeUnit.TimeBased): Instant
// public fun Instant.minus(value: Int, unit: DateTimeUnit.TimeBased): Instant
// public fun Instant.minus(value: Long, unit: DateTimeUnit.TimeBased): Instant

/// COMBINED MODIFICATION FUNCTIONS

// // Conflicts with the existing Instant.plus function.
// public fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): UnresolvedZonedDateTime = when (unit) {
//     is DateTimeUnit.DateBased -> unresolve(timeZone).plus(value, unit)
//     is DateTimeUnit.TimeBased -> plus(value, unit).unresolve(timeZone)
// }
//
// public fun Instant.minus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): UnresolvedZonedDateTime = when (unit) {
//     is DateTimeUnit.DateBased -> unresolve(timeZone).minus(value, unit)
//     is DateTimeUnit.TimeBased -> minus(value, unit).unresolve(timeZone)
// }
//
// public fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): UnresolvedZonedDateTime =
//     plus(value.toLong(), unit)
//
// public fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): UnresolvedZonedDateTime =
//     plus(-value.toLong(), unit)

public fun UnresolvedZonedDateTime.plus(period: DateTimePeriod, resolver: TransitionResolver): Instant = try {
    plus(period.datePeriodPortion())
        .resolve(resolver).plus(period.totalNanoseconds, DateTimeUnit.NANOSECOND)
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding CalendarPeriod to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding CalendarPeriod", e)
}

public fun UnresolvedZonedDateTime.minus(period: DateTimePeriod, resolver: TransitionResolver): Instant =
    /* An overflow can happen for any component, but we are only worried about nanoseconds, as having an overflow in
    any other component means that `plus` will throw due to the minimum value of the numeric type overflowing the
    `ZonedDateTime` limits. */
    if (period.totalNanoseconds != Long.MIN_VALUE) {
        val negatedPeriod = with(period) { buildDateTimePeriod(-totalMonths, -days, -totalNanoseconds) }
        plus(negatedPeriod, resolver)
    } else {
        val negatedPeriod = with(period) { buildDateTimePeriod(-totalMonths, -days, -(totalNanoseconds + 1)) }
        plus(negatedPeriod, resolver).plus(1L, DateTimeUnit.NANOSECOND)
    }


/// TIME-BASED DISTANCE FUNCTIONS

// public fun Instant.until(other: Instant, unit: DateTimeUnit.TimeBased): Long
// public fun Instant.minus(other: Instant, unit: DateTimeUnit.TimeBased): Long

/// DATE-BASED DISTANCE FUNCTIONS :(

// For date-based units:
// The 'ZonedDateTime' bound on the argument can not be relaxed into 'UnresolvedZonedDateTime' because of this case:
// this = 02:15 the day before DST change.
// other = 02:15 the day of DST change, and at 03:00 the clock is set back to 02:00.
// Depending on the resolver and on whether the 'other' is the moment before or after the DST change,
// the result of 'other.until(this, DateTimeUnit.DAY)' can be either 1 or 0 days.

public fun UnresolvedZonedDateTime.until(
    other: Instant,
    unit: DateTimeUnit.DateBased,
    resolver: TransitionResolver
): Long {
    val otherLdt = other.toLocalDateTime(timeZone)
    val timeAfterAddingDate = withDate { otherLdt.date }.resolve(resolver)
    val delta = when {
        otherLdt.date > rawLocalDateTime.date && timeAfterAddingDate > other -> -1 // addition won't throw: end date - date >= 1
        otherLdt.date < rawLocalDateTime.date && timeAfterAddingDate < other -> 1 // addition won't throw: date - end date >= 1
        else -> 0
    }
    return rawLocalDateTime.date.until(otherLdt.date.plus(delta, DateTimeUnit.DAY), unit)
}

public fun Instant.minus(
    other: UnresolvedZonedDateTime,
    unit: DateTimeUnit.DateBased,
    resolver: TransitionResolver
): Long = other.until(this, unit, resolver)

public fun UnresolvedZonedDateTime.daysUntil(other: Instant, resolver: TransitionResolver): Int =
    until(other, DateTimeUnit.DAY, resolver).clampToInt()

public fun UnresolvedZonedDateTime.monthsUntil(other: Instant, resolver: TransitionResolver): Int =
    until(other, DateTimeUnit.MONTH, resolver).clampToInt()

public fun UnresolvedZonedDateTime.yearsUntil(other: Instant, resolver: TransitionResolver): Int =
    until(other, DateTimeUnit.YEAR, resolver).clampToInt()

public fun UnresolvedZonedDateTime.periodUntil(other: Instant, resolver: TransitionResolver): DateTimePeriod {
    val otherLdt = other.toLocalDateTime(timeZone)
    val timeAfterAddingDate = withDate { otherLdt.date }.resolve(resolver)
    val delta = when {
        otherLdt.date > rawLocalDateTime.date && timeAfterAddingDate > other -> -1 // addition won't throw: end date - date >= 1
        otherLdt.date < rawLocalDateTime.date && timeAfterAddingDate < other -> 1 // addition won't throw: date - end date >= 1
        else -> 0
    }
    val endDate = otherLdt.date.plus(delta, DateTimeUnit.DAY) // `endDate` is guaranteed to be valid
    // won't throw: thisLdt + days <= otherLdt
    val nanoseconds =
        withDate { endDate }.resolve(resolver).until(other, DateTimeUnit.NANOSECOND) // |otherLdt - thisLdt| < 24h
    val datePeriod = endDate - rawLocalDateTime.date
    return buildDateTimePeriod(datePeriod.totalMonths, datePeriod.days, nanoseconds)
}

public fun Instant.minus(other: UnresolvedZonedDateTime, resolver: TransitionResolver): DateTimePeriod =
    other.periodUntil(this, resolver)


/// COMBINED DISTANCE FUNCTIONS

public fun Instant.until(other: Instant, unit: DateTimeUnit, resolver: TransitionResolver, timeZone: TimeZone): Long =
    when (unit) {
        is DateTimeUnit.DateBased -> unresolve(timeZone).until(other, unit, resolver)
        is DateTimeUnit.TimeBased -> until(other, unit)
    }

public fun Instant.minus(other: Instant, unit: DateTimeUnit, resolver: TransitionResolver, timeZone: TimeZone): Long =
    other.until(this, unit, resolver, timeZone)
