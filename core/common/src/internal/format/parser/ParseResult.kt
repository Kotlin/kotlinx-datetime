/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.parser

import kotlin.jvm.JvmInline

@JvmInline
internal value class ParseResult private constructor(val value: Any) {
    companion object {
        fun Ok(indexOfNextUnparsed: Int) = ParseResult(indexOfNextUnparsed)
        fun Error(position: Int, message: () -> String) =
            ParseResult(ParseError(position, message))
    }

    inline fun<T> match(onSuccess: (Int) -> T, onFailure: (ParseError) -> T): T =
        when (value) {
            is Int -> onSuccess(value)
            is ParseError -> onFailure(value)
            else -> error("Unexpected parse result: $value")
        }
}

internal class ParseError(val position: Int, val message: () -> String)
