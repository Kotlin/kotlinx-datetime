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

    override fun equals(other: Any?): Boolean = other is BasicFormatStructure<*> && directive == other.directive
    override fun hashCode(): Int = directive.hashCode()
}

internal class ConstantFormatStructure<in T>(
    val string: String
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "ConstantFormatStructure($string)"

    override fun equals(other: Any?): Boolean = other is ConstantFormatStructure<*> && string == other.string
    override fun hashCode(): Int = string.hashCode()
}

internal class SignedFormatStructure<in T>(
    val format: FormatStructure<T>,
    val withPlusSign: Boolean,
) : NonConcatenatedFormatStructure<T> {

    internal val fieldSigns = basicFormats(format).mapNotNull { it.field.sign }.toSet()

    init {
        require(fieldSigns.isNotEmpty()) { "Signed format must contain at least one field with a sign" }
    }

    override fun toString(): String = "SignedFormatStructure($format)"

    override fun equals(other: Any?): Boolean =
        other is SignedFormatStructure<*> && format == other.format && withPlusSign == other.withPlusSign
    override fun hashCode(): Int = 31 * format.hashCode() + withPlusSign.hashCode()
}

internal class AlternativesParsingFormatStructure<in T>(
    val mainFormat: FormatStructure<T>,
    val formats: List<FormatStructure<T>>,
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "AlternativesParsing($formats)"

    override fun equals(other: Any?): Boolean =
        other is AlternativesParsingFormatStructure<*> && mainFormat == other.mainFormat && formats == other.formats
    override fun hashCode(): Int = 31 * mainFormat.hashCode() + formats.hashCode()
}

internal class OptionalFormatStructure<in T>(
    val onZero: String,
    val format: FormatStructure<T>,
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "Optional($onZero, $format)"

    override fun equals(other: Any?): Boolean =
        other is OptionalFormatStructure<*> && onZero == other.onZero && format == other.format

    override fun hashCode(): Int = 31 * onZero.hashCode() + format.hashCode()
}

internal sealed interface NonConcatenatedFormatStructure<in T> : FormatStructure<T>

internal class ConcatenatedFormatStructure<in T>(
    val formats: List<NonConcatenatedFormatStructure<T>>
) : FormatStructure<T> {
    override fun toString(): String = "ConcatenatedFormatStructure(${formats.joinToString(", ")})"

    override fun equals(other: Any?): Boolean = other is ConcatenatedFormatStructure<*> && formats == other.formats
    override fun hashCode(): Int {
        return formats.hashCode()
    }
}

internal fun <T> FormatStructure<T>.formatter(): FormatterStructure<T> {
    fun FormatStructure<T>.rec(): Pair<FormatterStructure<T>, Set<FieldSpec<T, *>>> = when (this) {
        is BasicFormatStructure -> directive.formatter() to setOf(directive.field)
        is ConstantFormatStructure -> ConstantStringFormatterStructure<T>(string) to emptySet()
        is SignedFormatStructure -> {
            val (innerFormat, fieldSpecs) = format.rec()
            fun checkIfAllNegative(value: T): Boolean {
                var seenNonZero = false
                for (check in fieldSigns) {
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

        is AlternativesParsingFormatStructure -> mainFormat.rec()
        is OptionalFormatStructure -> {
            val (formatter, fields) = format.rec()
            val predicate = conjunctionPredicate(fields.map {
                it.toComparisonPredicate() ?: throw IllegalArgumentException(
                    "The field '${it.name}' does not define a default value, and only fields that define a default value can" +
                        "be used in an 'optional' format."
                )
            })
            ConditionalFormatter(
                listOf(
                    predicate::test to ConstantStringFormatterStructure(onZero),
                    Truth::test to formatter
                )
            ) to fields
        }

        is ConcatenatedFormatStructure -> {
            val (formatters, fields) = formats.map { it.rec() }.unzip()
            if (formatters.size == 1) {
                formatters.single()
            } else {
                ConcatenatedFormatter(formatters)
            } to
                fields.flatten().toSet()
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
        listOf(
            ParserStructure(
                operations = listOf(
                    SignParser(
                        isNegativeSetter = { value, isNegative ->
                            for (field in fieldSigns) {
                                val wasNegative = field.isNegative.get(value) == true
                                // TODO: replacing `!=` with `xor` fails on JS
                                field.isNegative.set(value, isNegative != wasNegative)
                            }
                        },
                        withPlusSign = withPlusSign,
                        whatThisExpects = "sign for ${this.fieldSigns}"
                    )
                ),
                emptyList()
            ), format.parser()
        ).concat()
    }

    is BasicFormatStructure -> directive.parser()

    is ConcatenatedFormatStructure -> formats.map { it.parser() }.concat()
    is AlternativesParsingFormatStructure -> {
        ParserStructure(operations = emptyList(), followedBy = buildList {
            add(mainFormat.parser())
            for (format in formats) {
                add(format.parser())
            }
        })
    }

    is OptionalFormatStructure -> ParserStructure(
        operations = emptyList(),
        followedBy = if (onZero.isNotEmpty()) {
            listOf(format.parser(), ConstantFormatStructure<T>(onZero).parser())
        } else {
            listOf(format.parser(), ParserStructure(operations = emptyList(), followedBy = emptyList()))
        }
    )
}

internal class StringFormat<in T>(internal val directives: ConcatenatedFormatStructure<T>) {
    val formatter: FormatterStructure<T> by lazy {
        directives.formatter()
    }
    val parser: ParserStructure<T> by lazy {
        directives.parser()
    }

    override fun toString(): String = directives.toString()
}

private fun <T> basicFormats(format: FormatStructure<T>): List<FieldFormatDirective<T>> = buildList {
    fun rec(format: FormatStructure<T>) {
        when (format) {
            is BasicFormatStructure -> add(format.directive)
            is ConcatenatedFormatStructure -> format.formats.forEach { rec(it) }
            is ConstantFormatStructure -> {}
            is SignedFormatStructure -> rec(format.format)
            is AlternativesParsingFormatStructure -> {
                rec(format.mainFormat); format.formats.forEach { rec(it) }
            }

            is OptionalFormatStructure -> rec(format.format)
        }
    }
    rec(format)
}
