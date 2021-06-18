/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package kotlinx.datetime.test

import kotlinx.datetime.DateTimeArithmeticException
import kotlinx.datetime.DateTimeFormatException
import kotlin.test.assertFailsWith
import kotlin.test.fail

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
inline fun <T> assertArithmeticFails(message: String? = null, f: () -> T) {
    assertFailsWith<DateTimeArithmeticException>(message) {
        val result = f()
        fail(result.toString())
    }
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
inline fun <T> assertInvalidFormat(message: String? = null, f: () -> T) {
    assertFailsWith<DateTimeFormatException>(message) {
        val result = f()
        fail(result.toString())
    }
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
inline fun <T> assertIllegalArgument(message: String? = null, f: () -> T) {
    assertFailsWith<IllegalArgumentException>(message) {
        val result = f()
        fail(result.toString())
    }
}