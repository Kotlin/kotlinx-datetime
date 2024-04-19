/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples.format

import kotlinx.datetime.*
import kotlinx.datetime.format.*
import kotlin.test.*

class UnicodeSamples {
    @Test
    fun byUnicodePattern() {
        // Using the Unicode pattern to define a custom format and obtain the corresponding Kotlin code
        val customFormat = LocalDate.Format {
            @OptIn(FormatStringsInDatetimeFormats::class)
            byUnicodePattern("MM/dd uuuu")
        }
        check(customFormat.format(LocalDate(2021, 1, 13)) == "01/13 2021")
        check(
            DateTimeFormat.formatAsKotlinBuilderDsl(customFormat) == """
                monthNumber()
                char('/')
                day()
                char(' ')
                year()
            """.trimIndent()
        )
    }
}
