/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

/**
 * A helper for reading binary data.
 */
internal class BinaryDataReader(private val bytes: ByteArray, private var position: Int = 0) {
    /**
     * Reads a byte.
     */
    fun readByte(): Byte = bytes[position++]

    /**
     * Reads an unsigned byte.
     */
    fun readUnsignedByte(): UByte =
        readByte().toUByte()

    /**
     * Reads a big-endian (network byte order) 32-bit integer.
     */
    fun readInt(): Int =
        (bytes[position].toInt() and 0xFF shl 24) or
            (bytes[position + 1].toInt() and 0xFF shl 16) or
            (bytes[position + 2].toInt() and 0xFF shl 8) or
            (bytes[position + 3].toInt() and 0xFF).also { position += 4 }

    /**
     * Reads a big-endian (network byte order) 64-bit integer.
     */
    fun readLong(): Long =
        (bytes[position].toLong() and 0xFF shl 56) or
            (bytes[position + 1].toLong() and 0xFF shl 48) or
            (bytes[position + 2].toLong() and 0xFF shl 40) or
            (bytes[position + 3].toLong() and 0xFF shl 32) or
            (bytes[position + 4].toLong() and 0xFF shl 24) or
            (bytes[position + 5].toLong() and 0xFF shl 16) or
            (bytes[position + 6].toLong() and 0xFF shl 8) or
            (bytes[position + 7].toLong() and 0xFF).also { position += 8 }

    fun readUtf8String(length: Int) =
        bytes.decodeToString(position, position + length).also { position += length }

    fun readAsciiChar(): Char = readByte().toInt().toChar()

    fun skip(length: Int) { position += length }
}
