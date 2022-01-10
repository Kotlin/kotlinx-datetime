/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.*

/**
 * A source of [Instant] values.
 *
 * @see Clock.System for the instance that queries the operating system.
 */
public interface Clock {
    /**
     * The current time, according to this clock.
     */
    public fun now(): Instant

    /**
     * The [Clock] instance that queries the operating system as its source of knowledge of time.
     */
    public object System : Clock {
        /** @suppress */
        override fun now(): Instant = @Suppress("DEPRECATION_ERROR") Instant.now()
    }

    public companion object {

    }
}

/**
 * Returns the current date at a given [time zone][timeZone], according to [this Clock][this].
 */
public fun Clock.todayAt(timeZone: TimeZone): LocalDate =
        now().toLocalDateTime(timeZone).date

/**
 * Returns the [TimeSource] that wraps the [Instant] values from [Clock.now] intro [TimeMark] instances.
 */
@ExperimentalTime
public fun Clock.asTimeSource(): TimeSource = object : TimeSource {
    override fun markNow(): TimeMark = InstantTimeMark(now(), this@asTimeSource)
}

@ExperimentalTime
private class InstantTimeMark(private val instant: Instant, private val clock: Clock) : TimeMark() {
    override fun elapsedNow(): Duration = clock.now() - instant

    override fun plus(duration: Duration): TimeMark = InstantTimeMark(instant + duration, clock)

    override fun minus(duration: Duration): TimeMark = InstantTimeMark(instant - duration, clock)
}
