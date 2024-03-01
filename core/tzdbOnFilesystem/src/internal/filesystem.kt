/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
package kotlinx.datetime.internal

import kotlinx.cinterop.*
import platform.posix.*

internal fun Path.chaseSymlinks(maxDepth: Int = 100): Path {
    var realPath = this
    var depth = maxDepth
    while (true) {
        realPath = realPath.readLink() ?: break
        if (depth-- == 0) throw RuntimeException("Too many levels of symbolic links")
    }
    return realPath
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
