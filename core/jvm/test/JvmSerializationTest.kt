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
        roundTripSerialization(Instant.MIN)
        roundTripSerialization(Instant.MAX)
        expectedDeserialization(Instant.parse("+150000-04-30T12:30:25.555998Z"), "0d010000043fa44d82612123db30")
    }

    @Test
    fun serializeLocalTime() {
        roundTripSerialization(LocalTime(12, 34, 56, 789))
        roundTripSerialization(LocalTime.MIN)
        roundTripSerialization(LocalTime.MAX)
        expectedDeserialization(LocalTime(23, 59, 15, 995_003_220), "090300004e8a52680954")
    }

    @Test
    fun serializeLocalDate() {
        roundTripSerialization(LocalDate(2022, 1, 23))
        roundTripSerialization(LocalDate.MIN)
        roundTripSerialization(LocalDate.MAX)
        expectedDeserialization(LocalDate(2024, 8, 12), "09020000000000004deb")
    }

    @Test
    fun serializeLocalDateTime() {
        roundTripSerialization(LocalDateTime(2022, 1, 23, 21, 35, 53, 125_123_612))
        roundTripSerialization(LocalDateTime.MIN)
        roundTripSerialization(LocalDateTime.MAX)
        expectedDeserialization(LocalDateTime(2024, 8, 12, 10, 15, 0, 997_665_331), "11040000000000004deb0000218faedb9233")
    }

    @Test
    fun serializeUtcOffset() {
        roundTripSerialization(UtcOffset(hours = 3, minutes = 30, seconds = 15))
        roundTripSerialization(UtcOffset(java.time.ZoneOffset.MIN))
        roundTripSerialization(UtcOffset(java.time.ZoneOffset.MAX))
        expectedDeserialization(UtcOffset.parse("-04:15:30"), "050affffc41e")
    }

    @Test
    fun serializeTimeZone() {
        assertFailsWith<NotSerializableException> {
            roundTripSerialization(TimeZone.of("Europe/Moscow"))
        }
    }

    private fun serialize(value: Any?): ByteArray {
        val bos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bos)
        oos.writeObject(value)
        return bos.toByteArray()
    }

    private fun deserialize(serialized: ByteArray): Any? {
        val bis = ByteArrayInputStream(serialized)
        ObjectInputStream(bis).use { ois ->
            return ois.readObject()
        }
    }

    private fun <T> roundTripSerialization(value: T) {
        val serialized = serialize(value)
        val deserialized = deserialize(serialized)
        assertEquals(value, deserialized)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun expectedDeserialization(expected: Any, blockData: String) {
        val serialized = "aced0005737200296b6f746c696e782e6461746574696d652e696e7465726e616c2e53657269616c697a656456616c756500000000000000000c0000787077${blockData}78"
        val hexFormat = HexFormat { bytes.byteSeparator = "" }

        val deserialized = deserialize(serialized.hexToByteArray(hexFormat))
        if (expected != deserialized) {
            assertEquals(expected, deserialized, "Golden serial form: $serialized\nActual serial form: ${serialize(expected).toHexString(hexFormat)}")
        }
    }

}
