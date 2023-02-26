/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

import kotlinx.datetime.internal.format.formatter.*
import kotlinx.datetime.internal.format.parser.*

internal sealed interface FormatStructure<in T>

internal class BasicFormatStructure<in T>(
    val directive: FieldFormatDirective<T>
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "BasicFormatStructure($directive)"
}

internal class ConstantFormatStructure<in T>(
    val string: String
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "ConstantFormatStructure($string)"
}

// TODO: should itself also be a field with the default value "not negative"
internal class SignedFormatStructure<in T>(
    val format: FormatStructure<T>,
    val plusSignRequired: Boolean,
) : NonConcatenatedFormatStructure<T> {

    internal val fields = basicFormats(format).mapNotNull(FieldFormatDirective<T>::signGetter).toSet()

    init {
        require(fields.isNotEmpty()) { "Signed format must contain at least one field with a negative sign" }
    }

    override fun toString(): String = "SignedFormatStructure($format)"
}

internal class AlternativesFormatStructure<in T>(
    val formats: List<ConcatenatedFormatStructure<T>>
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "AlternativesFormatStructure(${formats.joinToString(", ")})"
}

internal sealed interface NonConcatenatedFormatStructure<in T> : FormatStructure<T>

internal class ConcatenatedFormatStructure<in T>(
    val formats: List<NonConcatenatedFormatStructure<T>>
) : FormatStructure<T> {
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
                    val sign = check(value)
                    if (sign > 0) return false
                    if (sign < 0) seenNonZero = true
                }
                return seenNonZero
            }
            SignedFormatter(
                innerFormat,
                ::checkIfAllNegative,
                plusSignRequired
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

internal fun <T> ConcatenatedFormatStructure<T>.parser(): ParserStructure<T> {
    fun ParserStructure<T>.append(other: ParserStructure<T>): ParserStructure<T> = if (followedBy.isEmpty()) {
        ParserStructure(operations + other.operations, other.followedBy)
    } else {
        ParserStructure(operations, followedBy.map { it.append(other) })
    }

    fun FormatStructure<T>.rec(signsInverted: Boolean): ParserStructure<T> = when (this) {
        is ConstantFormatStructure -> ParserStructure(listOf(PlainStringParserOperation(string)), emptyList())
        is SignedFormatStructure -> {
            ParserStructure(
                emptyList(),
                listOf(
                    format.rec(signsInverted = signsInverted).let {
                        if (!plusSignRequired) it else
                            ParserStructure<T>(
                                listOf(PlainStringParserOperation("+")),
                                emptyList()
                            ).append(it)
                    },
                    ParserStructure<T>(
                        listOf(PlainStringParserOperation("-")),
                        emptyList()
                    ).append(format.rec(signsInverted = !signsInverted))
                )
            )
        }

        is BasicFormatStructure -> directive.parser(signsInverted)
        is AlternativesFormatStructure ->
            ParserStructure(emptyList(), formats.map { it.rec(signsInverted) })

        is ConcatenatedFormatStructure -> {
            var accumulator = ParserStructure<T>(emptyList(), emptyList())
            for (format in formats.reversed()) {
                accumulator = format.rec(signsInverted).append(accumulator)
            }
            accumulator
        }
    }

    fun ParserStructure<T>.simplify(): ParserStructure<T> {
        val newOperations = mutableListOf<ParserOperation<T>>()
        var currentNumberSpan: MutableList<NumberConsumer<T>>? = null
        // joining together the number consumers in this parser before the first alternative
        for (op in operations) {
            if (op is NumberSpanParserOperation) {
                if (currentNumberSpan != null) {
                    currentNumberSpan.addAll(op.consumers)
                } else {
                    currentNumberSpan = op.consumers.toMutableList()
                }
            } else {
                if (currentNumberSpan != null) {
                    newOperations.add(NumberSpanParserOperation(currentNumberSpan))
                    currentNumberSpan = null
                }
                newOperations.add(op)
            }
        }
        val simplifiedTails = followedBy.flatMap {
            val simplified = it.simplify()
            // parser `ParserStructure(emptyList(), p)` is equivalent to `p`,
            // unless `p` is empty. For example, ((a|b)|(c|d)) is equivalent to (a|b|c|d).
            // In that case, `ParserStructure(emptyList(), emptyList())` represents a parser that recognizes an empty
            // string. For example, (|a|b) is not equivalent to (a|b).
            if (simplified.operations.isEmpty())
                simplified.followedBy.ifEmpty { listOf(simplified) }
            else
                listOf(simplified)
        }
        return if (currentNumberSpan == null) {
            // the last operation was not a number span, or it was a number span that we are allowed to interrupt
            ParserStructure(newOperations, simplifiedTails)
        } else if (simplifiedTails.none {
                it.operations.firstOrNull()?.let { it is NumberSpanParserOperation } == true
            }) {
            // the last operation was a number span, but there are no alternatives that start with a number span.
            newOperations.add(NumberSpanParserOperation(currentNumberSpan))
            ParserStructure(newOperations, simplifiedTails)
        } else {
            val newTails = simplifiedTails.map {
                when (val firstOperation = it.operations.firstOrNull()) {
                    is NumberSpanParserOperation -> {
                        ParserStructure(
                            listOf(NumberSpanParserOperation(currentNumberSpan + firstOperation.consumers)) + it.operations.drop(
                                1
                            ),
                            it.followedBy
                        )
                    }
                    null -> ParserStructure(
                        listOf(NumberSpanParserOperation(currentNumberSpan)),
                        it.followedBy
                    )
                    else -> ParserStructure(
                        listOf(NumberSpanParserOperation(currentNumberSpan)) + it.operations,
                        it.followedBy
                    )
                }
            }
            ParserStructure(newOperations, newTails)
        }
    }
    val initialParser = rec(signsInverted = false)
    val simplifiedParser = initialParser.simplify()
    return simplifiedParser
}

internal class Format<T>(private val directives: ConcatenatedFormatStructure<T>) {
    val formatter: FormatterStructure<T> by lazy {
        directives.formatter()
    }
    val parser: ParserStructure<T> by lazy {
        directives.parser()
    }
}

private fun <T> basicFormats(format: FormatStructure<T>): List<FieldFormatDirective<T>> = when (format) {
    is BasicFormatStructure -> listOf(format.directive)
    is ConcatenatedFormatStructure -> format.formats.flatMap { basicFormats(it) }
    is AlternativesFormatStructure -> format.formats.flatMap { basicFormats(it) }
    is ConstantFormatStructure -> emptyList()
    is SignedFormatStructure -> basicFormats(format.format)
}
