/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

public interface Clock {
    fun now(): Instant

    object System : Clock {
        override fun now(): Instant = Instant.now()
    }

    companion object {
        // TODO: decide on how to provide system Clock
        val SYSTEM = System
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
