/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

// TODO: could be error-prone without explicitly named params
public sealed class DateTimePeriod {
    internal abstract val totalMonths: Int
    public abstract val days: Int
    internal abstract val totalNanoseconds: Long

    public val years: Int get() = totalMonths / 12
    public val months: Int get() = totalMonths % 12
    public open val hours: Int get() = (totalNanoseconds / 3_600_000_000_000).toInt()
    public open val minutes: Int get() = ((totalNanoseconds % 3_600_000_000_000) / 60_000_000_000).toInt()
    public open val seconds: Int get() = ((totalNanoseconds % 60_000_000_000) / NANOS_PER_ONE).toInt()
    public open val nanoseconds: Int get() = (totalNanoseconds % NANOS_PER_ONE).toInt()

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

    public companion object {
        public fun parse(text: String): DateTimePeriod {
            fun parseException(message: String, position: Int): Nothing =
                throw DateTimeFormatException("Parse error at char $position: $message")
            val START = 0
            val AFTER_P = 1
            val AFTER_YEAR = 2
            val AFTER_MONTH = 3
            val AFTER_WEEK = 4
            val AFTER_DAY = 5
            val AFTER_T = 6
            val AFTER_HOUR = 7
            val AFTER_MINUTE = 8
            val AFTER_SECOND_AND_NANO = 9

            var state = START
            // next unread character
            var i = 0
            var sign = 1
            var years = 0
            var months = 0
            var weeks = 0
            var days = 0
            var hours = 0
            var minutes = 0
            var seconds = 0
            var nanoseconds = 0
            while (true) {
                if (i >= text.length) {
                    if (state == START)
                        parseException("Unexpected end of input; 'P' designator is required", i)
                    if (state == AFTER_T)
                        parseException("Unexpected end of input; at least one time component is required after 'T'", i)
                    val daysTotal = when (val n = days.toLong() + weeks * 7) {
                        in Int.MIN_VALUE..Int.MAX_VALUE -> n.toInt()
                        else -> parseException("The total number of days under 'D' and 'W' designators should fit into an Int", 0)
                    }
                    return DateTimePeriod(years, months, daysTotal, hours, minutes, seconds, nanoseconds.toLong())
                }
                if (state == START) {
                    if (i + 1 >= text.length && (text[i] == '+' || text[i] == '-'))
                        parseException("Unexpected end of string; 'P' designator is required", i)
                    when (text[i]) {
                        '+', '-' -> {
                            if (text[i] == '-')
                                sign = -1
                            if (text[i + 1] != 'P')
                                parseException("Expected 'P', got '${text[i + 1]}'", i + 1)
                            i += 2
                        }
                        'P' -> { i += 1 }
                        else -> parseException("Expected '+', '-', 'P', got '${text[i]}'", i)
                    }
                    state = AFTER_P
                    continue
                }
                var localSign = sign
                val iStart = i
                when (text[i]) {
                    '+', '-' -> {
                        if (text[i] == '-') localSign *= -1
                        i += 1
                        if (i >= text.length || text[i] !in '0'..'9')
                            parseException("A number expected after '${text[i]}'", i)
                    }
                    in '0'..'9' -> { }
                    'T' -> {
                        if (state >= AFTER_T)
                            parseException("Only one 'T' designator is allowed", i)
                        state = AFTER_T
                        i += 1
                        continue
                    }
                }
                var number = 0L
                while (i < text.length && text[i] in '0'..'9') {
                    try {
                        number = safeAdd(safeMultiply(number, 10), (text[i] - '0').toLong())
                    } catch (e: ArithmeticException) {
                        parseException("The number is too large", iStart)
                    }
                    i += 1
                }
                number *= localSign
                if (i == text.length)
                    parseException("Expected a designator after the numerical value", i)
                val wrongOrder = "Wrong component order: should be 'Y', 'M', 'W', 'D', then designator 'T', then 'H', 'M', 'S'"
                fun Long.toIntThrowing(component: Char): Int {
                    if (this < Int.MIN_VALUE || this > Int.MAX_VALUE)
                        parseException("Value $this does not fit into an Int, which is required for component '$component'", iStart)
                    return toInt()
                }
                when (text[i].toUpperCase()) {
                    'Y' -> {
                        if (state >= AFTER_YEAR)
                            parseException(wrongOrder, i)
                        state = AFTER_YEAR
                        years = number.toIntThrowing('Y')
                    }
                    'M' -> {
                        if (state >= AFTER_T) {
                            // Minutes
                            if (state >= AFTER_MINUTE)
                                parseException(wrongOrder, i)
                            state = AFTER_MINUTE
                            minutes = number.toIntThrowing('M')
                        } else {
                            // Months
                            if (state >= AFTER_MONTH)
                                parseException(wrongOrder, i)
                            state = AFTER_MONTH
                            months = number.toIntThrowing('M')
                        }
                    }
                    'W' -> {
                        if (state >= AFTER_WEEK)
                            parseException(wrongOrder, i)
                        state = AFTER_WEEK
                        weeks = number.toIntThrowing('W')
                    }
                    'D' -> {
                        if (state >= AFTER_DAY)
                            parseException(wrongOrder, i)
                        state = AFTER_DAY
                        days = number.toIntThrowing('D')
                    }
                    'H' -> {
                        if (state >= AFTER_HOUR || state < AFTER_T)
                            parseException(wrongOrder, i)
                        state = AFTER_HOUR
                        hours = number.toIntThrowing('H')
                    }
                    'S' -> {
                        if (state >= AFTER_SECOND_AND_NANO || state < AFTER_T)
                            parseException(wrongOrder, i)
                        state = AFTER_SECOND_AND_NANO
                        seconds = number.toIntThrowing('S')
                    }
                    '.', ',' -> {
                        i += 1
                        if (i >= text.length)
                            parseException("Expected designator 'S' after ${text[i - 1]}", i)
                        val iStartFraction = i
                        while (i < text.length && text[i] in '0'..'9')
                            i += 1
                        val fractionLength = i - iStartFraction
                        if (fractionLength > 9)
                            parseException("Only the nanosecond fractions of a second are supported", iStartFraction)
                        val fractionalPart = text.substring(iStartFraction, i) + "0".repeat(9 - fractionLength)
                        nanoseconds = fractionalPart.toInt(10) * localSign
                        if (text[i] != 'S')
                            parseException("Expected the 'S' designator after a fraction", i)
                        if (state >= AFTER_SECOND_AND_NANO || state < AFTER_T)
                            parseException(wrongOrder, i)
                        state = AFTER_SECOND_AND_NANO
                        seconds = number.toIntThrowing('S')
                    }
                    else -> parseException("Expected a designator after the numerical value", i)
                }
                i += 1
           }
        }
    }
}

public fun String.toDateTimePeriod(): DateTimePeriod = DateTimePeriod.parse(this)

public class DatePeriod internal constructor(
    internal override val totalMonths: Int,
    override val days: Int,
) : DateTimePeriod() {
    public constructor(years: Int = 0, months: Int = 0, days: Int = 0): this(totalMonths(years, months), days)
    // avoiding excessive computations
    override val hours: Int get() = 0
    override val minutes: Int get() = 0
    override val seconds: Int get() = 0
    override val nanoseconds: Int get() = 0
    internal override val totalNanoseconds: Long get() = 0

    public companion object {
        public fun parse(text: String): DatePeriod =
            when (val period = DateTimePeriod.parse(text)) {
                is DatePeriod -> period
                else -> throw DateTimeFormatException("Period $period (parsed from string $text) is not date-based")
            }
    }
}

public fun String.toDatePeriod(): DatePeriod = DatePeriod.parse(this)

private class DateTimePeriodImpl(
    internal override val totalMonths: Int,
    override val days: Int,
    internal override val totalNanoseconds: Long,
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

public fun DateTimePeriod(
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
public fun Duration.toDateTimePeriod(): DateTimePeriod = buildDateTimePeriod(totalNanoseconds = toLongNanoseconds())

public operator fun DateTimePeriod.plus(other: DateTimePeriod): DateTimePeriod = buildDateTimePeriod(
    safeAdd(totalMonths, other.totalMonths),
    safeAdd(days, other.days),
    safeAdd(totalNanoseconds, other.totalNanoseconds),
)

public operator fun DatePeriod.plus(other: DatePeriod): DatePeriod = DatePeriod(
    safeAdd(totalMonths, other.totalMonths),
    safeAdd(days, other.days),
)

