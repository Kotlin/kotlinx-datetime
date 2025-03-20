/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.time.test

import kotlin.test.assertFailsWith
import kotlin.test.fail

inline fun <T> assertInvalidFormat(message: String? = null, f: () -> T) {
    assertFailsWith<IllegalArgumentException>(message) {
        val result = f()
        fail(result.toString())
    }
}

/**
 * The number of iterations to perform in nondeterministic tests.
 */
const val STRESS_TEST_ITERATIONS = 1000
