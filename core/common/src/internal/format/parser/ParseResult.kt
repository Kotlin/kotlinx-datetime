/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format.parser

import kotlin.jvm.*

@JvmInline
internal value class ParseResult(val value: Any) {
    companion object {
        fun Ok(indexOfNextUnparsed: Int) = ParseResult(indexOfNextUnparsed)
        fun Error(position: Int, message: () -> String) =
            ParseResult(ParseError(position, message))
    }

    fun tryGetIndex(): Int? = value as? Int
    fun tryGetError(): ParseError? = value as? ParseError
}

internal class ParseError(val position: Int, val message: () -> String)