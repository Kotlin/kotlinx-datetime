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
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The minimum supported epoch second.
 */
private const val MIN_SECOND = -31557014167219200L // -1000000000-01-01T00:00:00Z

/**
 * The maximum supported epoch second.
 */
private const val MAX_SECOND = 31556889864403199L // +1000000000-12-31T23:59:59

@Serializable(with = InstantSerializer::class)
public actual class Instant internal constructor(public actual val epochSeconds: Long, public actual val nanosecondsOfSecond: Int) : Comparable<Instant> {

    init {
        require(epochSeconds in MIN_SECOND..MAX_SECOND) { "Instant exceeds minimum or maximum instant" }
    }

    // org.threeten.bp.Instant#toEpochMilli
    public actual fun toEpochMilliseconds(): Long = try {
        if (epochSeconds >= 0) {
            val millis = safeMultiply(epochSeconds, MILLIS_PER_ONE.toLong())
            safeAdd(millis, (nanosecondsOfSecond / NANOS_PER_MILLI).toLong())
        } else {
            // prevent an overflow in seconds * 1000
            // instead of going form the second farther away from 0
            // going toward 0
            // we go from the second closer to 0 away from 0
            // that way we always stay in the valid long range
            // seconds + 1 can not overflow because it is negative
            val millis = safeMultiply(epochSeconds + 1, MILLIS_PER_ONE.toLong())
            safeAdd(millis, (nanosecondsOfSecond / NANOS_PER_MILLI - MILLIS_PER_ONE).toLong())
        }
    } catch (_: ArithmeticException) {
        if (epochSeconds > 0) Long.MAX_VALUE else Long.MIN_VALUE
    }

    public actual operator fun plus(duration: Duration): Instant = duration.toComponents { secondsToAdd, nanosecondsToAdd ->
        try {
            plus(secondsToAdd, nanosecondsToAdd.toLong())
        } catch (_: IllegalArgumentException) {
            if (duration.isPositive()) MAX else MIN
        } catch (_: ArithmeticException) {
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
        public actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant {
            val epochSeconds = epochMilliseconds.floorDiv(MILLIS_PER_ONE.toLong())
            val nanosecondsOfSecond = (epochMilliseconds.mod(MILLIS_PER_ONE.toLong()) * NANOS_PER_MILLI).toInt()
            return fromEpochSeconds(epochSeconds, nanosecondsOfSecond)
        }

        /**
         * @throws ArithmeticException if arithmetic overflow occurs
         * @throws IllegalArgumentException if the boundaries of Instant are overflown
         */
        internal fun fromEpochSecondsThrowing(epochSeconds: Long, nanosecondAdjustment: Long): Instant {
            val secs = safeAdd(epochSeconds, nanosecondAdjustment.floorDiv(NANOS_PER_ONE.toLong()))
            val nos = nanosecondAdjustment.mod(NANOS_PER_ONE.toLong()).toInt()
            return Instant(secs, nos)
        }

        // org.threeten.bp.Instant#ofEpochSecond(long, long)
        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant =
            try {
                fromEpochSecondsThrowing(epochSeconds, nanosecondAdjustment)
            } catch (_: ArithmeticException) {
                if (epochSeconds > 0) MAX else MIN
            } catch (_: IllegalArgumentException) {
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

// org.threeten.bp.Instant#plus(long, long)
internal actual fun Instant.plus(secondsToAdd: Long, nanosToAdd: Long): Instant {
    if ((secondsToAdd or nanosToAdd) == 0L) {
        return this
    }
    val newEpochSeconds: Long = safeAdd(safeAdd(epochSeconds, secondsToAdd), (nanosToAdd / NANOS_PER_ONE))
    val newNanosToAdd = nanosToAdd % NANOS_PER_ONE
    val nanoAdjustment = (nanosecondsOfSecond + newNanosToAdd) // safe int+NANOS_PER_ONE
    return Instant.fromEpochSecondsThrowing(newEpochSeconds, nanoAdjustment)
}
