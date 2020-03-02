/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

val localDateTimeParser: Parser<LocalDateTime>
    get() = localDateParser
        .chainIgnoring(concreteCharParser('T').or(concreteCharParser('t')))
        .chain(localTimeParser)
        .map { (date, time) ->
            LocalDateTime(date, time)
        }

public actual class LocalDateTime(actual val date: LocalDate, val time: LocalTime) : Comparable<LocalDateTime> {
    actual companion object {
        actual fun parse(isoString: String): LocalDateTime =
            localDateTimeParser.parse(isoString)
    }

    actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int):
        this(LocalDate(year, monthNumber, dayOfMonth), LocalTime(hour, minute, second, nanosecond))

    actual val year: Int get() = date.year
    actual val monthNumber: Int get() = date.monthNumber
    actual val month: Month get() = date.month
    actual val dayOfMonth: Int get() = date.dayOfMonth
    actual val dayOfWeek: DayOfWeek get() = date.dayOfWeek
    actual val dayOfYear: Int get() = date.dayOfYear
    actual val hour: Int get() = time.hour
    actual val minute: Int get() = time.minute
    actual val second: Int get() = time.second
    actual val nanosecond: Int get() = time.nanosecond

    actual override fun compareTo(other: LocalDateTime): Int =
        compareBy<LocalDateTime>({ it.date }, { it.time }).compare(this, other)

    override fun equals(other: Any?): Boolean =
        this === other || (other is LocalDateTime && compareTo(other) == 0)

    override fun hashCode(): Int {
        return date.hashCode() xor time.hashCode()
    }

    override fun toString(): String = date.toString() + 'T' + time.toString()

    internal fun toEpochSecond(offset: ZoneOffset): Long {
        val epochDay: Long = date.toEpochDay()
        var secs: Long = epochDay * 86400 + time.toSecondOfDay()
        secs -= offset.totalSeconds
        return secs
    }

    fun plusYears(years: Long): LocalDateTime = LocalDateTime(date.plusYears(years), time)
    fun plusMonths(months: Long): LocalDateTime = LocalDateTime(date.plusMonths(months), time)
    fun plusDays(days: Long): LocalDateTime = LocalDateTime(date.plusDays(days), time)

}

actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    with (timeZone) { toLocalDateTime() }

actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
    with (timeZone) { toInstant() }

actual fun Instant.offsetAt(timeZone: TimeZone): ZoneOffset =
    with (timeZone) { offset }

internal fun LocalDateTime.until(other: LocalDateTime, unit: CalendarUnit): Long =
    when (unit) {
        CalendarUnit.YEAR, CalendarUnit.MONTH, CalendarUnit.WEEK, CalendarUnit.DAY -> {
            var endDate: LocalDate = other.date
            if (endDate > date && other.time < time) {
                endDate = endDate.plusDays(-1)
            } else if (endDate < date && other.time > time) {
                endDate = endDate.plusDays(1)
            }
            date.until(endDate, unit)
        }
        CalendarUnit.HOUR, CalendarUnit.MINUTE, CalendarUnit.SECOND, CalendarUnit.NANOSECOND -> {
            var daysUntil = date.daysUntil(other.date)
            var timeUntil: Long = other.time.toNanoOfDay() - time.toNanoOfDay()
            if (daysUntil > 0 && timeUntil < 0) {
                daysUntil--
                timeUntil += NANOS_PER_DAY
            } else if (daysUntil < 0 && timeUntil > 0) {
                daysUntil++
                timeUntil -= NANOS_PER_DAY
            }
            val nanos = timeUntil
            when (unit) {
                CalendarUnit.HOUR -> safeAdd(nanos / NANOS_PER_HOUR, safeMultiply(daysUntil, HOURS_PER_DAY.toLong()))
                CalendarUnit.MINUTE -> safeAdd(nanos / NANOS_PER_MINUTE, safeMultiply(daysUntil, MINUTES_PER_DAY.toLong()))
                CalendarUnit.SECOND -> safeAdd(nanos / NANOS_PER_ONE, safeMultiply(daysUntil, SECONDS_PER_DAY.toLong()))
                CalendarUnit.NANOSECOND -> safeAdd(nanos, safeMultiply(daysUntil, NANOS_PER_DAY))
                else -> throw RuntimeException("impossible")
            }
        }
    }
