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
private class InstantTimeMark(private val instant: Instant, private val clock: Clock) : TimeMark {
    override fun elapsedNow(): Duration = clock.now() - instant

    override fun plus(duration: Duration): TimeMark = InstantTimeMark(instant + duration, clock)

    override fun minus(duration: Duration): TimeMark = InstantTimeMark(instant - duration, clock)
}
