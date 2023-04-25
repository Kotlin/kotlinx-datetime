/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal

internal fun String.repr(): String = buildString {
    append('"')
    for (c in this@repr) {
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

internal fun Char.repr(): String = when (this) {
    '\'' -> "'\\''"
    else -> "'$this'"
}

internal fun<T> List<T>.repr(elementRepr: T.() -> String): String =
    joinToString(", ", "listOf(", ")") { it.elementRepr() }
