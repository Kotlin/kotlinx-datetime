/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.test.Test

class LocalDateRangeSamples {

    @Test
    fun simpleRangeCreation() {
        // Creating LocalDateRange from LocalDate
        check((LocalDate(2000, 1, 1)..LocalDate(2000, 1, 5)).toList() == listOf(
            LocalDate(2000, 1, 1),
            LocalDate(2000, 1, 2),
            LocalDate(2000, 1, 3),
            LocalDate(2000, 1, 4),
            LocalDate(2000, 1, 5)
        ))
        check(
            (LocalDate(2000, 1, 1)..<LocalDate(2000, 1, 6)).toList() == listOf(
                LocalDate(2000, 1, 1),
                LocalDate(2000, 1, 2),
                LocalDate(2000, 1, 3),
                LocalDate(2000, 1, 4),
                LocalDate(2000, 1, 5)
            ))
        check(
            (LocalDate(2000, 1, 5) downTo LocalDate(2000, 1, 1)).toList() == listOf(
                LocalDate(2000, 1, 5),
                LocalDate(2000, 1, 4),
                LocalDate(2000, 1, 3),
                LocalDate(2000, 1, 2),
                LocalDate(2000, 1, 1)
            ))
    }

    @Test
    fun progressionWithStep() {
        // Creating LocalDateProgression with a step size other than 1 day
        check(
            (LocalDate(2000, 1, 1)..LocalDate(2000, 1, 5)).step(2, DateTimeUnit.DAY).toList() == listOf(
                LocalDate(2000, 1, 1),
                LocalDate(2000, 1, 3),
                LocalDate(2000, 1, 5)
            ))
        check(
            (LocalDate(2000, 1, 5) downTo LocalDate(2000, 1, 1)).step(2, DateTimeUnit.DAY).toList() == listOf(
                LocalDate(2000, 1, 5),
                LocalDate(2000, 1, 3),
                LocalDate(2000, 1, 1)
            ))
    }

    @Test
    fun reversedProgression() {
        // Creating LocalDateProgression and flipping its direction
        check(
            (LocalDate(2000, 1, 1)..LocalDate(2000, 1, 5)).reversed().toList() == listOf(
                LocalDate(2000, 1, 5),
                LocalDate(2000, 1, 4),
                LocalDate(2000, 1, 3),
                LocalDate(2000, 1, 2),
                LocalDate(2000, 1, 1)
            ))
        check(
            (LocalDate(2000, 1, 5) downTo LocalDate(2000, 1, 1)).reversed().toList() == listOf(
                LocalDate(2000, 1, 1),
                LocalDate(2000, 1, 2),
                LocalDate(2000, 1, 3),
                LocalDate(2000, 1, 4),
                LocalDate(2000, 1, 5)
            ))
    }

    @Test
    fun firstAndLast() {
        // Getting the first and last elements of a LocalDateProgression
        check((LocalDate(2000, 1, 1)..LocalDate(2000, 1, 5)).first() == LocalDate(2000, 1, 1))
        check((LocalDate(2000, 1, 1)..LocalDate(2000, 1, 5)).last() == LocalDate(2000, 1, 5))
        check((LocalDate(2000, 1, 1)..LocalDate(2000, 1, 6)).step(2, DateTimeUnit.DAY).first() == LocalDate(2000, 1, 1))
        check((LocalDate(2000, 1, 1)..LocalDate(2000, 1, 6)).step(2, DateTimeUnit.DAY).last() == LocalDate(2000, 1, 5))
        check((LocalDate(2000, 1, 5)..LocalDate(2000, 1, 1)).firstOrNull() == null)
        check((LocalDate(2000, 1, 5)..LocalDate(2000, 1, 1)).lastOrNull() == null)
    }

    @Test
    fun random() {
        // Getting a random element from a LocalDateProgression
        check((LocalDate(2000, 1, 1)..LocalDate(2000, 1, 5)).random() in LocalDate(2000, 1, 1)..LocalDate(2000, 1, 5))
        check(
            (LocalDate(2000, 1, 1)..LocalDate(2000, 1, 5)).random(Random(123456)) in LocalDate(2000, 1, 1)..LocalDate(
                2000,
                1,
                5
            )
        )
        check((LocalDate(2000, 1, 5)..LocalDate(2000, 1, 1)).randomOrNull() == null)
        check((LocalDate(2000, 1, 5)..LocalDate(2000, 1, 1)).randomOrNull(Random(123456)) == null)
    }
}