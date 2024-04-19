/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class LocalTimeFormatSamples {
    @Test
    fun hhmmss() {
        // Defining a custom format for the local time
        // format the local time as a single number
        val format = LocalTime.Format {
            hour(); minute(); second()
            optional { char('.'); secondFraction(1, 9) }
        }
        val formatted = format.format(LocalTime(9, 34, 58, 120_000_000))
        check(formatted == "093458.12")
    }

    @Test
    fun amPm() {
        // Defining a custom format for the local time that uses AM/PM markers
        val format = LocalTime.Format {
            amPmHour(); char(':'); minute(); char(':'); second()
            char(' '); amPmMarker("AM", "PM")
        }
        val formatted = format.format(LocalTime(9, 34, 58, 120_000_000))
        check(formatted == "09:34:58 AM")
    }

    @Test
    fun fixedLengthSecondFraction() {
        // Defining a custom format for the local time with a fixed-length second fraction
        val format = LocalTime.Format {
            hour(); char(':'); minute(); char(':'); second()
            char('.'); secondFraction(fixedLength = 3)
        }
        val formatted = format.format(LocalTime(9, 34, 58, 120_000_000))
        check(formatted == "09:34:58.120")
    }

    @Test
    fun time() {
        // Using a predefined format for the local time
        val format = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            time(LocalTime.Formats.ISO)
        }
        val formatted = format.format(LocalDateTime(2021, 1, 13, 9, 34, 58, 120_000_000))
        check(formatted == "2021-01-13 09:34:58.12")
    }
}
