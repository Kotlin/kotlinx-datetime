/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.format.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable

@Serializable(LocalTimeSerializer::class)
public actual class LocalTime actual constructor(
    public actual val hour: Int,
    public actual val minute: Int,
    public actual val second: Int,
    public actual val nanosecond: Int
    ) : Comparable<LocalTime> {

    init {
        fun check(value: Int, lower: Int, upper: Int, str: String) =
            require(value in lower..upper) {
                "Invalid time: $str must be a number between $lower and $upper, got $value"
            }
        check(hour, 0, 23, "hour")
        check(minute, 0, 59, "minute")
        check(second, 0, 59, "second")
        check(nanosecond, 0, NANOS_PER_ONE - 1, "nanosecond")
    }

    public actual companion object {
        public actual fun parse(input: CharSequence, format: DateTimeFormat<LocalTime>): LocalTime = format.parse(input)

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): LocalTime = parse(input = isoString)

        public actual fun fromSecondOfDay(secondOfDay: Int): LocalTime =
            ofSecondOfDay(secondOfDay, 0)

        public actual fun fromMillisecondOfDay(millisecondOfDay: Int): LocalTime =
            ofNanoOfDay(millisecondOfDay.toLong() * NANOS_PER_MILLI)

        public actual fun fromNanosecondOfDay(nanosecondOfDay: Long): LocalTime =
            ofNanoOfDay(nanosecondOfDay)

        // org.threeten.bp.LocalTime#ofSecondOfDay(long, int)
        internal fun ofSecondOfDay(secondOfDay: Int, nanoOfSecond: Int): LocalTime {
            require(secondOfDay in 0 until SECONDS_PER_DAY)
            require(nanoOfSecond in 0 until NANOS_PER_ONE)
            val hours = (secondOfDay / SECONDS_PER_HOUR)
            val secondWithoutHours = secondOfDay - hours * SECONDS_PER_HOUR
            val minutes = (secondWithoutHours / SECONDS_PER_MINUTE)
            val second = secondWithoutHours - minutes * SECONDS_PER_MINUTE
            return LocalTime(hours, minutes, second, nanoOfSecond)
        }

        internal fun of(hour: Int, minute: Int, second: Int, nanosecond: Int): LocalTime {
            return LocalTime(hour, minute, second, nanosecond)
        }

        // org.threeten.bp.LocalTime#ofNanoOfDay
        internal fun ofNanoOfDay(nanoOfDay: Long): LocalTime {
            require(nanoOfDay >= 0 && nanoOfDay < SECONDS_PER_DAY.toLong() * NANOS_PER_ONE)
            var newNanoOfDay = nanoOfDay
            val hours = (newNanoOfDay / NANOS_PER_HOUR).toInt()
            newNanoOfDay -= hours * NANOS_PER_HOUR
            val minutes = (newNanoOfDay / NANOS_PER_MINUTE).toInt()
            newNanoOfDay -= minutes * NANOS_PER_MINUTE
            val seconds = (newNanoOfDay / NANOS_PER_ONE).toInt()
            newNanoOfDay -= seconds * NANOS_PER_ONE
            return LocalTime(hours, minutes, seconds, newNanoOfDay.toInt())
        }

        internal actual val MIN: LocalTime = LocalTime(0, 0, 0, 0)
        internal actual val MAX: LocalTime = LocalTime(23, 59, 59, NANOS_PER_ONE - 1)

        @Suppress("FunctionName")
        public actual fun Format(builder: DateTimeFormatBuilder.WithTime.() -> Unit): DateTimeFormat<LocalTime> =
            LocalTimeFormat.build(builder)
    }

    public actual object Formats {
        public actual val ISO: DateTimeFormat<LocalTime> get() = ISO_TIME
    }

    // Several times faster than using `compareBy`
    actual override fun compareTo(other: LocalTime): Int {
        val h = hour.compareTo(other.hour)
        if (h != 0) {
            return h
        }
        val m = minute.compareTo(other.minute)
        if (m != 0) {
            return m
        }
        val s = second.compareTo(other.second)
        if (s != 0) {
            return s
        }
        return nanosecond.compareTo(other.nanosecond)
    }

    override fun hashCode(): Int {
        val nod: Long = this.toNanosecondOfDay()
        return (nod xor (nod ushr 32)).toInt()
    }

    // org.threeten.bp.LocalTime#toSecondOfDay
    public actual fun toSecondOfDay(): Int {
        var total: Int = hour * SECONDS_PER_HOUR
        total += minute * SECONDS_PER_MINUTE
        total += second
        return total
    }

    public actual fun toMillisecondOfDay(): Int =
        toSecondOfDay() * MILLIS_PER_ONE + nanosecond / NANOS_PER_MILLI

    // org.threeten.bp.LocalTime#toNanoOfDay
    public actual fun toNanosecondOfDay(): Long {
        var total: Long = hour.toLong() * NANOS_PER_ONE * SECONDS_PER_HOUR
        total += minute.toLong() * NANOS_PER_ONE * SECONDS_PER_MINUTE
        total += second.toLong() * NANOS_PER_ONE
        total += nanosecond.toLong()
        return total
    }

    actual override fun toString(): String = format(ISO_TIME_OPTIONAL_SECONDS_TRAILING_ZEROS)

    override fun equals(other: Any?): Boolean =
        other is LocalTime && this.compareTo(other) == 0

}

internal val ISO_TIME_OPTIONAL_SECONDS_TRAILING_ZEROS by lazy {
    LocalTimeFormat.build {
        hour()
        char(':')
        minute()
        optional {
            char(':')
            second()
            optional {
                char('.')
                secondFractionInternal(1, 9, FractionalSecondDirective.GROUP_BY_THREE)
            }
        }
    }
}
