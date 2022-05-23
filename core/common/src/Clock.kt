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
     */
    public fun now(): Instant

    /**
     * The [Clock] instance that queries the operating system as its source of knowledge of time.
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
 */
@ExperimentalTime
public fun Clock.asTimeSource(): TimeSource = object : TimeSource {
    override fun markNow(): TimeMark = InstantTimeMark(now(), this@asTimeSource)
}

@ExperimentalTime
private class InstantTimeMark(private val instant: Instant, private val clock: Clock) : TimeMark {
    override fun elapsedNow(): Duration = clock.now() - instant

    override fun plus(duration: Duration): TimeMark = InstantTimeMark(instant + duration, clock)

    override fun minus(duration: Duration): TimeMark = InstantTimeMark(instant - duration, clock)
}

@Deprecated("Use Clock.todayIn instead", ReplaceWith("this.todayIn(timeZone)"), DeprecationLevel.WARNING)
public fun Clock.todayAt(timeZone: TimeZone): LocalDate = todayIn(timeZone)
