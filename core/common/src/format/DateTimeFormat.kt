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
public sealed interface DateTimeFormat<T> {
    /**
     * Formats the given [value] into a string, using this format.
     */
    public fun format(value: T): String

    /**
     * Formats the given [value] into the given [appendable] using this format.
     */
    public fun <A : Appendable> formatTo(appendable: A, value: T): A

    /**
     * Parses the given [input] string as [T] using this format.
     *
     * @throws IllegalArgumentException if the input string is not in the expected format or the value is invalid.
     */
    public fun parse(input: CharSequence): T

    /**
     * Parses the given [input] string as [T] using this format.
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

internal fun Padding.toKotlinCode(): String = when (this) {
    Padding.NONE -> "Padding.NONE"
    Padding.ZERO -> "Padding.ZERO"
    Padding.SPACE -> "Padding.SPACE"
}

internal inline fun Padding.minDigits(width: Int) = if (this == Padding.ZERO) width else 1
internal inline fun Padding.spaces(width: Int) = if (this == Padding.SPACE) width else null

/** [T] is the user-visible type, whereas [U] is its mutable representation for parsing and formatting. */
internal sealed class AbstractDateTimeFormat<T, U : Copyable<U>> : DateTimeFormat<T> {

    abstract val actualFormat: CachedFormatStructure<U>

    abstract fun intermediateFromValue(value: T): U

    abstract fun valueFromIntermediate(intermediate: U): T

    abstract val emptyIntermediate: U // should be part of the `Copyable` interface once the language allows this

    open fun valueFromIntermediateOrNull(intermediate: U): T? = try {
        valueFromIntermediate(intermediate)
    } catch (e: IllegalArgumentException) {
        null
    }

    override fun format(value: T): String = StringBuilder().also {
        actualFormat.formatter().format(intermediateFromValue(value), it)
    }.toString()

    override fun <A : Appendable> formatTo(appendable: A, value: T): A = appendable.apply {
        actualFormat.formatter().format(intermediateFromValue(value), this)
    }

    override fun parse(input: CharSequence): T {
        val matched = try {
            // without the fully qualified name, the compilation fails for some reason
            Parser(actualFormat.parser()).match(input, emptyIntermediate)
        } catch (e: ParseException) {
            throw DateTimeFormatException("Failed to parse value from '$input'", e)
        }
        try {
            return valueFromIntermediate(matched)
        } catch (e: IllegalArgumentException) {
            throw DateTimeFormatException(e.message!!)
        }
    }

    override fun parseOrNull(input: CharSequence): T? =
        // without the fully qualified name, the compilation fails for some reason
        Parser(actualFormat.parser()).matchOrNull(input, emptyIntermediate)?.let { valueFromIntermediateOrNull(it) }

}
