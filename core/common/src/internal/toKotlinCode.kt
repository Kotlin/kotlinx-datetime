/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal fun String.toKotlinCode(): String = buildString {
    append('"')
    for (c in this@toKotlinCode) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            else -> append(c)
        }
    }
    append('"')
}

internal fun Char.toKotlinCode(): String = when (this) {
    '\'' -> "'\\''"
    else -> "'$this'"
}
