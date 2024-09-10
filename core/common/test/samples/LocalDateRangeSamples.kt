/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.test.Test

class LocalDateRangeSamples {
    val Jan_01_2000 = LocalDate(2000, 1, 1)
    val Jan_05_2000 = LocalDate(2000, 1, 5)
    val Jan_06_2000 = LocalDate(2000, 1, 6)

    @Test
    fun simpleRangeCreation() {
        // Creating LocalDateRange from LocalDate
        check(Jan_01_2000..Jan_05_2000 == LocalDateRange(Jan_01_2000, Jan_05_2000))
        check(Jan_01_2000..<Jan_06_2000 == LocalDateRange(Jan_01_2000, Jan_05_2000))
        check(Jan_05_2000 downTo Jan_01_2000 == LocalDateProgression(Jan_05_2000, Jan_01_2000, -1))
    }

    @Test
    fun progressionWithStep() {
        // Creating LocalDateProgression with a step size other than 1 day
        check((Jan_01_2000..Jan_05_2000).step(2, DateTimeUnit.DAY) == LocalDateProgression(Jan_01_2000, Jan_05_2000, 2))
        check((Jan_05_2000 downTo Jan_01_2000).step(2, DateTimeUnit.DAY) == LocalDateProgression(Jan_05_2000, Jan_01_2000, -2))
    }

    @Test
    fun reversedProgression() {
        // Creating LocalDateProgression and flipping its direction
        check((Jan_01_2000..Jan_05_2000).reversed() == LocalDateProgression(Jan_05_2000, Jan_01_2000, -1))
        check((Jan_05_2000 downTo Jan_01_2000).reversed() == LocalDateProgression(Jan_01_2000, Jan_05_2000, 1))
    }

    @Test
    fun firstAndLast() {
        // Getting the first and last elements of a LocalDateProgression
        check((Jan_01_2000..Jan_05_2000).first() == Jan_01_2000)
        check((Jan_01_2000..Jan_05_2000).last() == Jan_05_2000)
        check((Jan_01_2000..Jan_06_2000).step(2, DateTimeUnit.DAY).first() == Jan_01_2000)
        check((Jan_01_2000..Jan_06_2000).step(2, DateTimeUnit.DAY).last() == Jan_05_2000)
        check((Jan_05_2000..Jan_01_2000).firstOrNull() == null)
        check((Jan_05_2000..Jan_01_2000).lastOrNull() == null)
    }

    @Test
    fun random() {
        // Getting a random element from a LocalDateProgression
        check((Jan_01_2000..Jan_05_2000).random() in Jan_01_2000..Jan_05_2000)
        check((Jan_01_2000..Jan_05_2000).random(Random(123456)) in Jan_01_2000..Jan_05_2000)
        check((Jan_05_2000..Jan_01_2000).randomOrNull() == null)
        check((Jan_05_2000..Jan_01_2000).randomOrNull(Random(123456)) == null)
    }
}