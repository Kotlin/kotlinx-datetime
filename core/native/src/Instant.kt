/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.math.absoluteValue

public actual enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;
}

/**
 * The minimum supported epoch second.
 */
private const val MIN_SECOND = -31619119219200L // -1000000-01-01T00:00:00Z

/**
 * The maximum supported epoch second.
 */
private const val MAX_SECOND = 31494816403199L // +1000000-12-31T23:59:59

private fun isValidInstantSecond(second: Long) = second >= MIN_SECOND && second <= MAX_SECOND

@Serializable(with = InstantIso8601Serializer::class)
public actual class Instant internal constructor(public actual val epochSeconds: Long, public actual val nanosecondsOfSecond: Int) : Comparable<Instant> {

    init {
        require(isValidInstantSecond(epochSeconds)) { "Instant exceeds minimum or maximum instant" }
    }

    // org.threeten.bp.Instant#toEpochMilli
    public actual fun toEpochMilliseconds(): Long =
        epochSeconds * MILLIS_PER_ONE + nanosecondsOfSecond / NANOS_PER_MILLI

    // org.threeten.bp.Instant#plus(long, long)
    /**
     * @throws ArithmeticException if arithmetic overflow occurs
     * @throws IllegalArgumentException if the boundaries of Instant are overflown
     */
    internal fun plus(secondsToAdd: Long, nanosToAdd: Long): Instant {
        if ((secondsToAdd or nanosToAdd) == 0L) {
            return this
        }
        val newEpochSeconds: Long = safeAdd(safeAdd(epochSeconds, secondsToAdd), (nanosToAdd / NANOS_PER_ONE))
        val newNanosToAdd = nanosToAdd % NANOS_PER_ONE
        val nanoAdjustment = (nanosecondsOfSecond + newNanosToAdd) // safe int+NANOS_PER_ONE
        return fromEpochSecondsThrowing(newEpochSeconds, nanoAdjustment)
    }

    public actual operator fun plus(duration: Duration): Instant = duration.toComponents { secondsToAdd, nanosecondsToAdd ->
        try {
            plus(secondsToAdd, nanosecondsToAdd.toLong())
        } catch (e: IllegalArgumentException) {
            if (duration.isPositive()) MAX else MIN
        } catch (e: ArithmeticException) {
            if (duration.isPositive()) MAX else MIN
        }
    }

    public actual operator fun minus(duration: Duration): Instant = plus(-duration)

    public actual operator fun minus(other: Instant): Duration =
        (this.epochSeconds - other.epochSeconds).seconds + // won't overflow given the instant bounds
        (this.nanosecondsOfSecond - other.nanosecondsOfSecond).nanoseconds

    actual override fun compareTo(other: Instant): Int {
        val s = epochSeconds.compareTo(other.epochSeconds)
        if (s != 0) {
            return s
        }
        return nanosecondsOfSecond.compareTo(other.nanosecondsOfSecond)
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is Instant && this.epochSeconds == other.epochSeconds && this.nanosecondsOfSecond == other.nanosecondsOfSecond

    // org.threeten.bp.Instant#hashCode
    override fun hashCode(): Int =
        (epochSeconds xor (epochSeconds ushr 32)).toInt() + 51 * nanosecondsOfSecond

    // org.threeten.bp.format.DateTimeFormatterBuilder.InstantPrinterParser#print
    actual override fun toString(): String = format(ISO_DATE_TIME_OFFSET_WITH_TRAILING_ZEROS)

    public actual companion object {
        internal actual val MIN = Instant(MIN_SECOND, 0)
        internal actual val MAX = Instant(MAX_SECOND, 999_999_999)

        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        public actual fun now(): Instant = currentTime()

        // org.threeten.bp.Instant#ofEpochMilli
        public actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant =
            if (epochMilliseconds < MIN_SECOND * MILLIS_PER_ONE) MIN
            else if (epochMilliseconds > MAX_SECOND * MILLIS_PER_ONE) MAX
            else Instant(
                epochMilliseconds.floorDiv(MILLIS_PER_ONE.toLong()),
                (epochMilliseconds.mod(MILLIS_PER_ONE.toLong()) * NANOS_PER_MILLI).toInt()
            )

        /**
         * @throws ArithmeticException if arithmetic overflow occurs
         * @throws IllegalArgumentException if the boundaries of Instant are overflown
         */
        private fun fromEpochSecondsThrowing(epochSeconds: Long, nanosecondAdjustment: Long): Instant {
            val secs = safeAdd(epochSeconds, nanosecondAdjustment.floorDiv(NANOS_PER_ONE.toLong()))
            val nos = nanosecondAdjustment.mod(NANOS_PER_ONE.toLong()).toInt()
            return Instant(secs, nos)
        }

        // org.threeten.bp.Instant#ofEpochSecond(long, long)
        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant =
            try {
                fromEpochSecondsThrowing(epochSeconds, nanosecondAdjustment)
            } catch (e: ArithmeticException) {
                if (epochSeconds > 0) MAX else MIN
            } catch (e: IllegalArgumentException) {
                if (epochSeconds > 0) MAX else MIN
            }

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant =
            fromEpochSeconds(epochSeconds, nanosecondAdjustment.toLong())

        public actual fun parse(input: CharSequence, format: DateTimeFormat<DateTimeComponents>): Instant = try {
            format.parse(input).toInstantUsingOffset()
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Failed to parse an instant from '$input'", e)
        }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): Instant = parse(input = isoString)

        public actual val DISTANT_PAST: Instant = fromEpochSeconds(DISTANT_PAST_SECONDS, 999_999_999)

        public actual val DISTANT_FUTURE: Instant = fromEpochSeconds(DISTANT_FUTURE_SECONDS, 0)
    }

}

private class UnboundedLocalDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val nanosecond: Int,
) {
    fun toInstant(offsetSeconds: Int): Instant {
        val epochSeconds = run {
            // org.threeten.bp.LocalDate#toEpochDay
            val epochDays = run {
                val y = year
                var total = 365 * y
                if (y >= 0) {
                    total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
                } else {
                    total -= y / -4 - y / -100 + y / -400
                }
                total += ((367 * month - 362) / 12)
                total += day - 1
                if (month > 2) {
                    total--
                    if (!isLeapYear(year)) {
                        total--
                    }
                }
                total - DAYS_0000_TO_1970
            }
            // org.threeten.bp.LocalTime#toSecondOfDay
            val daySeconds = hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second
            // org.threeten.bp.chrono.ChronoLocalDateTime#toEpochSecond
            epochDays * 86400L + daySeconds - offsetSeconds
        }
        if (epochSeconds < Instant.MIN.epochSeconds || epochSeconds > Instant.MAX.epochSeconds)
            throw DateTimeFormatException(
                "The parsed date is outside the range representable by Instant (Unix epoch second $epochSeconds)"
            )
        return Instant.fromEpochSeconds(epochSeconds, nanosecond)
    }

    companion object {
        fun fromInstant(instant: Instant, offsetSeconds: Int): UnboundedLocalDateTime {
            val localSecond: Long = instant.epochSeconds + offsetSeconds
            val epochDays = localSecond.floorDiv(SECONDS_PER_DAY.toLong()).toInt()
            val secsOfDay = localSecond.mod(SECONDS_PER_DAY.toLong()).toInt()
            val year: Int
            val month: Int
            val day: Int
            // org.threeten.bp.LocalDate#toEpochDay
            run {
                var zeroDay = epochDays + DAYS_0000_TO_1970
                // find the march-based year
                zeroDay -= 60 // adjust to 0000-03-01 so leap day is at end of four year cycle

                var adjust = 0
                if (zeroDay < 0) { // adjust negative years to positive for calculation
                    val adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                    adjust = adjustCycles * 400
                    zeroDay += -adjustCycles * DAYS_PER_CYCLE
                }
                var yearEst = ((400 * zeroDay.toLong() + 591) / DAYS_PER_CYCLE).toInt()
                var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
                if (doyEst < 0) { // fix estimate
                    yearEst--
                    doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
                }
                yearEst += adjust // reset any negative year

                val marchDoy0 = doyEst

                // convert march-based values back to january-based
                val marchMonth0 = (marchDoy0 * 5 + 2) / 153
                month = (marchMonth0 + 2) % 12 + 1
                day = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
                year = yearEst + marchMonth0 / 10
            }
            val hours = (secsOfDay / SECONDS_PER_HOUR)
            val secondWithoutHours = secsOfDay - hours * SECONDS_PER_HOUR
            val minutes = (secondWithoutHours / SECONDS_PER_MINUTE)
            val second = secondWithoutHours - minutes * SECONDS_PER_MINUTE
            return UnboundedLocalDateTime(year, month, day, hours, minutes, second, instant.nanosecondsOfSecond)
        }
    }
}

internal fun parseIso(isoString: String): Instant {
    fun parseFailure(error: String): Nothing {
        throw IllegalArgumentException("$error when parsing an Instant from $isoString")
    }
    inline fun expect(what: String, where: Int, predicate: (Char) -> Boolean) {
        val c = isoString[where]
        if (!predicate(c)) {
            parseFailure("Expected $what, but got $c at position $where")
        }
    }
    val s = isoString
    var i = 0
    require(s.isNotEmpty()) { "An empty string is not a valid Instant" }
    val yearSign = when (val c = s[i]) {
        '+', '-' -> { ++i; c }
        else -> ' '
    }
    val yearStart = i
    var absYear = 0
    while (i < s.length && s[i] in '0'..'9') {
        absYear = absYear * 10 + (s[i] - '0')
        ++i
    }
    val year = when {
        i > yearStart + 9 -> {
            parseFailure("Expected at most 9 digits for the year number, got ${i - yearStart}")
        }
        i - yearStart < 4 -> {
            parseFailure("The year number must be padded to 4 digits, got ${i - yearStart} digits")
        }
        else -> {
            if (yearSign == '+' && i - yearStart == 4) {
                parseFailure("The '+' sign at the start is only valid for year numbers longer than 4 digits")
            }
            if (yearSign == ' ' && i - yearStart != 4) {
                parseFailure("A '+' or '-' sign is required for year numbers longer than 4 digits")
            }
            if (yearSign == '-') -absYear else absYear
        }
    }
    // reading at least -MM-DDTHH:MM:SSZ
    //                  0123456789012345 16 chars
    if (s.length < i + 16) {
        parseFailure("The input string is too short")
    }
    expect("'-'", i) { it == '-' }
    expect("'-'", i + 3) { it == '-' }
    expect("'T' or 't'", i + 6) { it == 'T' || it == 't' }
    expect("':'", i + 9) { it == ':' }
    expect("':'", i + 12) { it == ':' }
    for (j in listOf(1, 2, 4, 5, 7, 8, 10, 11, 13, 14)) {
        expect("an ASCII digit", i + j) { it in '0'..'9' }
    }
    fun twoDigitNumber(index: Int) = s[index].code * 10 + s[index + 1].code - '0'.code * 11
    val month = twoDigitNumber(i + 1)
    val day = twoDigitNumber(i + 4)
    val hour = twoDigitNumber(i + 7)
    val minute = twoDigitNumber(i + 10)
    val second = twoDigitNumber(i + 13)
    val nanosecond = if (s[i + 15] == '.') {
        val fractionStart = i + 16
        i = fractionStart
        var fraction = 0
        while (i < s.length && s[i] in '0'..'9') {
            fraction = fraction * 10 + (s[i] - '0')
            ++i
        }
        if (i - fractionStart in 1..9) {
            fraction * POWERS_OF_TEN[fractionStart + 9 - i]
        } else {
            parseFailure("1..9 digits are supported for the fraction of the second, got {i - fractionStart}")
        }
    } else {
        i += 15
        0
    }
    val offsetSeconds = when (val sign = s.getOrNull(i)) {
        null -> {
            parseFailure("The UTC offset at the end of the string is missing")
        }
        'z', 'Z' -> if (s.length == i + 1) {
            0
        } else {
            parseFailure("Extra text after the instant at position ${i + 1}")
        }
        '-', '+' -> {
            val offsetStrLength = s.length - i
            if (offsetStrLength % 3 != 0) { parseFailure("Invalid UTC offset string '${s.substring(i)}'") }
            if (offsetStrLength > 9) { parseFailure("The UTC offset string '${s.substring(i)}' is too long") }
            for (j in listOf(3, 6)) {
                if (s.getOrNull(i + j) ?: break != ':')
                    parseFailure("Expected ':' at index ${i + j}, got '${s[i + j]}'")
            }
            for (j in listOf(1, 2, 4, 5, 7, 8)) {
                if (s.getOrNull(i + j) ?: break !in '0'..'9')
                    parseFailure("Expected a digit at index ${i + j}, got '${s[i + j]}'")
            }
            val offsetHour = twoDigitNumber(i + 1)
            val offsetMinute = if (offsetStrLength > 3) { twoDigitNumber(i + 4) } else { 0 }
            val offsetSecond = if (offsetStrLength > 6) { twoDigitNumber(i + 7) } else { 0 }
            if (offsetMinute > 59) { parseFailure("Expected offset-minute-of-hour in 0..59, got $offsetMinute") }
            if (offsetSecond > 59) { parseFailure("Expected offset-second-of-minute in 0..59, got $offsetSecond") }
            if (offsetHour > 17 && !(offsetHour == 18 && offsetMinute == 0 && offsetSecond == 0)) {
                parseFailure("Expected an offset in -18:00..+18:00, got $sign$offsetHour:$offsetMinute:$offsetSecond")
            }
            (offsetHour * 3600 + offsetMinute * 60 + offsetSecond) * if (sign == '-') -1 else 1
        }
        else -> {
            parseFailure("Expected the UTC offset at position $i, got '$sign'")
        }
    }
    if (month !in 1..12) { parseFailure("Expected a month number in 1..12, got $month") }
    if (day !in 1..month.monthLength(isLeapYear(year))) {
        parseFailure("Expected a valid day-of-month for $year-$month, got $day")
    }
    if (hour > 23) { parseFailure("Expected hour in 0..23, got $hour") }
    if (minute > 59) { parseFailure("Expected minute-of-hour in 0..59, got $minute") }
    if (second > 59) { parseFailure("Expected second-of-minute in 0..59, got $second") }
    return UnboundedLocalDateTime(year, month, day, hour, minute, second, nanosecond).toInstant(offsetSeconds)
}

internal fun formatIso(instant: Instant): String = buildString {
    val ldt = UnboundedLocalDateTime.fromInstant(instant, 0)
    fun Appendable.appendTwoDigits(number: Int) {
        if (number < 10) append('0')
        append(number)
    }
    run {
        val number = ldt.year
        when {
            number.absoluteValue < 1_000 -> {
                val innerBuilder = StringBuilder()
                if (number >= 0) {
                    innerBuilder.append((number + 10_000)).deleteAt(0)
                } else {
                    innerBuilder.append((number - 10_000)).deleteAt(1)
                }
                append(innerBuilder)
            }
            else -> {
                if (number >= 10_000) append('+')
                append(number)
            }
        }
    }
    append('-')
    appendTwoDigits(ldt.month)
    append('-')
    appendTwoDigits(ldt.day)
    append('T')
    appendTwoDigits(ldt.hour)
    append(':')
    appendTwoDigits(ldt.minute)
    append(':')
    appendTwoDigits(ldt.second)
    if (ldt.nanosecond != 0) {
        append('.')
        var zerosToStrip = 0
        while (ldt.nanosecond % POWERS_OF_TEN[zerosToStrip + 1] == 0) {
            ++zerosToStrip
        }
        zerosToStrip -= (zerosToStrip.mod(3)) // rounding down to a multiple of 3
        val numberToOutput = ldt.nanosecond / POWERS_OF_TEN[zerosToStrip]
        append((numberToOutput + POWERS_OF_TEN[9 - zerosToStrip]).toString().substring(1))
    }
    append('Z')
}

private fun Instant.toZonedDateTimeFailing(zone: TimeZone): ZonedDateTime = try {
    toZonedDateTime(zone)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Can not convert instant $this to LocalDateTime to perform computations", e)
}

/**
 * @throws IllegalArgumentException if the [Instant] exceeds the boundaries of [LocalDateTime]
 */
private fun Instant.toZonedDateTime(zone: TimeZone): ZonedDateTime {
    val currentOffset = zone.offsetAt(this)
    return ZonedDateTime(toLocalDateTimeImpl(currentOffset), zone, currentOffset)
}

/** Check that [Instant] fits in [ZonedDateTime].
 * This is done on the results of computations for consistency with other platforms.
 */
private fun Instant.check(zone: TimeZone): Instant = this@check.also {
    toZonedDateTimeFailing(zone)
}

public actual fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant = try {
    with(period) {
        val withDate = toZonedDateTimeFailing(timeZone)
            .run { if (totalMonths != 0) plus(totalMonths, DateTimeUnit.MONTH) else this }
            .run { if (days != 0) plus(days, DateTimeUnit.DAY) else this }
        withDate.toInstant()
            .run { if (totalNanoseconds != 0L) plus(0, totalNanoseconds).check(timeZone) else this }
    }.check(timeZone)
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding CalendarPeriod to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding CalendarPeriod", e)
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit, timeZone)"))
public actual fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(1L, unit, timeZone)
public actual fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(value.toLong(), unit, timeZone)
public actual fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(-value.toLong(), unit, timeZone)
public actual fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant = try {
    when (unit) {
        is DateTimeUnit.DateBased -> {
            if (value < Int.MIN_VALUE || value > Int.MAX_VALUE)
                throw ArithmeticException("Can't add a Long date-based value, as it would cause an overflow")
            toZonedDateTimeFailing(timeZone).plus(value.toInt(), unit).toInstant()
        }
        is DateTimeUnit.TimeBased ->
            check(timeZone).plus(value, unit).check(timeZone)
    }
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding a value", e)
}

public actual fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    try {
        multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (seconds, nanoseconds) ->
            plus(seconds, nanoseconds)
        }
    } catch (e: ArithmeticException) {
        if (value > 0) Instant.MAX else Instant.MIN
    } catch (e: IllegalArgumentException) {
        if (value > 0) Instant.MAX else Instant.MIN
    }

public actual fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod {
    var thisLdt = toZonedDateTimeFailing(timeZone)
    val otherLdt = other.toZonedDateTimeFailing(timeZone)

    val months = thisLdt.until(otherLdt, DateTimeUnit.MONTH).toInt() // `until` on dates never fails
    thisLdt = thisLdt.plus(months, DateTimeUnit.MONTH) // won't throw: thisLdt + months <= otherLdt, which is known to be valid
    val days = thisLdt.until(otherLdt, DateTimeUnit.DAY).toInt() // `until` on dates never fails
    thisLdt = thisLdt.plus(days, DateTimeUnit.DAY) // won't throw: thisLdt + days <= otherLdt
    val nanoseconds = thisLdt.until(otherLdt, DateTimeUnit.NANOSECOND) // |otherLdt - thisLdt| < 24h

    return buildDateTimePeriod(months, days, nanoseconds)
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long =
    when (unit) {
        is DateTimeUnit.DateBased ->
            toZonedDateTimeFailing(timeZone).dateTime.until(other.toZonedDateTimeFailing(timeZone).dateTime, unit)
                .toLong()
        is DateTimeUnit.TimeBased -> {
            check(timeZone); other.check(timeZone)
            until(other, unit)
        }
    }

private val ISO_DATE_TIME_OFFSET_WITH_TRAILING_ZEROS = DateTimeComponents.Format {
    date(ISO_DATE)
    alternativeParsing({
        char('t')
    }) {
        char('T')
    }
    hour()
    char(':')
    minute()
    char(':')
    second()
    optional {
        char('.')
        secondFractionInternal(1, 9, FractionalSecondDirective.GROUP_BY_THREE)
    }
    isoOffset(
        zOnZero = true,
        useSeparator = true,
        outputMinute = WhenToOutput.IF_NONZERO,
        outputSecond = WhenToOutput.IF_NONZERO
    )
}
