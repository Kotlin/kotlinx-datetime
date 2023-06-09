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
public sealed interface Format<T> {
    /**
     * Formats the given [value] into a string, using this format.
     */
    public fun format(value: T): String

    /**
     * Parses the given [input] string as [T], using this format.
     */
    public fun parse(input: CharSequence): T

    /**
     * Finds the first occurrence of a value matching this format in the given [input], starting from [startIndex].
     * Returns the matched value, or `null` if no value was found.
     *
     * Matches that start in the middle of a number are not considered. For example, if the format is "hours, colon,
     * minutes" and the input is `987654321:00`, no match will be found, despite the substring `21:00` matching the
     * format.
     *
     * If a substring matches the format, but the value it represents is invalid, no match will be found, and
     * the substring will be skipped. For example, if the format is "hours, colon, minutes" and the input is
     * `25:14:30`, no match will be found, despite the substring `14:30` matching the format, because `25:14` also
     * matches it, but is not a valid time.
     *
     * If several values can be parsed from this format at the same position, the longest one is returned.
     */
    public fun find(input: CharSequence, startIndex: Int = 0): T?

    /**
     * Finds the list of occurrences of values matching this format in the given [input], starting from [startIndex].
     *
     * Matches that start in the middle of a number are not considered. For example, if the format is "hours, colon,
     * minutes" and the input is `Code 987654321:00 was invoked at 16:43`, only `16:43` will be included in the list,
     * despite `21:00` also matching the format.
     *
     * If a substring matches the format, but the value it represents is invalid, no match will be found, and
     * the substring will be skipped. For example, if the format is "hours, colon, minutes" and the input is
     * `25:14:30`, no match will be found, despite the substring `14:30` matching the format, because `25:14` also
     * matches it, but is not a valid time.
     *
     * If several values can be parsed from this format at the same position, only the longest one is included in the
     * list. Also, overlapping matches are not included in the list. For example, if the format is "hours, colon,
     * minutes" and the input is `16:12:00`, only `16:12` will be included in the list, despite `12:00` also matching
     * the format.
     */
    public fun findAll(input: CharSequence, startIndex: Int = 0): List<T>
}

internal abstract class AbstractFormat<T, U : Copyable<U>>(private val actualFormat: StringFormat<U>): Format<T> {

    abstract fun intermediateFromValue(value: T): U

    abstract fun valueFromIntermediate(intermediate: U): T

    abstract fun newIntermediate(): U

    open fun valueFromIntermediateOrNull(intermediate: U): T? = try {
        valueFromIntermediate(intermediate)
    } catch (e: IllegalArgumentException) {
        null
    }

    override fun format(value: T): String = StringBuilder().also {
        actualFormat.formatter.format(intermediateFromValue(value), it)
    }.toString()

    private fun copyIntermediate(intermediate: U): U = intermediate.copy()

    val parser: Parser<U> by lazy { Parser(::newIntermediate, ::copyIntermediate, actualFormat.parser) }

    override fun parse(input: CharSequence): T {
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

    override fun find(input: CharSequence, startIndex: Int): T? =
        parser.find(input, startIndex, ::valueFromIntermediateOrNull)

    override fun findAll(input: CharSequence, startIndex: Int): List<T> =
        parser.findAll(input, startIndex, ::valueFromIntermediateOrNull)

}
