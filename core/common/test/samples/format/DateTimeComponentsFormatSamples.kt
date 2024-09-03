/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class DateTimeComponentsFormatSamples {
    @Test
    fun timeZoneId() {
        // Defining a custom format that includes a time zone ID
        val format = DateTimeComponents.Format {
            dateTime(LocalDateTime.Formats.ISO)
            char('[')
            timeZoneId()
            char(']')
        }
        val formatted = format.format {
            setDateTime(LocalDate(2021, 1, 13).atTime(9, 34, 58, 120_000_000))
            timeZoneId = "Europe/Paris"
        }
        check(formatted == "2021-01-13T09:34:58.12[Europe/Paris]")
        val parsed = format.parse("2021-01-13T09:34:58.12[Europe/Paris]")
        check(parsed.toLocalDateTime() == LocalDate(2021, 1, 13).atTime(9, 34, 58, 120_000_000))
        check(parsed.timeZoneId == "Europe/Paris")
    }

    @Test
    fun dateTimeComponents() {
        // Using a predefined DateTimeComponents format in a larger format
        val format = DateTimeComponents.Format {
            char('{')
            dateTimeComponents(DateTimeComponents.Formats.RFC_1123)
            char('}')
        }
        val formatted = format.format {
            setDateTimeOffset(
                LocalDate(2021, 1, 13).atTime(9, 34, 58, 120_000_000),
                UtcOffset(hours = 3, minutes = 30)
            )
        }
        check(formatted == "{Wed, 13 Jan 2021 09:34:58 +0330}")
    }
}
