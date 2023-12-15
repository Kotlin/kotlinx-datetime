/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*


/**
 * This file takes tests from the 310 backport project and tests with them the parts that were adapted from it.
 */
class ThreeTenBpTimeZoneTest {

    @Test
    fun utcIsCached() {
        val values = arrayOf("Z", "+00:00", "+00:00:00", "-00:00", "-00:00:00")
        for (v in values) {
            val test = UtcOffset.parse(v)
            assertSame(test, UtcOffset.ZERO)
        }
        assertSame(UtcOffset.parse("-0", UtcOffset.Format { offsetHours(padding = Padding.NONE) }), UtcOffset.ZERO)
    }

    @Test
    fun nonExistentLocalTime() {
        val t1 = LocalDateTime(2020, 3, 29, 2, 14, 17, 201)
        val t2 = LocalDateTime(2020, 3, 29, 3, 14, 17, 201)
        val tz = TimeZone.of("Europe/Berlin")
        assertEquals(tz.localDateTimeToInstant(t1), tz.localDateTimeToInstant(t2))
    }

    @Test
    fun overlappingLocalTime() {
        val t = LocalDateTime(2007, 10, 28, 2, 30, 0, 0)
        val zone = TimeZone.of("Europe/Paris")
        assertEquals(t.toInstant(UtcOffset(hours = 2)), zone.localDateTimeToInstant(t))
    }

}
