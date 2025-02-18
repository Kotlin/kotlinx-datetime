/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */
package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.LocalDateTime
import kotlin.test.*

class ThreeTenBpLocalDateTimeTest {
    @Test
    fun toSecondsAfterEpoch() {
        for (i in -5..4) {
            val iHours = i * 3600
            val offset = UtcOffset(seconds = iHours)
            for (j in 0..99999) {
                val a = LocalDateTime(1970, 1, 1, 0, 0, 0, 0).plusSeconds(j)
                assertEquals((j - iHours).toLong(), a.toEpochSecond(offset))
            }
        }
    }

    @Test
    fun toSecondsBeforeEpoch() {
        for (i in 0..99999) {
            val a = LocalDateTime(1970, 1, 1, 0, 0, 0, 0).plusSeconds(-i)
            assertEquals(-i.toLong(), a.toEpochSecond(UtcOffset.ZERO))
        }
    }

    @Test
    fun plusSeconds() {
        var t = LocalDateTime(2007, 7, 15, 0, 0, 0, 0)
        val d: LocalDate = t.date
        var hour = 0
        var min = 0
        var sec = 0
        for (i in 0..3699) {
            t = t.plusSeconds(1)
            sec++
            if (sec == 60) {
                min++
                sec = 0
            }
            if (min == 60) {
                hour++
                min = 0
            }
            assertEquals(d, t.date)
            assertEquals(hour, t.hour)
            assertEquals(min, t.minute)
            assertEquals(sec, t.second)
        }
    }
}
