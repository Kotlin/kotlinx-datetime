/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.math.*
import kotlin.test.*


/**
 * This file takes tests from the 310 backport project and tests with them the parts that were adapted from it.
 */
class ThreeTenBpTimeZoneTest {

    @Test
    fun utcOffsetToString() {
        var offset: UtcOffset = UtcOffset.ofHoursMinutesSeconds(1, 0, 0)
        assertEquals("+01:00", offset.toString())
        offset = UtcOffset.ofHoursMinutesSeconds(1, 2, 3)
        assertEquals("+01:02:03", offset.toString())
        offset = UtcOffset.ZERO
        assertEquals("Z", offset.toString())
    }

    @Test
    fun utcIsCached() {
        val values = arrayOf(
            "Z", "+0",
            "+00", "+0000", "+00:00", "+000000", "+00:00:00",
            "-00", "-0000", "-00:00", "-000000", "-00:00:00")
        for (v in values) {
            val test = UtcOffset.parse(v)
            assertSame(test, UtcOffset.ZERO)
        }
    }

    @Test
    fun nonExistentLocalTime() {
        val t1 = LocalDateTime(2020, 3, 29, 2, 14, 17, 201)
        val t2 = LocalDateTime(2020, 3, 29, 3, 14, 17, 201)
        val tz = TimeZone.of("Europe/Berlin")
        assertEquals(with(tz) { t1.atZone() }, with(tz) { t2.atZone() })
    }

    @Test
    fun overlappingLocalTime() {
        val t = LocalDateTime(2007, 10, 28, 2, 30, 0, 0)
        val zone = TimeZone.of("Europe/Paris")
        assertEquals(ZonedDateTime(LocalDateTime(2007, 10, 28, 2, 30, 0, 0),
            zone, UtcOffset.ofSeconds(2 * 3600)), with(zone) { t.atZone() })
    }

}
