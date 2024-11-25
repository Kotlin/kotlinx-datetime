/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("InstantJvmKt")

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.temporal.*
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import java.time.Instant as jtInstant
import java.time.Clock as jtClock

@Serializable(with = InstantIso8601Serializer::class)
public actual class Instant internal constructor(
    internal val value: jtInstant
) : Comparable<Instant>, java.io.Serializable {

    public actual val epochSeconds: Long
        get() = value.epochSecond
    public actual val nanosecondsOfSecond: Int
        get() = value.nano

    public actual fun toEpochMilliseconds(): Long = try {
        value.toEpochMilli()
    } catch (e: ArithmeticException) {
        if (value.isAfter(java.time.Instant.EPOCH)) Long.MAX_VALUE else Long.MIN_VALUE
    }

    public actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        try {
            Instant(value.plusSeconds(seconds).plusNanos(nanoseconds.toLong()))
        } catch (e: java.lang.Exception) {
            if (e !is ArithmeticException && e !is DateTimeException) throw e
            if (duration.isPositive()) MAX else MIN
        }
    }

    public actual operator fun minus(duration: Duration): Instant = plus(-duration)

    public actual operator fun minus(other: Instant): Duration =
        (this.value.epochSecond - other.value.epochSecond).seconds + // won't overflow given the instant bounds
            (this.value.nano - other.value.nano).nanoseconds

    public actual override operator fun compareTo(other: Instant): Int = this.value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is Instant && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    public actual companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        public actual fun now(): Instant =
                Instant(jtClock.systemUTC().instant())

        public actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant =
                Instant(jtInstant.ofEpochMilli(epochMilliseconds))

        // TODO: implement a custom parser to 1) help DCE get rid of the formatting machinery 2) move Instant to stdlib
        public actual fun parse(input: CharSequence, format: DateTimeFormat<DateTimeComponents>): Instant = try {
            /**
             * Can't use built-in Java Time's handling of `Instant.parse` because it supports 24:00:00 and
             * 23:59:60, and also doesn't support non-`Z` UTC offsets on older JDKs.
             * Can't use custom Java Time's formats because Java 8 doesn't support the UTC offset format with
             * optional minutes and seconds and `:` between them:
             * https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatterBuilder.html#appendOffset-java.lang.String-java.lang.String-
             */
            format.parse(input).toInstantUsingOffset()
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Failed to parse an instant from '$input'", e)
        }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): Instant = parse(input = isoString)

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant = try {
            Instant(jtInstant.ofEpochSecond(epochSeconds, nanosecondAdjustment))
        } catch (e: Exception) {
            if (e !is ArithmeticException && e !is DateTimeException) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant =
            fromEpochSeconds(epochSeconds, nanosecondAdjustment.toLong())

        public actual val DISTANT_PAST: Instant = Instant(jtInstant.ofEpochSecond(DISTANT_PAST_SECONDS, 999_999_999))
        public actual val DISTANT_FUTURE: Instant = Instant(jtInstant.ofEpochSecond(DISTANT_FUTURE_SECONDS, 0))

        internal actual val MIN: Instant = Instant(jtInstant.MIN)
        internal actual val MAX: Instant = Instant(jtInstant.MAX)
    }

    private fun writeReplace(): Any = Ser(Ser.INSTANT_TAG, this)
}

private fun Instant.atZone(zone: TimeZone): java.time.ZonedDateTime = try {
    value.atZone(zone.zoneId)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

public actual fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant {
    try {
        val thisZdt = atZone(timeZone)
        return with(period) {
            thisZdt
                    .run { if (totalMonths != 0) plusMonths(totalMonths.toLong()) else this }
                    .run { if (days != 0) plusDays(days.toLong()) else this }
                    .run { if (totalNanoseconds != 0L) plusNanos(totalNanoseconds) else this }
        }.toInstant().let(::Instant)
    } catch (e: DateTimeException) {
        throw DateTimeArithmeticException(e)
    }
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit, timeZone)"))
public actual fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
        plus(1L, unit, timeZone)

public actual fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        plus(value.toLong(), unit, timeZone)

public actual fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        plus(-value.toLong(), unit, timeZone)

public actual fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        try {
            val thisZdt = atZone(timeZone)
            when (unit) {
                is DateTimeUnit.TimeBased ->
                    plus(value, unit).value.also { it.atZone(timeZone.zoneId) }
                is DateTimeUnit.DayBased ->
                    thisZdt.plusDays(safeMultiply(value, unit.days.toLong())).toInstant()
                is DateTimeUnit.MonthBased ->
                    thisZdt.plusMonths(safeMultiply(value, unit.months.toLong())).toInstant()
            }.let(::Instant)
        } catch (e: Exception) {
            if (e !is DateTimeException && e !is ArithmeticException) throw e
            throw DateTimeArithmeticException("Instant $this cannot be represented as local date when adding $value $unit to it", e)
        }

public actual fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    try {
        multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (d, r) ->
            Instant(this.value.plusSeconds(d).plusNanos(r))
        }
    } catch (e: Exception) {
        if (e !is DateTimeException && e !is ArithmeticException) throw e
        if (value > 0) Instant.MAX else Instant.MIN
    }

public actual fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod {
    var thisZdt = this.atZone(timeZone)
    val otherZdt = other.atZone(timeZone)

    val months = thisZdt.until(otherZdt, ChronoUnit.MONTHS); thisZdt = thisZdt.plusMonths(months)
    val days = thisZdt.until(otherZdt, ChronoUnit.DAYS); thisZdt = thisZdt.plusDays(days)
    val nanoseconds = thisZdt.until(otherZdt, ChronoUnit.NANOS)

    if (months > Int.MAX_VALUE || months < Int.MIN_VALUE) {
        throw DateTimeArithmeticException("The number of months between $this and $other does not fit in an Int")
    }
    return buildDateTimePeriod(months.toInt(), days.toInt(), nanoseconds)
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long = try {
    val thisZdt = this.atZone(timeZone)
    val otherZdt = other.atZone(timeZone)
    when(unit) {
        is DateTimeUnit.TimeBased -> until(other, unit)
        is DateTimeUnit.DayBased -> thisZdt.until(otherZdt, ChronoUnit.DAYS) / unit.days
        is DateTimeUnit.MonthBased -> thisZdt.until(otherZdt, ChronoUnit.MONTHS) / unit.months
    }
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
} catch (e: ArithmeticException) {
    if (this.value < other.value) Long.MAX_VALUE else Long.MIN_VALUE
}
