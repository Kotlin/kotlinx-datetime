/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.math.abs
import kotlin.test.*

class UtcOffsetTest {

    companion object {
        val invalidUtcOffsetStrings = listOf(
            "", *('A'..'Y').map { it.toString() }.toTypedArray(), "ZZ",
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

        val fixedOffsetTimeZoneIds = listOf(
            "UTC", "UTC+0", "GMT+01", "UT-01", "Etc/UTC",
            "+0000", "+0100", "+1800", "+180000",
            "+4", "+0", "-0",
        )

        val offsetSecondsRange = -18 * 60 * 60 .. +18 * 60 * 60
    }


    @Test
    fun construction() {
        for (totalSeconds in offsetSecondsRange) {
            val hours = totalSeconds / (60 * 60)
            val totalMinutes = totalSeconds / 60
            val minutes = totalMinutes % 60
            val seconds = totalSeconds % 60
            val offset = UtcOffset(hours, minutes, seconds)
            val offsetSeconds = UtcOffset(seconds = totalSeconds)
            val offsetMinutes = UtcOffset(minutes = totalMinutes, seconds = seconds)
            val offsetOrNull = UtcOffset.orNull(hours, minutes, seconds)
            val offsetSecondsOrNull = UtcOffset.orNull(seconds = totalSeconds)
            val offsetMinutesOrNull = UtcOffset.orNull(minutes = totalMinutes, seconds = seconds)
            assertEquals(totalSeconds, offset.totalSeconds)
            assertEquals(offset, offsetMinutes)
            assertEquals(offset, offsetSeconds)
            assertEquals(offset, offsetOrNull)
            assertEquals(offset, offsetSecondsOrNull)
            assertEquals(offset, offsetMinutesOrNull)
        }
    }

    @Test
    fun constructionErrors() {
        fun assertInvalidUtcOffset(
            hours: Int? = null,
            minutes: Int? = null,
            seconds: Int? = null,
        ) {
            assertIllegalArgument { UtcOffset(hours, minutes, seconds) }
            assertNull(UtcOffset.orNull(hours, minutes, seconds))
        }
        // total range
        assertInvalidUtcOffset(hours = -19)
        assertInvalidUtcOffset(hours = +19)
        assertInvalidUtcOffset(hours = -18, minutes = -1)
        assertInvalidUtcOffset(hours = -18, seconds = -1)
        assertInvalidUtcOffset(hours = +18, seconds = +1)
        assertInvalidUtcOffset(seconds = offsetSecondsRange.first - 1)
        assertInvalidUtcOffset(seconds = offsetSecondsRange.last + 1)
        // component ranges
        assertInvalidUtcOffset(hours = 0, minutes = 60)
        assertInvalidUtcOffset(hours = 0, seconds = -60)
        assertInvalidUtcOffset(minutes = 90, seconds = 90)
        assertInvalidUtcOffset(minutes = 0, seconds = 90)
        // component signs
        assertInvalidUtcOffset(hours = +1, minutes = -1)
        assertInvalidUtcOffset(hours = +1, seconds = -1)
        assertInvalidUtcOffset(hours = -1, minutes = +1)
        assertInvalidUtcOffset(hours = -1, seconds = +1)
        assertInvalidUtcOffset(minutes = +1, seconds = -1)
        assertInvalidUtcOffset(minutes = -1, seconds = +1)
    }

    @Test
    fun utcOffsetToString() {
        assertEquals("+01:00", UtcOffset(hours = 1, minutes = 0, seconds = 0).toString())
        assertEquals("+01:02:03", UtcOffset(hours = 1, minutes = 2, seconds = 3).toString())
        assertEquals("-01:00:30", UtcOffset(hours = -1, minutes = 0, seconds = -30).toString())
        assertEquals("Z", UtcOffset.ZERO.toString())
    }

    @Test
    fun invalidUtcOffsetStrings() {
        for (v in invalidUtcOffsetStrings) {
            assertFailsWith<DateTimeFormatException>("Should fail: $v") { UtcOffset.parse(v) }
        }
        for (v in fixedOffsetTimeZoneIds) {
            assertFailsWith<DateTimeFormatException>("Time zone name should not be parsed as UtcOffset: $v") { UtcOffset.parse(v) }
        }
    }

    @Test
    fun parseAllValidValues() {
        fun Int.pad() = toString().padStart(2, '0')
        fun check(offsetSeconds: Int, offsetString: String, canonical: Boolean = false) {
            val offset = UtcOffset.parse(offsetString)
            if (offsetSeconds != offset.totalSeconds) {
                fail("Expected string $offsetString to be parsed as $offset and have $offsetSeconds offset, got ${offset.totalSeconds}")
            }

            val actualOffsetString = offset.toString()
            if (canonical) {
                assertEquals(offsetString, actualOffsetString)
            } else {
                assertNotEquals(offsetString, actualOffsetString)
                val offset2 = UtcOffset.parse(actualOffsetString)
                assertEquals(offset, offset2)
            }
        }

        for (offsetSeconds in offsetSecondsRange) {
            val sign = when {
                offsetSeconds < 0 -> "-"
                else -> "+"
            }
            val hours = abs(offsetSeconds / 60 / 60)
            val minutes = abs(offsetSeconds / 60 % 60)
            val seconds = abs(offsetSeconds % 60)


            check(offsetSeconds, "$sign${hours.pad()}:${minutes.pad()}:${seconds.pad()}", canonical = seconds != 0)
            if (seconds == 0) {
                check(offsetSeconds, "$sign${hours.pad()}:${minutes.pad()}", canonical = offsetSeconds != 0)
            }
        }
        check(0, "+00:00")
        check(0, "-00:00")
        check(0, "Z", canonical = true)
    }

    @Test
    fun equality() {
        val equalOffsets = listOf(
            listOf("Z", "+00:00", "-00:00", "+00:00:00", "-00:00:00"),
            listOf("+04:00", "+04:00:00"),
            listOf("-18:00", "-18:00:00"),
        )
        for (equalGroup in equalOffsets) {
            val offsets = equalGroup.map { UtcOffset.parse(it) }
            val message = "$offsets"
            assertEquals(1, offsets.distinct().size, message)
            assertEquals(1, offsets.map { it.toString() }.distinct().size, message)
            assertEquals(1, offsets.map { it.hashCode() }.distinct().size, message)
        }
        for ((offset1, offset2) in equalOffsets.map { UtcOffset.parse(it.random()) }.shuffled().zipWithNext()) {
            assertNotEquals(offset1, offset2)
            assertNotEquals(offset1.toString(), offset2.toString())
        }
    }

    @Test
    fun asTimeZone() {
        val offset = UtcOffset(hours = 1, minutes = 20, seconds = 30)
        val timeZone = offset.asTimeZone()
        assertIs<FixedOffsetTimeZone>(timeZone)
        assertEquals(offset, timeZone.offset)
    }
}
