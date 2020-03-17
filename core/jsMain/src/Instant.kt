/*
 * Copyright 2016-2019 JetBrains s.r.o.
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
import kotlinx.datetime.internal.JSJoda.ZoneId

@OptIn(kotlin.time.ExperimentalTime::class)
public actual class Instant internal constructor(internal val value: jtInstant) : Comparable<Instant> {

    public actual fun toUnixMillis(): Long = value.toEpochMilli().toLong()


    actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        Instant(value.plusSeconds(seconds).plusNanos(nanoseconds.toLong()))
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
        actual fun now(): Instant =
                Instant(jtClock.systemUTC().instant())

        actual fun fromUnixMillis(millis: Long): Instant =
                Instant(jtInstant.ofEpochMilli(millis.toDouble()))

        actual fun parse(isoString: String): Instant =
                Instant(jtInstant.parse(isoString))
    }

}


public actual fun Instant.plus(period: CalendarPeriod, zone: TimeZone): Instant {
    val thisZdt = this.value.atZone(zone.zoneId)
    return with(period) {
        thisZdt
                .run { if (years != 0 && months == 0) plusYears(years) else this }
                .run { if (months != 0) plusMonths(years * 12.0 + months) else this }
                .run { if (days != 0) plusDays(days) as ZonedDateTime else this }
                .run { if (hours != 0) plusHours(hours) else this }
                .run { if (minutes != 0) plusMinutes(minutes) else this }
                .run { if (seconds != 0L) plusSeconds(seconds.toDouble()) else this }
                .run { if (nanoseconds != 0L) plusNanos(nanoseconds.toDouble()) else this }
    }.toInstant().let(::Instant)
}

public actual fun Instant.plus(value: Int, unit: CalendarUnit, zone: TimeZone): Instant =
        when (unit) {
            CalendarUnit.YEAR -> this.value.atZone(zone.zoneId).plusYears(value).toInstant()
            CalendarUnit.MONTH -> this.value.atZone(zone.zoneId).plusMonths(value).toInstant()
            CalendarUnit.WEEK -> this.value.atZone(zone.zoneId).plusWeeks(value).let { it as ZonedDateTime }.toInstant()
            CalendarUnit.DAY -> this.value.atZone(zone.zoneId).plusDays(value).let { it as ZonedDateTime }.toInstant()
            CalendarUnit.HOUR -> this.value.atZone(zone.zoneId).plusHours(value).toInstant()
            CalendarUnit.MINUTE -> this.value.atZone(zone.zoneId).plusMinutes(value).toInstant()
            CalendarUnit.SECOND -> this.value.plusSeconds(value)
            CalendarUnit.NANOSECOND -> this.value.plusNanos(value)
        }.let(::Instant)

public actual fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant =
        when (unit) {
            CalendarUnit.YEAR -> this.value.atZone(zone.zoneId).plusYears(value).toInstant()
            CalendarUnit.MONTH -> this.value.atZone(zone.zoneId).plusMonths(value).toInstant()
            CalendarUnit.WEEK -> this.value.atZone(zone.zoneId).plusWeeks(value).let { it as ZonedDateTime }.toInstant()
            CalendarUnit.DAY -> this.value.atZone(zone.zoneId).plusDays(value).let { it as ZonedDateTime }.toInstant()
            CalendarUnit.HOUR -> this.value.atZone(zone.zoneId).plusHours(value).toInstant()
            CalendarUnit.MINUTE -> this.value.atZone(zone.zoneId).plusMinutes(value).toInstant()
            CalendarUnit.SECOND -> this.value.plusSeconds(value)
            CalendarUnit.NANOSECOND -> this.value.plusNanos(value)
        }.let(::Instant)

@OptIn(ExperimentalTime::class)
public actual fun Instant.periodUntil(other: Instant, zone: TimeZone): CalendarPeriod {
    var thisZdt = this.value.atZone(zone.zoneId)
    val otherZdt = other.value.atZone(zone.zoneId)

    val months = thisZdt.until(otherZdt, ChronoUnit.MONTHS).toDouble(); thisZdt = thisZdt.plusMonths(months)
    val days = thisZdt.until(otherZdt, ChronoUnit.DAYS).toDouble(); thisZdt = thisZdt.plusDays(days) as ZonedDateTime
    val time = thisZdt.until(otherZdt, ChronoUnit.NANOS).toDouble().nanoseconds

    time.toComponents { hours, minutes, seconds, nanoseconds ->
        return CalendarPeriod((months / 12).toInt(), (months % 12).toInt(), days.toInt(), hours, minutes, seconds.toLong(), nanoseconds.toLong())
    }
}

actual fun Instant.until(other: Instant, unit: CalendarUnit, zone: TimeZone): Long =
        until(other, unit.toChronoUnit(), zone.zoneId)

private fun Instant.until(other: Instant, unit: ChronoUnit, zone: ZoneId): Long =
        this.value.atZone(zone).until(other.value.atZone(zone), unit).toLong()

private fun CalendarUnit.toChronoUnit(): ChronoUnit = when(this) {
    CalendarUnit.YEAR -> ChronoUnit.YEARS
    CalendarUnit.MONTH -> ChronoUnit.MONTHS
    CalendarUnit.WEEK -> ChronoUnit.WEEKS
    CalendarUnit.DAY -> ChronoUnit.DAYS
    CalendarUnit.HOUR -> ChronoUnit.HOURS
    CalendarUnit.MINUTE -> ChronoUnit.MINUTES
    CalendarUnit.SECOND -> ChronoUnit.SECONDS
    CalendarUnit.NANOSECOND -> ChronoUnit.NANOS
}

actual fun Instant.daysUntil(other: Instant, zone: TimeZone): Int = until(other, ChronoUnit.DAYS, zone.zoneId).toInt()
actual fun Instant.monthsUntil(other: Instant, zone: TimeZone): Int = until(other, ChronoUnit.MONTHS, zone.zoneId).toInt()
actual fun Instant.yearsUntil(other: Instant, zone: TimeZone): Int = until(other, ChronoUnit.YEARS, zone.zoneId).toInt()
