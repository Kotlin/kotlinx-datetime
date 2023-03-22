/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

import kotlin.native.concurrent.*

/**
 * String format:
 * * A string in single or double quotes is a literal.
 * * `designator<format>` means that `format` must be parsed and formatted in the context of a sub-builder chosen by
 *   `designator`.
 *   For example, in a `LocalDateTime` format builder, `ld<yyyy'-'mm'-'dd>` means that the `yyyy'-'mm'-'dd` format
 *   must be parsed and formatted in the context of a `LocalDate` builder.
 * * `format1|format2` means that either `format1` or `format2` must be used. For parsing, this means that, first,
 *   parsing `format1` is attempted, and if it fails, parsing `format2` is attempted. For formatting, this construct is
 *   only valid if `format2` includes all the fields from `format1` and also possibly some other fields that have
 *   default values. If those fields have their default values, `format1` is used for formatting.
 *   For example, for `UtcOffset`, `'Z'|+HH:mm` is valid and means that, if both the hour and minute components are
 *   zero, `'Z'` is output, otherwise `+HH:mm` is used.
 * * Parentheses, as in `(format)`, are used to establish precedence. For example, `hh:mm(|:ss)` means
 *   `hh:mm` or `hh:mm:ss`, but `hh:mm|:ss` means `hh:mm` or `:ss`.
 * * Symbol `+` before a signed numeric field means that the sign must always be present.
 * * Symbol `-` before a signed numeric field means that the sign must be present only if the value is negative.
 *   This is the default, but the symbol can still be useful, see below.
 * * Symbols `+` and `-` can be used before a format grouped in parentheses, as in `-(format)` and `+(format)`.
 *   In this case, the sign will be output for the whole group, possibly affecting the signs of the fields inside the
 *   group if necessary. For example, `-('P'yy'Y'mm'M')` in a `DatePeriod` means that, if
 *   there are `-15` years and `-10` months, `-P15Y10M` is output, but if there are `15` years and `-10` months,
 *   `P15Y-10M` is output.
 */
internal fun <T> AppendableFormatStructure<T>.appendFormatString(format: String, start: Int = 0): Int {
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
                '+' -> SignedFormatStructure(format, plusSignRequired = true)
                '-' -> SignedFormatStructure(format, plusSignRequired = false)
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
                } else if (c == '+' || c == '-') {
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
    this.add(
        if (alternatives.size == 1) {
            alternatives.first()
        } else {
            AlternativesFormatStructure(alternatives)
        }
    )
    return i
}

// TODO: think about what could eventually become useful
@SharedImmutable
private val reservedChars: List<Char> = listOf()
