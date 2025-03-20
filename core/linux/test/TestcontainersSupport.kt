/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

object Testcontainers {

    @OptIn(ExperimentalForeignApi::class)
    val available: Boolean
        get() = getenv("INSIDE_TESTCONTAINERS")?.toKString()?.toBoolean() == true

    inline fun runIfAvailable(block: () -> Unit) {
        if (available) block() else println("Skipping test that requires testcontainers...")
    }
}