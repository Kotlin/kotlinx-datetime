/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.internal.root
import kotlin.test.Test
import kotlin.test.assertEquals
import platform.posix.*
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TimeZoneNativeTest {

    @OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
    private fun pwd(): String {
        val PATH_MAX = 4096
        return memScoped {
            val buffer = allocArray<ByteVar>(PATH_MAX)
            if (getcwd(buffer, PATH_MAX.convert()) != null) {
                buffer.toKString()
            } else {
                throw Exception("Failed to get current directory: ${strerror(errno)?.toKString()}")
            }
        }
    }

    @Test
    fun correctSymlinkTest() {
        root = "${RESOURCES}correct-symlink/"

        println("PWD: ${pwd()}")

        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("Europe/Oslo"), tz, "PWD: ${pwd()}")
    }

    @Test
    fun fallsBackToUTC() {
        root = "${RESOURCES}missing-localtime/"

        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.UTC, tz)
    }

    @Test
    fun missingTimezoneTest() {
        root = "${RESOURCES}missing-timezone/"

        val exception = assertFailsWith<IllegalTimeZoneException> {
            TimeZone.currentSystemDefault()
        }

        assertTrue(
            exception.message?.startsWith("Could not determine the timezone ID") == true,
            "Exception message did not match"
        )
    }

    companion object {
        const val RESOURCES = "./linux/test/time-zone-native-test-resources/"
    }
}