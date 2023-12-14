/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlin.time.Instant
import kotlinx.datetime.internal.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private fun Instant.toLocalDateTimeFailing(offset: UtcOffset): LocalDateTime = try {
    toLocalDateTimeImpl(offset)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Can not convert instant $this to LocalDateTime to perform computations", e)
}

/** Check that [Instant] fits in [LocalDateTime].
 * This is done on the results of computations for consistency with other platforms.
 */
private fun Instant.check(zone: TimeZone): Instant = this@check.also {
    toLocalDateTimeFailing(offsetIn(zone))
}

public actual fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant = try {
    with(period) {
        val initialOffset = offsetIn(timeZone)
        val initialLdt = toLocalDateTimeFailing(initialOffset)
        val offsetAfterMonths: UtcOffset
        val ldtAfterMonths: LocalDateTime
        if (totalMonths != 0L) {
            val (ldt, offset) = timeZone.atZone(initialLdt.plus(totalMonths, DateTimeUnit.MONTH), initialOffset)
            offsetAfterMonths = offset
            ldtAfterMonths = ldt
        } else {
            offsetAfterMonths = initialOffset
            ldtAfterMonths = initialLdt
        }
        val instantAfterMonthsAndDays = if (days != 0) {
            timeZone.atZone(ldtAfterMonths.plus(days, DateTimeUnit.DAY), offsetAfterMonths).toInstant()
        } else {
            ldtAfterMonths.toInstant(offsetAfterMonths)
        }
        instantAfterMonthsAndDays
            .run { if (totalNanoseconds != 0L) plus(totalNanoseconds.nanoseconds).check(timeZone) else this }
    }.check(timeZone)
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding CalendarPeriod to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding CalendarPeriod", e)
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit, timeZone)"))
public actual fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(1L, unit, timeZone)
public actual fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(value.toLong(), unit, timeZone)
public actual fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(-value.toLong(), unit, timeZone)
public actual fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant = try {
    when (unit) {
        is DateTimeUnit.DateBased -> {
            val preferredOffset = offsetIn(timeZone)
            val initialLdt = toLocalDateTimeFailing(preferredOffset)
            timeZone.atZone(initialLdt.plus(value, unit), preferredOffset).toInstant()
        }
        is DateTimeUnit.TimeBased ->
            check(timeZone).plus(value, unit).check(timeZone)
    }
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding a value", e)
}

public actual fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    try {
        multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (seconds, nanoseconds) ->
            plus(seconds.seconds).plus(nanoseconds.nanoseconds)
        }
    } catch (_: ArithmeticException) {
        Instant.fromEpochSeconds(if (value > 0) Long.MAX_VALUE else Long.MIN_VALUE)
    } catch (_: IllegalArgumentException) {
        Instant.fromEpochSeconds(if (value > 0) Long.MAX_VALUE else Long.MIN_VALUE)
    }

public actual fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod {
    val thisOffset1 = offsetIn(timeZone)
    val thisLdt1 = toLocalDateTimeFailing(thisOffset1)
    val otherLdt = other.toLocalDateTimeFailing(other.offsetIn(timeZone))

    val months = thisLdt1.until(otherLdt, DateTimeUnit.MONTH) // `until` on dates never fails
    val (thisLdt2, thisOffset2) = timeZone.atZone(thisLdt1.plus(months, DateTimeUnit.MONTH), thisOffset1) // won't throw: thisLdt + months <= otherLdt, which is known to be valid
    val days = thisLdt2.until(otherLdt, DateTimeUnit.DAY) // `until` on dates never fails
    val (thisLdt3, thisOffset3) = timeZone.atZone(thisLdt2.plus(days, DateTimeUnit.DAY), thisOffset2) // won't throw: thisLdt + days <= otherLdt
    val nanoseconds = thisLdt3.toInstant(thisOffset3).until(other, DateTimeUnit.NANOSECOND) // |otherLdt - thisLdt| < 24h

    return buildDateTimePeriod(months, days.toInt(), nanoseconds)
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long =
    when (unit) {
        is DateTimeUnit.DateBased ->
            toLocalDateTimeFailing(offsetIn(timeZone)).until(other.toLocalDateTimeFailing(other.offsetIn(timeZone)), unit)
                .toLong()
        is DateTimeUnit.TimeBased -> {
            check(timeZone); other.check(timeZone)
            until(other, unit)
        }
    }

private fun LocalDateTime.plus(value: Long, unit: DateTimeUnit.DateBased) =
    date.plus(value, unit).atTime(time)

private fun LocalDateTime.plus(value: Int, unit: DateTimeUnit.DateBased) =
    date.plus(value, unit).atTime(time)
