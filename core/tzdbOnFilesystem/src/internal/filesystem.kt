/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.datetime.internal

import kotlinx.cinterop.*
import platform.posix.*

internal fun chaseSymlinks(name: String): Path? = memScoped {
    val buffer = allocArray<ByteVar>(PATH_MAX)
    realpath(name, buffer)?.let { Path.fromString(it.toKString()) }
}

