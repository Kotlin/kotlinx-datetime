/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.formatter.*
import kotlinx.datetime.internal.format.parser.*

internal sealed interface FormatStructure<in T> {
    fun formatString(): Pair<String?, String>?
    fun builderString(): String
}

internal class BasicFormatStructure<in T>(
    val directive: FieldFormatDirective<T>
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "BasicFormatStructure($directive)"

    override fun builderString(): String = directive.builderRepresentation

    override fun formatString(): Pair<String?, String>? = directive.formatStringRepresentation
}

internal class ConstantFormatStructure<in T>(
    val string: String
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "ConstantFormatStructure($string)"

    override fun builderString(): String = "appendLiteral(${string.repr()})"

    override fun formatString(): Pair<String?, String>? = if (string.contains('\'')) null else null to "'$string'"
}

internal class SignedFormatStructure<in T>(
    val format: FormatStructure<T>,
    val withPlusSign: Boolean,
) : NonConcatenatedFormatStructure<T> {

    internal val fields = basicFormats(format).mapNotNull { it.field.sign }.toSet()

    init {
        require(fields.isNotEmpty()) { "Signed format must contain at least one field with a sign" }
    }

    override fun formatString(): Pair<String?, String>? =
        if (withPlusSign) format.formatString()?.let { (group, format) -> return group to "+($format)" } else null

    override fun builderString(): String = buildString {
        if (withPlusSign) appendLine("withSharedSign(outputPlus = true) {")
        else appendLine("withSharedSign {")
        appendLine(format.builderString().prependIndent(CODE_INDENT))
        appendLine("}")
    }

    override fun toString(): String = "SignedFormatStructure($format)"
}

internal class AlternativesFormatStructure<in T>(
    val formats: List<ConcatenatedFormatStructure<T>>
) : NonConcatenatedFormatStructure<T> {

    override fun formatString(): Pair<String?, String>? {
        val formatStrings = formats.map { it.formatString() ?: return null }
        val groups = formatStrings.mapNotNull(Pair<String?, *>::first).distinct()
        return if (groups.size == 1) {
            groups.first() to "(" + formatStrings.joinToString("|") { it.second } + ")"
        } else {
            null to "(" + formatStrings.joinToString("|") { (group, format) ->
                if (group == null) format else "$group<$format>"
            } + ")"
        }
    }

    override fun builderString(): String = buildString {
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

    override fun toString(): String = "AlternativesFormatStructure(${formats.joinToString(", ")})"
}

internal sealed interface NonConcatenatedFormatStructure<in T> : FormatStructure<T>

internal class ConcatenatedFormatStructure<in T>(
    val formats: List<NonConcatenatedFormatStructure<T>>
) : FormatStructure<T> {
    override fun formatString(): Pair<String?, String>? {
        val formatStrings = formatStrings.map { it ?: return null }
        val groups: List<String> = formatStrings.mapNotNull(Pair<String?, *>::first).distinct()
        if (groups.size == 1) {
            return groups.first() to formatStrings.flatMap { it.second }.joinToString("")
        }
        return null to formatStringForSpan(formatStrings)
    }

    override fun builderString(): String {
        var i = 0
        val result = StringBuilder()
        var cumulativeIndex = 0
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
        return result.toString()
    }

    private fun formatStrings(): List<Pair<String?, List<String>>?> {
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

    val formatStrings: List<Pair<String?, List<String>>?> by lazy { formatStrings() }

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

    override fun toString(): String = "ConcatenatedFormatStructure(${formats.joinToString(", ")})"
}

internal fun <T> FormatStructure<T>.formatter(): FormatterStructure<T> {
    fun FormatStructure<T>.rec(): Pair<FormatterStructure<T>, Set<FieldSpec<T, *>>> = when (this) {
        is BasicFormatStructure -> BasicFormatter(directive.formatter()) to setOf(directive.field)
        is ConstantFormatStructure -> BasicFormatter(ConstantStringFormatterOperation<T>(string)) to emptySet()
        is SignedFormatStructure -> {
            val (innerFormat, fieldSpecs) = format.rec()
            fun checkIfAllNegative(value: T): Boolean {
                var seenNonZero = false
                for (check in fields) {
                    when {
                        check.isNegative.get(value) == true -> seenNonZero = true
                        check.isZero(value) -> continue
                        else -> return false
                    }
                }
                return seenNonZero
            }
            SignedFormatter(
                innerFormat,
                ::checkIfAllNegative,
                withPlusSign
            ) to fieldSpecs
        }

        is AlternativesFormatStructure -> {
            val maxFieldSet = mutableSetOf<FieldSpec<T, *>>()
            var lastFieldSet: Set<FieldSpec<T, *>>? = null
            val result = mutableListOf<Pair<T.() -> Boolean, FormatterStructure<T>>>()
            for (i in formats.indices.reversed()) {
                val (formatter, fields) = formats[i].rec()
                require(lastFieldSet?.containsAll(fields) != false) {
                    "The only formatters that include the OR operator are of the form (A|B) " +
                        "where B contains all fields of A, but $fields is not included in $lastFieldSet. " +
                        "If your use case requires other usages of the OR operator for formatting, please contact us at " +
                        "https://github.com/Kotlin/kotlinx-datetime/issues"
                }
                val fieldsToCheck = lastFieldSet?.minus(fields) ?: emptySet()
                val predicate = ConjunctionPredicate(fieldsToCheck.map {
                    it.toComparisonPredicate() ?: throw IllegalArgumentException(
                        "The only formatters that include the OR operator are of the form (A|B) " +
                            "where B contains all fields of A and some other fields that have a default value. " +
                            "However, the field ${it.name} does not have a default value. " +
                            "If your use case requires other usages of the OR operator for formatting, please contact us at " +
                            "https://github.com/Kotlin/kotlinx-datetime/issues"
                    )
                })
                if (predicate.isConstTrue()) {
                    result.clear()
                }
                result.add(predicate::test to formatter)
                maxFieldSet.addAll(fields)
                lastFieldSet = fields
            }
            result.reverse()
            ConditionalFormatter(result) to maxFieldSet
        }

        is ConcatenatedFormatStructure -> {
            val (formatters, fields) = formats.map { it.rec() }.unzip()
            ConcatenatedFormatter(formatters) to fields.flatten().toSet()
        }
    }
    return rec().first
}

// A workaround: for some reason, replacing `E` with `*` causes this not to type properly, and `*` is inferred on the
// call site, so we have to move this to a separate function.
internal fun <T, E> FieldSpec<T, E>.toComparisonPredicate(): ComparisonPredicate<T, E>? =
    defaultValue?.let { ComparisonPredicate(it, accessor::get) }

private fun <T> FormatStructure<T>.parser(): ParserStructure<T> = when (this) {
    is ConstantFormatStructure ->
        ParserStructure(operations = listOf(PlainStringParserOperation(string)), followedBy = emptyList())
    is SignedFormatStructure -> {
        listOf(ParserStructure(
            operations = listOf(SignParser(
                isNegativeSetter = { value, isNegative ->
                    for (field in fields) {
                        val wasNegative = field.isNegative.get(value) == true
                        // TODO: replacing `!=` with `xor` fails on JS
                        field.isNegative.set(value, isNegative != wasNegative)
                    }
                },
                withPlusSign = withPlusSign,
                whatThisExpects = "sign for ${this.fields}"
            )),
            emptyList()
        ), format.parser()
        ).concat()
    }
    is BasicFormatStructure -> directive.parser()
    is AlternativesFormatStructure ->
        ParserStructure(operations = emptyList(), followedBy = formats.map { it.parser() })
    is ConcatenatedFormatStructure -> formats.map { it.parser() }.concat()
}

internal class Format<T>(private val directives: ConcatenatedFormatStructure<T>) {
    val formatter: FormatterStructure<T> by lazy {
        directives.formatter()
    }
    val parser: ParserStructure<T> by lazy {
        directives.parser()
    }

    override fun toString(): String = directives.formatString()?.let { (group, contents) ->
        if (group == null) contents else "$group<${contents}>"
    } ?: directives.builderString()
}

private fun <T> basicFormats(format: FormatStructure<T>): List<FieldFormatDirective<T>> = when (format) {
    is BasicFormatStructure -> listOf(format.directive)
    is ConcatenatedFormatStructure -> format.formats.flatMap { basicFormats(it) }
    is AlternativesFormatStructure -> format.formats.flatMap { basicFormats(it) }
    is ConstantFormatStructure -> emptyList()
    is SignedFormatStructure -> basicFormats(format.format)
}

private const val CODE_INDENT = "  "
