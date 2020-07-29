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
    fun zoneOffsetToString() {
        var offset: ZoneOffset = ZoneOffset.ofHoursMinutesSeconds(1, 0, 0)
        assertEquals("+01:00", offset.toString())
        offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3)
        assertEquals("+01:02:03", offset.toString())
        offset = ZoneOffset.UTC
        assertEquals("Z", offset.toString())
    }

    @Test
    fun utcIsCached() {
        val values = arrayOf(
            "Z", "+0",
            "+00", "+0000", "+00:00", "+000000", "+00:00:00",
            "-00", "-0000", "-00:00", "-000000", "-00:00:00")
        for (v in values) {
            val test = ZoneOffset.of(v)
            assertSame(test, ZoneOffset.UTC)
        }
    }

    @Test
    fun invalidZoneOffsetNames() {
        val values = arrayOf(
            "", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "ZZ",
            "0", "+0:00", "+00:0", "+0:0",
            "+000", "+00000",
            "+0:00:00", "+00:0:00", "+00:00:0", "+0:0:0", "+0:0:00", "+00:0:0", "+0:00:0",
            "1", "+01_00", "+01;00", "+01@00", "+01:AA",
            "+19", "+19:00", "+18:01", "+18:00:01", "+1801", "+180001",
            "-0:00", "-00:0", "-0:0",
            "-000", "-00000",
            "-0:00:00", "-00:0:00", "-00:00:0", "-0:0:0", "-0:0:00", "-00:0:0", "-0:00:0",
            "-19", "-19:00", "-18:01", "-18:00:01", "-1801", "-180001",
            "-01_00", "-01;00", "-01@00", "-01:AA",
            "@01:00")
        for (v in values) {
            assertFailsWith(IllegalTimeZoneException::class, "should fail: $v") { ZoneOffset.of(v) }
        }
    }

    private fun zoneOffsetCheck(offset: ZoneOffset, hours: Int, minutes: Int, seconds: Int) {
        assertEquals(offset.totalSeconds, hours * 60 * 60 + minutes * 60 + seconds)
        val id: String
        if (hours == 0 && minutes == 0 && seconds == 0) {
            id = "Z"
        } else {
            var str = if (hours < 0 || minutes < 0 || seconds < 0) "-" else "+"
            str += (abs(hours) + 100).toString().substring(1)
            str += ":"
            str += (abs(minutes) + 100).toString().substring(1)
            if (seconds != 0) {
                str += ":"
                str += (abs(seconds) + 100).toString().substring(1)
            }
            id = str
        }
        assertEquals(id, offset.id)
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds), offset)
        assertEquals(offset, ZoneOffset.of(id))
        assertEquals(id, offset.toString())
    }

    @Test
    fun zoneOffsetEquals() {
        val offset1 = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3)
        val offset2 = ZoneOffset.ofHoursMinutesSeconds(2, 3, 4)
        val offset2b = ZoneOffset.ofHoursMinutesSeconds(2, 3, 4)
        assertEquals(false, offset1 == offset2)
        assertEquals(false, offset2 == offset1)
        assertEquals(true, offset1 == offset1)
        assertEquals(true, offset2 == offset2)
        assertEquals(true, offset2 == offset2b)
        assertEquals(offset1.hashCode(), offset1.hashCode())
        assertEquals(offset2.hashCode(), offset2.hashCode())
        assertEquals(offset2.hashCode(), offset2b.hashCode())
    }

    @Test
    fun zoneOffsetParsingFullForm() {
        for (i in -17..17) {
            for (j in -59..59) {
                for (k in -59..59) {
                    if (i < 0 && j <= 0 && k <= 0 || i > 0 && j >= 0 && k >= 0 ||
                        i == 0 && (j < 0 && k <= 0 || j > 0 && k >= 0 || j == 0)) {
                        val str = (if (i < 0 || j < 0 || k < 0) "-" else "+") +
                            (abs(i) + 100).toString().substring(1) + ":" +
                            (abs(j) + 100).toString().substring(1) + ":" +
                            (abs(k) + 100).toString().substring(1)
                        val test = ZoneOffset.of(str)
                        zoneOffsetCheck(test, i, j, k)
                    }
                }
            }
        }
        val test1 = ZoneOffset.of("-18:00:00")
        zoneOffsetCheck(test1, -18, 0, 0)
        val test2 = ZoneOffset.of("+18:00:00")
        zoneOffsetCheck(test2, 18, 0, 0)
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
            zone, ZoneOffset.ofSeconds(2 * 3600)), with(zone) { t.atZone() })
    }

    @Test
    fun atStartOfDay() {
        val paris = TimeZone.of("Europe/Paris")
        assertEquals(LocalDateTime(2008, 6, 30, 0, 0, 0, 0).toInstant(paris),
            paris.atStartOfDay(LocalDate(2008, 6, 30)), "paris")
        val gaza = TimeZone.of("Asia/Gaza")
        assertEquals(LocalDateTime(2007, 4, 1, 1, 0, 0, 0).toInstant(gaza),
            gaza.atStartOfDay(LocalDate(2007, 4, 1)), "gaza")
        val fixed = TimeZone.of("UTC+14")
        assertEquals(LocalDateTime(2007, 4, 1, 0, 0, 0, 0).toInstant(fixed),
            fixed.atStartOfDay(LocalDate(2007, 4, 1)), "fixed")
    }

}
