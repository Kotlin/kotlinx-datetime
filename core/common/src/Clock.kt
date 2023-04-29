/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.*

public interface Clock {
    public fun now(): Instant

    public object System : Clock {
        override fun now(): Instant = @Suppress("DEPRECATION_ERROR") Instant.now()
    }

    public companion object {

    }
}

public fun Clock.todayAt(timeZone: TimeZone): LocalDate =
        now().toLocalDateTime(timeZone).date

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

/**
 * Returns the [Clock] by storing an initial [TimeMark] using [TimeSource.markNow] and [returns][Clock.now] the elapsed
 * time using [TimeMark.elapsedNow] plus the provided [offset].
 *
 * This clock stores the initial [TimeMark], so repeatedly creating [Clock]s from the same [TimeSource] results
 * into different [Instant]s iff the time of the [TimeSource] was increased. To sync different [Clock]s, use the [offset]
 * parameter.
 */
@ExperimentalTime
public fun TimeSource.asClock(offset: Instant = Instant.fromEpochSeconds(0)): Clock = object : Clock {
    private val startMark: TimeMark = markNow()
    override fun now() = offset + startMark.elapsedNow()
}
