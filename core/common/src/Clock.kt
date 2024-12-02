/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("ClockKt")
package kotlinx.datetime

import kotlin.time.*
import kotlinx.time.Clock
import kotlinx.time.Instant
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.seconds

/**
 * Returns the current date at the given [time zone][timeZone], according to [this Clock][this].
 *
 * The time zone is important because the current date is not the same in all time zones at the same instant.
 *
 * @sample kotlinx.datetime.test.samples.ClockSamples.todayIn
 */
public fun Clock.todayIn(timeZone: TimeZone): LocalDate =
    now().toLocalDateTime(timeZone).date

/**
 * Returns a [TimeSource] that uses this [Clock] to mark a time instant and to find the amount of time elapsed since that mark.
 *
 * **Pitfall**: using this function with [Clock.System] is error-prone
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
