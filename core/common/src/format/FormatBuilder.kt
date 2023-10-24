/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.format

import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.*

/**
 * Common functions for all format builders.
 */
public sealed interface FormatBuilder {
    /**
     * Appends a literal string to the format.
     * When formatting, the string is appended to the result as is,
     * and when parsing, the string is expected to be present in the input.
     */
    public fun chars(value: String)
}

/**
 * Appends a format along with other ways to parse the same portion of the value.
 *
 * When parsing, the first, [primaryFormat] is used to parse the value, and if parsing fails using that, the formats
 * from [alternativeFormats] are tried in order.
 *
 * When formatting, the [primaryFormat] is used to format the value.
 */
@Suppress("UNCHECKED_CAST")
public fun <T: FormatBuilder> T.alternativeParsing(
    vararg alternativeFormats: T.() -> Unit,
    primaryFormat: T.() -> Unit
): Unit = when (this) {
    is AbstractFormatBuilder<*, *> ->
        appendAlternativeParsingImpl(*alternativeFormats as Array<out AbstractFormatBuilder<*, *>.() -> Unit>,
            mainFormat = primaryFormat as (AbstractFormatBuilder<*, *>.() -> Unit))
    else -> throw IllegalStateException("impossible")
}

/**
 * Appends an optional section to the format.
 *
 * When parsing, the section is parsed if it is present in the input.
 *
 * When formatting, the section is formatted if the value of any field in the block is not equal to the default value.
 * Only [optional] calls where all the fields have default values are permitted when formatting.
 *
 * Example:
 * ```
 * appendHours()
 * char(':')
 * appendMinutes()
 * optional {
 *   char(':')
 *   appendSeconds()
 * }
 * ```
 *
 * Here, because seconds have the default value of zero, they are formatted only if they are not equal to zero.
 *
 * [ifZero] defines the string that is used if values are the default ones.
 */
@Suppress("UNCHECKED_CAST")
public fun <T: FormatBuilder> T.optional(ifZero: String = "", format: T.() -> Unit): Unit = when (this) {
    is AbstractFormatBuilder<*, *> -> appendOptionalImpl(onZero = ifZero, format as (AbstractFormatBuilder<*, *>.() -> Unit))
    else -> throw IllegalStateException("impossible")
}

/**
 * Appends a literal character to the format.
 *
 * This is a shorthand for `chars(value.toString())`.
 */
public fun FormatBuilder.char(value: Char): Unit = chars(value.toString())

internal interface AbstractFormatBuilder<Target, ActualSelf> :
    FormatBuilder where ActualSelf : AbstractFormatBuilder<Target, ActualSelf> {

    val actualBuilder: AppendableFormatStructure<Target>
    fun createEmpty(): ActualSelf

    fun appendAlternativeParsingImpl(
        vararg otherFormats: ActualSelf.() -> Unit,
        mainFormat: ActualSelf.() -> Unit
    ) {
        val others = otherFormats.map { block ->
            createEmpty().also { block(it) }.actualBuilder.build()
        }
        val main = createEmpty().also { mainFormat(it) }.actualBuilder.build()
        actualBuilder.add(AlternativesParsingFormatStructure(main, others))
    }

    fun appendOptionalImpl(
        onZero: String,
        format: ActualSelf.() -> Unit
    ) {
        actualBuilder.add(OptionalFormatStructure(onZero, createEmpty().also { format(it) }.actualBuilder.build()))
    }

    override fun chars(value: String) = actualBuilder.add(ConstantFormatStructure(value))

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
    is ConstantFormatStructure -> "char(${string.toKotlinCode()})"
    is SignedFormatStructure -> {
        if (format is BasicFormatStructure && format.directive is UtcOffsetWholeHoursDirective) {
            format.directive.builderRepresentation
        } else {
            buildString {
                if (withPlusSign) appendLine("withSharedSign(outputPlus = true) {")
                else appendLine("withSharedSign {")
                appendLine(format.builderString().prependIndent(CODE_INDENT))
                append("}")
            }
        }
    }
    is OptionalFormatStructure -> buildString {
        if (onZero == "") {
            appendLine("optional {")
        } else {
            appendLine("optional(${onZero.toKotlinCode()}) {")
        }
        appendLine(format.builderString().prependIndent(CODE_INDENT))
        append("}")
    }
    is AlternativesParsingFormatStructure -> buildString {
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
        appendLine(") {")
        appendLine(mainFormat.builderString().prependIndent(CODE_INDENT))
        append("}")
    }
    is ConcatenatedFormatStructure -> buildString {
        for (format in formats) {
            appendLine(format.builderString())
        }
    }
}

private const val CODE_INDENT = "  "
