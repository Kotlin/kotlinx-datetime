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
public interface FormatBuilder<out Self> {
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
    public fun appendAlternatives(vararg blocks: Self.() -> Unit)

    /**
     * Appends a literal string to the format.
     * When formatting, the string is appended to the result as is,
     * and when parsing, the string is expected to be present in the input.
     */
    public fun appendLiteral(string: String)

    /**
     * Appends a format string to the format.
     *
     * String format:
     * * A string in single or double quotes is a literal.
     * * `designator<format>` means that `format` must be parsed and formatted in the context of a sub-builder chosen by
     *   `designator`.
     *   For example, in a `LocalDateTime` format builder, `ld<yyyy-mm-dd>` means that the `yyyy-mm-dd` format
     *   must be parsed and formatted in the context of a `LocalDate` builder.
     * * `format1|format2` means that either `format1` or `format2` must be used. For parsing, this means that, first,
     *   parsing `format1` is attempted, and if it fails, parsing `format2` is attempted. For formatting, this construct is
     *   only valid if `format2` includes all the fields from `format1` and also possibly some other fields that have
     *   default values. If those fields have their default values, `format1` is used for formatting.
     *   For example, for `UtcOffset`, `'Z'|+hh:mm` is valid and means that, if both the hour and minute components are
     *   zero, `'Z'` is output, otherwise `+hh:mm` is used.
     * * Parentheses, as in `(format)`, are used to establish precedence. For example, `hh:mm(|:ss)` means
     *   `hh:mm` or `hh:mm:ss`, but `hh:mm|:ss` means `hh:mm` or `:ss`.
     * * Symbol `+` before a signed numeric field means that the sign must always be present.
     * * The symbol `+` can be used before a format grouped in parentheses, as in `+(format)`.
     *   In this case, the sign will be output for the whole group, possibly affecting the signs of the fields inside the
     *   group if necessary. For example, `+('P'yy'Y'mm'M')` in a `DatePeriod` means that, if
     *   there are `-15` years and `-10` months, `-P15Y10M` is output, but if there are `15` years and `-10` months,
     *   `+P15Y-10M` is output.
     *
     * @throws IllegalArgumentException if the format string is invalid.
     */
    public fun appendFormatString(formatString: String)
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
public fun <Self> FormatBuilder<Self>.appendOptional(block: Self.() -> Unit): Unit =
    appendAlternatives({}, block)

/**
 * Appends a literal character to the format.
 *
 * This is a shorthand for `appendLiteral(char.toString())`.
 */
public fun <Self> FormatBuilder<Self>.appendLiteral(char: Char): Unit = appendLiteral(char.toString())

internal interface AbstractFormatBuilder<Target, out UserSelf, ActualSelf> :
    FormatBuilder<UserSelf> where ActualSelf : AbstractFormatBuilder<Target, UserSelf, ActualSelf> {

    val actualBuilder: AppendableFormatStructure<Target>
    fun createEmpty(): ActualSelf
    fun castToGeneric(actualSelf: ActualSelf): UserSelf

    override fun appendAlternatives(vararg blocks: UserSelf.() -> Unit) {
        actualBuilder.add(AlternativesFormatStructure(blocks.map { block ->
            createEmpty().also { block(castToGeneric(it)) }.actualBuilder.build()
        }))
    }

    override fun appendLiteral(string: String) = actualBuilder.add(ConstantFormatStructure(string))

    override fun appendFormatString(formatString: String) {
        val end = actualBuilder.appendFormatString(formatString)
        require(end == formatString.length) {
            "Unexpected char '${formatString[end]}' in $formatString at position $end"
        }
    }

    fun withSharedSign(outputPlus: Boolean, block: UserSelf.() -> Unit) {
        actualBuilder.add(
            SignedFormatStructure(
                createEmpty().also { block(castToGeneric(it)) }.actualBuilder.build(),
                outputPlus
            )
        )
    }

    fun build(): StringFormat<Target> = StringFormat(actualBuilder.build())
}

internal interface Copyable<Self> {
    fun copy(): Self
}

private fun <T> AppendableFormatStructure<T>.appendFormatString(format: String, start: Int = 0): Int {
    val alternatives = mutableListOf<ConcatenatedFormatStructure<T>>()
    var currentBuilder: AppendableFormatStructure<T> = createSibling()
    var inSingleQuotes = false
    var inDoubleQuotes = false
    var fragmentBeginning: Int? = null
    var sign: Char? = null
    fun add(format: FormatStructure<T>) {
        currentBuilder.add(
            when (sign) {
                null -> format
                '+' -> SignedFormatStructure(format, withPlusSign = true)
                '-' -> SignedFormatStructure(format, withPlusSign = false)
                else -> throw IllegalArgumentException("Unexpected sign $sign")
            }
        )
        sign = null
    }

    fun readDirectivesFromFragment() {
        while (true) {
            val directiveStart = fragmentBeginning ?: break
            var directiveEnd = directiveStart + 1
            while (directiveEnd < format.length && format[directiveEnd] == format[directiveStart]) {
                ++directiveEnd
            }
            val parsedDirective =
                currentBuilder.formatFromDirective(format[directiveStart], directiveEnd - directiveStart)
            require(parsedDirective != null) {
                "Builder $currentBuilder does not recognize the directive ${
                    format.substring(directiveStart, directiveEnd)
                } at position $directiveStart"
            }
            add(parsedDirective)
            fragmentBeginning = if (directiveEnd < format.length && format[directiveEnd].isLetter()) {
                directiveEnd
            } else {
                null
            }
        }
    }

    var i = start
    fun checkClosingParenthesis(beginning: Int, end: Int, expected: Char) {
        require(end < format.length) {
            "Expected '$expected' at position $end to match the start at position $beginning, but got end of string"
        }
        require(format[end] == expected) {
            "Expected '$expected' at position $end to match the start at position $beginning, but got '${format[end]}'"
        }
    }
    while (i < format.length) {
        val c = format[i]
        if (inSingleQuotes) {
            if (c == '\'') {
                add(ConstantFormatStructure(format.substring(fragmentBeginning!!, i)))
                fragmentBeginning = null
                inSingleQuotes = false
            }
        } else if (inDoubleQuotes) {
            if (c == '"') {
                add(ConstantFormatStructure(format.substring(fragmentBeginning!!, i)))
                fragmentBeginning = null
                inDoubleQuotes = false
            }
        } else {
            if (c == '<') {
                // we treat the letters before as the marker of a sub-builder
                val subBuilderNameStart = fragmentBeginning
                require(subBuilderNameStart != null) {
                    "Got '<' at position $i, but there was no sub-builder name before it"
                }
                fragmentBeginning = null
                val subBuilderName = format.substring(subBuilderNameStart, i)
                val subFormat: FormatStructure<T>? = currentBuilder.formatFromSubBuilder(subBuilderName) {
                    val end = appendFormatString(format, i + 1)
                    checkClosingParenthesis(subBuilderNameStart, end, '>')
                    i = end
                }
                require(subFormat != null) {
                    "Builder $currentBuilder does not recognize sub-builder $subBuilderName at position $subBuilderNameStart"
                }
                add(subFormat)
            } else if (c.isLetter()) {
                // we don't know yet how to treat this letter, so we'll skip over for now
                fragmentBeginning = fragmentBeginning ?: i
            } else {
                // if there were letters before, we'll treat them as directives, as there's no `<` after them
                readDirectivesFromFragment()
                if (c == '\'') {
                    inSingleQuotes = true
                    fragmentBeginning = i + 1
                } else if (c == '"') {
                    inDoubleQuotes = true
                    fragmentBeginning = i + 1
                } else if (c == '|') {
                    alternatives.add(currentBuilder.build())
                    currentBuilder = currentBuilder.createSibling()
                } else if (c == '+') {
                    require(sign == null) {
                        "Found '$c' on position $i, but a sign '$sign' was already specified at position ${i - 1}"
                    }
                    sign = c
                } else if (c == '(') {
                    val subBuilder = currentBuilder.createSibling()
                    val end = subBuilder.appendFormatString(format, i + 1)
                    checkClosingParenthesis(i, end, ')')
                    i = end
                    add(subBuilder.build())
                } else if (c == ')' || c == '>') {
                    break
                } else {
                    require(c !in reservedChars) {
                        "Character '$c' is reserved for use in future versions, but was encountered at position $i"
                    }
                    add(ConstantFormatStructure(format[i].toString()))
                }
            }
        }
        ++i
    }
    if (inSingleQuotes) checkClosingParenthesis(fragmentBeginning!!, i, '\'')
    if (inDoubleQuotes) checkClosingParenthesis(fragmentBeginning!!, i, '"')
    readDirectivesFromFragment()
    if (sign != null)
        throw IllegalArgumentException("Sign $sign is not followed by a format")
    alternatives.add(currentBuilder.build())
    this.add(alternatives.singleOrNull() ?: AlternativesFormatStructure(alternatives))
    return i
}

// TODO: think about what could eventually become useful
@SharedImmutable
private val reservedChars: List<Char> = listOf()

internal fun<T> StringFormat<T>.builderString(): String = directives.formatString()?.let { (group, contents) ->
    if (group == null) contents else "$group<${contents}>"
} ?: directives.builderString()

private fun<T> FormatStructure<T>.formatString(): Pair<String?, String>? = when (this) {
    is BasicFormatStructure -> directive.formatStringRepresentation
    is ConstantFormatStructure -> if (string.contains('\'')) null else null to "'$string'"
    is SignedFormatStructure ->
        if (withPlusSign) format.formatString()?.let { (group, format) -> group to "+($format)" } else null
    is AlternativesFormatStructure -> {
        val formatStrings = formats.map { it.formatString() ?: return null }
        val groups = formatStrings.mapNotNull(Pair<String?, *>::first).distinct()
        if (groups.size == 1) {
            groups.first() to "(" + formatStrings.joinToString("|") { it.second } + ")"
        } else {
            null to "(" + formatStrings.joinToString("|") { (group, format) ->
                if (group == null) format else "$group<$format>"
            } + ")"
        }
    }
    is ConcatenatedFormatStructure -> {
        val formatStrings = formatStrings().map { it ?: return null }
        val groups: List<String> = formatStrings.mapNotNull(Pair<String?, *>::first).distinct()
        if (groups.size == 1) {
            groups.first() to formatStrings.flatMap { it.second }.joinToString("")
        } else {
            null to formatStringForSpan(formatStrings)
        }
    }
}

private fun<T> FormatStructure<T>.builderString(): String = when (this) {
    is BasicFormatStructure -> directive.builderRepresentation
    is ConstantFormatStructure -> "appendLiteral(${string.repr()})"
    is SignedFormatStructure -> buildString {
        if (withPlusSign) appendLine("withSharedSign(outputPlus = true) {")
        else appendLine("withSharedSign {")
        appendLine(format.builderString().prependIndent(CODE_INDENT))
        appendLine("}")
    }
    is AlternativesFormatStructure -> buildString {
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
    is ConcatenatedFormatStructure -> {
        var i = 0
        val result = StringBuilder()
        var cumulativeIndex = 0
        val formatStrings = formatStrings()
        while (i < formatStrings.size) {
            val format = formatStrings[i]
            if (format == null) {
                result.appendLine(formats[cumulativeIndex].builderString())
                ++cumulativeIndex
                ++i
            } else {
                var j = i
                val copy = mutableListOf<Pair<String?, List<String>>>()
                while (j < formatStrings.size) {
                    copy.add(formatStrings[j] ?: break)
                    ++j
                }
                val totalDirectives = (i until j).sumOf { formatStrings[it]?.second?.size ?: 0 }
                if (totalDirectives >= 3 || j == formatStrings.size) {
                    result.appendLine("appendFormatString(${formatStringForSpan(copy).repr()})")
                } else {
                    for (k in cumulativeIndex until cumulativeIndex + totalDirectives) {
                        result.appendLine(formats[k].builderString())
                    }
                }
                cumulativeIndex += totalDirectives
                i = j
            }
        }
        result.toString()
    }
}

private const val CODE_INDENT = "  "

private fun <T> ConcatenatedFormatStructure<T>.formatStrings(): List<Pair<String?, List<String>>?> {
    val result = mutableListOf<Pair<String?, List<String>>?>()
    var currentGroup : String? = null
    val formatStrings = formats.map { it.formatString() }
    val currentInGroup: MutableList<String> = mutableListOf()
    fun flush() {
        result.add(currentGroup to currentInGroup.toList())
        currentInGroup.clear()
        currentGroup = null
    }
    // would be a nice use case for a `groupingBy`, but we have two kinds of `null` here for a group.
    for (i in formatStrings.indices) {
        when (val format = formatStrings[i]) {
            null -> {
                flush()
                result.add(null)
            }
            else -> {
                val (group, formatString) = format
                if (group == currentGroup) {
                    currentInGroup.add(formatString)
                } else {
                    flush()
                    currentGroup = group
                    currentInGroup.add(formatString)
                }
            }
        }
    }
    flush()
    return result
}

private fun formatStringForSpan(strings: List<Pair<String?, List<String>>>): String {
    var i = 0
    val result = StringBuilder()
    while (i < strings.size) {
        val (group, contents) = strings[i]
        if (group == null) {
            result.append(contents.joinToString(""))
            ++i
        } else {
            var j = i + 1
            while (j < strings.size && (strings[j].first == group || strings[j].first == null))
                ++j
            while (strings[j - 1].first == null)
                --j
            result.append("$group<${strings.subList(i, j).flatMap { it.second }.joinToString("")}>")
            i = j
        }
    }
    return result.toString()
}
