/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.*

/**
 * A format for parsing and formatting date-time-related values.
 */
public interface DateTimeFormat<T> {
    /**
     * Formats the given [value] into a string, using this format.
     */
    public fun format(value: T): String

    /**
     * Parses the given [input] string as [T], using this format.
     */
    public fun parse(input: String): T
}

internal abstract class AbstractDateTimeFormat<T, U : Copyable<U>>(private val actualFormat: Format<U>): DateTimeFormat<T> {

    abstract fun intermediateFromValue(value: T): U

    abstract fun valueFromIntermediate(intermediate: U): T

    abstract fun newIntermediate(): U

    override fun format(value: T): String = StringBuilder().also {
        actualFormat.formatter.format(intermediateFromValue(value), it)
    }.toString()

    private fun copyIntermediate(intermediate: U): U = intermediate.copy()

    override fun parse(input: String): T {
        val parser = Parser(::newIntermediate, ::copyIntermediate, actualFormat.parser)
        val matched = try {
            parser.match(input)
        } catch (e: ParseException) {
            throw DateTimeFormatException("Failed to parse value from '$input'", e)
        }
        try {
            return valueFromIntermediate(matched)
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException("Invalid value '$input'", e)
        }
    }

}
