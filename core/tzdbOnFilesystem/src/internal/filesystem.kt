/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
package kotlinx.datetime.internal

import kotlinx.cinterop.*
import platform.posix.*

internal fun chaseSymlinks(name: String): Path? = memScoped {
    val buffer = allocArray<ByteVar>(PATH_MAX)
    realpath(name, buffer)?.let { Path.fromString(it.toKString()) }
}

internal fun Path.containsFile(file: String): Boolean = access("$this/$file", F_OK) == 0

internal fun Path.tryTraverseDirectory(
    exclude: Regex,
    stripLeadingComponents: Int = this.components.size,
    maxDepth: Int = 100,
    actionOnFile: (Path) -> Unit
): Boolean {
    if (maxDepth <= 0) throw IllegalStateException("Max depth reached: $this")
    val handler = opendir(this.toString()) ?: return false
    try {
        while (true) {
            val entry = readdir(handler) ?: break
            val name = entry.pointed.d_name.toKString()
            if (name == "." || name == "..") continue
            if (exclude.matches(name)) continue
            val path = Path(isAbsolute, components + name)
            val isDirectory = path.tryTraverseDirectory(
                exclude, stripLeadingComponents, maxDepth = maxDepth - 1, actionOnFile
            )
            if (!isDirectory) {
                actionOnFile(Path(false, path.components.drop(stripLeadingComponents)))
            }
        }
    } finally {
        closedir(handler)
    }
    return true
}
