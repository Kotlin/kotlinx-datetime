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
import kotlinx.datetime.internal.JSJoda.LocalTime
import kotlin.math.nextTowards
import kotlin.math.truncate

@OptIn(kotlin.time.ExperimentalTime::class)
public actual class Instant internal constructor(internal val value: jtInstant) : Comparable<Instant> {

    actual val epochSeconds: Long
        get() = value.epochSecond().toLong()
    actual val nanosecondsOfSecond: Int
        get() = value.nano().toInt()

    public actual fun toEpochMilliseconds(): Long = epochSeconds * 1000 + nanosecondsOfSecond / 1_000_000

    actual operator fun plus(duration: Duration): Instant {
        val addSeconds = truncate(duration.inSeconds)
        val addNanos = (duration.inNanoseconds % 1e9).toInt()
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

    override fun toString(): String = value.toString()

    public actual companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        actual fun now(): Instant =
                Instant(jtClock.systemUTC().instant())

        actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant = try {
            fromEpochSeconds(epochMilliseconds / 1000, epochMilliseconds % 1000 * 1000_000)
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
            Instant(jtInstant.ofEpochSecond(epochSeconds, nanosecondAdjustment))
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException()) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        internal actual val MIN: Instant = Instant(jtInstant.MIN)
        internal actual val MAX: Instant = Instant(jtInstant.MAX)
    }
}


public actual fun Instant.plus(period: DateTimePeriod, zone: TimeZone): Instant = try {
    val thisZdt = this.value.atZone(zone.zoneId)
    with(period) {
        thisZdt
                .run { if (years != 0 && months == 0) plusYears(years) else this }
                .run { if (months != 0) plusMonths(years * 12.0 + months) else this }
                .run { if (days != 0) plusDays(days) as ZonedDateTime else this }
                .run { if (hours != 0) plusHours(hours) else this }
                .run { if (minutes != 0) plusMinutes(minutes) else this }
                .run { plusSecondsFix(seconds) }
                .run { plusNanosFix(nanoseconds) }
    }.toInstant().let(::Instant)
}    catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}

// workaround for https://github.com/js-joda/js-joda/issues/431
private fun ZonedDateTime.plusSecondsFix(seconds: Long): ZonedDateTime {
    val value = seconds.toDouble()
    return when {
        value == 0.0 -> this
        (value.unsafeCast<Int>() or 0) != 0 -> plusSeconds(value)
        else -> {
            val valueLittleLess = value.nextTowards(0.0)
            plusSeconds(valueLittleLess).plusSeconds(value - valueLittleLess)
        }
    }
}

// workaround for https://github.com/js-joda/js-joda/issues/431
private fun ZonedDateTime.plusNanosFix(nanoseconds: Long): ZonedDateTime {
    val value = nanoseconds.toDouble()
    return when {
        value == 0.0 -> this
        (value.unsafeCast<Int>() or 0) != 0 -> plusNanos(value)
        else -> {
            val valueLittleLess = value.nextTowards(0.0)
            plusNanos(valueLittleLess).plusNanos(value - valueLittleLess)
        }
    }
}

private fun jtInstant.atZone(zone: TimeZone): ZonedDateTime = atZone(zone.zoneId)

internal actual fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant = try {
    val thisZdt = this.value.atZone(zone)
    when (unit) {
        CalendarUnit.YEAR -> thisZdt.plusYears(value).toInstant()
        CalendarUnit.MONTH -> thisZdt.plusMonths(value).toInstant()
        CalendarUnit.DAY -> thisZdt.plusDays(value).let { it as ZonedDateTime }.toInstant()
        CalendarUnit.HOUR -> thisZdt.plusHours(value).toInstant()
        CalendarUnit.MINUTE -> thisZdt.plusMinutes(value).toInstant()
        CalendarUnit.SECOND -> this.plusFix(value.toDouble(), 0)
        CalendarUnit.MILLISECOND -> this.plusFix((value / 1_000).toDouble(), (value % 1_000).toInt() * 1_000_000).also { it.atZone(zone) }
        CalendarUnit.MICROSECOND -> this.plusFix((value / 1_000_000).toDouble(), (value % 1_000_000).toInt() * 1000).also { it.atZone(zone) }
        CalendarUnit.NANOSECOND -> this.plusFix((value / 1_000_000_000).toDouble(), (value % 1_000_000_000).toInt()).also { it.atZone(zone) }
    }.let(::Instant)
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}

@OptIn(ExperimentalTime::class)
public actual fun Instant.periodUntil(other: Instant, zone: TimeZone): DateTimePeriod = try {
    var thisZdt = this.value.atZone(zone.zoneId)
    val otherZdt = other.value.atZone(zone.zoneId)

    val months = thisZdt.until(otherZdt, ChronoUnit.MONTHS).toDouble(); thisZdt = thisZdt.plusMonths(months)
    val days = thisZdt.until(otherZdt, ChronoUnit.DAYS).toDouble(); thisZdt = thisZdt.plusDays(days) as ZonedDateTime
    val time = thisZdt.until(otherZdt, ChronoUnit.NANOS).toDouble().nanoseconds

    time.toComponents { hours, minutes, seconds, nanoseconds ->
        return DateTimePeriod((months / 12).toInt(), (months % 12).toInt(), days.toInt(), hours, minutes, seconds.toLong(), nanoseconds.toLong())
    }
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e) else throw e
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, zone: TimeZone): Long = try {
    when (unit) {
        is DateTimeUnit.DateBased ->
            this.value.atZone(zone).until(other.value.atZone(zone), unit.calendarUnit.toChronoUnit()).toLong() / unit.calendarScale
        is DateTimeUnit.TimeBased -> {
            this.value.atZone(zone)
            other.value.atZone(zone)
            try {
                // TODO: use fused multiplyAddDivide
                safeAdd(
                        safeMultiply(other.epochSeconds - this.epochSeconds, LocalTime.NANOS_PER_SECOND.toLong()),
                        (other.nanosecondsOfSecond - this.nanosecondsOfSecond).toLong()
                ) / unit.nanoseconds
            } catch (e: ArithmeticException) {
                if (this < other) Long.MAX_VALUE else Long.MIN_VALUE
            }
        }
    }
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e) else throw e
}


private fun CalendarUnit.toChronoUnit(): ChronoUnit = when(this) {
    CalendarUnit.YEAR -> ChronoUnit.YEARS
    CalendarUnit.MONTH -> ChronoUnit.MONTHS
    CalendarUnit.DAY -> ChronoUnit.DAYS
    else -> error("CalendarUnit $this should not be used")
}
