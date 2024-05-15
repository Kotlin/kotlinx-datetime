/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.test

import kotlinx.datetime.*

/**
 * To test the deprecation replacements, remove the `Suppress` annotation and try automatically replacing the deprecated
 * API usages with the new ones.
 */
@Suppress("DEPRECATION")
class DeprecationReplacements {
    fun localTimeAtDate() {
        LocalTime(18, 43, 15, 100500000)
            .atDate(2023, java.time.Month.JANUARY, 20)
    }

    fun monthNumber() {
        java.time.Month.JANUARY.number
    }

    fun localDateConstruction() {
        LocalDate(2023, java.time.Month.JANUARY, 20)
    }

    fun localDateTimeConstruction() {
        LocalDateTime(2023, java.time.Month.JANUARY, 20, 18, 43, 15, 100500000)
        LocalDateTime(2023, java.time.Month.JANUARY, 20, 18, 43, 15)
        LocalDateTime(2023, java.time.Month.JANUARY, 20, 18, 43)
    }
}
