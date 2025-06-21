/*
 * Copyright 2019-2025 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.serialization.test

import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.serializers.*
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.*

@OptIn(ExperimentalTime::class)
class InstantSerializationTest {
    object Rfc1123InstantSerializer : FormattedInstantSerializer("RFC_1123", DateTimeComponents.Formats.RFC_1123)

    @Test
    fun testCustomSerializer() {
        assertKSerializerName("kotlin.time.Instant/serializer/RFC_1123", Rfc1123InstantSerializer)
        for ((instant, json) in listOf(
            Pair(Instant.fromEpochSeconds(1607505416),
                "\"Wed, 9 Dec 2020 09:16:56 GMT\""),
            Pair(Instant.fromEpochSeconds(-1607505416),
                "\"Thu, 23 Jan 1919 14:43:04 GMT\""),
            Pair(Instant.fromEpochSeconds(987654321),
                "\"Thu, 19 Apr 2001 04:25:21 GMT\""),
        )) {
            assertEquals(json, Json.encodeToString(Rfc1123InstantSerializer, instant))
            assertEquals(instant, Json.decodeFromString(Rfc1123InstantSerializer, json))
        }
        assertEquals("\"Thu, 19 Apr 2001 04:25:21 GMT\"",
            Json.encodeToString(Rfc1123InstantSerializer, Instant.fromEpochSeconds(987654321, 123456789)))
        assertEquals(Instant.fromEpochSeconds(987654321),
            Json.decodeFromString(Rfc1123InstantSerializer, "\"Thu, 19 Apr 2001 08:25:21 +0400\""))
    }
}
