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
        fun Error(position: Int, cause: Throwable? = null, message: () -> String) =
            ParseResult(ParseError(position, cause, message))
    }

    fun isOk() = value is Int
    fun tryGetIndex(): Int? = if (value is Int) value else null
    fun tryGetError(): ParseError? = if (value is ParseError) value else null
}

internal class ParseError(val position: Int, val cause: Throwable? = null, val message: () -> String)

internal class ParseException(error: ParseError) : Exception("Position ${error.position}: ${error.message()}", error.cause)

