/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


// TODO: could be error-prone without explicitly named params
sealed class DateTimePeriod {
    internal abstract val totalMonths: Int
    abstract val days: Int
    internal abstract val totalNanoseconds: Long

    val years: Int get() = totalMonths / 12
    val months: Int get() = totalMonths % 12
    open val hours: Int get() = (totalNanoseconds / 3_600_000_000_000).toInt()
    open val minutes: Int get() = ((totalNanoseconds % 3_600_000_000_000) / 60_000_000_000).toInt()
    open val seconds: Int get() = ((totalNanoseconds % 60_000_000_000) / NANOS_PER_ONE).toInt()
    open val nanoseconds: Int get() = (totalNanoseconds % NANOS_PER_ONE).toInt()

    private fun allNonpositive() =
        totalMonths <= 0 && days <= 0 && totalNanoseconds <= 0 && (totalMonths or days != 0 || totalNanoseconds != 0L)

    override fun toString(): String = buildString {
        val sign = if (allNonpositive()) { append('-'); -1 } else 1
        append('P')
        if (years != 0) append(years * sign).append('Y')
        if (months != 0) append(months * sign).append('M')
        if (days != 0) append(days * sign).append('D')
        var t = "T"
        if (hours != 0) append(t).append(hours * sign).append('H').also { t = "" }
        if (minutes != 0) append(t).append(minutes * sign).append('M').also { t = "" }
        if (seconds or nanoseconds != 0) {
            append(t)
            append(when {
                seconds != 0 -> seconds * sign
                nanoseconds * sign < 0 -> "-0"
                else -> "0"
            })
            if (nanoseconds != 0) append('.').append((nanoseconds.absoluteValue).toString().padStart(9, '0'))
            append('S')
        }

        if (length == 1) append("0D")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DateTimePeriod) return false

        if (totalMonths != other.totalMonths) return false
        if (days != other.days) return false
        if (totalNanoseconds != other.totalNanoseconds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = totalMonths
        result = 31 * result + days
        result = 31 * result + totalNanoseconds.hashCode()
        return result
    }

    // TODO: parsing from iso string
}

class DatePeriod internal constructor(
    internal override val totalMonths: Int,
    override val days: Int = 0,
) : DateTimePeriod() {
    constructor(years: Int = 0, months: Int = 0, days: Int = 0): this(totalMonths(years, months), days)
    // avoiding excessive computations
    override val hours: Int get() = 0
    override val minutes: Int get() = 0
    override val seconds: Int get() = 0
    override val nanoseconds: Int get() = 0
    override val totalNanoseconds: Long get() = 0
}

private class DateTimePeriodImpl(
    internal override val totalMonths: Int = 0,
    override val days: Int = 0,
    override val totalNanoseconds: Long = 0,
) : DateTimePeriod()

// TODO: these calculations fit in a JS Number. Possible to do an expect/actual here.
private fun totalMonths(years: Int, months: Int): Int =
    when (val totalMonths = years.toLong() * 12 + months.toLong()) {
        in Int.MIN_VALUE..Int.MAX_VALUE -> totalMonths.toInt()
        else -> throw IllegalArgumentException("The total number of months in $years years and $months months overflows an Int")
    }

private fun totalNanoseconds(hours: Int, minutes: Int, seconds: Int, nanoseconds: Long): Long {
    val totalMinutes: Long = hours.toLong() * 60 + minutes
    // absolute value at most 61 * Int.MAX_VALUE
    val totalMinutesAsSeconds: Long = totalMinutes * 60
    // absolute value at most 61 * 60 * Int.MAX_VALUE < 64 * 64 * 2^31 = 2^43
    val minutesAndNanosecondsAsSeconds: Long = totalMinutesAsSeconds + nanoseconds / NANOS_PER_ONE
    // absolute value at most 2^43 + 2^63 / 10^9 < 2^43 + 2^34 < 2^44
    val totalSeconds = minutesAndNanosecondsAsSeconds + seconds
    // absolute value at most 2^44 + 2^31 < 2^45
    return try {
        multiplyAndAdd(totalSeconds, 1_000_000_000, nanoseconds % NANOS_PER_ONE)
    } catch (e: ArithmeticException) {
        throw IllegalArgumentException("The total number of nanoseconds in $hours hours, $minutes minutes, $seconds seconds, and $nanoseconds nanoseconds overflows a Long")
    }
}

internal fun buildDateTimePeriod(totalMonths: Int = 0, days: Int = 0, totalNanoseconds: Long): DateTimePeriod =
    if (totalNanoseconds != 0L)
        DateTimePeriodImpl(totalMonths, days, totalNanoseconds)
    else
        DatePeriod(totalMonths, days)

fun DateTimePeriod(
    years: Int = 0,
    months: Int = 0,
    days: Int = 0,
    hours: Int = 0,
    minutes: Int = 0,
    seconds: Int = 0,
    nanoseconds: Long = 0
): DateTimePeriod = buildDateTimePeriod(totalMonths(years, months), days,
    totalNanoseconds(hours, minutes, seconds, nanoseconds))

@OptIn(ExperimentalTime::class)
fun Duration.toDateTimePeriod(): DateTimePeriod = toComponents { hours, minutes, seconds, nanoseconds ->
    buildDateTimePeriod(totalNanoseconds = totalNanoseconds(hours, minutes, seconds, nanoseconds.toLong()))
}

operator fun DateTimePeriod.plus(other: DateTimePeriod): DateTimePeriod = buildDateTimePeriod(
    safeAdd(totalMonths, other.totalMonths),
    safeAdd(days, other.days),
    safeAdd(totalNanoseconds, other.totalNanoseconds),
)

operator fun DatePeriod.plus(other: DatePeriod): DatePeriod = DatePeriod(
    safeAdd(totalMonths, other.totalMonths),
    safeAdd(days, other.days),
)

