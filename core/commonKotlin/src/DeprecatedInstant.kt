/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:JvmMultifileClass
@file:JvmName("InstantKt")
package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.MILLIS_PER_ONE
import kotlinx.datetime.internal.NANOS_PER_MILLI
import kotlinx.datetime.internal.NANOS_PER_ONE
import kotlinx.datetime.internal.safeAdd
import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The minimum supported epoch second.
 */
private const val MIN_SECOND = -31557014167219200L // -1000000000-01-01T00:00:00Z

/**
 * The maximum supported epoch second.
 */
private const val MAX_SECOND = 31556889864403199L // +1000000000-12-31T23:59:59Z

@Deprecated(
    "Use kotlin.time.Instant instead",
    ReplaceWith("kotlin.time.Instant", "kotlin.time.Instant"),
    level = DeprecationLevel.WARNING
)
@Serializable(with = InstantSerializer::class)
public actual class Instant internal constructor(
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().epochSeconds")
    )
    public actual val epochSeconds: Long,
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().nanosecondsOfSecond")
    )
    public actual val nanosecondsOfSecond: Int
) : Comparable<Instant> {

    init {
        require(epochSeconds in MIN_SECOND..MAX_SECOND) { "Instant exceeds minimum or maximum instant" }
    }

    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().nanosecondsOfSecond")
    )
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

    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("(this.toStdlibInstant() + duration).toDeprecatedInstant()")
    )
    public actual operator fun plus(duration: Duration): Instant = duration.toComponents { secondsToAdd, nanosecondsToAdd ->
        try {
            plus(secondsToAdd, nanosecondsToAdd.toLong())
        } catch (_: IllegalArgumentException) {
            if (duration.isPositive()) MAX else MIN
        } catch (_: ArithmeticException) {
            if (duration.isPositive()) MAX else MIN
        }
    }

    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("(this.toStdlibInstant() - duration).toDeprecatedInstant()")
    )
    public actual operator fun minus(duration: Duration): Instant = plus(-duration)

    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant() - other.toStdlibInstant()")
    )
    public actual operator fun minus(other: Instant): Duration =
        (this.epochSeconds - other.epochSeconds).seconds + // won't overflow given the instant bounds
                (this.nanosecondsOfSecond - other.nanosecondsOfSecond).nanoseconds

    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().compareTo(other.toStdlibInstant())")
    )
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

    @Suppress("POTENTIALLY_NON_REPORTED_ANNOTATION")
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().toString()")
    )
    // org.threeten.bp.format.DateTimeFormatterBuilder.InstantPrinterParser#print
    actual override fun toString(): String = format(ISO_DATE_TIME_OFFSET_WITH_TRAILING_ZEROS)

    public actual companion object {
        internal actual val MIN = Instant(MIN_SECOND, 0)
        internal actual val MAX = Instant(MAX_SECOND, 999_999_999)

        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlin.time.Clock"), level = DeprecationLevel.ERROR)
        public actual fun now(): Instant = Clock.System.now()

        // org.threeten.bp.Instant#ofEpochMilli
        public actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant {
            val epochSeconds = epochMilliseconds.floorDiv(MILLIS_PER_ONE.toLong())
            val nanosecondsOfSecond = (epochMilliseconds.mod(MILLIS_PER_ONE.toLong()) * NANOS_PER_MILLI).toInt()
            return when {
                epochSeconds < MIN_SECOND -> MIN
                epochSeconds > MAX_SECOND -> MAX
                else -> fromEpochSeconds(epochSeconds, nanosecondsOfSecond)
            }
        }

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
            } catch (_: ArithmeticException) {
                if (epochSeconds > 0) MAX else MIN
            } catch (_: IllegalArgumentException) {
                if (epochSeconds > 0) MAX else MIN
            }

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant =
            fromEpochSeconds(epochSeconds, nanosecondAdjustment.toLong())

        public actual fun parse(input: CharSequence, format: DateTimeFormat<DateTimeComponents>): Instant = try {
            format.parse(input).toInstantUsingOffset().toDeprecatedInstant()
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Failed to parse an instant from '$input'", e)
        }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): Instant = parse(input = isoString)

        public actual val DISTANT_PAST: Instant = fromEpochSeconds(DISTANT_PAST_SECONDS, 999_999_999)

        public actual val DISTANT_FUTURE: Instant = fromEpochSeconds(DISTANT_FUTURE_SECONDS, 0)
    }

}

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(period, timeZone).toDeprecatedInstant()")
)
public actual fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant =
    toStdlibInstant().plus(period, timeZone).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(1, unit, timeZone).toDeprecatedInstant()")
)
public actual fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(1L, unit, timeZone)
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()")
)
public actual fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(value.toLong(), unit, timeZone)
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(value, unit, timeZone).toDeprecatedInstant()")
)
public actual fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(-value.toLong(), unit, timeZone)
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()")
)
public actual fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit).toDeprecatedInstant()")
)
public actual fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    toStdlibInstant().plus(value, unit).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().periodUntil(other.toStdlibInstant(), timeZone)")
)
public actual fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod =
    toStdlibInstant().periodUntil(other.toStdlibInstant(), timeZone)

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().until(other.toStdlibInstant(), unit, timeZone)")
)
public actual fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long =
    toStdlibInstant().until(other.toStdlibInstant(), unit, timeZone)


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
