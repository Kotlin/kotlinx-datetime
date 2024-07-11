/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.format.*
import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.datetime.serializers.InstantComponentSerializer
import kotlinx.serialization.Serializable
import kotlin.time.*

/**
 * A moment in time.
 *
 * A point in time must be uniquely identified in a way that is independent of a time zone.
 * For example, `1970-01-01, 00:00:00` does not represent a moment in time since this would happen at different times
 * in different time zones: someone in Tokyo would think it is already `1970-01-01` several hours earlier than someone in
 * Berlin would. To represent such entities, use [LocalDateTime].
 * In contrast, "the moment the clocks in London first showed 00:00 on Jan 1, 2000" is a specific moment
 * in time, as is "1970-01-01, 00:00:00 UTC+0", so it can be represented as an [Instant].
 *
 * `Instant` uses the UTC-SLS (smeared leap second) time scale. This time scale doesn't contain instants
 * corresponding to leap seconds, but instead "smears" positive and negative leap seconds among the last 1000 seconds
 * of the day when a leap second happens.
 *
 * ### Obtaining the current moment
 *
 * The [Clock] interface is the primary way to obtain the current moment:
 *
 * ```
 * val clock: Clock = Clock.System
 * val instant = clock.now()
 * ```
 *
 * The [Clock.System] implementation uses the platform-specific system clock to obtain the current moment.
 * Note that this clock is not guaranteed to be monotonic, and the user or the system may adjust it at any time,
 * so it should not be used for measuring time intervals.
 * For that, consider using [TimeSource.Monotonic] and [TimeMark] instead of [Clock.System] and [Instant].
 *
 * ### Obtaining human-readable representations
 *
 * #### Date and time
 *
 * [Instant] is essentially the number of seconds and nanoseconds since a designated moment in time,
 * stored as something like `1709898983.123456789`.
 * [Instant] does not contain information about the day or time, as this depends on the time zone.
 * To work with this information for a specific time zone, obtain a [LocalDateTime] using [Instant.toLocalDateTime]:
 *
 * ```
 * val instant = Instant.fromEpochSeconds(1709898983, 123456789)
 * instant.toLocalDateTime(TimeZone.of("Europe/Berlin")) // 2024-03-08T12:56:23.123456789
 * instant.toLocalDateTime(TimeZone.UTC) // 2024-03-08T11:56:23.123456789
 * ```
 *
 * For values very far in the past or the future, this conversion may fail.
 * The specific range of values that can be converted to [LocalDateTime] is platform-specific, but at least
 * [DISTANT_PAST], [DISTANT_FUTURE], and all values between them can be converted to [LocalDateTime].
 *
 * #### Date or time separately
 *
 * To obtain a [LocalDate] or [LocalTime], first, obtain a [LocalDateTime], and then use its [LocalDateTime.date]
 * and [LocalDateTime.time] properties:
 *
 * ```
 * val instant = Instant.fromEpochSeconds(1709898983, 123456789)
 * instant.toLocalDateTime(TimeZone.of("Europe/Berlin")).date // 2024-03-08
 * ```
 *
 * ### Arithmetic operations
 *
 * #### Elapsed-time-based
 *
 * The [plus] and [minus] operators can be used to add [Duration]s to and subtract them from an [Instant]:
 *
 * ```
 * Clock.System.now() + 5.seconds // 5 seconds from now
 * ```
 *
 * Durations can also be represented as multiples of some [time-based datetime unit][DateTimeUnit.TimeBased]:
 *
 * ```
 * Clock.System.now().plus(4, DateTimeUnit.HOUR) // 4 hours from now
 * ```
 *
 * Also, there is a [minus] operator that returns the [Duration] representing the difference between two instants:
 *
 * ```
 * val start = Clock.System.now()
 * val concertStart = LocalDateTime(2023, 1, 1, 20, 0, 0).toInstant(TimeZone.of("Europe/Berlin"))
 * val timeUntilConcert = concertStart - start
 * ```
 *
 * #### Calendar-based
 *
 * Since [Instant] represents a point in time, it is always well-defined what the result of arithmetic operations on it
 * is, including the cases when a calendar is used.
 * This is not the case for [LocalDateTime], where the result of arithmetic operations depends on the time zone.
 * See the [LocalDateTime] documentation for more details.
 *
 * Adding and subtracting calendar-based units can be done using the [plus] and [minus] operators,
 * requiring a [TimeZone]:
 *
 * ```
 * // One day from now in Berlin
 * Clock.System.now().plus(1, DateTimeUnit.DAY, TimeZone.of("Europe/Berlin"))
 *
 * // A day and two hours short from two months later in Berlin
 * Clock.System.now().plus(DateTimePeriod(months = 2, days = -1, hours = -2), TimeZone.of("Europe/Berlin"))
 * ```
 *
 * The difference between [Instant] values in terms of calendar-based units can be obtained using the [periodUntil]
 * method:
 *
 * ```
 * val start = Clock.System.now()
 * val concertStart = LocalDateTime(2023, 1, 1, 20, 0, 0).toInstant(TimeZone.of("Europe/Berlin"))
 * val timeUntilConcert = start.periodUntil(concertStart, TimeZone.of("Europe/Berlin"))
 * // Two months, three days, four hours, and five minutes until the concert
 * ```
 *
 * Or the [Instant.until] method, as well as [Instant.daysUntil], [Instant.monthsUntil],
 * and [Instant.yearsUntil] extension functions:
 *
 * ```
 * val start = Clock.System.now()
 * val concertStart = LocalDateTime(2023, 1, 1, 20, 0, 0).toInstant(TimeZone.of("Europe/Berlin"))
 * val timeUntilConcert = start.until(concertStart, DateTimeUnit.DAY, TimeZone.of("Europe/Berlin"))
 * // 63 days until the concert, rounded down
 * ```
 *
 * ### Platform specifics
 *
 * On the JVM, there are `Instant.toJavaInstant()` and `java.time.Instant.toKotlinInstant()`
 * extension functions to convert between `kotlinx.datetime` and `java.time` objects used for the same purpose.
 * Similarly, on the Darwin platforms, there are `Instant.toNSDate()` and `NSDate.toKotlinInstant()`
 * extension functions.
 *
 * ### Construction, serialization, and deserialization
 *
 * [fromEpochSeconds] can be used to construct an instant from the number of seconds since
 * `1970-01-01T00:00:00Z` (the Unix epoch).
 * [epochSeconds] and [nanosecondsOfSecond] can be used to obtain the number of seconds and nanoseconds since the epoch.
 *
 * ```
 * val instant = Instant.fromEpochSeconds(1709898983, 123456789)
 * instant.epochSeconds // 1709898983
 * instant.nanosecondsOfSecond // 123456789
 * ```
 *
 * [fromEpochMilliseconds] allows constructing an instant from the number of milliseconds since the epoch.
 * [toEpochMilliseconds] can be used to obtain the number of milliseconds since the epoch.
 * Note that [Instant] supports nanosecond precision, so converting to milliseconds is a lossy operation.
 *
 * ```
 * val instant1 = Instant.fromEpochSeconds(1709898983, 123456789)
 * instant1.nanosecondsOfSecond // 123456789
 * val milliseconds = instant1.toEpochMilliseconds() // 1709898983123
 * val instant2 = Instant.fromEpochMilliseconds(milliseconds)
 * instant2.nanosecondsOfSecond // 123000000
 * ```
 *
 * [parse] and [toString] methods can be used to obtain an [Instant] from and convert it to a string in the
 * ISO 8601 extended format.
 *
 * ```
 * val instant = Instant.parse("2023-01-02T22:35:01+01:00")
 * instant.toString() // 2023-01-02T21:35:01Z
 * ```
 *
 * During parsing, the UTC offset is not returned separately.
 * If the UTC offset is important, use [DateTimeComponents] with [DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET] to
 * parse the string instead.
 *
 * [Instant.parse] and [Instant.format] also accept custom formats:
 *
 * ```
 * val customFormat = DateTimeComponents.Format {
 *   date(LocalDate.Formats.ISO)
 *   char(' ')
 *   time(LocalTime.Formats.ISO)
 *   char(' ')
 *   offset(UtcOffset.Formats.ISO)
 * }
 * val instant = Instant.parse("2023-01-02 22:35:01.14 +01:00", customFormat)
 * instant.format(customFormat, offset = UtcOffset(hours = 2)) // 2023-01-02 23:35:01.14 +02:00
 * ```
 *
 * Additionally, there are several `kotlinx-serialization` serializers for [Instant]:
 * - [InstantIso8601Serializer] for the ISO 8601 extended format.
 * - [InstantComponentSerializer] for an object with components.
 *
 * @see LocalDateTime for a user-visible representation of moments in time in an unspecified time zone.
 */
@Serializable(with = InstantIso8601Serializer::class)
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
    public val epochSeconds: Long

    /**
     * The number of nanoseconds by which this instant is later than [epochSeconds] from the epoch instant.
     *
     * The value is always non-negative and lies in the range `0..999_999_999`.
     *
     * @see fromEpochSeconds
     * @sample kotlinx.datetime.test.samples.InstantSamples.nanosecondsOfSecond
     */
    public val nanosecondsOfSecond: Int

    /**
     * Returns the number of milliseconds from the epoch instant `1970-01-01T00:00:00Z`.
     *
     * Any fractional part of a millisecond is rounded toward zero to the whole number of milliseconds.
     *
     * If the result does not fit in [Long], returns [Long.MAX_VALUE] for a positive result or [Long.MIN_VALUE] for a negative result.
     *
     * @see fromEpochMilliseconds
     * @sample kotlinx.datetime.test.samples.InstantSamples.toEpochMilliseconds
     */
    public fun toEpochMilliseconds(): Long

    /**
     * Returns an instant that is the result of adding the specified [duration] to this instant.
     *
     * If the [duration] is positive, the returned instant is later than this instant.
     * If the [duration] is negative, the returned instant is earlier than this instant.
     *
     * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
     *
     * **Pitfall**: [Duration.Companion.days] are multiples of 24 hours and are not calendar-based.
     * Consider using the [plus] overload that accepts a multiple of a [DateTimeUnit] instead for calendar-based
     * operations instead of using [Duration].
     * For an explanation of why some days are not 24 hours, see [DateTimeUnit.DayBased].
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.plusDuration
     */
    public operator fun plus(duration: Duration): Instant

    /**
     * Returns an instant that is the result of subtracting the specified [duration] from this instant.
     *
     * If the [duration] is positive, the returned instant is earlier than this instant.
     * If the [duration] is negative, the returned instant is later than this instant.
     *
     * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
     *
     * **Pitfall**: [Duration.Companion.days] are multiples of 24 hours and are not calendar-based.
     * Consider using the [minus] overload that accepts a multiple of a [DateTimeUnit] instead for calendar-based
     * operations instead of using [Duration].
     * For an explanation of why some days are not 24 hours, see [DateTimeUnit.DayBased].
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.minusDuration
     */
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
    public operator fun minus(other: Instant): Duration

    /**
     * Compares `this` instant with the [other] instant.
     * Returns zero if this instant represents the same moment as the other (meaning they are equal to one another),
     * a negative number if this instant is earlier than the other,
     * and a positive number if this instant is later than the other.
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.compareToSample
     */
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
    public override fun toString(): String


    public companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        public fun now(): Instant

        /**
         * Returns an [Instant] that is [epochMilliseconds] number of milliseconds from the epoch instant `1970-01-01T00:00:00Z`.
         *
         * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
         * In any case, it is guaranteed that instants between [DISTANT_PAST] and [DISTANT_FUTURE] can be represented.
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
         * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
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
         * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
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
         * [LocalDateTime] without exceptions in every time zone on all supported platforms.
         *
         * [isDistantPast] returns true for this value and all earlier ones.
         */
        public val DISTANT_PAST: Instant // -100001-12-31T23:59:59.999999999Z

        /**
         * An instant value that is far in the future.
         *
         * All instants in the range `DISTANT_PAST..DISTANT_FUTURE` can be [converted][Instant.toLocalDateTime] to
         * [LocalDateTime] without exceptions in every time zone on all supported platforms.
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
public val Instant.isDistantPast: Boolean
    get() = this <= Instant.DISTANT_PAST

/**
 * Returns true if the instant is [Instant.DISTANT_FUTURE] or later.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.isDistantFuture
 */
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
public fun Instant.minus(period: DateTimePeriod, timeZone: TimeZone): Instant =
    /* An overflow can happen for any component, but we are only worried about nanoseconds, as having an overflow in
    any other component means that `plus` will throw due to the minimum value of the numeric type overflowing the
    platform-specific limits. */
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
 *     Or (only on the JVM) if the number of months between the two dates exceeds an Int.
 * @sample kotlinx.datetime.test.samples.InstantSamples.periodUntil
 */
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
public fun Instant.until(other: Instant, unit: DateTimeUnit.TimeBased): Long =
    try {
        multiplyAddAndDivide(other.epochSeconds - epochSeconds,
            NANOS_PER_ONE.toLong(),
            (other.nanosecondsOfSecond - nanosecondsOfSecond).toLong(),
            unit.nanoseconds)
    } catch (e: ArithmeticException) {
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
 *   Or (only on the JVM) if the number of months between the two dates exceeds an Int.
 * @see Instant.periodUntil
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusInstantInZone
 */
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
@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit, timeZone)"))
public expect fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant

/**
 * Returns an instant that is the result of subtracting one [unit] from this instant
 * in the specified [timeZone].
 *
 * The returned instant is earlier than this instant.
 *
 * @throws DateTimeArithmeticException if this value or the result is too large to fit in [LocalDateTime].
 */
@Deprecated("Use the minus overload with an explicit number of units", ReplaceWith("this.minus(1, unit, timeZone)"))
public fun Instant.minus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(-1, unit, timeZone)

/**
 * Returns an instant that is the result of adding one [unit] to this instant.
 *
 * The returned instant is later than this instant.
 *
 * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
 */
@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public fun Instant.plus(unit: DateTimeUnit.TimeBased): Instant =
    plus(1L, unit)

/**
 * Returns an instant that is the result of subtracting one [unit] from this instant.
 *
 * The returned instant is earlier than this instant.
 *
 * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
 */
@Deprecated("Use the minus overload with an explicit number of units", ReplaceWith("this.minus(1, unit)"))
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
public expect fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant

/**
 * Returns an instant that is the result of adding the [value] number of the specified [unit] to this instant.
 *
 * If the [value] is positive, the returned instant is later than this instant.
 * If the [value] is negative, the returned instant is earlier than this instant.
 *
 * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusTimeBasedUnit
 */
public fun Instant.plus(value: Int, unit: DateTimeUnit.TimeBased): Instant =
    plus(value.toLong(), unit)

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant.
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusTimeBasedUnit
 */
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
 * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.plusTimeBasedUnit
 */
public expect fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant

/**
 * Returns an instant that is the result of subtracting the [value] number of the specified [unit] from this instant.
 *
 * If the [value] is positive, the returned instant is earlier than this instant.
 * If the [value] is negative, the returned instant is later than this instant.
 *
 * The return value is clamped to the platform-specific boundaries for [Instant] if the result exceeds them.
 *
 * @sample kotlinx.datetime.test.samples.InstantSamples.minusTimeBasedUnit
 */
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
public fun Instant.format(format: DateTimeFormat<DateTimeComponents>, offset: UtcOffset = UtcOffset.ZERO): String {
    val instant = this
    return format.format { setDateTimeOffset(instant, offset) }
}

internal const val DISTANT_PAST_SECONDS = -3217862419201
internal const val DISTANT_FUTURE_SECONDS = 3093527980800
