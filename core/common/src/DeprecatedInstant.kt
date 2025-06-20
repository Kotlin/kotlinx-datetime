/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:JvmMultifileClass
@file:JvmName("InstantKt")
package kotlinx.datetime

import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.format
import kotlinx.datetime.internal.NANOS_PER_ONE
import kotlinx.datetime.internal.clampToInt
import kotlinx.datetime.internal.multiplyAddAndDivide
import kotlinx.datetime.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Creates a [kotlin.time.Instant] (the standard library version of `Instant`) identical to `this`.
 */
public fun Instant.toStdlibInstant(): kotlin.time.Instant =
    kotlin.time.Instant.fromEpochSeconds(epochSeconds, nanosecondsOfSecond)

/**
 * Creates a [kotlinx.datetime.Instant] identical to the version of `Instant` from the standard library.
 */
public fun kotlin.time.Instant.toDeprecatedInstant(): Instant =
    Instant.fromEpochSeconds(epochSeconds, nanosecondsOfSecond)

/**
 * The `kotlinx-datetime` version of [kotlin.time.Instant].
 *
 * Using this class is discouraged in favor of the standard library version, [kotlin.time.Instant].
 * Initially, `Instant` and `Clock` were introduced in `kotlinx-datetime`,
 * but it turned out they were useful even in contexts where no datetime processing was needed.
 * As a result, starting from 2.1.20, Kotlin's standard library includes its own `Instant` and `Clock` classes.
 *
 * `kotlinx.datetime.Instant` is still available for compatibility reasons in the compatibility artifact of
 * `kotlinx-datetime`, but it is deprecated.
 * In the normal release artifact, it is replaced by `kotlin.time.Instant`.
 * See [https://github.com/Kotlin/kotlinx-datetime?tab=readme-ov-file#deprecation-of-instant] for more information.
 */
@Deprecated(
    "Use kotlin.time.Instant instead",
    ReplaceWith("kotlin.time.Instant", "kotlin.time.Instant"),
    level = DeprecationLevel.WARNING
)
@Serializable(with = InstantSerializer::class)
public expect class Instant : Comparable<Instant> {

    /**
     * The number of seconds from the epoch instant `1970-01-01T00:00:00Z` rounded down to a [Long] number.
     *
     * The difference between the rounded number of seconds and the actual number of seconds
     * is returned by [nanosecondsOfSecond] property expressed in nanoseconds.
     *
     * Note that this number doesn't include leap seconds added or removed since the epoch.
     *
     * @see fromEpochSeconds
     * @sample kotlinx.datetime.test.samples.InstantSamples.epochSeconds
     */
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().epochSeconds")
    )
    public val epochSeconds: Long

    /**
     * The number of nanoseconds by which this instant is later than [epochSeconds] from the epoch instant.
     *
     * The value is always non-negative and lies in the range `0..999_999_999`.
     *
     * @see fromEpochSeconds
     * @sample kotlinx.datetime.test.samples.InstantSamples.nanosecondsOfSecond
     */
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().nanosecondsOfSecond")
    )
    public val nanosecondsOfSecond: Int

    /**
     * Returns the number of milliseconds from the epoch instant `1970-01-01T00:00:00Z`.
     *
     * Any fractional part of a millisecond is rounded toward zero to the whole number of milliseconds.
     *
     * If the result does not fit in [Long],
     * returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
     *
     * @see fromEpochMilliseconds
     * @sample kotlinx.datetime.test.samples.InstantSamples.toEpochMilliseconds
     */
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().nanosecondsOfSecond")
    )
    public fun toEpochMilliseconds(): Long

    /**
     * Returns an instant that is the result of adding the specified [duration] to this instant.
     *
     * If the [duration] is positive, the returned instant is later than this instant.
     * If the [duration] is negative, the returned instant is earlier than this instant.
     *
     * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
     *
     * **Pitfall**: [Duration.Companion.days] are multiples of 24 hours and are not calendar-based.
     * Consider using the [plus] overload that accepts a multiple of a [DateTimeUnit] instead for calendar-based
     * operations instead of using [Duration].
     * For an explanation of why some days are not 24 hours, see [DateTimeUnit.DayBased].
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.plusDuration
     */
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("(this.toStdlibInstant() + duration).toDeprecatedInstant()")
    )
    public operator fun plus(duration: Duration): Instant

    /**
     * Returns an instant that is the result of subtracting the specified [duration] from this instant.
     *
     * If the [duration] is positive, the returned instant is earlier than this instant.
     * If the [duration] is negative, the returned instant is later than this instant.
     *
     * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
     *
     * **Pitfall**: [Duration.Companion.days] are multiples of 24 hours and are not calendar-based.
     * Consider using the [minus] overload that accepts a multiple of a [DateTimeUnit] instead for calendar-based
     * operations instead of using [Duration].
     * For an explanation of why some days are not 24 hours, see [DateTimeUnit.DayBased].
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.minusDuration
     */
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("(this.toStdlibInstant() - duration).toDeprecatedInstant()")
    )
    public operator fun minus(duration: Duration): Instant

    // questionable
    /**
     * Returns the [Duration] between two instants: [other] and `this`.
     *
     * The duration returned is positive if this instant is later than the other,
     * and negative if this instant is earlier than the other.
     *
     * The result is never clamped, but note that for instants that are far apart,
     * the value returned may represent the duration between them inexactly due to the loss of precision.
     *
     * Note that sources of [Instant] values (in particular, [Clock]) are not guaranteed to be in sync with each other
     * or even monotonic, so the result of this operation may be negative even if the other instant was observed later
     * than this one, or vice versa.
     * For measuring time intervals, consider using [TimeSource.Monotonic].
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.minusInstant
     */
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant() - other.toStdlibInstant()")
    )
    public operator fun minus(other: Instant): Duration

    /**
     * Compares `this` instant with the [other] instant.
     * Returns zero if this instant represents the same moment as the other (meaning they are equal to one another),
     * a negative number if this instant is earlier than the other,
     * and a positive number if this instant is later than the other.
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.compareToSample
     */
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().compareTo(other.toStdlibInstant())")
    )
    public override operator fun compareTo(other: Instant): Int

    /**
     * Converts this instant to the ISO 8601 string representation, for example, `2023-01-02T23:40:57.120Z`.
     *
     * The representation uses the UTC-SLS time scale instead of UTC.
     * In practice, this means that leap second handling will not be readjusted to the UTC.
     * Leap seconds will not be added or skipped, so it is impossible to acquire a string
     * where the component for seconds is 60, and for any day, it's possible to observe 23:59:59.
     *
     * @see parse
     * @see DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET for a very similar format. The difference is that
     * [DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET] will not add trailing zeros for readability to the
     * fractional part of the second.
     * @sample kotlinx.datetime.test.samples.InstantSamples.toStringSample
     */
    @Suppress("POTENTIALLY_NON_REPORTED_ANNOTATION")
    @Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this.toStdlibInstant().toString()")
    )
    public override fun toString(): String


    public companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlin.time.Clock"), level = DeprecationLevel.ERROR)
        public fun now(): Instant

        /**
         * Returns an [Instant] that is [epochMilliseconds] number of milliseconds from the epoch instant `1970-01-01T00:00:00Z`.
         *
         * Every value of [epochMilliseconds] is guaranteed to be representable as an [Instant].
         *
         * Note that [Instant] also supports nanosecond precision via [fromEpochSeconds].
         *
         * @see Instant.toEpochMilliseconds
         * @sample kotlinx.datetime.test.samples.InstantSamples.fromEpochMilliseconds
         */
        public fun fromEpochMilliseconds(epochMilliseconds: Long): Instant

        /**
         * Returns an [Instant] that is the [epochSeconds] number of seconds from the epoch instant `1970-01-01T00:00:00Z`
         * and the [nanosecondAdjustment] number of nanoseconds from the whole second.
         *
         * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
         * In any case, it is guaranteed that instants between [DISTANT_PAST] and [DISTANT_FUTURE] can be represented.
         *
         * [fromEpochMilliseconds] is a similar function for when input data only has millisecond precision.
         *
         * @see Instant.epochSeconds
         * @see Instant.nanosecondsOfSecond
         * @sample kotlinx.datetime.test.samples.InstantSamples.fromEpochSeconds
         */
        public fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long = 0): Instant

        /**
         * Returns an [Instant] that is the [epochSeconds] number of seconds from the epoch instant `1970-01-01T00:00:00Z`
         * and the [nanosecondAdjustment] number of nanoseconds from the whole second.
         *
         * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
         * In any case, it is guaranteed that instants between [DISTANT_PAST] and [DISTANT_FUTURE] can be represented.
         *
         * [fromEpochMilliseconds] is a similar function for when input data only has millisecond precision.
         *
         * @see Instant.epochSeconds
         * @see Instant.nanosecondsOfSecond
         * @sample kotlinx.datetime.test.samples.InstantSamples.fromEpochSecondsIntNanos
         */
        public fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant

        /**
         * A shortcut for calling [DateTimeFormat.parse], followed by [DateTimeComponents.toInstantUsingOffset].
         *
         * Parses a string that represents an instant, including date and time components and a mandatory
         * time zone offset and returns the parsed [Instant] value.
         *
         * The string is considered to represent time on the UTC-SLS time scale instead of UTC.
         * In practice, this means that, even if there is a leap second on the given day, it will not affect how the
         * time is parsed, even if it's in the last 1000 seconds of the day.
         * Instead, even if there is a negative leap second on the given day, 23:59:59 is still considered a valid time.
         * 23:59:60 is invalid on UTC-SLS, so parsing it will fail.
         *
         * If the format is not specified, [DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET] is used.
         * `2023-01-02T23:40:57.120Z` is an example of a string in this format.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [Instant] are exceeded.
         *
         * @see Instant.toString for formatting using the default format.
         * @see Instant.format for formatting using a custom format.
         * @sample kotlinx.datetime.test.samples.InstantSamples.parsing
         */
        public fun parse(
            input: CharSequence,
            format: DateTimeFormat<DateTimeComponents> = DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
        ): Instant

        /**
         * An instant value that is far in the past.
         *
         * All instants in the range `DISTANT_PAST..DISTANT_FUTURE` can be [converted][Instant.toLocalDateTime] to
         * [LocalDateTime] without exceptions in every time zone.
         *
         * [isDistantPast] returns true for this value and all earlier ones.
         */
        public val DISTANT_PAST: Instant // -100001-12-31T23:59:59.999999999Z

        /**
         * An instant value that is far in the future.
         *
         * All instants in the range `DISTANT_PAST..DISTANT_FUTURE` can be [converted][Instant.toLocalDateTime] to
         * [LocalDateTime] without exceptions in every time zone.
         *
         * [isDistantFuture] returns true for this value and all later ones.
         */
        public val DISTANT_FUTURE: Instant // +100000-01-01T00:00:00Z

        internal val MIN: Instant
        internal val MAX: Instant
    }
}

/**
 * Returns true if the instant is [Instant.DISTANT_PAST] or earlier.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.isDistantPast
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().isDistantPast", "kotlin.time.isDistantPast")
)
public val Instant.isDistantPast: Boolean
    get() = this <= Instant.DISTANT_PAST

/**
 * Returns true if the instant is [Instant.DISTANT_FUTURE] or later.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.isDistantFuture
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().isDistantPast", "kotlin.time.isDistantFuture")
)
public val Instant.isDistantFuture: Boolean
    get() = this >= Instant.DISTANT_FUTURE

/**
 * @suppress
 */
@Deprecated("Removed to support more idiomatic code. See https://github.com/Kotlin/kotlinx-datetime/issues/339", ReplaceWith("Instant.parse(this)"), DeprecationLevel.WARNING)
public fun String.toInstant(): Instant = Instant.parse(this)

/**
 * Returns an instant that is the result of adding components of [DateTimePeriod] to this instant. The components are
 * added in the order from the largest units to the smallest, i.e., from years to nanoseconds.
 *
 * - If the [DateTimePeriod] only contains time-based components, please consider adding a [Duration] instead,
 *   as in `Clock.System.now() + 5.hours`.
 *   Then, it will not be necessary to pass the [timeZone].
 * - If the [DateTimePeriod] only has a single non-zero component (only the months or only the days),
 *   please consider using a multiple of [DateTimeUnit.DAY] or [DateTimeUnit.MONTH], like in
 *   `Clock.System.now().plus(5, DateTimeUnit.DAY, TimeZone.currentSystemDefault())`.
 *
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusPeriod
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(period, timeZone).toDeprecatedInstant()")
)
public expect fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant

/**
 * Returns an instant that is the result of subtracting components of [DateTimePeriod] from this instant. The components
 * are subtracted in the order from the largest units to the smallest, i.e., from years to nanoseconds.
 *
 * - If the [DateTimePeriod] only contains time-based components, please consider subtracting a [Duration] instead,
 *   as in `Clock.System.now() - 5.hours`.
 *   Then, it is not necessary to pass the [timeZone].
 * - If the [DateTimePeriod] only has a single non-zero component (only the months or only the days),
 *   please consider using a multiple of [DateTimeUnit.DAY] or [DateTimeUnit.MONTH], as in
 *   `Clock.System.now().minus(5, DateTimeUnit.DAY, TimeZone.currentSystemDefault())`.
 *
 * @throws DateTimeArithmeticException if this value or the results of intermediate computations are too large to fit in
 * [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusPeriod
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(period, timeZone).toDeprecatedInstant()")
)
public fun Instant.minus(period: DateTimePeriod, timeZone: TimeZone): Instant =
    /* An overflow can happen for any component, but we are only worried about nanoseconds, as having an overflow in
    any other component means that `plus` will throw due to the minimum value of the numeric type overflowing the
    `Instant` limits. */
    if (period.totalNanoseconds != Long.MIN_VALUE) {
        val negatedPeriod = with(period) { buildDateTimePeriod(-totalMonths, -days, -totalNanoseconds) }
        plus(negatedPeriod, timeZone)
    } else {
        val negatedPeriod = with(period) { buildDateTimePeriod(-totalMonths, -days, -(totalNanoseconds+1)) }
        plus(negatedPeriod, timeZone).plus(1, DateTimeUnit.NANOSECOND)
    }

/**
 * Returns a [DateTimePeriod] representing the difference between `this` and [other] instants.
 *
 * The components of [DateTimePeriod] are calculated so that adding it to `this` instant results in the [other] instant.
 *
 * All components of the [DateTimePeriod] returned are:
 * - Positive or zero if this instant is earlier than the other.
 * - Negative or zero if this instant is later than the other.
 * - Exactly zero if this instant is equal to the other.
 *
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.periodUntil
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().periodUntil(other.toStdlibInstant(), timeZone)")
)
public expect fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod

/**
 * Returns the whole number of the specified date or time [units][unit] between `this` and [other] instants
 * in the specified [timeZone].
 *
 * The value returned is:
 * - Positive or zero if this instant is earlier than the other.
 * - Negative or zero if this instant is later than the other.
 * - Zero if this instant is equal to the other.
 *
 * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
 *
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.untilAsDateTimeUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().until(other.toStdlibInstant(), unit, timeZone)")
)
public expect fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long

/**
 * Returns the whole number of the specified time [units][unit] between `this` and [other] instants.
 *
 * The value returned is:
 * - Positive or zero if this instant is earlier than the other.
 * - Negative or zero if this instant is later than the other.
 * - Zero if this instant is equal to the other.
 *
 * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.untilAsTimeBasedUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().until(other.toStdlibInstant(), unit)")
)
public fun Instant.until(other: Instant, unit: DateTimeUnit.TimeBased): Long =
    try {
        multiplyAddAndDivide(other.epochSeconds - epochSeconds,
            NANOS_PER_ONE.toLong(),
            (other.nanosecondsOfSecond - nanosecondsOfSecond).toLong(),
            unit.nanoseconds)
    } catch (_: ArithmeticException) {
        if (this < other) Long.MAX_VALUE else Long.MIN_VALUE
    }

/**
 * Returns the number of whole days between two instants in the specified [timeZone].
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see Instant.until
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.daysUntil
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().daysUntil(other.toStdlibInstant(), timeZone)")
)
public fun Instant.daysUntil(other: Instant, timeZone: TimeZone): Int =
        until(other, DateTimeUnit.DAY, timeZone).clampToInt()

/**
 * Returns the number of whole months between two instants in the specified [timeZone].
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see Instant.until
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.monthsUntil
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().monthsUntil(other.toStdlibInstant(), timeZone)")
)
public fun Instant.monthsUntil(other: Instant, timeZone: TimeZone): Int =
        until(other, DateTimeUnit.MONTH, timeZone).clampToInt()

/**
 * Returns the number of whole years between two instants in the specified [timeZone].
 *
 * If the result does not fit in [Int], returns [Int.MAX_VALUE] for a positive result or [Int.MIN_VALUE] for a negative result.
 *
 * @see Instant.until
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.yearsUntil
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().yearsUntil(other.toStdlibInstant(), timeZone)")
)
public fun Instant.yearsUntil(other: Instant, timeZone: TimeZone): Int =
        until(other, DateTimeUnit.YEAR, timeZone).clampToInt()

/**
 * Returns a [DateTimePeriod] representing the difference between [other] and `this` instants.
 *
 * The components of [DateTimePeriod] are calculated so that adding it back to the `other` instant results in this instant.
 *
 * All components of the [DateTimePeriod] returned are:
 * - Negative or zero if this instant is earlier than the other.
 * - Positive or zero if this instant is later than the other.
 * - Exactly zero if this instant is equal to the other.
 *
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @see Instant.periodUntil
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusInstantInZone
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(other.toStdlibInstant(), timeZone)")
)
public fun Instant.minus(other: Instant, timeZone: TimeZone): DateTimePeriod =
        other.periodUntil(this, timeZone)


/**
 * Returns an instant that is the result of adding one [unit] to this instant
 * in the specified [timeZone].
 *
 * The returned instant is later than this instant.
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(1, unit, timeZone).toDeprecatedInstant()")
)
public expect fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant

/**
 * Returns an instant that is the result of subtracting one [unit] from this instant
 * in the specified [timeZone].
 *
 * The returned instant is earlier than this instant.
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(1, unit, timeZone).toDeprecatedInstant()")
)
public fun Instant.minus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(-1, unit, timeZone)

/**
 * Returns an instant that is the result of adding one [unit] to this instant.
 *
 * The returned instant is later than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(1, unit).toDeprecatedInstant()")
)
public fun Instant.plus(unit: DateTimeUnit.TimeBased): Instant =
    plus(1L, unit)

/**
 * Returns an instant that is the result of subtracting one [unit] from this instant.
 *
 * The returned instant is earlier than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(1, unit).toDeprecatedInstant()")
)
public fun Instant.minus(unit: DateTimeUnit.TimeBased): Instant =
    plus(-1L, unit)

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant
 * in the specified [timeZone].
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * Note that the time zone does not need to be passed when the [unit] is a time-based unit.
 * It is also not needed when adding date-based units to a [LocalDate][LocalDate.plus].
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusDateTimeUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()")
)
public expect fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant
 * in the specified [timeZone].
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * Note that the time zone does not need to be passed when the [unit] is a time-based unit.
 * It is also not needed when subtracting date-based units from a [LocalDate].
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusDateTimeUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(value, unit, timeZone).toDeprecatedInstant()")
)
public expect fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant.
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusTimeBasedUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit).toDeprecatedInstant()")
)
public fun Instant.plus(value: Int, unit: DateTimeUnit.TimeBased): Instant =
    plus(value.toLong(), unit)

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant.
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusTimeBasedUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(value, unit).toDeprecatedInstant()")
)
public fun Instant.minus(value: Int, unit: DateTimeUnit.TimeBased): Instant =
    minus(value.toLong(), unit)

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant
 * in the specified [timeZone].
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * Note that the time zone does not need to be passed when the [unit] is a time-based unit.
 * It is also not needed when adding date-based units to a [LocalDate].
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusDateTimeUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit, timeZone).toDeprecatedInstant()")
)
public expect fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant
 * in the specified [timeZone].
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * Note that the time zone does not need to be passed when the [unit] is a time-based unit.
 * It is also not needed when subtracting date-based units from a [LocalDate].
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusDateTimeUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(value, unit, timeZone).toDeprecatedInstant()")
)
public fun Instant.minus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    if (value != Long.MIN_VALUE) {
        plus(-value, unit, timeZone)
    } else {
        plus(-(value + 1), unit, timeZone).plus(1, unit, timeZone)
    }

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant.
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusTimeBasedUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().plus(value, unit).toDeprecatedInstant()")
)
public expect fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant.
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusTimeBasedUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(value, unit).toDeprecatedInstant()")
)
public fun Instant.minus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    if (value != Long.MIN_VALUE) {
        plus(-value, unit)
    } else {
        plus(-(value + 1), unit).plus(1, unit)
    }

/**
 * Returns the whole number of the specified date or time [units][unit] between [other] and `this` instants
 * in the specified [timeZone].
 *
 * The value returned is negative or zero if this instant is earlier than the other,
 * and positive or zero if this instant is later than the other.
 *
 * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
 *
 * @throws DateTimeArithmeticException if `this` or [other] instant is too large to fit in [LocalDateTime].
 * @see Instant.until for the same operation but with swapped arguments.
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusAsDateTimeUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(other.toStdlibInstant(), unit, timeZone)")
)
public fun Instant.minus(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long =
        other.until(this, unit, timeZone)

/**
 * Returns the whole number of the specified time [units][unit] between [other] and `this` instants.
 *
 * The value returned is negative or zero if this instant is earlier than the other,
 * and positive or zero if this instant is later than the other.
 *
 * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
 *
 * @see Instant.until for the same operation but with swapped arguments.
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusAsTimeBasedUnit
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().minus(other.toStdlibInstant(), unit)")
)
public fun Instant.minus(other: Instant, unit: DateTimeUnit.TimeBased): Long =
    other.until(this, unit)

/**
 * Formats this value using the given [format] using the given [offset].
 *
 * Equivalent to calling [DateTimeFormat.format] on [format] and using [DateTimeComponents.setDateTimeOffset] in
 * the lambda.
 *
 * [DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET] is a format very similar to the one used by [toString].
 * The only difference is that [Instant.toString] adds trailing zeros to the fraction-of-second component so that the
 * number of digits after a dot is a multiple of three.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.formatting
 */
@Deprecated("kotlinx.datetime.Instant is superseded by kotlin.time.Instant",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("this.toStdlibInstant().format(format, offset)")
)
public fun Instant.format(format: DateTimeFormat<DateTimeComponents>, offset: UtcOffset = UtcOffset.ZERO): String {
    val instant = this
    return format.format { setDateTimeOffset(instant.toStdlibInstant(), offset) }
}

/**
 * An instance of this class can not be obtained, and it should not be used.
 *
 * The purpose of this class is to allow defining functions that return `kotlin.time.Instant`,
 * but still keep the functions returning `kotlinx.datetime.Instant` for binary compatibility.
 * Kotlin does not allow two functions with the same name and parameter types but different return types,
 * so we need to use a trick to achieve this.
 * By introducing a fictional parameter of this type, we can pretend that the function has a different signature,
 * even though it can only be called exactly the same way as the function without this parameter used to be.
 * There is no ambiguity, as the old functions are deprecated and hidden and can not actually be called.
 *
 * @suppress this class is not meant to be used, so the documentation is only here for the curious reader.
 */
@Deprecated(
    "It is meaningless to try to pass an OverloadMarker to a function directly. " +
            "All functions accepting it have its instance as a default value.",
    level = DeprecationLevel.ERROR
)
public class OverloadMarker private constructor() {
    internal companion object {
        @Suppress("DEPRECATION_ERROR")
        internal val INSTANCE = OverloadMarker()
    }
}
