/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.JSJoda.Instant as jtInstant
import kotlinx.datetime.internal.JSJoda.Duration as jtDuration
import kotlinx.datetime.internal.JSJoda.Clock as jtClock
import kotlinx.datetime.internal.JSJoda.ChronoUnit as jtChronoUnit
import kotlinx.datetime.internal.JSJoda.ZonedDateTime as jtZonedDateTime
import kotlinx.datetime.internal.safeAdd
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Serializable(with = InstantSerializer::class)
public actual class Instant internal constructor(internal val value: jtInstant) : Comparable<Instant> {

    public actual val epochSeconds: Long
        get() = value.epochSecond().toLong()
    public actual val nanosecondsOfSecond: Int
        get() = value.nano().toInt()

    public actual fun toEpochMilliseconds(): Long =
            epochSeconds * MILLIS_PER_ONE + nanosecondsOfSecond / NANOS_PER_MILLI

    public actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        return try {
            Instant(plusFix(seconds.toDouble(), nanoseconds))
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException()) throw e
            if (duration.isPositive()) MAX else MIN
        }
    }

    internal fun plusFix(seconds: Double, nanos: Int): jtInstant {
        val newSeconds = value.epochSecond() + seconds
        val newNanos = value.nano() + nanos
        return jsTry { jtInstant.ofEpochSecond(newSeconds, newNanos.toInt()) }
    }

    public actual operator fun minus(duration: Duration): Instant = plus(-duration)

    public actual operator fun minus(other: Instant): Duration {
        val diff = jtDuration.between(other.value, this.value)
        return diff.seconds().seconds + diff.nano().nanoseconds
    }

    public actual override operator fun compareTo(other: Instant): Int = this.value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
            (this === other) || (other is Instant && (this.value === other.value || this.value.equals(other.value)))

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    public actual companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        public actual fun now(): Instant =
                Instant(jtClock.systemUTC().instant())

        public actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant = try {
            fromEpochSeconds(epochMilliseconds / MILLIS_PER_ONE, epochMilliseconds % MILLIS_PER_ONE * NANOS_PER_MILLI)
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException()) throw e
            if (epochMilliseconds > 0) MAX else MIN
        }

        // TODO: implement a custom parser to 1) help DCE get rid of the formatting machinery 2) move Instant to stdlib
        public actual fun parse(input: CharSequence, format: DateTimeFormat<DateTimeComponents>): Instant = try {
            // This format is not supported properly by Joda-Time, so we can't delegate to it.
            format.parse(input).toInstantUsingOffset()
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Failed to parse an instant from '$input'", e)
        }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): Instant = parse(input = isoString)

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant = try {
            /* Performing normalization here because otherwise this fails:
               assertEquals((Long.MAX_VALUE % 1_000_000_000).toInt(),
                            Instant.fromEpochSeconds(0, Long.MAX_VALUE).nanosecondsOfSecond) */
            val secs = safeAdd(epochSeconds, nanosecondAdjustment.floorDiv(NANOS_PER_ONE.toLong()))
            val nos = nanosecondAdjustment.mod(NANOS_PER_ONE.toLong()).toInt()
            Instant(jsTry { jtInstant.ofEpochSecond(secs.toDouble(), nos) })
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException() && e !is ArithmeticException) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant = try {
            Instant(jsTry { jtInstant.ofEpochSecond(epochSeconds.toDouble(), nanosecondAdjustment) })
        } catch (e: Throwable) {
            if (!e.isJodaDateTimeException()) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        public actual val DISTANT_PAST: Instant = Instant(jsTry { jtInstant.ofEpochSecond(DISTANT_PAST_SECONDS.toDouble(), 999_999_999) })
        public actual val DISTANT_FUTURE: Instant = Instant(jsTry { jtInstant.ofEpochSecond(DISTANT_FUTURE_SECONDS.toDouble(), 0) })

        internal actual val MIN: Instant = Instant(jtInstant.MIN)
        internal actual val MAX: Instant = Instant(jtInstant.MAX)
    }
}


public actual fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant = try {
    val thisZdt = jsTry { this.value.atZone(timeZone.zoneId) }
    with(period) {
        thisZdt
                .run { if (totalMonths != 0) jsTry { plusMonths(totalMonths) } else this }
                .run { if (days != 0) jsTry { plusDays(days) } else this }
                .run { if (hours != 0) jsTry { plusHours(hours) } else this }
                .run { if (minutes != 0) jsTry { plusMinutes(minutes) } else this }
                .run { if (seconds != 0) jsTry { plusSeconds(seconds) } else this }
                .run { if (nanoseconds != 0) jsTry { plusNanos(nanoseconds.toDouble()) } else this }
    }.toInstant().let(::Instant)
}    catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
    throw e
}

private fun Instant.atZone(zone: TimeZone): jtZonedDateTime = jsTry { value.atZone(zone.zoneId) }
private fun jtInstant.checkZone(zone: TimeZone): jtInstant = apply { jsTry { atZone(zone.zoneId) } }

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit, timeZone)"))
public actual fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
        plus(1, unit, timeZone)

public actual fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        try {
            val thisZdt = this.atZone(timeZone)
            when (unit) {
                is DateTimeUnit.TimeBased -> {
                    plus(value, unit).value.checkZone(timeZone)
                }
                is DateTimeUnit.DayBased ->
                    jsTry {thisZdt.plusDays(value.toDouble() * unit.days) }.toInstant()
                is DateTimeUnit.MonthBased ->
                    jsTry { thisZdt.plusMonths(value.toDouble() * unit.months) }.toInstant()
            }.let(::Instant)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
            throw e
        }

public actual fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
        try {
            val thisZdt = this.atZone(timeZone)
            when (unit) {
                is DateTimeUnit.TimeBased ->
                    plus(value.toLong(), unit).value.checkZone(timeZone)
                is DateTimeUnit.DayBased ->
                    jsTry { thisZdt.plusDays(value.toDouble() * unit.days) }.toInstant()
                is DateTimeUnit.MonthBased ->
                    jsTry { thisZdt.plusMonths(value.toDouble() * unit.months) }.toInstant()
            }.let(::Instant)
        } catch (e: Throwable) {
            if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e)
            throw e
        }

public actual fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    if (value == Int.MIN_VALUE)
        plus(-value.toLong(), unit, timeZone)
    else
        plus(-value, unit, timeZone)

public actual fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    try {
        multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (d, r) ->
            Instant(plusFix(d.toDouble(), r.toInt()))
        }
    } catch (e: Throwable) {
        if (!e.isJodaDateTimeException()) {
            throw e
        }
        if (value > 0) Instant.MAX else Instant.MIN
    }

public actual fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod = try {
    var thisZdt = jsTry { this.value.atZone(timeZone.zoneId) }
    val otherZdt = jsTry { other.value.atZone(timeZone.zoneId) }

    val months = thisZdt.until(otherZdt, jtChronoUnit.MONTHS); thisZdt = jsTry { thisZdt.plusMonths(months) }
    val days = thisZdt.until(otherZdt, jtChronoUnit.DAYS); thisZdt = jsTry { thisZdt.plusDays(days) }
    val nanoseconds = thisZdt.until(otherZdt, jtChronoUnit.NANOS)

    buildDateTimePeriod(months.toInt(), days.toInt(), nanoseconds.toLong())
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e) else throw e
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long = try {
    val thisZdt = this.atZone(timeZone)
    val otherZdt = other.atZone(timeZone)
    when(unit) {
        is DateTimeUnit.TimeBased -> until(other, unit)
        is DateTimeUnit.DayBased -> (thisZdt.until(otherZdt, jtChronoUnit.DAYS) / unit.days).toLong()
        is DateTimeUnit.MonthBased -> (thisZdt.until(otherZdt, jtChronoUnit.MONTHS) / unit.months).toLong()
    }
} catch (e: ArithmeticException) {
    if (this < other) Long.MAX_VALUE else Long.MIN_VALUE
} catch (e: Throwable) {
    if (e.isJodaDateTimeException()) throw DateTimeArithmeticException(e) else throw e
}
