/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@OptIn(kotlin.time.ExperimentalTime::class)
public expect class Instant : Comparable<Instant> {

    /** */
    public val epochSeconds: Long

    /** */
    public val nanosecondsOfSecond: Int

    /**
     * The return value is clamped to [Long.MAX_VALUE] or [Long.MIN_VALUE] if the result does not fit in [Long].
     */
    public fun toEpochMilliseconds(): Long

    /**
     * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
     */
    @ExperimentalTime
    public operator fun plus(duration: Duration): Instant

    /**
     * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
     */
    @ExperimentalTime
    public operator fun minus(duration: Duration): Instant

    // questionable
    /** */
    @ExperimentalTime
    public operator fun minus(other: Instant): Duration

    /** */
    public override operator fun compareTo(other: Instant): Int

    companion object {
        /** */
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        fun now(): Instant

        /**
         * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
         */
        fun fromEpochMilliseconds(epochMilliseconds: Long): Instant

        /**
         * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
         */
        fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long = 0): Instant

        /**
         * @throws DateTimeFormatException if the text cannot be parsed or the boundaries of [Instant] are exceeded.
         */
        fun parse(isoString: String): Instant
    }
}

/**
 * @throws DateTimeFormatException if the text cannot be parsed or the boundaries of [Instant] are exceeded.
 */
public fun String.toInstant(): Instant = Instant.parse(this)

/**
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDateTime].
 */
public expect fun Instant.plus(period: CalendarPeriod, zone: TimeZone): Instant

/**
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 */
public expect fun Instant.plus(value: Int, unit: CalendarUnit, zone: TimeZone): Instant

/**
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 */
public expect fun Instant.plus(value: Long, unit: CalendarUnit, zone: TimeZone): Instant

/**
 * @throws DateTimeArithmeticException if this [Instant] or [other] is too large to fit in [LocalDateTime].
 */
public expect fun Instant.periodUntil(other: Instant, zone: TimeZone): CalendarPeriod

/**
 * The return value is clamped to [Long.MAX_VALUE] or [Long.MIN_VALUE] if [unit] is more granular than
 * [CalendarUnit.DAY] and the result is too large.
 *
 * @throws DateTimeArithmeticException if this [Instant] or [other] is too large to fit in [LocalDateTime].
 */
public expect fun Instant.until(other: Instant, unit: CalendarUnit, zone: TimeZone): Long

/**
 * The return value is clamped to [Int.MAX_VALUE] or [Int.MIN_VALUE] if the result would otherwise cause an arithmetic
 * overflow.
 *
 * @throws DateTimeArithmeticException if this [Instant] or [other] is too large to fit in [LocalDateTime].
 */
public fun Instant.daysUntil(other: Instant, zone: TimeZone): Int =
    until(other, CalendarUnit.DAY, zone).clampToInt()

/**
 * The return value is clamped to [Int.MAX_VALUE] or [Int.MIN_VALUE] if the result would otherwise cause an arithmetic
 * overflow.
 *
 * @throws DateTimeArithmeticException if this [Instant] or [other] is too large to fit in [LocalDateTime].
 */
public fun Instant.monthsUntil(other: Instant, zone: TimeZone): Int =
    until(other, CalendarUnit.MONTH, zone).clampToInt()

/**
 * The return value is clamped to [Int.MAX_VALUE] or [Int.MIN_VALUE] if the result would otherwise cause an arithmetic
 * overflow.
 *
 * @throws DateTimeArithmeticException if this [Instant] or [other] is too large to fit in [LocalDateTime].
 */
public fun Instant.yearsUntil(other: Instant, zone: TimeZone): Int =
    until(other, CalendarUnit.YEAR, zone).clampToInt()

private fun Long.clampToInt(): Int =
    if (this > Int.MAX_VALUE) Int.MAX_VALUE else if (this < Int.MIN_VALUE) Int.MIN_VALUE else toInt()

public fun Instant.minus(other: Instant, zone: TimeZone): CalendarPeriod = other.periodUntil(this, zone)
public fun Instant.minus(other: Instant, unit: CalendarUnit, zone: TimeZone): Long = other.until(this, unit, zone)
