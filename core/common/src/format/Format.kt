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
     * Formats the given [value] into the given [appendable], using this format.
     */
    public fun formatTo(appendable: Appendable, value: T)

    /**
     * Parses the given [input] string as [T], using this format.
     *
     * @throws DateTimeFormatException if the input string is not in the expected format or the value is invalid.
     */
    public fun parse(input: CharSequence): T

    /**
     * Parses the given [input] string as [T], using this format.
     *
     * @return the parsed value, or `null` if the input string is not in the expected format or the value is invalid.
     */
    public fun parseOrNull(input: CharSequence): T?
}

/**
 * The style of padding to use when formatting a value.
 */
public enum class Padding {
    /**
     * No padding.
     */
    NONE,

    /**
     * Pad with zeros.
     */
    ZERO,

    /**
     * Pad with spaces.
     */
    SPACE
}

internal inline fun Padding.minDigits(width: Int) = if (this == Padding.ZERO) width else 1
internal inline fun Padding.spaces(width: Int) = if (this == Padding.SPACE) width else null

/** [T] is the user-visible type, whereas [U] is its mutable representation for parsing and formatting. */
internal sealed class AbstractFormat<T, U : Copyable<U>>(private val actualFormat: StringFormat<U>): Format<T> {

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

    override fun formatTo(appendable: Appendable, value: T) {
        actualFormat.formatter.format(intermediateFromValue(value), appendable)
    }

    private fun copyIntermediate(intermediate: U): U = intermediate.copy()

    private val parser: Parser<U> by lazy { Parser(::newIntermediate, ::copyIntermediate, actualFormat.parser) }

    override fun parse(input: CharSequence): T {
        val matched = try {
            parser.match(input)
        } catch (e: ParseException) {
            throw DateTimeFormatException("Failed to parse value from '$input'", e)
        }
        try {
            return valueFromIntermediate(matched)
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException(e.message!!)
        }
    }

    override fun parseOrNull(input: CharSequence): T? = try {
        parser.match(input)
    } catch (e: ParseException) {
        null
    }?.let { valueFromIntermediateOrNull(it) }

}
