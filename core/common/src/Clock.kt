/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.*

/**
 * A source of [Instant] values.
 *
 * See [Clock.System][Clock.System] for the clock instance that queries the operating system.
 */
public interface Clock {
    /**
     * Returns the [Instant] corresponding to the current time, according to this clock.
     *
     * It is not guaranteed that calling [now] later will return a larger [Instant].
     * In particular, for [System], violations of this are completely expected and must be taken into account.
     * See the documentation of [System] for details.
     */
    public fun now(): Instant

    /**
     * The [Clock] instance that queries the operating system as its source of time knowledge.
     *
     * Successive calls to [now] will not necessarily return increasing [Instant] values, and when they do,
     * these increases will not necessarily correspond to the elapsed time.
     *
     * For example, when using [Clock.System], the following could happen:
     * - [now] returns `2023-01-02T22:35:01Z`;
     * - The system queries the Internet and recognizes that its clock needs adjusting;
     * - [now] returns `2023-01-02T22:32:05Z`.
     *
     * When predictable intervals between successive measurements are needed, consider using
     * [TimeSource.Monotonic].
     */
    public object System : Clock {
        override fun now(): Instant = @Suppress("DEPRECATION_ERROR") Instant.now()
    }

    public companion object {

    }
}

/**
 * Returns the current date at the given [time zone][timeZone], according to [this Clock][this].
 */
public fun Clock.todayIn(timeZone: TimeZone): LocalDate =
    now().toLocalDateTime(timeZone).date

/**
 * Returns a [TimeSource] that uses this [Clock] to mark a time instant and to find the amount of time elapsed since that mark.
 *
 * **Pitfall**: using this function with [Clock.System] is error-prone,
 * because [Clock.System] is not well suited for measuring time intervals.
 * Please only use this conversion function on the [Clock] instances that are fully controlled programmatically.
 */
@ExperimentalTime
public fun Clock.asTimeSource(): TimeSource.WithComparableMarks = object : TimeSource.WithComparableMarks {
    override fun markNow(): ComparableTimeMark = InstantTimeMark(now(), this@asTimeSource)
}

@ExperimentalTime
private class InstantTimeMark(private val instant: Instant, private val clock: Clock) : ComparableTimeMark {
    override fun elapsedNow(): Duration = saturatingDiff(clock.now(), instant)

    override fun plus(duration: Duration): ComparableTimeMark = InstantTimeMark(instant.saturatingAdd(duration), clock)
    override fun minus(duration: Duration): ComparableTimeMark = InstantTimeMark(instant.saturatingAdd(-duration), clock)

    override fun minus(other: ComparableTimeMark): Duration {
        if (other !is InstantTimeMark || other.clock != this.clock) {
            throw IllegalArgumentException("Subtracting or comparing time marks from different time sources is not possible: $this and $other")
        }
        return saturatingDiff(this.instant, other.instant)
    }

    override fun equals(other: Any?): Boolean {
        return other is InstantTimeMark && this.clock == other.clock && this.instant == other.instant
    }

    override fun hashCode(): Int = instant.hashCode()

    override fun toString(): String = "InstantTimeMark($instant, $clock)"

    private fun Instant.isSaturated() = this == Instant.MAX || this == Instant.MIN
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
