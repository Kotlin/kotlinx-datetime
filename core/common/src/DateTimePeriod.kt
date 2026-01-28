/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.DatePeriodIso8601Serializer
import kotlinx.datetime.serializers.DateTimePeriodIso8601Serializer
import kotlinx.datetime.serializers.DatePeriodComponentSerializer
import kotlinx.datetime.serializers.DatePeriodSerializer
import kotlinx.datetime.serializers.DateTimePeriodComponentSerializer
import kotlinx.datetime.serializers.DateTimePeriodSerializer
import kotlin.math.*
import kotlin.time.Duration
import kotlinx.serialization.Serializable

/**
 * A difference between two [instants][kotlin.time.Instant], decomposed into date and time components.
 *
 * The date components are: [years] ([DateTimeUnit.YEAR]), [months] ([DateTimeUnit.MONTH]), and [days] ([DateTimeUnit.DAY]).
 *
 * The time components are: [hours] ([DateTimeUnit.HOUR]), [minutes] ([DateTimeUnit.MINUTE]),
 * [seconds] ([DateTimeUnit.SECOND]), and [nanoseconds] ([DateTimeUnit.NANOSECOND]).
 *
 * The time components are not independent and are always normalized together.
 * Likewise, months are normalized together with years.
 * For example, there is no difference between `DateTimePeriod(months = 24, hours = 2, minutes = 63)` and
 * `DateTimePeriod(years = 2, hours = 3, minutes = 3)`.
 *
 * All components can also be negative: for example, `DateTimePeriod(months = -5, days = 6, hours = -3)`.
 * Whereas `months = 5` means "5 months after," `months = -5` means "5 months earlier."
 *
 * A constant time interval that consists of a single non-zero component (like "yearly" or "quarterly") should be
 * represented by a [DateTimeUnit] directly instead of a [DateTimePeriod]:
 * for example, instead of `DateTimePeriod(months = 6)`, one could use `DateTimeUnit.MONTH * 6`.
 * This provides a wider variety of operations: for example, finding how many such intervals fit between two instants
 * or dates or adding a multiple of such intervals at once.
 *
 * ### Interaction with other entities
 *
 * [DateTimePeriod] can be returned from [Instant.periodUntil], representing the difference between two instants.
 * Conversely, there is an [Instant.plus] overload that accepts a [DateTimePeriod] and returns a new instant.
 *
 * [DatePeriod] is a subtype of [DateTimePeriod] that only stores the date components and has all time components equal
 * to zero.
 *
 * [DateTimePeriod] can be thought of as a combination of a [Duration] and a [DatePeriod], as it contains both the
 * time components of [Duration] and the date components of [DatePeriod].
 * [Duration.toDateTimePeriod] can be used to convert a [Duration] to the corresponding [DateTimePeriod].
 *
 * ### Construction, serialization, and deserialization
 *
 * When a [DateTimePeriod] is constructed in any way, a [DatePeriod] value, which is a subtype of [DateTimePeriod],
 * will be returned if all time components happen to be zero.
 *
 * A `DateTimePeriod` can be constructed using the constructor function with the same name.
 * See sample 1.
 *
 * [parse] and [toString] methods can be used to obtain a [DateTimePeriod] from and convert it to a string in the
 * ISO 8601 extended format.
 * See sample 2.
 *
 * `DateTimePeriod` can also be returned as the result of instant arithmetic operations (see [Instant.periodUntil]).
 *
 * Additionally, there are several `kotlinx-serialization` serializers for [DateTimePeriod]:
 * - The default serializer, delegating to [toString] and [parse].
 * - [DateTimePeriodIso8601Serializer] for the ISO 8601 format.
 * - [DateTimePeriodComponentSerializer]  for an object with components.
 *
 * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.construction
 * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.simpleParsingAndFormatting
 */
@Serializable(with = DateTimePeriodSerializer::class)
// TODO: could be error-prone without explicitly named params
public sealed class DateTimePeriod {
    internal abstract val totalMonths: Long

    /**
     * The number of calendar days. Can be negative.
     *
     * Note that a calendar day is not identical to 24 hours, see [DateTimeUnit.DayBased] for details.
     * Also, this field does not get normalized together with months, so values larger than 31 can be present.
     *
     * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.valueNormalization
     */
    public abstract val days: Int
    internal abstract val totalNanoseconds: Long

    /**
     * The number of whole years. Can be negative.
     *
     * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.valueNormalization
     */
    public val years: Int get() = (totalMonths / 12).toInt()

    /**
     * The number of months in this period that don't form a whole year, so this value is always in `(-11..11)`.
     *
     * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.valueNormalization
     */
    public val months: Int get() = (totalMonths % 12).toInt()

    /**
     * The number of whole hours in this period. Can be negative.
     *
     * This field does not get normalized together with days, so values larger than 23 or smaller than -23 can be present.
     *
     * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.valueNormalization
     */
    public open val hours: Int get() = (totalNanoseconds / 3_600_000_000_000).toInt()

    /**
     * The number of whole minutes in this period that don't form a whole hour, so this value is always in `(-59..59)`.
     *
     * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.valueNormalization
     */
    public open val minutes: Int get() = ((totalNanoseconds % 3_600_000_000_000) / 60_000_000_000).toInt()

    /**
     * The number of whole seconds in this period that don't form a whole minute, so this value is always in `(-59..59)`.
     *
     * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.valueNormalization
     */
    public open val seconds: Int get() = ((totalNanoseconds % 60_000_000_000) / NANOS_PER_ONE).toInt()

    /**
     * The number of whole nanoseconds in this period that don't form a whole second, so this value is always in
     * `(-999_999_999..999_999_999)`.
     *
     * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.valueNormalization
     */
    public open val nanoseconds: Int get() = (totalNanoseconds % NANOS_PER_ONE).toInt()

    private fun allNonpositive() =
        totalMonths <= 0 && days <= 0 && totalNanoseconds <= 0 && (totalMonths or totalNanoseconds != 0L || days != 0)

    /**
     * Converts this period to the ISO 8601 string representation for durations, for example, `P2M1DT3H`.
     *
     * Note that the ISO 8601 duration is not the same as [Duration],
     * but instead includes the date components, like [DateTimePeriod] does.
     *
     * Examples of the output:
     * - `P2Y4M-1D`: two years, four months, minus one day;
     * - `-P2Y4M1D`: minus two years, minus four months, minus one day;
     * - `P1DT3H2M4.123456789S`: one day, three hours, two minutes, four seconds, 123456789 nanoseconds;
     * - `P1DT-3H-2M-4.123456789S`: one day, minus three hours, minus two minutes,
     *   minus four seconds, minus 123456789 nanoseconds;
     *
     * See ISO-8601-1:2019, 5.5.2.2a)
     *
     * @see DateTimePeriod.parse for the detailed description of the format.
     * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.toStringSample
     */
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
        var result = totalMonths.hashCode()
        result = 31 * result + days
        result = 31 * result + totalNanoseconds.hashCode()
        return result
    }

    public companion object {
        /**
         * Parses a ISO 8601 duration string as a [DateTimePeriod].
         *
         * If the time components are absent or equal to zero, returns a [DatePeriod].
         *
         * Note that the ISO 8601 duration is not the same as [Duration],
         * but instead includes the date components, like [DateTimePeriod] does.
         *
         * Examples of durations in the ISO 8601 format:
         * - `P1Y40D` is one year and 40 days
         * - `-P1DT1H` is minus (one day and one hour)
         * - `P1DT-1H` is one day minus one hour
         * - `-PT0.000000001S` is minus one nanosecond
         *
         * The format is defined as follows:
         * - First, optionally, a `-` or `+`.
         *   If `-` is present, the whole period after the `-` is negated: `-P-2M1D` is the same as `P2M-1D`.
         * - Then, the letter `P`.
         * - Optionally, the number of years, followed by `Y`.
         * - Optionally, the number of months, followed by `M`.
         * - Optionally, the number of weeks, followed by `W`.
         * - Optionally, the number of days, followed by `D`.
         * - The string can end here if there are no more time components.
         *   If there are time components, the letter `T` is required.
         * - Optionally, the number of hours, followed by `H`.
         * - Optionally, the number of minutes, followed by `M`.
         * - Optionally, the number of seconds, followed by `S`.
         *   Seconds can optionally have a fractional part with up to nine digits.
         *   The fractional part is separated with a `.`.
         *
         * An explicit `+` or `-` sign can be prepended to any number.
         * `-` means that the number is negative, and `+` has no effect.
         *
         * See ISO-8601-1:2019, 5.5.2.2a) and 5.5.2.2b).
         * We combine the two formats into one by allowing the number of weeks to go after the number of months
         * and before the number of days.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [DateTimePeriod] are
         * exceeded.
         * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.parsing
         */
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
            var someComponentParsed = false
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
                    if (!someComponentParsed)
                        parseException("At least one component is required, but none were found", 0)
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
                    } catch (_: ArithmeticException) {
                        parseException("The number is too large", iStart)
                    }
                    i += 1
                }
                someComponentParsed = true
                number *= localSign
                if (i == text.length)
                    parseException("Expected a designator after the numerical value", i)
                val wrongOrder = "Wrong component order: should be 'Y', 'M', 'W', 'D', then designator 'T', then 'H', 'M', 'S'"
                fun Long.toIntThrowing(component: Char): Int {
                    if (this < Int.MIN_VALUE || this > Int.MAX_VALUE)
                        parseException("Value $this does not fit into an Int, which is required for component '$component'", iStart)
                    return toInt()
                }
                when (text[i].uppercaseChar()) {
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

/**
 * @suppress
 */
@Deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339", ReplaceWith("DateTimePeriod.parse(this)"), DeprecationLevel.WARNING)
public fun String.toDateTimePeriod(): DateTimePeriod = DateTimePeriod.parse(this)

/**
 * A special case of [DateTimePeriod] that only stores the date components and has all time components equal to zero.
 *
 * A `DatePeriod` is automatically returned from all constructor functions for [DateTimePeriod] if it turns out that
 * the time components are zero.
 *
 * ```
 * DateTimePeriod.parse("P1Y3D") as DatePeriod // 1 year and 3 days
 * ```
 *
 * Additionally, [DatePeriod] has its own constructor, the [parse] function that will fail if any of the time components
 * are not zero, and [DatePeriodIso8601Serializer] and [DatePeriodComponentSerializer], mirroring those of
 * [DateTimePeriod].
 *
 * `DatePeriod` values are used in operations on [LocalDates][LocalDate] and are returned from operations
 * on [LocalDates][LocalDate], but they also can be passed anywhere a [DateTimePeriod] is expected.
 *
 * On the JVM, there are `DatePeriod.toJavaPeriod()` and `java.time.Period.toKotlinDatePeriod()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 *
 * @sample kotlinx.datetime.test.samples.DatePeriodSamples.simpleParsingAndFormatting
 */
@Serializable(with = DatePeriodSerializer::class)
public class DatePeriod internal constructor(
    internal override val totalMonths: Long,
    override val days: Int,
) : DateTimePeriod() {
    /**
     * Constructs a new [DatePeriod].
     *
     * It is recommended to always name the arguments explicitly when constructing this manually,
     * like `DatePeriod(years = 1, months = 12, days = 16)`.
     *
     * The passed numbers are not stored as is but are normalized instead for human readability, so, for example,
     * `DateTimePeriod(months = 24, days = 41)` becomes `DateTimePeriod(years = 2, days = 41)`.
     *
     * If only a single component is set and is always non-zero and is semantically a fixed time interval
     * (like "yearly" or "quarterly"), please consider using a multiple of [DateTimeUnit.DateBased] instead.
     * For example, instead of `DatePeriod(months = 6)`, one can use `DateTimeUnit.MONTH * 6`.
     *
     * @throws IllegalArgumentException if the total number of years
     * (together with full years in [months]) overflows an [Int].
     * @sample kotlinx.datetime.test.samples.DatePeriodSamples.construction
     */
    public constructor(years: Int = 0, months: Int = 0, days: Int = 0): this(totalMonths(years, months), days)
    // avoiding excessive computations
    /** The number of whole hours in this period. Always equal to zero. */
    override val hours: Int get() = 0

    /** The number of whole minutes in this period. Always equal to zero. */
    override val minutes: Int get() = 0

    /** The number of whole seconds in this period. Always equal to zero. */
    override val seconds: Int get() = 0

    /** The number of nanoseconds in this period. Always equal to zero. */
    override val nanoseconds: Int get() = 0
    internal override val totalNanoseconds: Long get() = 0

    public companion object {
        /**
         * Parses the ISO 8601 duration representation as a [DatePeriod], for example, `P1Y2M30D`.
         *
         * This function is equivalent to [DateTimePeriod.parse], but will fail if any of the time components are not
         * zero.
         *
         * @throws IllegalArgumentException if the text cannot be parsed, the boundaries of [DatePeriod] are exceeded,
         * or any time components are not zero.
         *
         * @see DateTimePeriod.parse
         * @sample kotlinx.datetime.test.samples.DatePeriodSamples.parsing
         */
        public fun parse(text: String): DatePeriod =
            when (val period = DateTimePeriod.parse(text)) {
                is DatePeriod -> period
                else -> throw DateTimeFormatException("Period $period (parsed from string $text) is not date-based")
            }
    }
}

/**
 * @suppress
 */
@Deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339", ReplaceWith("DatePeriod.parse(this)"), DeprecationLevel.WARNING)
public fun String.toDatePeriod(): DatePeriod = DatePeriod.parse(this)

private class DateTimePeriodImpl(
    internal override val totalMonths: Long,
    override val days: Int,
    internal override val totalNanoseconds: Long,
) : DateTimePeriod()

private fun totalMonths(years: Int, months: Int): Long = (years.toLong() * 12 + months.toLong()).also {
    require(it / 12 in Int.MIN_VALUE..Int.MAX_VALUE) {
        "The total number of years in $years years and $months months overflows an Int"
    }
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
    } catch (_: ArithmeticException) {
        throw IllegalArgumentException("The total number of nanoseconds in $hours hours, $minutes minutes, $seconds seconds, and $nanoseconds nanoseconds overflows a Long")
    }
}

internal fun buildDateTimePeriod(totalMonths: Long = 0, days: Int = 0, totalNanoseconds: Long): DateTimePeriod =
    if (totalNanoseconds != 0L)
        DateTimePeriodImpl(totalMonths, days, totalNanoseconds)
    else
        DatePeriod(totalMonths, days)

/**
 * Constructs a new [DateTimePeriod]. If all the time components are zero, returns a [DatePeriod].
 *
 * It is recommended to always name the arguments explicitly when constructing this manually,
 * like `DateTimePeriod(years = 1, months = 12, days = 16)`.
 *
 * The passed numbers are not stored as is but are normalized instead for human readability, so, for example,
 * `DateTimePeriod(months = 24, days = 41)` becomes `DateTimePeriod(years = 2, days = 41)`.
 *
 * If only a single component is set and is always non-zero and is semantically a fixed time interval
 * (like "yearly" or "quarterly"), please consider using a multiple of [DateTimeUnit] instead.
 * For example, instead of `DateTimePeriod(months = 6)`, one can use `DateTimeUnit.MONTH * 6`.
 *
 * @throws IllegalArgumentException if the total number of years
 * (together with full years in [months]) overflows an [Int].
 * @throws IllegalArgumentException if the total number of nanoseconds in
 * [hours], [minutes], [seconds] and [nanoseconds] overflows a [Long].
 * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.constructorFunction
 */
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

/**
 * Constructs a [DateTimePeriod] from a [Duration].
 *
 * If the duration value is too big to be represented as a [Long] number of nanoseconds,
 * the result will be [Long.MAX_VALUE] nanoseconds.
 *
 * **Pitfall**: a [DateTimePeriod] obtained this way will always have zero date components.
 * The reason is that even a [Duration] obtained via [Duration.Companion.days] just means a multiple of 24 hours,
 * whereas in `kotlinx-datetime`, a day is a calendar day, which can be different from 24 hours.
 * See [DateTimeUnit.DayBased] for details.
 *
 * @sample kotlinx.datetime.test.samples.DateTimePeriodSamples.durationToDateTimePeriod
 */
// TODO: maybe it's more consistent to throw here on overflow?
public fun Duration.toDateTimePeriod(): DateTimePeriod = buildDateTimePeriod(totalNanoseconds = inWholeNanoseconds)

/**
 * Adds two [DateTimePeriod] instances.
 *
 * **Pitfall**: given three instants, adding together the periods between the first and the second and between the
 * second and the third *does not* necessarily equal the period between the first and the third.
 *
 * @throws DateTimeArithmeticException if arithmetic overflow happens.
 */
@Deprecated(
    "Adding periods is not a well-defined operation. See https://github.com/Kotlin/kotlinx-datetime/issues/381",
    level = DeprecationLevel.WARNING
)
public operator fun DateTimePeriod.plus(other: DateTimePeriod): DateTimePeriod = buildDateTimePeriod(
    safeAdd(totalMonths, other.totalMonths),
    safeAdd(days, other.days),
    safeAdd(totalNanoseconds, other.totalNanoseconds),
)

/**
 * Adds two [DatePeriod] instances.
 *
 * **Pitfall**: given three dates, adding together the periods between the first and the second and between the
 * second and the third *does not* necessarily equal the period between the first and the third.
 *
 * @throws DateTimeArithmeticException if arithmetic overflow happens.
 */
@Deprecated(
    "Adding periods is not a well-defined operation. See https://github.com/Kotlin/kotlinx-datetime/issues/381",
    level = DeprecationLevel.WARNING
)
public operator fun DatePeriod.plus(other: DatePeriod): DatePeriod = DatePeriod(
    safeAdd(totalMonths, other.totalMonths),
    safeAdd(days, other.days),
)
