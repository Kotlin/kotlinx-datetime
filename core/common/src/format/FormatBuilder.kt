/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*
import kotlinx.datetime.internal.format.AlternativesFormatStructure
import kotlin.native.concurrent.*

@DslMarker
public annotation class DateTimeBuilder

/**
 * Common functions for all the date-time format builders.
 */
@DateTimeBuilder
public sealed interface FormatBuilder {
    /**
     * Appends a literal string to the format.
     * When formatting, the string is appended to the result as is,
     * and when parsing, the string is expected to be present in the input.
     */
    public fun appendLiteral(string: String)
}

/**
 * Appends a set of alternative blocks to the format.
 *
 * When parsing, the blocks are tried in order until one of them succeeds.
 *
 * When formatting, there is a requirement that the later blocks contain all the fields that are present in the
 * earlier blocks. Moreover, the additional fields must have a *default value* defined for them.
 * Then, during formatting, the block that has the most information is chosen.
 *
 * Example:
 * ```
 * appendAlternatives({
 *   appendLiteral("Z")
 * }, {
 *   appendOffsetHours()
 *   appendOptional {
 *     appendLiteral(":")
 *     appendOffsetMinutes()
 *     appendOptional {
 *       appendLiteral(":")
 *       appendOffsetSeconds()
 *     }
 *   }
 * })
 * ```
 * Here, all values have the default value of zero, so the first block is chosen when formatting `UtcOffset.ZERO`.
 */
@Suppress("UNCHECKED_CAST")
public fun <T: FormatBuilder> T.appendAlternatives(vararg blocks: T.() -> Unit): Unit = when (this) {
    is AbstractFormatBuilder<*, *> -> appendAlternativesImpl(*blocks as Array<out AbstractFormatBuilder<*, *>.() -> Unit>)
    else -> TODO()
}

@Suppress("UNCHECKED_CAST")
public fun <T: FormatBuilder> T.withSharedSign(outputPlus: Boolean, block: T.() -> Unit): Unit = when (this) {
    is AbstractFormatBuilder<*, *> -> withSharedSignImpl(outputPlus, block as AbstractFormatBuilder<*, *>.() -> Unit)
    else -> TODO()
}

/**
 * Appends an optional section to the format.
 *
 * When parsing, the section is parsed if it is present in the input.
 *
 * When formatting, the section is formatted if the value of any field in the block is not equal to the default value.
 * Only [appendOptional] calls where all the fields have default values are permitted when formatting.
 *
 * Example:
 * ```
 * appendHours()
 * appendLiteral(":")
 * appendMinutes()
 * appendOptional {
 *   appendLiteral(":")
 *   appendSeconds()
 * }
 * ```
 *
 * Here, because seconds have the default value of zero, they are formatted only if they are not equal to zero.
 *
 * This is a shorthand for `appendAlternatives({}, block)`.
 */
public fun <T: FormatBuilder> T.appendOptional(block: T.() -> Unit): Unit =
    appendAlternatives({}, block)

/**
 * Appends a literal character to the format.
 *
 * This is a shorthand for `appendLiteral(char.toString())`.
 */
public fun FormatBuilder.appendLiteral(char: Char): Unit = appendLiteral(char.toString())

internal interface AbstractFormatBuilder<Target, ActualSelf> :
    FormatBuilder where ActualSelf : AbstractFormatBuilder<Target, ActualSelf> {

    val actualBuilder: AppendableFormatStructure<Target>
    fun createEmpty(): ActualSelf

    fun appendAlternativesImpl(vararg blocks: ActualSelf.() -> Unit) {
        actualBuilder.add(AlternativesFormatStructure(blocks.map { block ->
            createEmpty().also { block(it) }.actualBuilder.build()
        }))
    }

    override fun appendLiteral(string: String) = actualBuilder.add(ConstantFormatStructure(string))

    fun withSharedSignImpl(outputPlus: Boolean, block: ActualSelf.() -> Unit) {
        actualBuilder.add(
            SignedFormatStructure(
                createEmpty().also { block(it) }.actualBuilder.build(),
                outputPlus
            )
        )
    }

    fun build(): StringFormat<Target> = StringFormat(actualBuilder.build())
}

internal interface Copyable<Self> {
    fun copy(): Self
}

internal inline fun<T> StringFormat<T>.builderString(): String = directives.builderString()

private fun<T> FormatStructure<T>.builderString(): String = when (this) {
    is BasicFormatStructure -> directive.builderRepresentation
    is ConstantFormatStructure -> "appendLiteral(${string.repr()})"
    is SignedFormatStructure -> buildString {
        if (withPlusSign) appendLine("withSharedSign(outputPlus = true) {")
        else appendLine("withSharedSign {")
        appendLine(format.builderString().prependIndent(CODE_INDENT))
        append("}")
    }
    is AlternativesFormatStructure -> buildString {
        if (formats.size == 2 && formats.first().formats.isEmpty()) {
            appendLine("appendOptional {")
            appendLine(formats[1].builderString().prependIndent(CODE_INDENT))
            append("}")
        }
        append("appendAlternatives(")
        for (alternative in formats) {
            appendLine("{")
            appendLine(alternative.builderString().prependIndent(CODE_INDENT))
            append("}, ")
        }
        if (this[length - 2] == ',') {
            repeat(2) {
                deleteAt(length - 1)
            }
        }
        append(")")
    }
    is ConcatenatedFormatStructure -> buildString {
        for (format in formats) {
            appendLine(format.builderString())
        }
    }
}

private const val CODE_INDENT = "  "
