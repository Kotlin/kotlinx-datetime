/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class UtcOffsetFormatSamples {
    @Test
    fun isoOrGmt() {
        // Defining a custom format for the UTC offset
        val format = UtcOffset.Format {
            // if the offset is zero, `GMT` is printed
            optional("GMT") {
                offsetHours(); char(':'); offsetMinutesOfHour()
                // if seconds are zero, they are omitted
                optional { char(':'); offsetSecondsOfMinute() }
            }
        }
        check(format.format(UtcOffset.ZERO) == "GMT")
        check(format.format(UtcOffset(hours = -2)) == "-02:00")
        check(format.format(UtcOffset(hours = -2, minutes = -30)) == "-02:30")
        check(format.format(UtcOffset(hours = -2, minutes = -30, seconds = -59)) == "-02:30:59")
    }

    @Test
    fun offset() {
        // Using a predefined format for the UTC offset
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
