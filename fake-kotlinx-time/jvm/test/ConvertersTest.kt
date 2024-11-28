/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.time.test

import kotlinx.time.*
import kotlin.test.*
import kotlin.random.*
import java.time.Instant as JTInstant

class ConvertersTest {

    @Test
    fun instant() {
        fun test(seconds: Long, nanosecond: Int) {
            val ktInstant = Instant.fromEpochSeconds(seconds, nanosecond.toLong())
            val jtInstant = JTInstant.ofEpochSecond(seconds, nanosecond.toLong())

            assertEquals(ktInstant, jtInstant.toKotlinInstant())
            assertEquals(jtInstant, ktInstant.toJavaInstant())

            assertEquals(ktInstant, jtInstant.toString().let(Instant::parse))
            assertEquals(jtInstant, ktInstant.toString().let(JTInstant::parse))
        }

        repeat(STRESS_TEST_ITERATIONS) {
            val seconds = Random.nextLong(1_000_000_000_000)
            val nanos = Random.nextInt()
            test(seconds, nanos)
        }
    }

}
