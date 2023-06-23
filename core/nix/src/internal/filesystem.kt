/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

import kotlinx.cinterop.*
import platform.posix.*

internal class Path(val isAbsolute: Boolean, val components: List<String>) {
    fun check(): PathInfo? = memScoped {
        val stat = alloc<stat>()
        val err = stat(this@Path.toString(), stat.ptr)
        if (err != 0) return null
        object : PathInfo {
            override val isDirectory: Boolean = stat.st_mode.toInt() and S_IFMT == S_IFDIR // `inode(7)`, S_ISDIR
            override val isSymlink: Boolean = stat.st_mode.toInt() and S_IFMT == S_IFLNK // `inode(7)`, S_ISLNK
        }
    }

    fun readLink(): Path? = memScoped {
        val buffer = allocArray<ByteVar>(PATH_MAX)
        val err = readlink(this@Path.toString(), buffer, PATH_MAX.toULong())
        if (err == -1L) return null
        buffer[err] = 0
        fromString(buffer.toKString())
    }

    fun resolve(other: Path): Path = when {
        other.isAbsolute -> other
        else -> Path(isAbsolute, components + other.components)
    }

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

// `stat(2)` lists the other available fields
internal interface PathInfo {
    val isDirectory: Boolean
    val isSymlink: Boolean
}

internal fun Path.traverseDirectory(exclude: Set<String> = emptySet(), stripLeadingComponents: Int = this.components.size, actionOnFile: (Path) -> Unit) {
    val handler = opendir(this.toString()) ?: return
    try {
        while (true) {
            val entry = readdir(handler) ?: break
            val name = entry.pointed.d_name.toKString()
            if (name == "." || name == "..") continue
            if (name in exclude) continue
            val path = Path(isAbsolute, components + name)
            val info = path.check() ?: continue // skip broken symlinks
            if (info.isDirectory) {
                if (!info.isSymlink) {
                    path.traverseDirectory(exclude, stripLeadingComponents, actionOnFile)
                }
            } else {
                actionOnFile(Path(false, path.components.drop(stripLeadingComponents)))
            }
        }
    } finally {
        closedir(handler)
    }
}


internal fun Path.readBytes(): ByteArray {
    val handler = fopen(this.toString(), "rb") ?: throw RuntimeException("Cannot open file $this")
    try {
        fseek(handler, 0, SEEK_END)
        val size = ftell(handler)
        fseek(handler, 0, SEEK_SET)
        val buffer = ByteArray(size.toInt())
        fread(buffer.refTo(0), size.toULong(), 1, handler)
        return buffer
    } finally {
        fclose(handler)
    }
}
