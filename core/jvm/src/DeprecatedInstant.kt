/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:JvmMultifileClass
@file:JvmName("InstantJvmKt")
package kotlinx.datetime

import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.internal.NANOS_PER_ONE
import kotlinx.datetime.internal.multiplyAndDivide
import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.DateTimeException
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Deprecated(
    "Use kotlin.time.Instant instead",
    ReplaceWith("kotlin.time.Instant", "kotlin.time.Instant"),
    level = DeprecationLevel.WARNING
)
@Serializable(with = InstantSerializer::class)
public actual class Instant internal constructor(internal val value: java.time.Instant) : Comparable<Instant> {
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().epochSeconds")
    )
    public actual val epochSeconds: Long
        get() = value.epochSecond
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().nanosecondsOfSecond")
    )
    public actual val nanosecondsOfSecond: Int
        get() = value.nano

    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().nanosecondsOfSecond")
    )
    public actual fun toEpochMilliseconds(): Long = try {
        value.toEpochMilli()
    } catch (e: ArithmeticException) {
        if (value.isAfter(java.time.Instant.EPOCH)) Long.MAX_VALUE else Long.MIN_VALUE
    }

    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("(this.toStdlibInstant() + duration).toDeprecatedInstant()")
    )
    public actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        try {
            Instant(value.plusSeconds(seconds).plusNanos(nanoseconds.toLong()))
        } catch (e: java.lang.Exception) {
            if (e !is ArithmeticException && e !is DateTimeException) throw e
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
        (this.value.epochSecond - other.value.epochSecond).seconds + // won't overflow given the instant bounds
                (this.value.nano - other.value.nano).nanoseconds

    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().compareTo(other.toStdlibInstant())")
    )
    public actual override operator fun compareTo(other: Instant): Int = this.value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is Instant && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    @Suppress("POTENTIALLY_NON_REPORTED_ANNOTATION")
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().toString()")
    )
    actual override fun toString(): String = value.toString()

    public actual companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlin.time.Clock"), level = DeprecationLevel.ERROR)
        public actual fun now(): Instant =
            Instant(Clock.systemUTC().instant())

        public actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant =
            Instant(java.time.Instant.ofEpochMilli(epochMilliseconds))

        public actual fun parse(input: CharSequence, format: DateTimeFormat<DateTimeComponents>): Instant = try {
            /**
             * Can't use built-in Java Time's handling of `Instant.parse` because it supports 24:00:00 and
             * 23:59:60, and also doesn't support non-`Z` UTC offsets on older JDKs.
             * Can't use custom Java Time's formats because Java 8 doesn't support the UTC offset format with
             * optional minutes and seconds and `:` between them:
             * https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatterBuilder.html#appendOffset-java.lang.String-java.lang.String-
             */
            format.parse(input).toInstantUsingOffset().toDeprecatedInstant()
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Failed to parse an instant from '$input'", e)
        }

        @Deprecated("This overload is only kept for binary compatibility", level = DeprecationLevel.HIDDEN)
        public fun parse(isoString: String): Instant = parse(input = isoString)

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant = try {
            Instant(java.time.Instant.ofEpochSecond(epochSeconds, nanosecondAdjustment))
        } catch (e: Exception) {
            if (e !is ArithmeticException && e !is DateTimeException) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant =
            fromEpochSeconds(epochSeconds, nanosecondAdjustment.toLong())

        public actual val DISTANT_PAST: Instant = Instant(java.time.Instant.ofEpochSecond(DISTANT_PAST_SECONDS, 999_999_999))
        public actual val DISTANT_FUTURE: Instant = Instant(java.time.Instant.ofEpochSecond(DISTANT_FUTURE_SECONDS, 0))

        internal actual val MIN: Instant = Instant(java.time.Instant.MIN)
        internal actual val MAX: Instant = Instant(java.time.Instant.MAX)
    }
}

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(period, timeZone).toDeprecatedInstant()")
)
@PublishedApi
@JvmName("plus")
internal fun Instant.plusJvm(period: DateTimePeriod, timeZone: TimeZone): Instant =
    toStdlibInstant().plus(period, timeZone).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(1, unit, timeZone).toDeprecatedInstant()")
)
@PublishedApi
@JvmName("plus")
internal fun Instant.plusJvm(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    toStdlibInstant().plus(unit, timeZone).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()")
)
@PublishedApi
@JvmName("plus")
internal fun Instant.plusJvm(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(value, unit, timeZone).toDeprecatedInstant()")
)
@PublishedApi
@JvmName("minus")
internal fun Instant.minusJvm(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    toStdlibInstant().minus(value, unit, timeZone).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()")
)
@PublishedApi
@JvmName("plus")
internal fun Instant.plusJvm(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit).toDeprecatedInstant()")
)
@PublishedApi
@JvmName("plus")
internal fun Instant.plusJvm(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    toStdlibInstant().plus(value, unit).toDeprecatedInstant()

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("this.toStdlibInstant().periodUntil(other.toStdlibInstant(), timeZone)")
)
@PublishedApi
@JvmName("periodUntil")
internal fun Instant.periodUntilJvm(other: Instant, timeZone: TimeZone): DateTimePeriod =
    toStdlibInstant().periodUntil(other.toStdlibInstant(), timeZone)

@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.HIDDEN,
    replaceWith = ReplaceWith("this.toStdlibInstant().until(other.toStdlibInstant(), unit, timeZone)")
)
@PublishedApi
@JvmName("until")
internal fun Instant.untilJvm(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long =
    toStdlibInstant().until(other.toStdlibInstant(), unit, timeZone)
