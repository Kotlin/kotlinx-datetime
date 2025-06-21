/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmMultifileClass
@file:JvmName("InstantJvmKt")

package kotlinx.datetime

import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.internal.*
import java.time.DateTimeException
import java.time.temporal.*
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

private fun Instant.atZone(zone: TimeZone): java.time.ZonedDateTime = try {
    toJavaInstant().atZone(zone.zoneId)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

public actual fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant {
    try {
        val thisZdt = atZone(timeZone)
        return with(period) {
            thisZdt
                    .run { if (totalMonths != 0L) plusMonths(totalMonths) else this }
                    .run { if (days != 0) plusDays(days.toLong()) else this }
                    .run { if (totalNanoseconds != 0L) plusNanos(totalNanoseconds) else this }
        }.toInstant().toKotlinInstant()
    } catch (e: DateTimeException) {
        throw DateTimeArithmeticException(e)
    }
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit, timeZone)"))
public actual fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
        plus(1L, unit, timeZone)

public actual fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        plus(value.toLong(), unit, timeZone)

public actual fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        plus(-value.toLong(), unit, timeZone)

public actual fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        try {
            val thisZdt = atZone(timeZone)
            when (unit) {
                is DateTimeUnit.TimeBased ->
                    plus(value, unit).toJavaInstant().also { it.atZone(timeZone.zoneId) }
                is DateTimeUnit.DayBased ->
                    thisZdt.plusDays(safeMultiply(value, unit.days.toLong())).toInstant()
                is DateTimeUnit.MonthBased ->
                    thisZdt.plusMonths(safeMultiply(value, unit.months.toLong())).toInstant()
            }.toKotlinInstant()
        } catch (e: Exception) {
            if (e !is DateTimeException && e !is ArithmeticException) throw e
            throw DateTimeArithmeticException("Instant $this cannot be represented as local date when adding $value $unit to it", e)
        }

public actual fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    try {
        multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (d, r) ->
            this.plus(d.seconds).plus(r.nanoseconds)
        }
    } catch (e: Exception) {
        if (e !is DateTimeException && e !is ArithmeticException) throw e
        Instant.fromEpochSeconds(if (value > 0) Long.MAX_VALUE else Long.MIN_VALUE)
    }

public actual fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod {
    var thisZdt = this.atZone(timeZone)
    val otherZdt = other.atZone(timeZone)

    val months = thisZdt.until(otherZdt, ChronoUnit.MONTHS); thisZdt = thisZdt.plusMonths(months)
    val days = thisZdt.until(otherZdt, ChronoUnit.DAYS); thisZdt = thisZdt.plusDays(days)
    val nanoseconds = thisZdt.until(otherZdt, ChronoUnit.NANOS)

    return buildDateTimePeriod(months, days.toInt(), nanoseconds)
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long = try {
    val thisZdt = this.atZone(timeZone)
    val otherZdt = other.atZone(timeZone)
    when(unit) {
        is DateTimeUnit.TimeBased -> until(other, unit)
        is DateTimeUnit.DayBased -> thisZdt.until(otherZdt, ChronoUnit.DAYS) / unit.days
        is DateTimeUnit.MonthBased -> thisZdt.until(otherZdt, ChronoUnit.MONTHS) / unit.months
    }
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
} catch (e: ArithmeticException) {
    if (this < other) Long.MAX_VALUE else Long.MIN_VALUE
}
