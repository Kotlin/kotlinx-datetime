/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.test.*

class ClockSamples {
    @Test
    fun system() {
        val zone = TimeZone.of("Europe/Berlin")
        val currentInstant = Clock.System.now()
        val currentLocalDateTime = currentInstant.toLocalDateTime(zone)
        currentLocalDateTime.toString() // show the current date and time, according to the OS
    }

    @Test
    fun todayIn() {
        val clock = object : Clock {
            override fun now(): Instant = Instant.parse("2020-01-01T02:00:00Z")
        }
        check(clock.todayIn(TimeZone.UTC) == LocalDate(2020, 1, 1))
        check(clock.todayIn(TimeZone.of("America/New_York")) == LocalDate(2019, 12, 31))
    }
}
