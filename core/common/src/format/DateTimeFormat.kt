/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.parser.*

/**
 * A format for parsing and formatting datetime-related values.
 *
 * By convention, predefined formats for each applicable class can be found in the `Formats` object of the class, and
 * custom formats can be created using the `Format` function in the companion object of that class.
 * For example, [LocalDate.Formats] contains predefined formats for [LocalDate], and [LocalDate.Format] can be used
 * to define new ones.
 */
public sealed interface DateTimeFormat<T> {
    /**
     * Formats the given [value] into a string using this format.
     *
     * @throws IllegalArgumentException if the value does not contain all the information required by the format.
     * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.format
     */
    public fun format(value: T): String

    /**
     * Formats the given [value] into the given [appendable] using this format.
     *
     * @throws IllegalArgumentException if the value does not contain all the information required by the format.
     * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.formatTo
     */
    public fun <A : Appendable> formatTo(appendable: A, value: T): A

    /**
     * Parses the given [input] string as [T] using this format.
     *
     * @throws IllegalArgumentException if the input string is not in the expected format or the value is invalid.
     * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.parse
     */
    public fun parse(input: CharSequence): T

    /**
     * Parses the given [input] string as [T] using this format.
     *
     * @return the parsed value, or `null` if the input string is not in the expected format or the value is invalid.
     * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.parseOrNull
     */
    public fun parseOrNull(input: CharSequence): T?

    public companion object {
        /**
         * Produces Kotlin code that, when pasted into a Kotlin source file, creates a [DateTimeFormat] instance that
         * behaves identically to [format].
         *
         * The typical use case for this is to create a [DateTimeFormat] instance using a non-idiomatic approach and
         * then convert it to a builder DSL.
         *
         * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.formatAsKotlinBuilderDsl
         */
        public fun formatAsKotlinBuilderDsl(format: DateTimeFormat<*>): String = when (format) {
            is AbstractDateTimeFormat<*, *> -> format.actualFormat.builderString(allFormatConstants)
        }
    }
}

/**
 * The style of padding to use when formatting a value.
 *
 * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.PaddingSamples.usage
 */
public enum class Padding {
    /**
     * No padding during formatting. Parsing does not require padding, but it is allowed.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.PaddingSamples.none
     */
    NONE,

    /**
     * Pad with zeros during formatting. During parsing, padding is required; otherwise, parsing fails.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.PaddingSamples.zero
     */
    ZERO,

    /**
     * Pad with spaces during formatting. During parsing, padding is required; otherwise, parsing fails.
     *
     * @sample kotlinx.datetime.test.samples.format.DateTimeFormatSamples.PaddingSamples.spaces
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
            throw DateTimeFormatException(when (val message = e.message) {
                null -> "The value parsed from '$input' is invalid"
                else -> "$message (when parsing '$input')"
            }, e)
        }
    }

    override fun parseOrNull(input: CharSequence): T? =
        // without the fully qualified name, the compilation fails for some reason
        Parser(actualFormat.parser()).matchOrNull(input, emptyIntermediate)?.let { valueFromIntermediateOrNull(it) }

}

private val allFormatConstants: List<Pair<String, CachedFormatStructure<*>>> by lazy {
    fun unwrap(format: DateTimeFormat<*>): CachedFormatStructure<*> = (format as AbstractDateTimeFormat<*, *>).actualFormat
    // the formats are ordered vaguely by decreasing length, as the topmost among suitable ones is chosen.
    listOf(
        "${DateTimeFormatBuilder.WithDateTimeComponents::dateTimeComponents.name}(DateTimeComponents.Formats.RFC_1123)" to
            unwrap(DateTimeComponents.Formats.RFC_1123),
        "${DateTimeFormatBuilder.WithDateTimeComponents::dateTimeComponents.name}(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)" to
            unwrap(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET),
        "${DateTimeFormatBuilder.WithDateTime::date.name}(LocalDateTime.Formats.ISO)" to unwrap(LocalDateTime.Formats.ISO),
        "${DateTimeFormatBuilder.WithDate::date.name}(LocalDate.Formats.ISO)" to unwrap(LocalDate.Formats.ISO),
        "${DateTimeFormatBuilder.WithDate::date.name}(LocalDate.Formats.ISO_BASIC)" to unwrap(LocalDate.Formats.ISO_BASIC),
        "${DateTimeFormatBuilder.WithTime::time.name}(LocalTime.Formats.ISO)" to unwrap(LocalTime.Formats.ISO),
        "${DateTimeFormatBuilder.WithUtcOffset::offset.name}(UtcOffset.Formats.ISO)" to unwrap(UtcOffset.Formats.ISO),
        "${DateTimeFormatBuilder.WithUtcOffset::offset.name}(UtcOffset.Formats.ISO_BASIC)" to unwrap(UtcOffset.Formats.ISO_BASIC),
        "${DateTimeFormatBuilder.WithUtcOffset::offset.name}(UtcOffset.Formats.FOUR_DIGITS)" to unwrap(UtcOffset.Formats.FOUR_DIGITS),
    )
}
