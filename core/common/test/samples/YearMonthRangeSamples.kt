/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test.samples

import kotlinx.datetime.*
import kotlin.random.Random
import kotlin.test.Test

class YearMonthRangeSamples {

    @Test
    fun simpleRangeCreation() {
        // Creating YearMonthRange from YearMonth
        check((YearMonth(2000, 1)..YearMonth(2000, 5)).toList() == listOf(
            YearMonth(2000, 1),
            YearMonth(2000, 2),
            YearMonth(2000, 3),
            YearMonth(2000, 4),
            YearMonth(2000, 5)
        ))
        check(
            (YearMonth(2000, 1)..<YearMonth(2000, 6)).toList() == listOf(
                YearMonth(2000, 1),
                YearMonth(2000, 2),
                YearMonth(2000, 3),
                YearMonth(2000, 4),
                YearMonth(2000, 5)
            ))
        check(
            (YearMonth(2000, 5) downTo YearMonth(2000, 1)).toList() == listOf(
                YearMonth(2000, 5),
                YearMonth(2000, 4),
                YearMonth(2000, 3),
                YearMonth(2000, 2),
                YearMonth(2000, 1)
            ))
    }

    @Test
    fun progressionWithStep() {
        // Creating YearMonthProgression with a step size other than 1 day
        check(
            (YearMonth(2000, 1)..YearMonth(2000, 5)).step(2, DateTimeUnit.MONTH).toList() == listOf(
                YearMonth(2000, 1),
                YearMonth(2000, 3),
                YearMonth(2000, 5)
            ))
        check(
            (YearMonth(2000, 5) downTo YearMonth(2000, 1)).step(2, DateTimeUnit.MONTH).toList() == listOf(
                YearMonth(2000, 5),
                YearMonth(2000, 3),
                YearMonth(2000, 1)
            ))
    }

    @Test
    fun reversedProgression() {
        // Creating YearMonthProgression and flipping its direction
        check(
            (YearMonth(2000, 1)..YearMonth(2000, 5)).reversed().toList() == listOf(
                YearMonth(2000, 5),
                YearMonth(2000, 4),
                YearMonth(2000, 3),
                YearMonth(2000, 2),
                YearMonth(2000, 1)
            ))
        check(
            (YearMonth(2000, 5) downTo YearMonth(2000, 1)).reversed().toList() == listOf(
                YearMonth(2000, 1),
                YearMonth(2000, 2),
                YearMonth(2000, 3),
                YearMonth(2000, 4),
                YearMonth(2000, 5)
            ))
    }

    @Test
    fun firstAndLast() {
        // Getting the first and last elements of a YearMonthProgression
        check((YearMonth(2000, 1)..YearMonth(2000, 5)).first() == YearMonth(2000, 1))
        check((YearMonth(2000, 1)..YearMonth(2000, 5)).last() == YearMonth(2000, 5))
        check((YearMonth(2000, 1)..YearMonth(2000, 6)).step(2, DateTimeUnit.MONTH).first() == YearMonth(2000, 1))
        check((YearMonth(2000, 1)..YearMonth(2000, 6)).step(2, DateTimeUnit.MONTH).last() == YearMonth(2000, 5))
        check((YearMonth(2000, 5)..YearMonth(2000, 1)).firstOrNull() == null)
        check((YearMonth(2000, 5)..YearMonth(2000, 1)).lastOrNull() == null)
    }

    @Test
    fun random() {
        // Getting a random element from a YearMonthProgression
        check((YearMonth(2000, 1)..YearMonth(2000, 5)).random() in YearMonth(2000, 1)..YearMonth(2000, 5))
        check((YearMonth(2000, 1)..YearMonth(2000, 5)).random(Random(123456)) in YearMonth(2000, 1)..YearMonth(2000, 5))
        check((YearMonth(2000, 5)..YearMonth(2000, 1)).randomOrNull() == null)
        check((YearMonth(2000, 5)..YearMonth(2000, 1)).randomOrNull(Random(123456)) == null)
    }
}