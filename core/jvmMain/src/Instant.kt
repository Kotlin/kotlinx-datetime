/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("InstantJvmKt")

package kotlinx.datetime

import java.time.ZoneId
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

    public actual fun toEpochMilliseconds(): Long = value.toEpochMilli()

    actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        Instant(value.plusSeconds(seconds).plusNanos(nanoseconds.toLong()))
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

        actual fun parse(isoString: String): Instant =
                Instant(jtInstant.parse(isoString))

        actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant =
                Instant(jtInstant.ofEpochSecond(epochSeconds, nanosecondAdjustment))
    }
}

public actual fun Instant.plus(period: DateTimePeriod, zone: TimeZone): Instant {
    val thisZdt = this.value.atZone(zone.zoneId)
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
}

internal actual fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant =
        when (unit) {
            CalendarUnit.YEAR -> this.value.atZone(zone.zoneId).plusYears(value).toInstant()
            CalendarUnit.MONTH -> this.value.atZone(zone.zoneId).plusMonths(value).toInstant()
            CalendarUnit.DAY -> this.value.atZone(zone.zoneId).plusDays(value).toInstant()
            CalendarUnit.HOUR -> this.value.atZone(zone.zoneId).plusHours(value).toInstant()
            CalendarUnit.MINUTE -> this.value.atZone(zone.zoneId).plusMinutes(value).toInstant()
            CalendarUnit.SECOND -> this.value.plusSeconds(value)
            CalendarUnit.MILLISECOND -> this.value.plusMillis(value)
            CalendarUnit.MICROSECOND -> this.value.plusSeconds(value / 1_000_000).plusNanos((value % 1_000_000) * 1000)
            CalendarUnit.NANOSECOND -> this.value.plusNanos(value)
        }.let(::Instant)

@OptIn(ExperimentalTime::class)
public actual fun Instant.periodUntil(other: Instant, zone: TimeZone): DateTimePeriod {
    var thisZdt = this.value.atZone(zone.zoneId)
    val otherZdt = other.value.atZone(zone.zoneId)

    val months = thisZdt.until(otherZdt, ChronoUnit.MONTHS); thisZdt = thisZdt.plusMonths(months)
    val days = thisZdt.until(otherZdt, ChronoUnit.DAYS); thisZdt = thisZdt.plusDays(days)
    val time = thisZdt.until(otherZdt, ChronoUnit.NANOS).nanoseconds

    time.toComponents { hours, minutes, seconds, nanoseconds ->
        return DateTimePeriod((months / 12).toInt(), (months % 12).toInt(), days.toInt(), hours, minutes, seconds.toLong(), nanoseconds.toLong())
    }
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, zone: TimeZone): Long =
        until(other, unit.calendarUnit.toChronoUnit(), zone.zoneId) / unit.calendarScale

private fun Instant.until(other: Instant, unit: ChronoUnit, zone: ZoneId): Long =
        this.value.atZone(zone).until(other.value.atZone(zone), unit)

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
