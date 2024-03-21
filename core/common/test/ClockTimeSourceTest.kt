/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds

@OptIn(ExperimentalTime::class)
class ClockTimeSourceTest {
    @Test
    fun arithmetic() {
        val timeSource = Clock.System.asTimeSource()
        val mark0 = timeSource.markNow()

        val markPast = mark0 - 1.days
        val markFuture = mark0 + 1.days

        assertTrue(markPast < mark0)
        assertTrue(markFuture > mark0)
        assertEquals(mark0, markPast + 1.days)
        assertEquals(2.days, markFuture - markPast)
    }

    @Test
    fun elapsed() {
        val clock = object : Clock {
            var instant = Clock.System.now()
            override fun now(): Instant = instant
        }
        val timeSource = clock.asTimeSource()
        val mark = timeSource.markNow()
        assertEquals(Duration.ZERO, mark.elapsedNow())

        clock.instant += 1.days
        assertEquals(1.days, mark.elapsedNow())

        clock.instant -= 2.days
        assertEquals(-1.days, mark.elapsedNow())

        clock.instant = Instant.MAX
        assertEquals(Duration.INFINITE, mark.elapsedNow())
    }

    @Test
    fun differentSources() {
        val mark1 = Clock.System.asTimeSource().markNow()
        val mark2 = object : Clock {
            override fun now(): Instant = Instant.DISTANT_FUTURE
        }.asTimeSource().markNow()
        assertNotEquals(mark1, mark2)
        assertFailsWith<IllegalArgumentException> { mark1 - mark2 }
        assertFailsWith<IllegalArgumentException> { mark1 compareTo mark2 }
    }

    @Test
    fun saturation() {
        val mark0 = Clock.System.asTimeSource().markNow()

        val markFuture = mark0 + Duration.INFINITE
        val markPast = mark0 - Duration.INFINITE

        for (delta in listOf(Duration.ZERO, 1.nanoseconds, 1.days)) {
            assertEquals(markFuture, markFuture - delta)
            assertEquals(markFuture, markFuture + delta)

            assertEquals(markPast, markPast - delta)
            assertEquals(markPast, markPast + delta)
        }
        val infinitePairs = listOf(markFuture to markPast, markFuture to mark0, mark0 to markPast)
        for ((later, earlier) in infinitePairs) {
            assertEquals(Duration.INFINITE, later - earlier)
            assertEquals(-Duration.INFINITE, earlier - later)
        }
        assertEquals(Duration.ZERO, markFuture - markFuture)
        assertEquals(Duration.ZERO, markPast - markPast)

        assertFailsWith<IllegalArgumentException> { markFuture - Duration.INFINITE }
        assertFailsWith<IllegalArgumentException> { markPast + Duration.INFINITE }
    }
}
