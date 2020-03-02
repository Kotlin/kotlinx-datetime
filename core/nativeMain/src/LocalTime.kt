/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

val localTimeParser: Parser<LocalTime>
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
            val (second, nanosecond) = when(secNano) {
                null -> Pair(0, 0)
                else -> Pair(secNano.first, secNano.second ?: 0)
            }
            LocalTime(hour, minute, second, nanosecond)
        }

class LocalTime(val hour: Int, val minute: Int, val second: Int, val nanosecond: Int): Comparable<LocalTime> {

    companion object {
        internal fun ofSecondOfDay(secondOfDay: Long, nanoOfSecond: Int): LocalTime {
            require(secondOfDay in 0..SECONDS_PER_DAY)
            require(nanoOfSecond in 0..1_000_000_000)
            val hours = (secondOfDay / SECONDS_PER_HOUR).toInt()
            val secondWithoutHours = secondOfDay - hours * SECONDS_PER_HOUR.toLong()
            val minutes = (secondWithoutHours / SECONDS_PER_MINUTE).toInt()
            val second = secondWithoutHours - minutes * SECONDS_PER_MINUTE.toLong()
            return LocalTime(hours, minutes, second.toInt(), nanoOfSecond)
        }
    }

    override fun compareTo(other: LocalTime): Int =
        compareBy<LocalTime>({ it.hour }, { it.minute }, { it.second }, { it.nanosecond }).compare(this, other)

    override fun hashCode(): Int {
        val nod: Long = toNanoOfDay()
        return (nod xor (nod ushr 32)).toInt()
    }

    /**
     * Extracts the time as nanos of day,
     * from `0` to `24 * 60 * 60 * 1,000,000,000 - 1`.
     *
     * @return the nano of day equivalent to this time
     */
    internal fun toNanoOfDay(): Long {
        var total: Long = hour * NANOS_PER_ONE * SECONDS_PER_HOUR.toLong()
        total += minute * NANOS_PER_ONE * SECONDS_PER_MINUTE
        total += second * NANOS_PER_ONE
        total += nanosecond.toLong()
        return total
    }

    internal fun toSecondOfDay(): Int {
        var total: Int = hour * SECONDS_PER_HOUR
        total += minute * SECONDS_PER_MINUTE
        total += second
        return total
    }

    override fun toString(): String {
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
                if (nanoValue % 1000000 == 0) {
                    buf.append((nanoValue / 1000000 + 1000).toString().substring(1))
                } else if (nanoValue % 1000 == 0) {
                    buf.append((nanoValue / 1000 + 1000000).toString().substring(1))
                } else {
                    buf.append((nanoValue + 1000000000).toString().substring(1))
                }
            }
        }
        return buf.toString()
    }

    override fun equals(other: Any?): Boolean =
        other is LocalTime && this.compareTo(other) == 0

}