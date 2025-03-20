/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:JvmMultifileClass
@file:JvmName("ClockKt")
package kotlinx.datetime

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Creates a [kotlin.time.Clock] (the standard library version of `Clock`) delegating to `this`.
 */
public fun Clock.toStdlibClock(): kotlin.time.Clock =
    (this as? DateTimeClock)?.clock ?: StdlibClock(this)

/**
 * Creates a [kotlinx.datetime.Clock] delegating to the version of `Clock` from the standard library.
 */
public fun kotlin.time.Clock.toDeprecatedClock(): Clock =
    (this as? StdlibClock)?.clock ?: DateTimeClock(this)

private class DateTimeClock(val clock: kotlin.time.Clock): kotlinx.datetime.Clock {
    override fun now(): Instant = clock.now().toDeprecatedInstant()
}

private class StdlibClock(val clock: Clock): kotlin.time.Clock {
    override fun now(): kotlin.time.Instant = clock.now().toStdlibInstant()
}

/**
 * A source of [Instant] values.
 *
 * See [Clock.System][Clock.System] for the clock instance that queries the operating system.
 *
 * It is not recommended to use [Clock.System] directly in the implementation. Instead, you can pass a
 * [Clock] explicitly to the necessary functions or classes.
 * This way, tests can be written deterministically by providing custom [Clock] implementations
 * to the system under test.
 */
@Deprecated(
    "Use kotlin.time.Clock instead",
    ReplaceWith("kotlin.time.Clock", "kotlin.time.Clock"),
    level = DeprecationLevel.WARNING
)
public interface Clock {
    /**
     * Returns the [Instant] corresponding to the current time, according to this clock.
     *
     * Calling [now] later is not guaranteed to return a larger [Instant].
     * In particular, for [Clock.System], the opposite is completely expected,
     * and it must be taken into account.
     * See the [System] documentation for details.
     *
     * Even though [Instant] is defined to be on the UTC-SLS time scale, which enforces a specific way of handling
     * leap seconds, [now] is not guaranteed to handle leap seconds in any specific way.
     */
    public fun now(): Instant

    /**
     * The [Clock] instance that queries the platform-specific system clock as its source of time knowledge.
     *
     * Successive calls to [now] will not necessarily return increasing [Instant] values, and when they do,
     * these increases will not necessarily correspond to the elapsed time.
     *
     * For example, when using [Clock.System], the following could happen:
     * - [now] returns `2023-01-02T22:35:01Z`.
     * - The system queries the Internet and recognizes that its clock needs adjusting.
     * - [now] returns `2023-01-02T22:32:05Z`.
     *
     * When you need predictable intervals between successive measurements, consider using [TimeSource.Monotonic].
     *
     * For improved testability, you should avoid using [Clock.System] directly in the implementation
     * and pass a [Clock] explicitly instead. For example:
     *
     * @sample kotlinx.datetime.test.samples.ClockSamples.system
     * @sample kotlinx.datetime.test.samples.ClockSamples.dependencyInjection
     */
    public object System : Clock {
        override fun now(): Instant = kotlin.time.Clock.System.now().toDeprecatedInstant()
    }

    /** A companion object used purely for namespacing. */
    public companion object {

    }
}

/**
 * Returns the current date at the given [time zone][timeZone], according to [this Clock][this].
 *
 * The time zone is important because the current date is not the same in all time zones at the same instant.
 *
 * @sample kotlinx.datetime.test.samples.ClockSamples.todayIn
 */
public fun Clock.todayIn(timeZone: TimeZone): LocalDate =
    now().toStdlibInstant().toLocalDateTime(timeZone).date

/**
 * Returns a [TimeSource] that uses this [Clock] to mark a time instant and to find the amount of time elapsed since that mark.
 *
 * **Pitfall**: using this function with [Clock.System] is error-prone
 * because [Clock.System] is not well suited for measuring time intervals.
 * Please only use this conversion function on the [Clock] instances that are fully controlled programmatically.
 */
@ExperimentalTime
public fun Clock.asTimeSource(): TimeSource.WithComparableMarks = object : TimeSource.WithComparableMarks {
    override fun markNow(): ComparableTimeMark = DeprecatedInstantTimeMark(now(), this@asTimeSource)
}

@ExperimentalTime
private class DeprecatedInstantTimeMark(private val instant: Instant, private val clock: Clock) : ComparableTimeMark {
    override fun elapsedNow(): Duration = saturatingDiff(clock.now(), instant)

    override fun plus(duration: Duration): ComparableTimeMark =
        DeprecatedInstantTimeMark(instant.saturatingAdd(duration), clock)
    override fun minus(duration: Duration): ComparableTimeMark =
        DeprecatedInstantTimeMark(instant.saturatingAdd(-duration), clock)

    override fun minus(other: ComparableTimeMark): Duration {
        if (other !is DeprecatedInstantTimeMark || other.clock != this.clock) {
            throw IllegalArgumentException("Subtracting or comparing time marks from different time sources is not possible: $this and $other")
        }
        return saturatingDiff(this.instant, other.instant)
    }

    override fun equals(other: Any?): Boolean {
        return other is DeprecatedInstantTimeMark && this.clock == other.clock && this.instant == other.instant
    }

    override fun hashCode(): Int = instant.hashCode()

    override fun toString(): String = "InstantTimeMark($instant, $clock)"

    private fun Instant.isSaturated() = this.plus(1.seconds) == this || this.plus((-1).seconds) == this
    private fun Instant.saturatingAdd(duration: Duration): Instant {
        if (isSaturated()) {
            if (duration.isInfinite() && duration.isPositive() != this.isDistantFuture) {
                throw IllegalArgumentException("Summing infinities of different signs")
            }
            return this
        }
        return this + duration
    }
    private fun saturatingDiff(instant1: Instant, instant2: Instant): Duration = when {
        instant1 == instant2 ->
            Duration.ZERO
        instant1.isSaturated() || instant2.isSaturated() ->
            (instant1 - instant2) * Double.POSITIVE_INFINITY
        else ->
            instant1 - instant2
    }
}

@Deprecated("Use Clock.todayIn instead", ReplaceWith("this.todayIn(timeZone)"), DeprecationLevel.WARNING)
public fun Clock.todayAt(timeZone: TimeZone): LocalDate = todayIn(timeZone)
