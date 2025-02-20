import kotlinx.datetime.*
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

class UnambiguousInstantTest {
    @Test
    fun `return unambiguous instant`() {
         val regularTime = LocalDateTime(2025, 2, 20, 2, 30)
         val actual = UnambiguousInstant.of(regularTime, TimeZone(ZoneId.of("America/Chicago")))
         val instant = regularTime.toInstant(timeZone = TimeZone(ZoneId.of("America/Chicago")))
         assertEquals(UnambiguousInstant.Unique(instant), actual)
    }

    @Test
    fun `detect impossible time`() {
        val impossibleTime = LocalDateTime(2025, 3, 9, 2, 30)
        val actual = UnambiguousInstant.of(impossibleTime, TimeZone(ZoneId.of("America/Chicago")))
        assertEquals(UnambiguousInstant.Impossible, actual)
    }

    @Test
    fun `detect duplicate time`() {
        val duplicateTime = LocalDateTime(2025, 11, 2, 1, 30)
        val actual = UnambiguousInstant.of(duplicateTime, TimeZone(ZoneId.of("America/Chicago")))
        val instant0 = duplicateTime.toInstant(timeZone = TimeZone(ZoneId.of("America/Chicago")))
        val instant1 = duplicateTime.toInstant(timeZone = TimeZone(ZoneId.of("America/Chicago")))
            .plus(1, DateTimeUnit.HOUR)
        assertEquals(UnambiguousInstant.Duplicate(instant0, instant1), actual)
    }
}