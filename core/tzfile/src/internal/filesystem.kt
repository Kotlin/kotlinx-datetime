/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
package kotlinx.datetime.internal

import kotlinx.cinterop.*
import platform.posix.*

internal class Path(val isAbsolute: Boolean, val components: List<String>) {
    override fun toString(): String = buildString {
        if (isAbsolute) append("/")
        if (components.isNotEmpty()) {
            for (i in 0 until components.size - 1) {
                append(components[i])
                append("/")
            }
            append(components.last())
        }
    }

    companion object {
        fun fromString(path: String): Path {
            val absolutePath = path.startsWith("/")
            val components = path.split("/").filter { it.isNotEmpty() }
            return Path(absolutePath, components)
        }
    }
}

internal fun Path.readBytes(): ByteArray? {
    val handler = fopen(this.toString(), "rb") ?: return null
    try {
        var err = fseek(handler, 0, SEEK_END)
        if (err == -1) throw RuntimeException("Cannot jump to the end of $this: $errnoString")
        val size = ftell(handler).convert<Long>()
        if (size == -1L) throw RuntimeException("Cannot get file size for $this: $errnoString")
        err = fseek(handler, 0, SEEK_SET)
        if (err == -1) throw RuntimeException("Cannot jump to the start of $this: $errnoString")
        val buffer = ByteArray(size.toInt())
        val readAmount = fread(buffer.refTo(0), size.convert<size_t>(), 1u, handler)
        check(readAmount.convert<ULong>() == 1uL) { "Cannot read file $this: $errnoString" }
        return buffer
    } finally {
        fclose(handler)
    }
}

private val errnoString
    get() = strerror(errno)?.toKString() ?: "Unknown error"
