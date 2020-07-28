/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("InstantJvmKt")

package kotlinx.datetime

import java.time.DateTimeException
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.time.*
import java.time.Instant as jtInstant
import java.time.Clock as jtClock

@OptIn(kotlin.time.ExperimentalTime::class)
public actual class Instant internal constructor(internal val value: jtInstant) : Comparable<Instant> {

    actual val epochSeconds: Long
        get() = value.epochSecond
    actual val nanosecondsOfSecond: Int
        get() = value.nano

    public actual fun toEpochMilliseconds(): Long = try {
        value.toEpochMilli()
    } catch (e: ArithmeticException) {
        if (value.isAfter(java.time.Instant.EPOCH)) Long.MAX_VALUE else Long.MIN_VALUE
    }

    actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        try {
            Instant(value.plusSeconds(seconds).plusNanos(nanoseconds.toLong()))
        } catch (e: java.lang.Exception) {
            if (e !is ArithmeticException && e !is DateTimeException) throw e
            if (duration.isPositive()) MAX else MIN
        }
    }

    actual operator fun minus(duration: Duration): Instant = plus(-duration)

    actual operator fun minus(other: Instant): Duration =
            (this.value.epochSecond - other.value.epochSecond).seconds + // won't overflow given the instant bounds
            (this.value.nano - other.value.nano).nanoseconds

    public actual override operator fun compareTo(other: Instant): Int = this.value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is Instant && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    public actual companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        actual fun now(): Instant =
                Instant(jtClock.systemUTC().instant())

        actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant =
                Instant(jtInstant.ofEpochMilli(epochMilliseconds))

        actual fun parse(isoString: String): Instant = try {
            Instant(jtInstant.parse(isoString))
        } catch (e: DateTimeParseException) {
            throw DateTimeFormatException(e)
        }

        actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant = try {
            Instant(jtInstant.ofEpochSecond(epochSeconds, nanosecondAdjustment))
        } catch (e: Exception) {
            if (e !is ArithmeticException && e !is DateTimeException) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        actual val DISTANT_PAST: Instant = Instant(jtInstant.ofEpochSecond(DISTANT_PAST_SECONDS, 999_999_999))
        actual val DISTANT_FUTURE: Instant = Instant(jtInstant.ofEpochSecond(DISTANT_FUTURE_SECONDS, 0))

        internal actual val MIN: Instant = Instant(jtInstant.MIN)
        internal actual val MAX: Instant = Instant(jtInstant.MAX)
    }
}

private fun Instant.atZone(zone: TimeZone): java.time.ZonedDateTime = try {
    value.atZone(zone.zoneId)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

public actual fun Instant.plus(period: DateTimePeriod, zone: TimeZone): Instant {
    try {
        val thisZdt = atZone(zone)
        return with(period) {
            thisZdt
                    .run { if (years != 0 && months == 0) plusYears(years.toLong()) else this }
                    .run { if (months != 0) plusMonths(years * 12L + months.toLong()) else this }
                    .run { if (days != 0) plusDays(days.toLong()) else this }
                    .run { if (hours != 0) plusHours(hours.toLong()) else this }
                    .run { if (minutes != 0) plusMinutes(minutes.toLong()) else this }
                    .run { if (seconds != 0L) plusSeconds(seconds) else this }
                    .run { if (nanoseconds != 0L) plusNanos(nanoseconds) else this }
        }.toInstant().let(::Instant)
    } catch (e: DateTimeException) {
        throw DateTimeArithmeticException(e)
    }
}

internal actual fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant = try {
    val thisZdt = atZone(zone)
    when (unit) {
        CalendarUnit.YEAR -> thisZdt.plusYears(value).toInstant()
        CalendarUnit.MONTH -> thisZdt.plusMonths(value).toInstant()
        CalendarUnit.DAY -> thisZdt.plusDays(value).toInstant()
        CalendarUnit.HOUR -> thisZdt.plusHours(value).toInstant()
        CalendarUnit.MINUTE -> thisZdt.plusMinutes(value).toInstant()
        CalendarUnit.SECOND -> this.value.plusSeconds(value).also { it.atZone(zone.zoneId) }
        CalendarUnit.MILLISECOND -> this.value.plusMillis(value).also { it.atZone(zone.zoneId) }
        CalendarUnit.MICROSECOND -> this.value.plusSeconds(value / 1_000_000).plusNanos((value % 1_000_000) * 1000).also { it.atZone(zone.zoneId) }
        CalendarUnit.NANOSECOND -> this.value.plusNanos(value).also { it.atZone(zone.zoneId) }
    }.let(::Instant)
} catch (e: Throwable) {
    if (e !is DateTimeException && e !is ArithmeticException) throw e
    throw DateTimeArithmeticException("Instant $this cannot be represented as local date when adding $value $unit to it", e)
}

@OptIn(ExperimentalTime::class)
public actual fun Instant.periodUntil(other: Instant, zone: TimeZone): DateTimePeriod {
    var thisZdt = this.atZone(zone)
    val otherZdt = other.atZone(zone)

    val months = thisZdt.until(otherZdt, ChronoUnit.MONTHS); thisZdt = thisZdt.plusMonths(months)
    val days = thisZdt.until(otherZdt, ChronoUnit.DAYS); thisZdt = thisZdt.plusDays(days)
    val time = thisZdt.until(otherZdt, ChronoUnit.NANOS).nanoseconds

    time.toComponents { hours, minutes, seconds, nanoseconds ->
        return DateTimePeriod((months / 12).toInt(), (months % 12).toInt(), days.toInt(), hours, minutes, seconds.toLong(), nanoseconds.toLong())
    }
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, zone: TimeZone): Long =
        until(other, unit.calendarUnit.toChronoUnit(), zone.zoneId) / unit.calendarScale

private fun Instant.until(other: Instant, unit: ChronoUnit, zone: ZoneId): Long = try {
    this.value.atZone(zone).until(other.value.atZone(zone), unit)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
} catch (e: ArithmeticException) {
    if (this.value < other.value) Long.MAX_VALUE else Long.MIN_VALUE
}

private fun CalendarUnit.toChronoUnit(): ChronoUnit = when(this) {
    CalendarUnit.YEAR -> ChronoUnit.YEARS
    CalendarUnit.MONTH -> ChronoUnit.MONTHS
    CalendarUnit.DAY -> ChronoUnit.DAYS
    CalendarUnit.HOUR -> ChronoUnit.HOURS
    CalendarUnit.MINUTE -> ChronoUnit.MINUTES
    CalendarUnit.SECOND -> ChronoUnit.SECONDS
    CalendarUnit.MILLISECOND -> ChronoUnit.MILLIS
    CalendarUnit.MICROSECOND -> ChronoUnit.MICROS
    CalendarUnit.NANOSECOND -> ChronoUnit.NANOS
}
