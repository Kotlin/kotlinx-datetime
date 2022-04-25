/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.LocalTimeIso8601Serializer
import kotlinx.serialization.Serializable

// This is a function and not a value due to https://github.com/Kotlin/kotlinx-datetime/issues/5
// org.threeten.bp.format.DateTimeFormatter#ISO_LOCAL_TIME
internal val localTimeParser: Parser<LocalTime>
    get() = intParser(2, 2) // hour
        .chainIgnoring(concreteCharParser(':'))
        .chain(intParser(2, 2)) // minute
        .chain(optional(
            concreteCharParser(':')
                .chainSkipping(intParser(2, 2)) // second
                .chain(optional(
                    concreteCharParser('.')
                        .chainSkipping(fractionParser(0, 9, 9))
                ))
        ))
        .map {
            val (hourMinute, secNano) = it
            val (hour, minute) = hourMinute
            val (sec, nanosecond) = when (secNano) {
                null -> Pair(0, 0)
                else -> Pair(secNano.first, secNano.second ?: 0)
            }
            try {
                LocalTime.of(hour, minute, sec, nanosecond)
            } catch (e: IllegalArgumentException) {
                throw DateTimeFormatException(e)
            }
        }

@Serializable(LocalTimeIso8601Serializer::class)
public actual class LocalTime actual constructor(
    public actual val hour: Int,
    public actual val minute: Int,
    public actual val second: Int,
    public actual val nanosecond: Int
    ) : Comparable<LocalTime> {

    init {
        fun check(value: Int, lower: Int, upper: Int, str: String) =
            require(value >= lower && value <= upper) {
                "Invalid time: $str must be a number between $lower and $upper, got $value"
            }
        check(hour, 0, 23, "hour")
        check(minute, 0, 59, "minute")
        check(second, 0, 59, "second")
        check(nanosecond, 0, NANOS_PER_ONE - 1, "nanosecond")
    }

    public actual companion object {
        public actual fun parse(isoString: String): LocalTime =
            localTimeParser.parse(isoString)

        // org.threeten.bp.LocalTime#ofSecondOfDay(long, int)
        internal fun ofSecondOfDay(secondOfDay: Int, nanoOfSecond: Int): LocalTime {
            // Unidiomatic code due to https://github.com/Kotlin/kotlinx-datetime/issues/5
            require(secondOfDay >= 0 && secondOfDay < SECONDS_PER_DAY)
            require(nanoOfSecond >= 0 && nanoOfSecond < NANOS_PER_ONE)
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
        val nod: Long = toNanoOfDay()
        return (nod xor (nod ushr 32)).toInt()
    }

    // org.threeten.bp.LocalTime#toNanoOfDay
    internal fun toNanoOfDay(): Long {
        var total: Long = hour.toLong() * NANOS_PER_ONE * SECONDS_PER_HOUR
        total += minute.toLong() * NANOS_PER_ONE * SECONDS_PER_MINUTE
        total += second.toLong() * NANOS_PER_ONE
        total += nanosecond.toLong()
        return total
    }

    // org.threeten.bp.LocalTime#toSecondOfDay
    internal fun toSecondOfDay(): Int {
        var total: Int = hour * SECONDS_PER_HOUR
        total += minute * SECONDS_PER_MINUTE
        total += second
        return total
    }

    // org.threeten.bp.LocalTime#toString
    actual override fun toString(): String {
        val buf = StringBuilder(18)
        val hourValue = hour
        val minuteValue = minute
        val secondValue = second
        val nanoValue: Int = nanosecond
        buf.append(if (hourValue < 10) "0" else "").append(hourValue)
            .append(if (minuteValue < 10) ":0" else ":").append(minuteValue)
        if (secondValue > 0 || nanoValue > 0) {
            buf.append(if (secondValue < 10) ":0" else ":").append(secondValue)
            if (nanoValue > 0) {
                buf.append('.')
                when {
                    nanoValue % 1000000 == 0 -> {
                        buf.append((nanoValue / 1000000 + 1000).toString().substring(1))
                    }
                    nanoValue % 1000 == 0 -> {
                        buf.append((nanoValue / 1000 + 1000000).toString().substring(1))
                    }
                    else -> {
                        buf.append((nanoValue + 1000000000).toString().substring(1))
                    }
                }
            }
        }
        return buf.toString()
    }

    override fun equals(other: Any?): Boolean =
        other is LocalTime && this.compareTo(other) == 0

}
