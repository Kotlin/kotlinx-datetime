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
        fun Error(message: String, position: Int, cause: Throwable? = null) =
            ParseResult(ParseException(message, position, cause))
    }

    fun isOk() = value is Int
    fun tryGetIndex(): Int? = if (value is Int) value else null
    fun tryGetError(): ParseException? = if (value is ParseException) value else null
}

internal class ParseException(message: String, val position: Int, cause: Throwable? = null) : Exception("Position $position: $message", cause)
