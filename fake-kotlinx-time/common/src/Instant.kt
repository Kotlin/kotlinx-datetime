/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.time

import kotlin.math.absoluteValue
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A moment in time.
 *
 * A point in time must be uniquely identified in a way that is independent of a time zone.
 * For example, `1970-01-01, 00:00:00` does not represent a moment in time since this would happen at different times
 * in different time zones: someone in Tokyo would think it is already `1970-01-01` several hours earlier than someone in
 * Berlin would. To represent such entities, use the `LocalDateTime` from `kotlinx-datetime`.
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
 * ### Arithmetic operations
 *
 * The [plus] and [minus] operators can be used to add [Duration]s to and subtract them from an [Instant]:
 *
 * ```
 * Clock.System.now() + 5.seconds // 5 seconds from now
 * ```
 *
 * Also, there is a [minus] operator that returns the [Duration] representing the difference between two instants:
 *
 * ```
 * val kotlinRelease = Instant.parse("2016-02-15T02:00T12:00:00+03:00")
 * val kotlinStableDuration = Clock.System.now() - kotlinRelease
 * ```
 *
 * ### Platform specifics
 *
 * On the JVM, there are `Instant.toJavaInstant()` and `java.time.Instant.toKotlinInstant()`
 * extension functions to convert between `kotlin.time` and `java.time` objects used for the same purpose.
 * Likewise, on JS, there are `Instant.toJSDate()` and `Date.toKotlinInstant()` extension functions.
 *
 * For technical reasons, converting [Instant] to and from Foundation's `NSDate` is provided in
 * `kotlinx-datetime` via `Instant.toNSDate()` and `NSDate.toKotlinInstant()` extension functions.
 * These functions will be made available in `kotlin.time` in the future.
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
 */
public class Instant internal constructor(
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
    public val epochSeconds: Long,
    /**
     * The number of nanoseconds by which this instant is later than [epochSeconds] from the epoch instant.
     *
     * The value is always non-negative and lies in the range `0..999_999_999`.
     *
     * @see fromEpochSeconds
     * @sample kotlinx.datetime.test.samples.InstantSamples.nanosecondsOfSecond
     */
    public val nanosecondsOfSecond: Int
) : Comparable<Instant> {

    init {
        require(epochSeconds in MIN_SECOND..MAX_SECOND) { "Instant exceeds minimum or maximum instant" }
    }

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
    // org.threeten.bp.Instant#toEpochMilli
    public fun toEpochMilliseconds(): Long = try {
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

    /**
     * Returns an instant that is the result of adding the specified [duration] to this instant.
     *
     * If the [duration] is positive, the returned instant is later than this instant.
     * If the [duration] is negative, the returned instant is earlier than this instant.
     *
     * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
     *
     * **Pitfall**: [Duration.Companion.days] are multiples of 24 hours, but in some time zones,
     * some days can be shorter or longer because clocks are shifted.
     * Consider using `kotlinx-datetime` for arithmetic operations that take time zone transitions into account.
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.plusDuration
     */
    public operator fun plus(duration: Duration): Instant = duration.toComponents { secondsToAdd, nanosecondsToAdd ->
        try {
            plus(secondsToAdd, nanosecondsToAdd.toLong())
        } catch (_: IllegalArgumentException) {
            if (duration.isPositive()) MAX else MIN
        } catch (_: ArithmeticException) {
            if (duration.isPositive()) MAX else MIN
        }
    }

    /**
     * Returns an instant that is the result of subtracting the specified [duration] from this instant.
     *
     * If the [duration] is positive, the returned instant is earlier than this instant.
     * If the [duration] is negative, the returned instant is later than this instant.
     *
     * The return value is clamped to the boundaries of [Instant] if the result exceeds them.
     *
     * **Pitfall**: [Duration.Companion.days] are multiples of 24 hours, but in some time zones,
     * some days can be shorter or longer because clocks are shifted.
     * Consider using `kotlinx-datetime` for arithmetic operations that take time zone transitions into account.
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.minusDuration
     */
    public operator fun minus(duration: Duration): Instant = plus(-duration)

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
    public operator fun minus(other: Instant): Duration =
        (this.epochSeconds - other.epochSeconds).seconds + // won't overflow given the instant bounds
                (this.nanosecondsOfSecond - other.nanosecondsOfSecond).nanoseconds

    /**
     * Compares `this` instant with the [other] instant.
     * Returns zero if this instant represents the same moment as the other (meaning they are equal to one another),
     * a negative number if this instant is earlier than the other,
     * and a positive number if this instant is later than the other.
     */
    public override operator fun compareTo(other: Instant): Int {
        val s = epochSeconds.compareTo(other.epochSeconds)
        if (s != 0) {
            return s
        }
        return nanosecondsOfSecond.compareTo(other.nanosecondsOfSecond)
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is Instant && this.epochSeconds == other.epochSeconds
                && this.nanosecondsOfSecond == other.nanosecondsOfSecond

    // org.threeten.bp.Instant#hashCode
    override fun hashCode(): Int =
        (epochSeconds xor (epochSeconds ushr 32)).toInt() + 51 * nanosecondsOfSecond

    /**
     * Converts this instant to the ISO 8601 string representation, for example, `2023-01-02T23:40:57.120Z`.
     *
     * The representation uses the UTC-SLS time scale instead of UTC.
     * In practice, this means that leap second handling will not be readjusted to the UTC.
     * Leap seconds will not be added or skipped, so it is impossible to acquire a string
     * where the component for seconds is 60, and for any day, it's possible to observe 23:59:59.
     *
     * @see parse
     *
     * @sample kotlinx.datetime.test.samples.InstantSamples.toStringSample
     */
    public override fun toString(): String = formatIso(this)

    public companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        public fun now(): Instant = currentTime()

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
        // org.threeten.bp.Instant#ofEpochMilli
        public fun fromEpochMilliseconds(epochMilliseconds: Long): Instant {
            val epochSeconds = epochMilliseconds.floorDiv(MILLIS_PER_ONE.toLong())
            val nanosecondsOfSecond = (epochMilliseconds.mod(MILLIS_PER_ONE.toLong()) * NANOS_PER_MILLI).toInt()
            return when {
                epochSeconds < MIN_SECOND -> MIN
                epochSeconds > MAX_SECOND -> MAX
                else -> fromEpochSeconds(epochSeconds, nanosecondsOfSecond)
            }
        }

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
        // org.threeten.bp.Instant#ofEpochSecond(long, long)
        public fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long = 0): Instant =
            try {
                fromEpochSecondsThrowing(epochSeconds, nanosecondAdjustment)
            } catch (_: ArithmeticException) {
                if (epochSeconds > 0) MAX else MIN
            } catch (_: IllegalArgumentException) {
                if (epochSeconds > 0) MAX else MIN
            }

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
        public fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant =
            fromEpochSeconds(epochSeconds, nanosecondAdjustment.toLong())

        /**
         * Parses an ISO 8601 string that represents an instant (for example, `2020-08-30T18:43:00Z`).
         *
         * Guaranteed to parse all strings that [Instant.toString] produces.
         *
         * Examples of instants in the ISO 8601 format:
         * - `2020-08-30T18:43:00Z`
         * - `2020-08-30T18:43:00.50Z`
         * - `2020-08-30T18:43:00.123456789Z`
         * - `2020-08-30T18:40:00+03:00`
         * - `2020-08-30T18:40:00+03:30:20`
         * * `2020-01-01T23:59:59.123456789+01`
         * * `+12020-01-31T23:59:59Z`
         *
         * See ISO-8601-1:2019, 5.4.2.1b), excluding the format without the offset.
         *
         * The string is considered to represent time on the UTC-SLS time scale instead of UTC.
         * In practice, this means that, even if there is a leap second on the given day, it will not affect how the
         * time is parsed, even if it's in the last 1000 seconds of the day.
         * Instead, even if there is a negative leap second on the given day, 23:59:59 is still considered a valid time.
         * 23:59:60 is invalid on UTC-SLS, so parsing it will fail.
         *
         * @throws IllegalArgumentException if the text cannot be parsed or the boundaries of [Instant] are exceeded.
         *
         * @see Instant.toString for formatting.
         * @sample kotlinx.datetime.test.samples.InstantSamples.parsing
         */
        public fun parse(input: CharSequence): Instant = parseIso(input)


        /**
         * An instant value that is far in the past.
         *
         * [isDistantPast] returns true for this value and all earlier ones.
         */
        public val DISTANT_PAST: Instant // -100001-12-31T23:59:59.999999999Z
            get() = fromEpochSeconds(DISTANT_PAST_SECONDS, 999_999_999)

        /**
         * An instant value that is far in the future.
         *
         * [isDistantFuture] returns true for this value and all later ones.
         */
        public val DISTANT_FUTURE: Instant // +100000-01-01T00:00:00Z
            get() = fromEpochSeconds(DISTANT_FUTURE_SECONDS, 0)

        /**
         * @throws ArithmeticException if arithmetic overflow occurs
         * @throws IllegalArgumentException if the boundaries of Instant are overflown
         */
        private fun fromEpochSecondsThrowing(epochSeconds: Long, nanosecondAdjustment: Long): Instant {
            val secs = safeAdd(epochSeconds, nanosecondAdjustment.floorDiv(NANOS_PER_ONE.toLong()))
            val nos = nanosecondAdjustment.mod(NANOS_PER_ONE.toLong()).toInt()
            return Instant(secs, nos)
        }

        internal val MIN = Instant(MIN_SECOND, 0)
        internal val MAX = Instant(MAX_SECOND, 999_999_999)
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

internal const val DISTANT_PAST_SECONDS = -3217862419201
internal const val DISTANT_FUTURE_SECONDS = 3093527980800

internal expect fun currentTime(): Instant

/**
 * The minimum supported epoch second.
 */
private const val MIN_SECOND = -31557014167219200L // -1000000000-01-01T00:00:00Z

/**
 * The maximum supported epoch second.
 */
private const val MAX_SECOND = 31556889864403199L // +1000000000-12-31T23:59:59

private class UnboundedLocalDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val nanosecond: Int,
) {
    fun toInstant(offsetSeconds: Int): Instant {
        val epochSeconds = run {
            // org.threeten.bp.LocalDate#toEpochDay
            val epochDays = run {
                val y = year.toLong()
                var total = 365 * y
                if (y >= 0) {
                    total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
                } else {
                    total -= y / -4 - y / -100 + y / -400
                }
                total += ((367 * month - 362) / 12)
                total += day - 1
                if (month > 2) {
                    total--
                    if (!isLeapYear(year)) {
                        total--
                    }
                }
                total - DAYS_0000_TO_1970
            }
            // org.threeten.bp.LocalTime#toSecondOfDay
            val daySeconds = hour * SECONDS_PER_HOUR + minute * SECONDS_PER_MINUTE + second
            // org.threeten.bp.chrono.ChronoLocalDateTime#toEpochSecond
            epochDays * 86400L + daySeconds - offsetSeconds
        }
        if (epochSeconds < Instant.MIN.epochSeconds || epochSeconds > Instant.MAX.epochSeconds)
            throw IllegalArgumentException(
                "The parsed date is outside the range representable by Instant (Unix epoch second $epochSeconds)"
            )
        return Instant.fromEpochSeconds(epochSeconds, nanosecond)
    }

    override fun toString(): String = "UnboundedLocalDateTime($year-$month-$day $hour:$minute:$second.$nanosecond)"

    companion object {
        fun fromInstant(instant: Instant, offsetSeconds: Int): UnboundedLocalDateTime {
            val localSecond: Long = instant.epochSeconds + offsetSeconds
            val epochDays = localSecond.floorDiv(SECONDS_PER_DAY.toLong())
            val secsOfDay = localSecond.mod(SECONDS_PER_DAY.toLong()).toInt()
            val year: Int
            val month: Int
            val day: Int
            // org.threeten.bp.LocalDate#toEpochDay
            run {
                var zeroDay = epochDays + DAYS_0000_TO_1970
                // find the march-based year
                zeroDay -= 60 // adjust to 0000-03-01 so leap day is at end of four year cycle

                var adjust = 0L
                if (zeroDay < 0) { // adjust negative years to positive for calculation
                    val adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1
                    adjust = adjustCycles * 400
                    zeroDay += -adjustCycles * DAYS_PER_CYCLE
                }
                var yearEst = ((400 * zeroDay + 591) / DAYS_PER_CYCLE)
                var doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
                if (doyEst < 0) { // fix estimate
                    yearEst--
                    doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400)
                }
                yearEst += adjust // reset any negative year

                val marchDoy0 = doyEst.toInt()

                // convert march-based values back to january-based
                val marchMonth0 = (marchDoy0 * 5 + 2) / 153
                month = (marchMonth0 + 2) % 12 + 1
                day = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
                year = (yearEst + marchMonth0 / 10).toInt()
            }
            val hours = (secsOfDay / SECONDS_PER_HOUR)
            val secondWithoutHours = secsOfDay - hours * SECONDS_PER_HOUR
            val minutes = (secondWithoutHours / SECONDS_PER_MINUTE)
            val second = secondWithoutHours - minutes * SECONDS_PER_MINUTE
            return UnboundedLocalDateTime(year, month, day, hours, minutes, second, instant.nanosecondsOfSecond)
        }
    }
}

private fun parseIso(isoString: CharSequence): Instant {
    fun parseFailure(error: String): Nothing {
        throw IllegalArgumentException("$error when parsing an Instant from $isoString")
    }
    fun expect(what: String, where: Int, predicate: (Char) -> Boolean) {
        val c = isoString[where]
        if (!predicate(c)) {
            parseFailure("Expected $what, but got $c at position $where")
        }
    }
    val s = isoString
    var i = 0
    require(s.isNotEmpty()) { "An empty string is not a valid Instant" }
    val yearSign = when (val c = s[i]) {
        '+', '-' -> { ++i; c }
        else -> ' '
    }
    val yearStart = i
    var absYear = 0
    while (i < s.length && s[i] in '0'..'9') {
        absYear = absYear * 10 + (s[i] - '0')
        ++i
    }
    val year = when {
        i > yearStart + 10 -> {
            parseFailure("Expected at most 10 digits for the year number, got ${i - yearStart}")
        }
        i == yearStart + 10 && s[yearStart] >= '2' -> {
            parseFailure("Expected at most 9 digits for the year number or year 1000000000, got ${i - yearStart}")
        }
        i - yearStart < 4 -> {
            parseFailure("The year number must be padded to 4 digits, got ${i - yearStart} digits")
        }
        else -> {
            if (yearSign == '+' && i - yearStart == 4) {
                parseFailure("The '+' sign at the start is only valid for year numbers longer than 4 digits")
            }
            if (yearSign == ' ' && i - yearStart != 4) {
                parseFailure("A '+' or '-' sign is required for year numbers longer than 4 digits")
            }
            if (yearSign == '-') -absYear else absYear
        }
    }
    // reading at least -MM-DDTHH:MM:SSZ
    //                  0123456789012345 16 chars
    if (s.length < i + 16) {
        parseFailure("The input string is too short")
    }
    expect("'-'", i) { it == '-' }
    expect("'-'", i + 3) { it == '-' }
    expect("'T' or 't'", i + 6) { it == 'T' || it == 't' }
    expect("':'", i + 9) { it == ':' }
    expect("':'", i + 12) { it == ':' }
    for (j in listOf(1, 2, 4, 5, 7, 8, 10, 11, 13, 14)) {
        expect("an ASCII digit", i + j) { it in '0'..'9' }
    }
    fun twoDigitNumber(index: Int) = s[index].code * 10 + s[index + 1].code - '0'.code * 11
    val month = twoDigitNumber(i + 1)
    val day = twoDigitNumber(i + 4)
    val hour = twoDigitNumber(i + 7)
    val minute = twoDigitNumber(i + 10)
    val second = twoDigitNumber(i + 13)
    val nanosecond = if (s[i + 15] == '.') {
        val fractionStart = i + 16
        i = fractionStart
        var fraction = 0
        while (i < s.length && s[i] in '0'..'9') {
            fraction = fraction * 10 + (s[i] - '0')
            ++i
        }
        if (i - fractionStart in 1..9) {
            fraction * POWERS_OF_TEN[fractionStart + 9 - i]
        } else {
            parseFailure("1..9 digits are supported for the fraction of the second, got {i - fractionStart}")
        }
    } else {
        i += 15
        0
    }
    val offsetSeconds = when (val sign = s.getOrNull(i)) {
        null -> {
            parseFailure("The UTC offset at the end of the string is missing")
        }
        'z', 'Z' -> if (s.length == i + 1) {
            0
        } else {
            parseFailure("Extra text after the instant at position ${i + 1}")
        }
        '-', '+' -> {
            val offsetStrLength = s.length - i
            if (offsetStrLength % 3 != 0) { parseFailure("Invalid UTC offset string '${s.substring(i)}'") }
            if (offsetStrLength > 9) { parseFailure("The UTC offset string '${s.substring(i)}' is too long") }
            for (j in listOf(3, 6)) {
                if ((s.getOrNull(i + j) ?: break) != ':')
                    parseFailure("Expected ':' at index ${i + j}, got '${s[i + j]}'")
            }
            for (j in listOf(1, 2, 4, 5, 7, 8)) {
                if ((s.getOrNull(i + j) ?: break) !in '0'..'9')
                    parseFailure("Expected a digit at index ${i + j}, got '${s[i + j]}'")
            }
            val offsetHour = twoDigitNumber(i + 1)
            val offsetMinute = if (offsetStrLength > 3) { twoDigitNumber(i + 4) } else { 0 }
            val offsetSecond = if (offsetStrLength > 6) { twoDigitNumber(i + 7) } else { 0 }
            if (offsetMinute > 59) { parseFailure("Expected offset-minute-of-hour in 0..59, got $offsetMinute") }
            if (offsetSecond > 59) { parseFailure("Expected offset-second-of-minute in 0..59, got $offsetSecond") }
            if (offsetHour > 17 && !(offsetHour == 18 && offsetMinute == 0 && offsetSecond == 0)) {
                parseFailure("Expected an offset in -18:00..+18:00, got $sign$offsetHour:$offsetMinute:$offsetSecond")
            }
            (offsetHour * 3600 + offsetMinute * 60 + offsetSecond) * if (sign == '-') -1 else 1
        }
        else -> {
            parseFailure("Expected the UTC offset at position $i, got '$sign'")
        }
    }
    if (month !in 1..12) { parseFailure("Expected a month number in 1..12, got $month") }
    if (day !in 1..month.monthLength(isLeapYear(year))) {
        parseFailure("Expected a valid day-of-month for $year-$month, got $day")
    }
    if (hour > 23) { parseFailure("Expected hour in 0..23, got $hour") }
    if (minute > 59) { parseFailure("Expected minute-of-hour in 0..59, got $minute") }
    if (second > 59) { parseFailure("Expected second-of-minute in 0..59, got $second") }
    return UnboundedLocalDateTime(year, month, day, hour, minute, second, nanosecond).toInstant(offsetSeconds)
}

private fun formatIso(instant: Instant): String = buildString {
    val ldt = UnboundedLocalDateTime.fromInstant(instant, 0)
    fun Appendable.appendTwoDigits(number: Int) {
        if (number < 10) append('0')
        append(number)
    }
    run {
        val number = ldt.year
        when {
            number.absoluteValue < 1_000 -> {
                val innerBuilder = StringBuilder()
                if (number >= 0) {
                    innerBuilder.append((number + 10_000)).deleteAt(0)
                } else {
                    innerBuilder.append((number - 10_000)).deleteAt(1)
                }
                append(innerBuilder)
            }
            else -> {
                if (number >= 10_000) append('+')
                append(number)
            }
        }
    }
    append('-')
    appendTwoDigits(ldt.month)
    append('-')
    appendTwoDigits(ldt.day)
    append('T')
    appendTwoDigits(ldt.hour)
    append(':')
    appendTwoDigits(ldt.minute)
    append(':')
    appendTwoDigits(ldt.second)
    if (ldt.nanosecond != 0) {
        append('.')
        var zerosToStrip = 0
        while (ldt.nanosecond % POWERS_OF_TEN[zerosToStrip + 1] == 0) {
            ++zerosToStrip
        }
        zerosToStrip -= (zerosToStrip.mod(3)) // rounding down to a multiple of 3
        val numberToOutput = ldt.nanosecond / POWERS_OF_TEN[zerosToStrip]
        append((numberToOutput + POWERS_OF_TEN[9 - zerosToStrip]).toString().substring(1))
    }
    append('Z')
}

/**
 * All code below was taken from various places of https://github.com/ThreeTen/threetenbp with few changes
 */

/**
 * The number of days in a 400 year cycle.
 */
private const val DAYS_PER_CYCLE = 146097

/**
 * The number of days from year zero to year 1970.
 * There are five 400 year cycles from year zero to 2000.
 * There are 7 leap years from 1970 to 2000.
 */
private const val DAYS_0000_TO_1970 = DAYS_PER_CYCLE * 5 - (30 * 365 + 7)

/**
 * Safely adds two long values.
 * throws [ArithmeticException] if the result overflows a long
 */
private fun safeAdd(a: Long, b: Long): Long {
    val sum = a + b
    // check for a change of sign in the result when the inputs have the same sign
    if ((a xor sum) < 0 && (a xor b) >= 0) {
        throw ArithmeticException("Addition overflows a long: $a + $b")
    }
    return sum
}

/**
 * Safely multiply a long by a long.
 *
 * @param a  the first value
 * @param b  the second value
 * @return the new total
 * @throws ArithmeticException if the result overflows a long
 */
private fun safeMultiply(a: Long, b: Long): Long {
    if (b == 1L) {
        return a
    }
    if (a == 1L) {
        return b
    }
    if (a == 0L || b == 0L) {
        return 0
    }
    val total = a * b
    if (total / b != a || a == Long.MIN_VALUE && b == -1L || b == Long.MIN_VALUE && a == -1L) {
        throw ArithmeticException("Multiplication overflows a long: $a * $b")
    }
    return total
}

private const val SECONDS_PER_HOUR = 60 * 60

private const val SECONDS_PER_MINUTE = 60

private const val HOURS_PER_DAY = 24

private const val SECONDS_PER_DAY: Int = SECONDS_PER_HOUR * HOURS_PER_DAY

internal const val NANOS_PER_ONE = 1_000_000_000
private const val NANOS_PER_MILLI = 1_000_000
private const val MILLIS_PER_ONE = 1_000

// org.threeten.bp.chrono.IsoChronology#isLeapYear
internal fun isLeapYear(year: Int): Boolean {
    val prolepticYear: Long = year.toLong()
    return prolepticYear and 3 == 0L && (prolepticYear % 100 != 0L || prolepticYear % 400 == 0L)
}

private fun Int.monthLength(isLeapYear: Boolean): Int =
    when (this) {
        2 -> if (isLeapYear) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

private val POWERS_OF_TEN = intArrayOf(
    1,
    10,
    100,
    1000,
    10000,
    100000,
    1000000,
    10000000,
    100000000,
    1000000000
)
