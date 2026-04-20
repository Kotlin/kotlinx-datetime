/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("InstantKt")
package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlin.time.*
import kotlin.time.Instant
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A shortcut for calling [DateTimeFormat.parse], followed by [DateTimeComponents.toInstantUsingOffset].
 *
 * Parses a string that represents an instant, including date and time components and a mandatory
 * time zone offset and returns the parsed [Instant] value.
 *
 * The string is considered to represent time on the UTC-SLS time scale instead of UTC.
 * In practice, this means that, even if there is a leap second on the given day, it will not affect how the
 * time is parsed, even if it's in the last 1000 seconds of the day.
 * Instead, even if there is a negative leap second on the given day, 23:59:59 is still considered a valid time.
 * 23:59:60 is invalid on UTC-SLS, so parsing it will fail.
 *
 * [Instant.parse] is equivalent to calling this function with the
 * [DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET] format.
 * `2023-01-02T23:40:57.120Z` is an example of a string in this format.
 *
 * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [Instant] are exceeded.
 *
 * @see Instant.toString for formatting using the default format.
 * @see Instant.format for formatting using a custom format.
 * @see Instant.parse for parsing an ISO string without involving `kotlinx-datetime`.
 * @sample kotlinx.datetime.test.samples.InstantSamples.parsing
 */
public fun Instant.Companion.parse(
    input: CharSequence,
    format: DateTimeFormat<DateTimeComponents>,
): Instant = try {
    format.parse(input).toInstantUsingOffset()
} catch (e: IllegalArgumentException) {
    throw DateTimeFormatException("Failed to parse an instant from '$input'", e)
}

/**
 * Returns an instant that is the result of adding components of [DateTimePeriod] to this instant. The components are
 * added in the order from the largest units to the smallest, i.e., from years to nanoseconds.
 *
 * - If the [DateTimePeriod] only contains time-based components, please consider adding a [Duration] instead,
 *   as in `Clock.System.now() + 5.hours`.
 *   Then, it will not be necessary to pass the [timeZone].
 * - If the [DateTimePeriod] only has a single non-zero component (only the months or only the days),
 *   please consider using a multiple of [DateTimeUnit.DAY] or [DateTimeUnit.MONTH], like in
 *   `Clock.System.now().plus(5, DateTimeUnit.DAY, TimeZone.currentSystemDefault())`.
 *
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusPeriod
 */
public fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant = try {
    with(period) {
        val initialOffset = offsetIn(timeZone)
        val ldtPlusDate = toLocalDateTimeFailing(initialOffset)
            .run { if (totalMonths != 0L) { plus(totalMonths, DateTimeUnit.MONTH) } else { this } }
            .run { if (days != 0) { this.plus(days, DateTimeUnit.DAY) } else { this } }
        localDateTimeToInstant(ldtPlusDate, timeZone, preferred = initialOffset)
            .run { if (totalNanoseconds != 0L) plus(totalNanoseconds.nanoseconds).check(timeZone) else this }
    }.check(timeZone)
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding CalendarPeriod to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding CalendarPeriod", e)
}

/**
 * Returns an instant that is the result of subtracting components of [DateTimePeriod] from this instant. The components
 * are subtracted in the order from the largest units to the smallest, i.e., from years to nanoseconds.
 *
 * - If the [DateTimePeriod] only contains time-based components, please consider subtracting a [Duration] instead,
 *   as in `Clock.System.now() - 5.hours`.
 *   Then, it is not necessary to pass the [timeZone].
 * - If the [DateTimePeriod] only has a single non-zero component (only the months or only the days),
 *   please consider using a multiple of [DateTimeUnit.DAY] or [DateTimeUnit.MONTH], as in
 *   `Clock.System.now().minus(5, DateTimeUnit.DAY, TimeZone.currentSystemDefault())`.
 *
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusPeriod
 */
public fun Instant.minus(period: DateTimePeriod, timeZone: TimeZone): Instant =
    /* An overflow can happen for any component, but we are only worried about nanoseconds, as having an overflow in
    any other component means that `plus` will throw due to the minimum value of the numeric type overflowing the
    `Instant` limits. */
    if (period.totalNanoseconds != Long.MIN_VALUE) {
        val negatedPeriod = with(period) { buildDateTimePeriod(-totalMonths, -days, -totalNanoseconds) }
        plus(negatedPeriod, timeZone)
    } else {
        val negatedPeriod = with(period) { buildDateTimePeriod(-totalMonths, -days, -(totalNanoseconds+1)) }
        plus(negatedPeriod, timeZone).plus(1, DateTimeUnit.NANOSECOND)
    }

/**
 * Returns a [DateTimePeriod] representing the difference between `this` and [other] instants.
 *
 * The components of [DateTimePeriod] are calculated so that adding it to `this` instant results in the [other] instant.
 *
 * All components of the [DateTimePeriod] returned are:
 * - Positive or zero if this instant is earlier than the other.
 * - Negative or zero if this instant is later than the other.
 * - Exactly zero if this instant is equal to the other.
 *
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.periodUntil
 */
public fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod {
    val initialOffset = offsetIn(timeZone)
    val initialLdt = toLocalDateTimeFailing(initialOffset)
    val otherLdt = other.toLocalDateTimeFailing(timeZone)
    val timeAfterAddingDate =
        localDateTimeToInstant(otherLdt.date.atTime(initialLdt.time), timeZone, preferred = initialOffset)
    val delta = when {
        other > this && timeAfterAddingDate > other -> -1 // addition won't throw: end date - date >= 1
        other < this && timeAfterAddingDate < other -> 1 // addition won't throw: date - end date >= 1
        else -> 0
    }
    val endDate = otherLdt.date.plus(delta, DateTimeUnit.DAY) // `endDate` is guaranteed to be valid
    val unresolvedLdtWithDays = endDate.atTime(initialLdt.time)
    val newInstant = localDateTimeToInstant(unresolvedLdtWithDays, timeZone, preferred = initialOffset)
        // won't throw: thisLdt + days <= otherLdt
    val nanoseconds = newInstant.until(other, DateTimeUnit.NANOSECOND) // |otherLdt - thisLdt| < 24h
    val datePeriod = endDate - initialLdt.date
    return buildDateTimePeriod(datePeriod.totalMonths, datePeriod.days, nanoseconds)
}

/**
 * Returns the whole number of the specified date or time [units][unit] between `this` and [other] instants
 * in the specified [timeZone].
 *
 * The value returned is:
 * - Positive or zero if this instant is earlier than the other.
 * - Negative or zero if this instant is later than the other.
 * - Zero if this instant is equal to the other.
 *
 * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
 *
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.untilAsDateTimeUnit
 */
public fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long =
    when (unit) {
        is DateTimeUnit.DateBased -> {
            val start = toLocalDateTimeFailing(timeZone)
            val end = other.toLocalDateTimeFailing(timeZone)
            val timeAfterAddingDate =
                localDateTimeToInstant(end.date.atTime(start.time), timeZone, preferred = this.offsetIn(timeZone))
            val delta = when {
                other > this && timeAfterAddingDate > other -> -1 // addition won't throw: end date - date >= 1
                other < this && timeAfterAddingDate < other -> 1 // addition won't throw: date - end date >= 1
                else -> 0
            }
            start.date.until(end.date.plus(delta, DateTimeUnit.DAY), unit)
        }
        is DateTimeUnit.TimeBased -> {
            check(timeZone); other.check(timeZone)
            until(other, unit)
        }
    }

/**
 * Returns the whole number of the specified time [units][unit] between `this` and [other] instants.
 *
 * The value returned is:
 * - Positive or zero if this instant is earlier than the other.
 * - Negative or zero if this instant is later than the other.
 * - Zero if this instant is equal to the other.
 *
 * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.untilAsTimeBasedUnit
 */
public fun Instant.until(other: Instant, unit: DateTimeUnit.TimeBased): Long =
    try {
        multiplyAddAndDivide(other.epochSeconds - epochSeconds,
            NANOS_PER_ONE.toLong(),
            (other.nanosecondsOfSecond - nanosecondsOfSecond).toLong(),
            unit.nanoseconds)
    } catch (_: ArithmeticException) {
        if (this < other) Long.MAX_VALUE else Long.MIN_VALUE
    }

/**
 * Returns the number of whole days between two instants in the specified [timeZone].
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see Instant.until
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.daysUntil
 */
public fun Instant.daysUntil(other: Instant, timeZone: TimeZone): Int =
        until(other, DateTimeUnit.DAY, timeZone).clampToInt()

/**
 * Returns the number of whole months between two instants in the specified [timeZone].
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see Instant.until
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.monthsUntil
 */
public fun Instant.monthsUntil(other: Instant, timeZone: TimeZone): Int =
        until(other, DateTimeUnit.MONTH, timeZone).clampToInt()

/**
 * Returns the number of whole years between two instants in the specified [timeZone].
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see Instant.until
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.yearsUntil
 */
public fun Instant.yearsUntil(other: Instant, timeZone: TimeZone): Int =
        until(other, DateTimeUnit.YEAR, timeZone).clampToInt()

/**
 * Returns a [DateTimePeriod] representing the difference between [other] and `this` instants.
 *
 * The components of [DateTimePeriod] are calculated so that adding it back to the `other` instant results in this instant.
 *
 * All components of the [DateTimePeriod] returned are:
 * - Negative or zero if this instant is earlier than the other.
 * - Positive or zero if this instant is later than the other.
 * - Exactly zero if this instant is equal to the other.
 *
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @see Instant.periodUntil
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusInstantInZone
 */
public fun Instant.minus(other: Instant, timeZone: TimeZone): DateTimePeriod =
        other.periodUntil(this, timeZone)


/**
 * Returns an instant that is the result of adding one [unit] to this instant
 * in the specified [timeZone].
 *
 * The returned instant is later than this instant.
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 */
@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit, timeZone)"))
public fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(1L, unit, timeZone)

/**
 * Returns an instant that is the result of subtracting one [unit] from this instant
 * in the specified [timeZone].
 *
 * The returned instant is earlier than this instant.
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 */
@Deprecated("Use the minus overload with an explicit number of units", ReplaceWith("this.minus(1, unit, timeZone)"))
public fun Instant.minus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(-1, unit, timeZone)

/**
 * Returns an instant that is the result of adding one [unit] to this instant.
 *
 * The returned instant is later than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 */
@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public fun Instant.plus(unit: DateTimeUnit.TimeBased): Instant =
    plus(1L, unit)

/**
 * Returns an instant that is the result of subtracting one [unit] from this instant.
 *
 * The returned instant is earlier than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 */
@Deprecated("Use the minus overload with an explicit number of units", ReplaceWith("this.minus(1, unit)"))
public fun Instant.minus(unit: DateTimeUnit.TimeBased): Instant =
    plus(-1L, unit)

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant
 * in the specified [timeZone].
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * Note that the time zone does not need to be passed when the [unit] is a time-based unit.
 * It is also not needed when adding date-based units to a [LocalDate][LocalDate.plus].
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusDateTimeUnit
 */
public fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(value.toLong(), unit, timeZone)

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant
 * in the specified [timeZone].
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * Note that the time zone does not need to be passed when the [unit] is a time-based unit.
 * It is also not needed when subtracting date-based units from a [LocalDate].
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusDateTimeUnit
 */
public fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(-value.toLong(), unit, timeZone)

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant.
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusTimeBasedUnit
 */
public fun Instant.plus(value: Int, unit: DateTimeUnit.TimeBased): Instant =
    plus(value.toLong(), unit)

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant.
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusTimeBasedUnit
 */
public fun Instant.minus(value: Int, unit: DateTimeUnit.TimeBased): Instant =
    minus(value.toLong(), unit)

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant
 * in the specified [timeZone].
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * Note that the time zone does not need to be passed when the [unit] is a time-based unit.
 * It is also not needed when adding date-based units to a [LocalDate].
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusDateTimeUnit
 */
public fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant = try {
    when (unit) {
        is DateTimeUnit.DateBased -> {
            val initialOffset = offsetIn(timeZone)
            val initialLdt = toLocalDateTimeFailing(initialOffset)
            localDateTimeToInstant(initialLdt.plus(value, unit), timeZone, preferred = initialOffset)
        }
        is DateTimeUnit.TimeBased ->
            check(timeZone).plus(value, unit).check(timeZone)
    }
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding a value", e)
}

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant
 * in the specified [timeZone].
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * Note that the time zone does not need to be passed when the [unit] is a time-based unit.
 * It is also not needed when subtracting date-based units from a [LocalDate].
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusDateTimeUnit
 */
public fun Instant.minus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    if (value != Long.MIN_VALUE) {
        plus(-value, unit, timeZone)
    } else {
        plus(-(value + 1), unit, timeZone).plus(1, unit, timeZone)
    }

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant.
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusTimeBasedUnit
 */
public fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    try {
        multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (seconds, nanoseconds) ->
            plus(seconds.seconds).plus(nanoseconds.nanoseconds)
        }
    } catch (_: ArithmeticException) {
        Instant.fromEpochSeconds(if (value > 0) Long.MAX_VALUE else Long.MIN_VALUE)
    } catch (_: IllegalArgumentException) {
        Instant.fromEpochSeconds(if (value > 0) Long.MAX_VALUE else Long.MIN_VALUE)
    }


/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant.
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusTimeBasedUnit
 */
public fun Instant.minus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    if (value != Long.MIN_VALUE) {
        plus(-value, unit)
    } else {
        plus(-(value + 1), unit).plus(1, unit)
    }

/**
 * Returns the whole number of the specified date or time [units][unit] between [other] and `this` instants
 * in the specified [timeZone].
 *
 * The value returned is negative or zero if this instant is earlier than the other,
 * and positive or zero if this instant is later than the other.
 *
 * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
 *
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @see Instant.until for the same operation but with swapped arguments.
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusAsDateTimeUnit
 */
public fun Instant.minus(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long =
        other.until(this, unit, timeZone)

/**
 * Returns the whole number of the specified time [units][unit] between [other] and `this` instants.
 *
 * The value returned is negative or zero if this instant is earlier than the other,
 * and positive or zero if this instant is later than the other.
 *
 * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
 *
 * @see Instant.until for the same operation but with swapped arguments.
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusAsTimeBasedUnit
 */
public fun Instant.minus(other: Instant, unit: DateTimeUnit.TimeBased): Long =
    other.until(this, unit)

/**
 * Formats this value using the given [format] using the given [offset].
 *
 * Equivalent to calling [DateTimeFormat.format] on [format] and using [DateTimeComponents.setDateTimeOffset] in
 * the lambda.
 *
 * [DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET] is a format very similar to the one used by [toString].
 * The only difference is that [Instant.toString] adds trailing zeros to the fraction-of-second component so that the
 * number of digits after a dot is a multiple of three.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.formatting
 */
public fun Instant.format(format: DateTimeFormat<DateTimeComponents>, offset: UtcOffset = UtcOffset.ZERO): String {
    val instant = this
    return format.format { setDateTimeOffset(instant, offset) }
}

/**
 * A shortcut for calling [DateTimeFormat.parseOrNull], followed by [DateTimeComponents.toInstantUsingOffset],
 * returning `null` if any of the two operations fail.
 *
 * Parses a string that represents an instant, including date and time components and a mandatory
 * time zone offset and returns the parsed [Instant] value.
 *
 * The string is considered to represent time on the UTC-SLS time scale instead of UTC.
 * In practice, this means that, even if there is a leap second on the given day, it will not affect how the
 * time is parsed, even if it's in the last 1000 seconds of the day.
 * Instead, even if there is a negative leap second on the given day, 23:59:59 is still considered a valid time.
 * 23:59:60 is invalid on UTC-SLS, so parsing it will fail.
 *
 * If the format is not specified, [DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET] is used.
 * `2023-01-02T23:40:57.120Z` is an example of a string in this format.
 *
 * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [Instant] are exceeded.
 *
 * @see Instant.toString for formatting using the default format.
 * @see Instant.format for formatting using a custom format.
 * @see Instant.parse for a variant that throws an exception on failure.
 * @sample kotlinx.datetime.test.samples.InstantSamples.parseOrNull
 */
public fun Instant.Companion.parseOrNull(
    input: CharSequence,
    format: DateTimeFormat<DateTimeComponents> = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
): Instant? = try {
    format.parseOrNull(input)?.toInstantUsingOffset()
} catch (e: IllegalArgumentException) {
    null
}

internal const val DISTANT_PAST_SECONDS = -3217862419201
internal const val DISTANT_FUTURE_SECONDS = 3093527980800

private fun Instant.toLocalDateTimeFailing(offset: UtcOffset): LocalDateTime = try {
    toLocalDateTime(offset)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Can not convert instant $this to LocalDateTime to perform computations", e)
}

private fun Instant.toLocalDateTimeFailing(timeZone: TimeZone): LocalDateTime =
    toLocalDateTimeFailing(offsetIn(timeZone))

/** Check that [Instant] fits in [LocalDateTime].
 * This is done on the results of computations for consistency with other platforms.
 */
private fun Instant.check(zone: TimeZone): Instant = this@check.also {
    toLocalDateTimeFailing(zone)
}

private fun LocalDateTime.plus(value: Long, unit: DateTimeUnit.DateBased) =
    date.plus(value, unit).atTime(time)

private fun LocalDateTime.plus(value: Int, unit: DateTimeUnit.DateBased) =
    date.plus(value, unit).atTime(time)
