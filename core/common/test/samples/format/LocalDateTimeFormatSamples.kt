/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class LocalDateTimeFormatSamples {
    @Test
    fun dateTime() {
        // Using a predefined LocalDateTime format in a larger format
        val format = DateTimeComponents.Format {
            dateTime(LocalDateTime.Formats.ISO)
            offset(UtcOffset.Formats.FOUR_DIGITS)
        }
        val formatted = format.format {
            setDateTimeOffset(
                LocalDate(2021, 1, 13).atTime(9, 34, 58, 120_000_000),
                UtcOffset(hours = 2)
            )
        }
        check(formatted == "2021-01-13T09:34:58.12+0200")
    }
}
