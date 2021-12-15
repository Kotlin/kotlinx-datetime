/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

@ExperimentalTime
class ClockTest {
    @Test
    fun timeSourceToClock() {
        val timeSource = TestTimeSource()
        val clock = timeSource.toClock()

        assertEquals(Instant.fromEpochSeconds(0), clock.now())
        assertEquals(Instant.fromEpochSeconds(0), clock.now())

        timeSource += 1.seconds
        assertEquals(Instant.fromEpochSeconds(1), clock.now())
        assertEquals(Instant.fromEpochSeconds(1), clock.now())
    }

    @Test
    fun syncMultipleClocksFromTimeSource() {
        val timeSource = TestTimeSource()
        val clock1 = timeSource.toClock()

        assertEquals(0, clock1.now().epochSeconds)

        timeSource += 1.seconds
        assertEquals(1, clock1.now().epochSeconds)

        val clock2 = timeSource.toClock(offset = Instant.fromEpochSeconds(1))
        assertEquals(clock1.now(), clock2.now())

        timeSource += 1.seconds
        assertEquals(2, clock1.now().epochSeconds)
        assertEquals(clock1.now(), clock2.now())

        val clock3 = timeSource.toClock(offset = clock2.now())
        timeSource += 1.seconds
        assertEquals(3, clock3.now().epochSeconds)
        assertEquals(clock1.now(), clock2.now())
        assertEquals(clock1.now(), clock3.now())
    }
}
