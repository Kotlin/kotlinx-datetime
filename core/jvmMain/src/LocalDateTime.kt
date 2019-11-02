/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateTimeJvmKt")
package kotlinx.datetime

import kotlin.math.sign
import kotlin.time.*
import java.time.LocalDateTime as jtLocalDateTime
import java.time.Period as jtPeriod


public actual typealias Month = java.time.Month
public actual typealias DayOfWeek = java.time.DayOfWeek

public actual class LocalDateTime internal constructor(internal val value: jtLocalDateTime) : Comparable<LocalDateTime> {

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(jtLocalDateTime.of(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond))

    public actual val year: Int get() = value.year
    public actual val monthNumber: Int get() = value.monthValue
    public actual val month: Month get() = value.month
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = value.dayOfWeek
    public actual val dayOfYear: Int get() = value.dayOfYear

    public actual val hour: Int get() = value.hour
    public actual val minute: Int get() = value.minute
    public actual val second: Int get() = value.second
    public actual val nanosecond: Int get() = value.nano

    public actual val date: LocalDate get() = LocalDate(value.toLocalDate()) // cache?

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is LocalDateTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDateTime): Int = this.value.compareTo(other.value)

    actual companion object {
        public actual fun parse(isoString: String): LocalDateTime {
            return jtLocalDateTime.parse(isoString).let(::LocalDateTime)
        }
    }

}


public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
        jtLocalDateTime.ofInstant(this.value, timeZone.zoneId).let(::LocalDateTime)

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
        this.value.atZone(timeZone.zoneId).toInstant().let(::Instant)

actual fun LocalDateTime.plus(value: Long, unit: CalendarUnit): LocalDateTime =
        when (unit) {
            CalendarUnit.YEAR -> this.value.plusYears(value)
            CalendarUnit.MONTH -> this.value.plusMonths(value)
            CalendarUnit.WEEK -> this.value.plusWeeks(value)
            CalendarUnit.DAY -> this.value.plusDays(value)
            CalendarUnit.HOUR -> this.value.plusHours(value)
            CalendarUnit.MINUTE -> this.value.plusMinutes(value)
            CalendarUnit.SECOND -> this.value.plusSeconds(value)
            CalendarUnit.NANOSECOND -> this.value.plusNanos(value)
        }.let(::LocalDateTime)

actual fun LocalDateTime.plus(value: Int, unit: CalendarUnit): LocalDateTime =
        plus(value.toLong(), unit)

actual operator fun LocalDateTime.plus(period: CalendarPeriod): LocalDateTime =
        with(period) {
            value
                    .run { if (years != 0 && months == 0) plusYears(years.toLong()) else this }
                    .run { if (months != 0) plusMonths(years * 12L + months.toLong()) else this }
                    .run { if (days != 0) plusDays(days.toLong()) else this }
                    .run { if (hours != 0) plusHours(hours.toLong()) else this }
                    .run { if (minutes != 0) plusMinutes(minutes.toLong()) else this }
                    .run { if (seconds != 0L) plusSeconds(seconds) else this }
                    .run { if (nanoseconds != 0L) plusNanos(nanoseconds) else this }
        }.let(::LocalDateTime)

@UseExperimental(ExperimentalTime::class)
actual operator fun LocalDateTime.minus(other: LocalDateTime): CalendarPeriod {
    val timeNanoDiff = this.value.toLocalTime().toNanoOfDay() - other.value.toLocalTime().toNanoOfDay()
    val resultSign = this.compareTo(other).sign
    val borrowDay: Int
    val resultTime = if (timeNanoDiff * resultSign < 0) {
        borrowDay = -resultSign
        timeNanoDiff.nanoseconds + resultSign.days
    } else {
        borrowDay = 0
        timeNanoDiff.nanoseconds
    }

    val resultPeriod = jtPeriod.between(other.value.toLocalDate(), this.value.toLocalDate().plusDays(borrowDay.toLong()))

    return with(resultPeriod) {
        resultTime.toComponents { hours, minutes, seconds, nanoseconds ->
            CalendarPeriod(years = years, months = months, days = days, hours = hours, minutes = minutes, seconds = seconds.toLong(), nanoseconds = nanoseconds.toLong())
        }
    }
}