/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.TimeZone
import kotlinx.datetime.internal.root
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeZoneNativeTest {

    @Test
    fun correctSymlinkTest() {
        root = "./core/linux/test/time-zone-native-test-resources/correct-symlink/"

        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.of("Europe/Oslo"), tz)
    }

    @Test
    fun fallsBackToUTC() {
        root = "./core/linux/test/time-zone-native-test-resources/missing-localtime/"

        val tz = TimeZone.currentSystemDefault()
        assertEquals(TimeZone.UTC, tz)
    }
}