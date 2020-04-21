/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.*
import kotlin.time.*

public actual enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;
}

// This is a function and not a value due to https://github.com/Kotlin/kotlinx-datetime/issues/5
// org.threeten.bp.format.DateTimeFormatterBuilder.InstantPrinterParser#parse
private val instantParser: Parser<Instant>
    get() = localDateParser
        .chainIgnoring(concreteCharParser('T').or(concreteCharParser('t')))
        .chain(intParser(2, 2)) // hour
        .chainIgnoring(concreteCharParser(':'))
        .chain(intParser(2, 2)) // minute
        .chainIgnoring(concreteCharParser(':'))
        .chain(intParser(2, 2)) // second
        .chain(optional(
            concreteCharParser('.')
                .chainSkipping(fractionParser(0, 9, 9)) // nanos
        ))
        .chainIgnoring(concreteCharParser('Z').or(concreteCharParser('z')))
        .map {
            val (dateHourMinuteSecond, nanosVal) = it
            val (dateHourMinute, secondsVal) = dateHourMinuteSecond
            val (dateHour, minutesVal) = dateHourMinute
            val (dateVal, hoursVal) = dateHour

            val nano = nanosVal ?: 0
            val (days, hours, min, seconds) = if (hoursVal == 24 && minutesVal == 0 && secondsVal == 0 && nano == 0) {
                listOf(1, 0, 0, 0)
            } else if (hoursVal == 23 && minutesVal == 59 && secondsVal == 60) {
                // parsed a leap second, but it seems it isn't used
                listOf(0, 23, 59, 59)
            } else {
                listOf(0, hoursVal, minutesVal, secondsVal)
            }

            // never fails: 9_999 years are always supported
            val localDate = dateVal.withYear(dateVal.year % 10000).plus(days, DateTimeUnit.DAY)
            val localTime = LocalTime.of(hours, min, seconds, 0)
            val secDelta: Long = try {
                safeMultiply((dateVal.year / 10000).toLong(), SECONDS_PER_10000_YEARS)
            } catch (e: ArithmeticException) {
                throw DateTimeFormatException(e)
            }
            val epochDay = localDate.toEpochDay().toLong()
            val instantSecs = epochDay * 86400 + localTime.toSecondOfDay() + secDelta
            try {
                Instant(instantSecs, nano)
            } catch (e: IllegalArgumentException) {
                throw DateTimeFormatException(e)
            }
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

@OptIn(ExperimentalTime::class)
public actual class Instant internal constructor(actual val epochSeconds: Long, actual val nanosecondsOfSecond: Int) : Comparable<Instant> {

    init {
        require(isValidInstantSecond(epochSeconds)) { "Instant exceeds minimum or maximum instant" }
    }

    // org.threeten.bp.Instant#toEpochMilli
    actual fun toEpochMilliseconds(): Long =
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

    actual operator fun plus(duration: Duration): Instant = duration.toComponents { secondsToAdd, nanosecondsToAdd ->
        try {
            plus(secondsToAdd, nanosecondsToAdd.toLong())
        } catch (e: IllegalArgumentException) {
            if (secondsToAdd > 0) MAX else MIN
        } catch (e: ArithmeticException) {
            if (secondsToAdd > 0) MAX else MIN
        }
    }

    actual operator fun minus(duration: Duration): Instant = plus(-duration)

    actual operator fun minus(other: Instant): Duration =
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
    override fun toString(): String {
        val buf = StringBuilder()
        val inNano: Int = nanosecondsOfSecond
        if (epochSeconds >= -SECONDS_0000_TO_1970) { // current era
            val zeroSecs: Long = epochSeconds - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970
            val hi: Long = floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1
            val lo: Long = floorMod(zeroSecs, SECONDS_PER_10000_YEARS)
            val ldt: LocalDateTime = Instant(lo - SECONDS_0000_TO_1970, 0)
                .toLocalDateTime(TimeZone.UTC)
            if (hi > 0) {
                buf.append('+').append(hi)
            }
            buf.append(ldt)
            if (ldt.second == 0) {
                buf.append(":00")
            }
        } else { // before current era
            val zeroSecs: Long = epochSeconds + SECONDS_0000_TO_1970
            val hi: Long = zeroSecs / SECONDS_PER_10000_YEARS
            val lo: Long = zeroSecs % SECONDS_PER_10000_YEARS
            val ldt: LocalDateTime = Instant(lo - SECONDS_0000_TO_1970, 0)
                .toLocalDateTime(TimeZone.UTC)
            val pos = buf.length
            buf.append(ldt)
            if (ldt.second == 0) {
                buf.append(":00")
            }
            if (hi < 0) {
                when {
                    ldt.year == -10000 -> {
                        buf.deleteCharAt(pos)
                        buf.deleteCharAt(pos)
                        buf.insert(pos, (hi - 1).toString())
                    }
                    lo == 0L -> {
                        buf.insert(pos, hi)
                    }
                    else -> {
                        buf.insert(pos + 1, abs(hi))
                    }
                }
            }
        }
        //fraction
        if (inNano != 0) {
            buf.append('.')
            when {
                inNano % 1000000 == 0 -> {
                    buf.append((inNano / 1000000 + 1000).toString().substring(1))
                }
                inNano % 1000 == 0 -> {
                    buf.append((inNano / 1000 + 1000000).toString().substring(1))
                }
                else -> {
                    buf.append((inNano + 1000000000).toString().substring(1))
                }
            }
        }
        buf.append('Z')
        return buf.toString()
    }

    actual companion object {
        internal actual val MIN = Instant(MIN_SECOND, 0)
        internal actual val MAX = Instant(MAX_SECOND, 999_999_999)

        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        actual fun now(): Instant = memScoped {
            val timespecBuf = alloc<timespec>()
            val error = clock_gettime(CLOCK_REALTIME, timespecBuf.ptr)
            assert(error == 0)
            // according to https://en.cppreference.com/w/c/chrono/timespec,
            // tv_nsec in [0; 10^9), so no need to call [ofEpochSecond].
            val seconds = timespecBuf.tv_sec.convert<Long>()
            val nanosec = timespecBuf.tv_nsec.toInt()
            try {
                Instant(seconds, nanosec)
            } catch (e: IllegalArgumentException) {
                throw IllegalStateException("The readings from the system clock are not representable as an Instant")
            }
        }

        // org.threeten.bp.Instant#ofEpochMilli
        actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant =
            if (epochMilliseconds < MIN_SECOND * MILLIS_PER_ONE) MIN
            else if (epochMilliseconds > MAX_SECOND * MILLIS_PER_ONE) MAX
            else Instant(floorDiv(epochMilliseconds, MILLIS_PER_ONE.toLong()),
                (floorMod(epochMilliseconds, MILLIS_PER_ONE.toLong()) * NANOS_PER_MILLI).toInt())

        /**
         * @throws ArithmeticException if arithmetic overflow occurs
         * @throws IllegalArgumentException if the boundaries of Instant are overflown
         */
        private fun fromEpochSecondsThrowing(epochSeconds: Long, nanosecondAdjustment: Long): Instant {
            val secs = safeAdd(epochSeconds, floorDiv(nanosecondAdjustment, NANOS_PER_ONE.toLong()))
            val nos = floorMod(nanosecondAdjustment, NANOS_PER_ONE.toLong()).toInt()
            return Instant(secs, nos)
        }

        // org.threeten.bp.Instant#ofEpochSecond(long, long)
        actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant =
            try {
                fromEpochSecondsThrowing(epochSeconds, nanosecondAdjustment)
            } catch (e: ArithmeticException) {
                if (epochSeconds > 0) MAX else MIN
            } catch (e: IllegalArgumentException) {
                if (epochSeconds > 0) MAX else MIN
            }

        actual fun parse(isoString: String): Instant =
            instantParser.parse(isoString)

        @SharedImmutable
        actual val DISTANT_PAST: Instant = fromEpochSeconds(DISTANT_PAST_SECONDS, 999_999_999)

        @SharedImmutable
        actual val DISTANT_FUTURE: Instant = fromEpochSeconds(DISTANT_FUTURE_SECONDS, 0)
    }

}

private fun Instant.toZonedLocalDateTimeFailing(zone: TimeZone): ZonedDateTime = try {
    toZonedLocalDateTime(zone)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Can not convert instant $this to LocalDateTime to perform computations", e)
}

/** Check that [Instant] fits in [ZonedDateTime].
 * This is done on the results of computations for consistency with other platforms.
 */
private fun Instant.check(zone: TimeZone): Instant = this@check.also {
    toZonedLocalDateTimeFailing(zone)
}

actual fun Instant.plus(period: DateTimePeriod, zone: TimeZone): Instant = try {
    with(period) {
        val withDate = toZonedLocalDateTimeFailing(zone)
            .run { if (years != 0 && months == 0) plus(years, DateTimeUnit.YEAR) else this }
            .run { if (months != 0) plus(safeAdd(safeMultiply(years, 12), months), DateTimeUnit.MONTH) else this }
            .run { if (days != 0) plus(days, DateTimeUnit.DAY) else this }
        withDate.toInstant()
            .run { if (hours != 0)
                plus(hours.toLong() * SECONDS_PER_HOUR, 0).check(zone) else this }
            .run { if (minutes != 0)
                plus(minutes.toLong() * SECONDS_PER_MINUTE, 0).check(zone) else this }
            .run { if (seconds != 0L) plus(seconds, 0).check(zone) else this }
            .run { if (nanoseconds != 0L) plus(0, nanoseconds).check(zone) else this }
    }.check(zone)
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding CalendarPeriod to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding CalendarPeriod", e)
}

internal actual fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant =
    plusDateTimeUnit(value, unit.dateTimeUnit, zone)

internal fun Instant.plusDateTimeUnit(value: Long, unit: DateTimeUnit, zone: TimeZone): Instant = try {
    when (unit) {
        is DateTimeUnit.DateBased -> {
            if (value < Int.MIN_VALUE || value > Int.MAX_VALUE)
                throw ArithmeticException("Can't add a Long date-based value, as it would cause an overflow")
            toZonedLocalDateTimeFailing(zone).plus(value.toInt(), unit).toInstant()
        }
        is DateTimeUnit.TimeBased -> multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (seconds, nanoseconds) ->
            check(zone).plus(seconds, nanoseconds).check(zone)
        }
    }
} catch (e: ArithmeticException) {
    throw DateTimeArithmeticException("Arithmetic overflow when adding to an Instant", e)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Boundaries of Instant exceeded when adding a value", e)
}

@OptIn(ExperimentalTime::class)
actual fun Instant.periodUntil(other: Instant, zone: TimeZone): DateTimePeriod {
    var thisLdt = toZonedLocalDateTimeFailing(zone)
    val otherLdt = other.toZonedLocalDateTimeFailing(zone)

    val months = thisLdt.until(otherLdt, DateTimeUnit.MONTH).toInt() // `until` on dates never fails
    thisLdt = thisLdt.plus(months, DateTimeUnit.MONTH) // won't throw: thisLdt + months <= otherLdt, which is known to be valid
    val days = thisLdt.until(otherLdt, DateTimeUnit.DAY).toInt() // `until` on dates never fails
    thisLdt = thisLdt.plus(days, DateTimeUnit.DAY) // won't throw: thisLdt + days <= otherLdt
    val time = thisLdt.until(otherLdt, DateTimeUnit.NANOSECOND).nanoseconds // |otherLdt - thisLdt| < 24h

    time.toComponents { hours, minutes, seconds, nanoseconds ->
        return DateTimePeriod((months / 12), (months % 12), days, hours, minutes, seconds.toLong(), nanoseconds.toLong())
    }
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, zone: TimeZone): Long =
    when (unit) {
        is DateTimeUnit.DateBased ->
            toZonedLocalDateTimeFailing(zone).dateTime.until(other.toZonedLocalDateTimeFailing(zone).dateTime, unit)
                .toLong()
        is DateTimeUnit.TimeBased -> {
            check(zone); other.check(zone)
            try {
                multiplyAddAndDivide(other.epochSeconds - epochSeconds,
                    NANOS_PER_ONE.toLong(),
                    (other.nanosecondsOfSecond - nanosecondsOfSecond).toLong(),
                    unit.nanoseconds)
            } catch (e: ArithmeticException) {
                if (this < other) Long.MAX_VALUE else Long.MIN_VALUE
            }
        }
    }
