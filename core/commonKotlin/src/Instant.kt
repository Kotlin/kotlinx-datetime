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
