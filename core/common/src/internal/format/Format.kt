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
    val withPlusSign: Boolean,
) : NonConcatenatedFormatStructure<T> {

    internal val fields = basicFormats(format).mapNotNull { it.field.sign }.toSet()

    init {
        require(fields.isNotEmpty()) { "Signed format must contain at least one field with a sign" }
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
                        field.isNegative.set(value, isNegative xor wasNegative)
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
}

private fun <T> basicFormats(format: FormatStructure<T>): List<FieldFormatDirective<T>> = when (format) {
    is BasicFormatStructure -> listOf(format.directive)
    is ConcatenatedFormatStructure -> format.formats.flatMap { basicFormats(it) }
    is AlternativesFormatStructure -> format.formats.flatMap { basicFormats(it) }
    is ConstantFormatStructure -> emptyList()
    is SignedFormatStructure -> basicFormats(format.format)
}
