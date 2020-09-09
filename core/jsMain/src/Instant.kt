/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.JSJoda.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.nanoseconds
import kotlin.time.seconds
import kotlinx.datetime.internal.JSJoda.Instant as jtInstant
import kotlinx.datetime.internal.JSJoda.Duration as jtDuration
import kotlinx.datetime.internal.JSJoda.Clock as jtClock
import kotlinx.datetime.internal.JSJoda.ChronoUnit
import kotlin.math.truncate

@OptIn(ExperimentalTime::class)
public actual class Instant internal constructor(internal val value: jtInstant) : Comparable<Instant> {

    actual val epochSeconds: Long
        get() = value.epochSecond().toLong()
    actual val nanosecondsOfSecond: Int
        get() = value.nano().toInt()

    public actual fun toEpochMilliseconds(): Long =
            epochSeconds * MILLIS_PER_ONE + nanosecondsOfSecond / NANOS_PER_MILLI

    actual operator fun plus(duration: Duration): Instant {
        val addSeconds = truncate(duration.inSeconds)
        val addNanos = (duration.inNanoseconds % NANOS_PER_ONE).toInt()
        return try {
            Instant(plusFix(addSeconds, addNanos))
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException()) throw e
            if (addSeconds > 0) MAX else MIN
        }
    }

    internal fun plusFix(seconds: Double, nanos: Int): jtInstant {
        val newSeconds = value.epochSecond().toDouble() + seconds
        val newNanos = value.nano().toDouble() + nanos
        return jtInstant.ofEpochSecond(newSeconds, newNanos)
    }

    actual operator fun minus(duration: Duration): Instant = plus(-duration)

    actual operator fun minus(other: Instant): Duration {
        val diff = jtDuration.between(other.value, this.value)
        return diff.seconds().toDouble().seconds + diff.nano().toDouble().nanoseconds
    }

    public actual override operator fun compareTo(other: Instant): Int = this.value.compareTo(other.value).toInt()

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is Instant && this.value == other.value)

    override fun hashCode(): Int = value.hashCode().toInt()

    actual override fun toString(): String = value.toString()

    public actual companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        actual fun now(): Instant =
                Instant(jtClock.systemUTC().instant())

        actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant = try {
            fromEpochSeconds(epochMilliseconds / MILLIS_PER_ONE, epochMilliseconds % MILLIS_PER_ONE * NANOS_PER_MILLI)
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException()) throw e
            if (epochMilliseconds > 0) MAX else MIN
        }

        actual fun parse(isoString: String): Instant = try {
            Instant(jtInstant.parse(isoString))
        } catch (e: Throwable) {
            if (e.isJodaDateTimeParseException()) throw DateTimeFormatException(e)
            throw e
        }

        actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant = try {
            /* Performing normalization here because otherwise this fails:
               assertEquals((Long.MAX_VALUE % 1_000_000_000).toInt(),
                            Instant.fromEpochSeconds(0, Long.MAX_VALUE).nanosecondsOfSecond) */
            val secs = safeAdd(epochSeconds, floorDiv(nanosecondAdjustment, NANOS_PER_ONE.toLong()))
            val nos = floorMod(nanosecondAdjustment, NANOS_PER_ONE.toLong()).toInt()
            Instant(jtInstant.ofEpochSecond(secs, nos))
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException() && e !is ArithmeticException) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant = try {
            Instant(jtInstant.ofEpochSecond(epochSeconds, nanosecondAdjustment))
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException()) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        actual val DISTANT_PAST: Instant = Instant(jtInstant.ofEpochSecond(DISTANT_PAST_SECONDS, 999_999_999))
        actual val DISTANT_FUTURE: Instant = Instant(jtInstant.ofEpochSecond(DISTANT_FUTURE_SECONDS, 0))

        internal actual val MIN: Instant = Instant(jtInstant.MIN)
        internal actual val MAX: Instant = Instant(jtInstant.MAX)
    }
}


public actual fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant = try {
    val thisZdt = this.value.atZone(timeZone.zoneId)
    with(period) {
        thisZdt
                .run { if (years != 0 && months == 0) plusYears(years) else this }
                .run { if (months != 0) plusMonths(years * 12.0 + months) else this }
                .run { if (days != 0) plusDays(days) as ZonedDateTime else this }
                .run { if (hours != 0) plusHours(hours) else this }
                .run { if (minutes != 0) plusMinutes(minutes) else this }
                .run { if (seconds != 0L) plusSeconds(seconds.toDouble()) else this }
                .run { if (nanoseconds != 0L) plusNanos(nanoseconds.toDouble()) else this }
    }.toInstant().let(::Instant)
}    catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}

private fun Instant.atZone(zone: TimeZone): ZonedDateTime = value.atZone(zone.zoneId)
private fun jtInstant.checkZone(zone: TimeZone): jtInstant = apply { atZone(zone.zoneId) }

public actual fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
        plus(1, unit, timeZone)

public actual fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        try {
            val thisZdt = this.atZone(timeZone)
            when (unit) {
                is DateTimeUnit.TimeBased -> {
                    plus(value, unit).value.checkZone(timeZone)
                }
                is DateTimeUnit.DateBased.DayBased ->
                    (thisZdt.plusDays(value.toDouble() * unit.days) as ZonedDateTime).toInstant()
                is DateTimeUnit.DateBased.MonthBased ->
                    thisZdt.plusMonths(value.toDouble() * unit.months).toInstant()
            }.let(::Instant)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
            throw e
        }

public actual fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        try {
            val thisZdt = this.atZone(timeZone)
            when (unit) {
                is DateTimeUnit.TimeBased ->
                    plus(value.toLong(), unit).value.checkZone(timeZone)
                is DateTimeUnit.DateBased.DayBased ->
                    (thisZdt.plusDays(value.toDouble() * unit.days) as ZonedDateTime).toInstant()
                is DateTimeUnit.DateBased.MonthBased ->
                    thisZdt.plusMonths(value.toDouble() * unit.months).toInstant()
            }.let(::Instant)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
            throw e
        }

public actual fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    if (value == Int.MIN_VALUE)
        plus(-value.toLong(), unit, timeZone)
    else
        plus(-value, unit, timeZone)

actual fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    try {
        multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (d, r) ->
            Instant(plusFix(d.toDouble(), r.toInt()))
        }
    } catch (e: Throwable) {
        if (!e.isJodaDateTimeException()) {
            throw e
        }
        if (value > 0) Instant.MAX else Instant.MIN
    }

@OptIn(ExperimentalTime::class)
public actual fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod = try {
    var thisZdt = this.value.atZone(timeZone.zoneId)
    val otherZdt = other.value.atZone(timeZone.zoneId)

    val months = thisZdt.until(otherZdt, ChronoUnit.MONTHS).toDouble(); thisZdt = thisZdt.plusMonths(months)
    val days = thisZdt.until(otherZdt, ChronoUnit.DAYS).toDouble(); thisZdt = thisZdt.plusDays(days) as ZonedDateTime
    val time = thisZdt.until(otherZdt, ChronoUnit.NANOS).toDouble().nanoseconds

    time.toComponents { hours, minutes, seconds, nanoseconds ->
        return DateTimePeriod((months / 12).toInt(), (months % 12).toInt(), days.toInt(), hours, minutes, seconds.toLong(), nanoseconds.toLong())
    }
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e) else throw e
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long = try {
    val thisZdt = this.atZone(timeZone)
    val otherZdt = other.atZone(timeZone)
    when(unit) {
        is DateTimeUnit.TimeBased -> until(other, unit)
        is DateTimeUnit.DateBased.DayBased -> (thisZdt.until(otherZdt, ChronoUnit.DAYS).toDouble() / unit.days).toLong()
        is DateTimeUnit.DateBased.MonthBased -> (thisZdt.until(otherZdt, ChronoUnit.MONTHS).toDouble() / unit.months).toLong()
    }
} catch (e: ArithmeticException) {
    if (this < other) Long.MAX_VALUE else Long.MIN_VALUE
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e) else throw e
}
