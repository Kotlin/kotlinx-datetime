/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("InstantJvmKt")

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.safeMultiply
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import java.time.DateTimeException
import java.time.temporal.*
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import java.time.Instant as jtInstant
import java.time.Clock as jtClock

@Serializable(with = InstantSerializer::class)
public actual class Instant internal constructor(
    internal val value: jtInstant
) : Comparable<Instant> {

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
}

internal actual fun Instant.plus(secondsToAdd: Long, nanosToAdd: Long): Instant = try {
    Instant(this.value.plusSeconds(secondsToAdd).plusNanos(nanosToAdd))
} catch (e: DateTimeException) {
    throw IllegalArgumentException(e)
}
