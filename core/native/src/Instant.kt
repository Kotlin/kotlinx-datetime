/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable
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

/** A parser for the string representation of [ZoneOffset] as seen in `OffsetDateTime`.
 *
 * We can't just reuse the parsing logic of [ZoneOffset.of], as that version is more lenient: here, strings like
 * "0330" are not considered valid zone offsets, whereas [ZoneOffset.of] sees treats the example above as "03:30". */
private val zoneOffsetParser: Parser<UtcOffset>
    get() = (concreteCharParser('z').or(concreteCharParser('Z')).map { UtcOffset.ZERO })
        .or(
            concreteCharParser('+').or(concreteCharParser('-'))
                .chain(intParser(2, 2))
                .chain(
                    optional(
                        // minutes
                        concreteCharParser(':').chainSkipping(intParser(2, 2))
                            .chain(optional(
                                    // seconds
                                    concreteCharParser(':').chainSkipping(intParser(2, 2))
                            ))))
                .map {
                    val (signHours, minutesSeconds) = it
                    val (sign, hours) = signHours
                    val minutes: Int
                    val seconds: Int
                    if (minutesSeconds == null) {
                        minutes = 0
                        seconds = 0
                    } else {
                        minutes = minutesSeconds.first
                        seconds = minutesSeconds.second ?: 0
                    }
                    try {
                        if (sign == '-')
                            UtcOffset.ofHoursMinutesSeconds(-hours, -minutes, -seconds)
                        else
                            UtcOffset.ofHoursMinutesSeconds(hours, minutes, seconds)
                    } catch (e: IllegalArgumentException) {
                        throw DateTimeFormatException(e)
                    }
                }
        )

// This is a function and not a value due to https://github.com/Kotlin/kotlinx-datetime/issues/5
// org.threeten.bp.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME
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
        .chain(zoneOffsetParser)
        .map {
            val (localDateTime, offset) = it
            val (dateHourMinuteSecond, nanosVal) = localDateTime
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
            val instantSecs = epochDay * 86400 - offset.totalSeconds + localTime.toSecondOfDay() + secDelta
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

internal expect fun currentTime(): Instant

@Serializable(with = InstantIso8601Serializer::class)
@OptIn(ExperimentalTime::class)
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
            if (secondsToAdd > 0) MAX else MIN
        } catch (e: ArithmeticException) {
            if (secondsToAdd > 0) MAX else MIN
        }
    }

    public actual operator fun minus(duration: Duration): Instant = plus(-duration)

    public actual operator fun minus(other: Instant): Duration =
        Duration.seconds(this.epochSeconds - other.epochSeconds) + // won't overflow given the instant bounds
        Duration.nanoseconds(this.nanosecondsOfSecond - other.nanosecondsOfSecond)

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
    actual override fun toString(): String = toStringWithOffset(UtcOffset.ZERO)

    public actual companion object {
        internal actual val MIN = Instant(MIN_SECOND, 0)
        internal actual val MAX = Instant(MAX_SECOND, 999_999_999)

        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        public actual fun now(): Instant = currentTime()

        // org.threeten.bp.Instant#ofEpochMilli
        public actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant =
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

        public actual fun parse(isoString: String): Instant =
            instantParser.parse(isoString)

        public actual val DISTANT_PAST: Instant = fromEpochSeconds(DISTANT_PAST_SECONDS, 999_999_999)

        public actual val DISTANT_FUTURE: Instant = fromEpochSeconds(DISTANT_FUTURE_SECONDS, 0)
    }

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

@OptIn(ExperimentalTime::class)
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

internal actual fun Instant.toStringWithOffset(offset: UtcOffset): String {
    val buf = StringBuilder()
    val inNano: Int = nanosecondsOfSecond
    val seconds = epochSeconds + offset.totalSeconds
    if (seconds >= -SECONDS_0000_TO_1970) { // current era
        val zeroSecs: Long = seconds - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970
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
        val zeroSecs: Long = seconds + SECONDS_0000_TO_1970
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
                    buf.deleteAt(pos)
                    buf.deleteAt(pos)
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
    buf.append(offset)
    return buf.toString()
}
