/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import java.io.*
import kotlin.test.*

class JvmSerializationTest {

    @Test
    fun serializeInstant() {
        roundTripSerialization(Instant.fromEpochSeconds(1234567890, 123456789))
    }

    @Test
    fun serializeLocalTime() {
        roundTripSerialization(LocalTime(12, 34, 56, 789))
    }

    @Test
    fun serializeLocalDateTime() {
        roundTripSerialization(LocalDateTime(2022, 1, 23, 21, 35, 53, 125_123_612))
    }

    @Test
    fun serializeUtcOffset() {
        roundTripSerialization(UtcOffset(hours = 3, minutes = 30, seconds = 15))
    }

    @Test
    fun serializeTimeZone() {
        assertFailsWith<NotSerializableException> {
            roundTripSerialization(TimeZone.of("Europe/Moscow"))
        }
    }

    private fun <T> roundTripSerialization(value: T) {
        val bos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bos)
        oos.writeObject(value)
        val serialized = bos.toByteArray()
        val bis = ByteArrayInputStream(serialized)
        ObjectInputStream(bis).use { ois ->
            assertEquals(value, ois.readObject())
        }
    }
}
