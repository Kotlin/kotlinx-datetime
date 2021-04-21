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
    public actual val date: LocalDate, internal val time: LocalTime) : Comparable<LocalDateTime> {
    public actual companion object {
        public actual fun parse(isoString: String): LocalDateTime =
            localDateTimeParser.parse(isoString)

        internal actual val MIN: LocalDateTime = LocalDateTime(LocalDate.MIN, LocalTime.MIN)
        internal actual val MAX: LocalDateTime = LocalDateTime(LocalDate.MAX, LocalTime.MAX)
    }

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
        this(LocalDate(year, monthNumber, dayOfMonth), LocalTime.of(hour, minute, second, nanosecond))

    public actual constructor(year: Int, month: Month, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
        this(LocalDate(year, month, dayOfMonth), LocalTime.of(hour, minute, second, nanosecond))

    public actual val year: Int get() = date.year
    public actual val monthNumber: Int get() = date.monthNumber
    public actual val month: Month get() = date.month
    public actual val dayOfMonth: Int get() = date.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = date.dayOfWeek
    public actual val dayOfYear: Int get() = date.dayOfYear
    public actual val hour: Int get() = time.hour
    public actual val minute: Int get() = time.minute
    public actual val second: Int get() = time.second
    public actual val nanosecond: Int get() = time.nanosecond

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
    actual override fun toString(): String = date.toString() + 'T' + time.toString()

    // org.threeten.bp.chrono.ChronoLocalDateTime#toEpochSecond
    internal fun toEpochSecond(offset: ZoneOffsetImpl): Long {
        val epochDay = date.toEpochDay().toLong()
        var secs: Long = epochDay * 86400 + time.toSecondOfDay()
        secs -= offset.totalSeconds
        return secs
    }

    /**
     * @throws IllegalArgumentException if the result exceeds the boundaries
     * @throws ArithmeticException if arithmetic overflow occurs
     */
    internal fun plus(value: Int, unit: DateTimeUnit.DateBased): LocalDateTime =
        LocalDateTime(date.plus(value, unit), time)
}

// org.threeten.bp.LocalDateTime#until
internal fun LocalDateTime.until(other: LocalDateTime, unit: DateTimeUnit.DateBased): Int {
    var endDate: LocalDate = other.date
    if (endDate > date && other.time < time) {
        endDate = endDate.plusDays(-1) // won't throw: endDate - date >= 1
    } else if (endDate < date && other.time > time) {
        endDate = endDate.plusDays(1) // won't throw: date - endDate >= 1
    }
    return when (unit) {
        is DateTimeUnit.DateBased.MonthBased -> date.monthsUntil(endDate) / unit.months
        is DateTimeUnit.DateBased.DayBased -> date.daysUntil(endDate) / unit.days
    }
}

// org.threeten.bp.LocalDateTime#until
/** @throws ArithmeticException on arithmetic overflow. */
internal fun LocalDateTime.until(other: LocalDateTime, unit: DateTimeUnit.TimeBased): Long {
    val daysUntil = date.daysUntil(other.date)
    val timeUntil: Long = other.time.toNanoOfDay() - time.toNanoOfDay()
    return multiplyAddAndDivide(daysUntil.toLong(), NANOS_PER_DAY, timeUntil, unit.nanoseconds)
}

// org.threeten.bp.LocalDateTime#plusWithOverflow
/**
 * @throws IllegalArgumentException if the result exceeds the boundaries
 * @throws ArithmeticException if arithmetic overflow occurs
 */
internal fun LocalDateTime.plusSeconds(seconds: Int): LocalDateTime
{
    if (seconds == 0) {
        return this
    }
    val currentNanoOfDay = time.toNanoOfDay() // at most a day
    val totalNanos: Long = seconds % SECONDS_PER_DAY * NANOS_PER_ONE.toLong() + // at most a day
        currentNanoOfDay
    val totalDays = seconds / SECONDS_PER_DAY + // max/24*60*60 < max * 0.000012
        floorDiv(totalNanos, NANOS_PER_DAY) // max 2 days
    val newNanoOfDay: Long = floorMod(totalNanos, NANOS_PER_DAY)
    val newTime: LocalTime = if (newNanoOfDay == currentNanoOfDay) time else LocalTime.ofNanoOfDay(newNanoOfDay)
    return LocalDateTime(date.plusDays(totalDays.toInt()), newTime)
}
