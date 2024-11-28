/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.time.*
import kotlin.test.*

class ClockSamples {
    @Test
    fun system() {
        // Getting the current date and time
        val currentInstant = Clock.System.now()
        currentInstant.toEpochMilliseconds() // the number of milliseconds since the Unix epoch
    }

    @Test
    fun dependencyInjection() {
        fun formatCurrentTime(clock: Clock): String =
            clock.now().toString()

        // In the production code:
        val currentTimeInProduction = formatCurrentTime(Clock.System)
        // Testing this value is tricky because it changes all the time.

        // In the test code:
        val testClock = object: Clock {
            override fun now(): Instant = Instant.parse("2023-01-02T22:35:01Z")
        }
        // Then, one can write a completely deterministic test:
        val currentTimeForTests = formatCurrentTime(testClock)
        check(currentTimeForTests == "2023-01-02T22:35:01Z")
    }
}
