/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.test

import kotlinx.datetime.*
import kotlin.js.*
import kotlin.test.*
import kotlinx.datetime.Date

class JsConverterTest {
    @Test
    fun toJSDateTest() {
        val releaseInstant = "2016-02-15T00:00Z".toInstant()
        val releaseDate = releaseInstant.toJSDate()
        assertEquals(2016, releaseDate.getUTCFullYear())
        assertEquals(1, releaseDate.getUTCMonth())
        assertEquals(15, releaseDate.getUTCDate())
    }

    @Test
    fun toInstantTest() {
        val kotlinReleaseEpochMilliseconds = 1455494400000
        val releaseDate = Date(milliseconds = kotlinReleaseEpochMilliseconds.toDouble())
        val releaseInstant = "2016-02-15T00:00Z".toInstant()
        assertEquals(releaseInstant, releaseDate.toKotlinInstant())
    }
}
