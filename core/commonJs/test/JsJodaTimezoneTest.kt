/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.internal.RuleBasedTimeZone
import kotlin.math.roundToInt
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.test.JSJoda.Instant as jtInstant
import kotlinx.datetime.test.JSJoda.ZoneId as jtZoneId
import kotlin.time.Instant

class JsJodaTimezoneTest {
    @Test
    fun system() {
        val tz = TimeZoneContext.System.currentTimeZone()
        assertNotEquals("SYSTEM", tz.id)
        val systemTz = TimeZoneContext.System.get("SYSTEM")
        assertEquals(tz, systemTz)
        assertEquals(tz.id, systemTz.id)
    }

    @Test
    fun iterateOverAllTimezones() {
        for (id in TimeZoneContext.System.availableZoneIds()) {
            val ourZone = TimeZoneContext.System.get(id)
            val jodaZone = jtZoneId.of(id)
            fun checkAtInstant(instant: Instant) {
                val offset = ourZone.offsetAt(instant)
                val ourLdt = instant.toLocalDateTime(offset)
                val zdt = jtInstant.ofEpochMilli(instant.toEpochMilliseconds().toDouble()).atZone(jodaZone)
                val theirLdt = with(zdt) {
                    LocalDateTime(
                        year(),
                        monthValue(),
                        dayOfMonth(),
                        hour(),
                        minute(),
                        second(),
                        nano().roundToInt()
                    )
                }
                // It seems that sometimes, js-joda interprets its data incorrectly by at most one second,
                // and we don't want to replicate that.
                // Example: America/Noronha at 1914-01-01T02:09:39.998Z:
                // - Computed 1913-12-31T23:59:59.998 with offset -02:09:40
                // - 1914-01-01T00:00:00.998-02:09:39[America/Noronha] is js-joda's interpretation
                // The raw data representing the offset is `29.E`, which is `2 * 60 + 9 + (ord 'E' - 29) / 60`,
                // and `ord 'E'` is 69, so the offset is -2:09:40.
                // Thus, we allow a difference of 1 second.
                assertTrue(
                    (ourLdt.toInstant(TimeZone.UTC) - theirLdt.toInstant(TimeZone.UTC)).absoluteValue <= 1.seconds,
                    "Failed for $id at $instant: computed $ourLdt with offset $offset, but $zdt is correct"
                )
            }
            fun checkTransition(instant: Instant) {
                checkAtInstant(instant - 2.milliseconds)
                checkAtInstant(instant)
            }
            // check historical data
            if (ourZone is RuleBasedTimeZone) {
                val transitionEpochSeconds = ourZone.rules.transitionEpochSeconds
                for (transition in transitionEpochSeconds) {
                    checkTransition(Instant.fromEpochSeconds(transition))
                }
            }
        }
    }
}
