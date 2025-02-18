/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.*

@Serializable(with = LocalDateTimeSerializer::class)
public actual class LocalDateTime
public actual constructor(public actual val date: LocalDate, public actual val time: LocalTime) : Comparable<LocalDateTime> {
    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalDateTime>): LocalDateTime =
            format.parse(input)

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): LocalDateTime = parse(input = isoString)

        internal actual val MIN: LocalDateTime = LocalDateTime(LocalDate.MIN, LocalTime.MIN)
        internal actual val MAX: LocalDateTime = LocalDateTime(LocalDate.MAX, LocalTime.MAX)

        @Suppress("FunctionName")
        public actual fun Format(builder: DateTimeFormatBuilder.WithDateTime.() -> Unit): DateTimeFormat<LocalDateTime> =
            LocalDateTimeFormat.build(builder)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalDateTime> = ISO_DATETIME
    }

    public actual constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
        this(LocalDate(year, month, day), LocalTime.of(hour, minute, second, nanosecond))

    public actual constructor(year: Int, month: Month, day: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
        this(LocalDate(year, month, day), LocalTime.of(hour, minute, second, nanosecond))

    public actual val year: Int get() = date.year
    @Deprecated("Use the 'month' property instead", ReplaceWith("this.month.number"), level = DeprecationLevel.WARNING)
    public actual val monthNumber: Int get() = date.month.number
    public actual val month: Month get() = date.month
    @Deprecated("Use the 'day' property instead", ReplaceWith("this.day"), level = DeprecationLevel.WARNING)
    public actual val dayOfMonth: Int get() = date.day
    public actual val day: Int get() = date.day
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
    actual override fun toString(): String = format(ISO_DATETIME_OPTIONAL_SECONDS_TRAILING_ZEROS)

    // org.threeten.bp.chrono.ChronoLocalDateTime#toEpochSecond
    internal fun toEpochSecond(offset: UtcOffset): Long {
        val epochDay = date.toEpochDays().toLong()
        var secs: Long = epochDay * 86400 + time.toSecondOfDay()
        secs -= offset.totalSeconds
        return secs
    }
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
    val currentNanoOfDay = time.toNanosecondOfDay() // at most a day
    val totalNanos: Long = seconds % SECONDS_PER_DAY * NANOS_PER_ONE.toLong() + // at most a day
        currentNanoOfDay
    val totalDays = seconds / SECONDS_PER_DAY + // max/24*60*60 < max * 0.000012
        totalNanos.floorDiv(NANOS_PER_DAY) // max 2 days
    val newNanoOfDay: Long = totalNanos.mod(NANOS_PER_DAY)
    val newTime: LocalTime = if (newNanoOfDay == currentNanoOfDay) time else LocalTime.ofNanoOfDay(newNanoOfDay)
    return LocalDateTime(date.plusDays(totalDays), newTime)
}

private val ISO_DATETIME_OPTIONAL_SECONDS_TRAILING_ZEROS by lazy {
    LocalDateTimeFormat.build {
        date(ISO_DATE)
        alternativeParsing({ char('t') }) { char('T') }
        time(ISO_TIME_OPTIONAL_SECONDS_TRAILING_ZEROS)
    }
}
