/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

// This is a function and not a value due to https://github.com/Kotlin/kotlinx-datetime/issues/5
// org.threeten.bp.format.DateTimeFormatter#ISO_LOCAL_DATE_TIME
internal val localDateTimeParser: Parser<LocalDateTime>
    get() = localDateParser
        .chainIgnoring(concreteCharParser('T').or(concreteCharParser('t')))
        .chain(localTimeParser)
        .map { (date, time) ->
            LocalDateTime(date, time)
        }

public actual class LocalDateTime internal constructor(
    actual val date: LocalDate, internal val time: LocalTime) : Comparable<LocalDateTime> {
    actual companion object {
        actual fun parse(isoString: String): LocalDateTime =
            localDateTimeParser.parse(isoString)
    }

    actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
        this(LocalDate(year, monthNumber, dayOfMonth), LocalTime.of(hour, minute, second, nanosecond))

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

    // Several times faster than using `compareBy`
    actual override fun compareTo(other: LocalDateTime): Int {
        val d = date.compareTo(other.date)
        if (d != 0) {
            return d
        }
        return time.compareTo(other.time)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is LocalDateTime && compareTo(other) == 0)

    // org.threeten.bp.LocalDateTime#hashCode
    override fun hashCode(): Int {
        return date.hashCode() xor time.hashCode()
    }

    // org.threeten.bp.LocalDateTime#toString
    override fun toString(): String = date.toString() + 'T' + time.toString()

    // org.threeten.bp.chrono.ChronoLocalDateTime#toEpochSecond
    internal fun toEpochSecond(offset: ZoneOffset): Long {
        val epochDay: Long = date.toEpochDay()
        var secs: Long = epochDay * 86400 + time.toSecondOfDay()
        secs -= offset.totalSeconds
        return secs
    }

    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusYears(years: Long): LocalDateTime = LocalDateTime(date.plusYears(years), time)
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusMonths(months: Long): LocalDateTime = LocalDateTime(date.plusMonths(months), time)
    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plusDays(days: Long): LocalDateTime = LocalDateTime(date.plusDays(days), time)

}

actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    with(timeZone) { toLocalDateTime() }

actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
    with(timeZone) { toInstant() }

actual fun Instant.offsetAt(timeZone: TimeZone): ZoneOffset =
    with(timeZone) { offset }

// org.threeten.bp.LocalDateTime#until
/** @throws ArithmeticException on arithmetic overflow. Only possible for time-based units. */
internal fun LocalDateTime.until(other: LocalDateTime, unit: CalendarUnit): Long =
    when (unit) {
        CalendarUnit.YEAR, CalendarUnit.MONTH, CalendarUnit.WEEK, CalendarUnit.DAY -> {
            var endDate: LocalDate = other.date
            if (endDate > date && other.time < time) {
                endDate = endDate.plusDays(-1) // won't throw: endDate - date >= 1
            } else if (endDate < date && other.time > time) {
                endDate = endDate.plusDays(1) // won't throw: date - endDate >= 1
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
                CalendarUnit.MINUTE -> safeAdd(nanos / NANOS_PER_MINUTE,
                    safeMultiply(daysUntil, MINUTES_PER_DAY.toLong()))
                CalendarUnit.SECOND -> safeAdd(nanos / NANOS_PER_ONE,
                    safeMultiply(daysUntil, SECONDS_PER_DAY.toLong()))
                CalendarUnit.NANOSECOND -> safeAdd(nanos, safeMultiply(daysUntil, NANOS_PER_DAY))
                else -> throw RuntimeException("impossible")
            }
        }
    }

// org.threeten.bp.LocalDateTime#plusSeconds
/**
 * @throws IllegalArgumentException if the result exceeds the boundaries
 * @throws ArithmeticException if arithmetic overflow occurs
 */
internal fun LocalDateTime.plusSeconds(seconds: Long): LocalDateTime =
    plusWithOverflow(date, 0, 0, seconds, 0, 1)

// org.threeten.bp.LocalDateTime#plusWithOverflow
/**
 * @throws IllegalArgumentException if the result exceeds the boundaries
 * @throws ArithmeticException if arithmetic overflow occurs
 */
internal fun LocalDateTime.plusWithOverflow(
    newDate: LocalDate, hours: Long, minutes: Long, seconds: Long, nanos: Long, sign: Int): LocalDateTime
{
    if (hours or minutes or seconds or nanos == 0L) {
        return LocalDateTime(newDate, time)
    }
    var totDays = nanos / NANOS_PER_DAY + //   max/24*60*60*1B
        seconds / SECONDS_PER_DAY + //   max/24*60*60
        minutes / MINUTES_PER_DAY + //   max/24*60
        hours / HOURS_PER_DAY //   max/24
    totDays *= sign.toLong() // total max*0.4237...
    var totNanos: Long = nanos % NANOS_PER_DAY + //   max  86400000000000
        seconds % SECONDS_PER_DAY * NANOS_PER_ONE + //   max  86400000000000
        minutes % MINUTES_PER_DAY * NANOS_PER_MINUTE + //   max  86400000000000
        hours % HOURS_PER_DAY * NANOS_PER_HOUR //   max  86400000000000
    val curNoD: Long = time.toNanoOfDay() //   max  86400000000000
    totNanos = totNanos * sign + curNoD // total 432000000000000
    totDays += floorDiv(totNanos, NANOS_PER_DAY)
    val newNoD: Long = floorMod(totNanos, NANOS_PER_DAY)
    val newTime: LocalTime = if (newNoD == curNoD) time else LocalTime.ofNanoOfDay(newNoD)
    return LocalDateTime(newDate.plusDays(totDays), newTime)
}
