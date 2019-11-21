/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

typealias Period = CalendarPeriod

class CalendarPeriod(val years: Int = 0, val months: Int = 0, val days: Int = 0,
                     val hours: Int = 0, val minutes: Int = 0, val seconds: Long = 0, val nanoseconds: Long = 0) {
    object Builder {
        val Int.years get() = CalendarPeriod(years = this)
        val Int.months get() = CalendarPeriod(months = this)
        val Int.days get() = CalendarPeriod(days = this)
    }

    private fun allPositive() = years >= 0 && months >= 0 && days >= 0 && hours >= 0 && minutes >= 0 && seconds >= 0 && nanoseconds >= 0

    override fun toString(): String = buildString {
        val sign = if (!allPositive()) { append('-'); -1 } else 1
        append('P')
        if (years != 0) append(years * sign).append('Y')
        if (months != 0) append(months * sign).append('M')
        if (days != 0) append(days * sign).append('D')
        var t = "T"
        if (hours != 0) append(t).append(hours * sign).append('H').also { t = "" }
        if (minutes != 0) append(t).append(minutes * sign).append('M').also { t = "" }
        if (seconds != 0L || nanoseconds != 0L) {
            append(t).append(seconds * sign)
            if (nanoseconds != 0L) append('.').append((nanoseconds * sign).toString().padStart(9, '0'))
            append('S')
        }

        if (length == 1) append("0D")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CalendarPeriod) return false

        if (years != other.years) return false
        if (months != other.months) return false
        if (days != other.days) return false
        if (hours != other.hours) return false
        if (minutes != other.minutes) return false
        if (seconds != other.seconds) return false
        if (nanoseconds != other.nanoseconds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = years
        result = 31 * result + months
        result = 31 * result + days
        result = 31 * result + hours
        result = 31 * result + minutes
        result = 31 * result + seconds.hashCode()
        result = 31 * result + nanoseconds.hashCode()
        return result
    }
}

inline fun period(builder: CalendarPeriod.Builder.() -> CalendarPeriod): CalendarPeriod = CalendarPeriod.Builder.builder()

val Int.calendarDays: CalendarPeriod get() = CalendarPeriod(days = this)
val Int.calendarMonths: CalendarPeriod get() = CalendarPeriod(months = this)
val Int.calendarYears: CalendarPeriod get() = CalendarPeriod(years = this)


@UseExperimental(ExperimentalTime::class)
fun Duration.toCalendarPeriod(): CalendarPeriod = toComponents { hours, minutes, seconds, nanoseconds ->
    CalendarPeriod(hours = hours, minutes = minutes, seconds = seconds.toLong(), nanoseconds = nanoseconds.toLong())
}

operator fun CalendarPeriod.plus(other: CalendarPeriod): CalendarPeriod = CalendarPeriod(
        this.years + other.years,
        this.months + other.months,
        this.days + other.days,
        this.hours + other.hours,
        this.minutes + other.minutes,
        this.seconds + other.seconds,
        this.nanoseconds + other.nanoseconds
)

enum class CalendarUnit {
    YEAR,
    MONTH,
    WEEK,
    DAY,
    HOUR,
    MINUTE,
    SECOND,
    NANOSECOND
}
