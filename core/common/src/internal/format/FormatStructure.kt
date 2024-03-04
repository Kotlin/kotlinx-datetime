/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

import kotlinx.datetime.internal.format.formatter.*
import kotlinx.datetime.internal.format.parser.*

internal sealed interface FormatStructure<in T> {
    fun parser(): ParserStructure<T>
    fun formatter(): FormatterStructure<T>
}

internal class BasicFormatStructure<in T>(
    val directive: FieldFormatDirective<T>
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "BasicFormatStructure($directive)"

    override fun equals(other: Any?): Boolean = other is BasicFormatStructure<*> && directive == other.directive
    override fun hashCode(): Int = directive.hashCode()

    override fun parser(): ParserStructure<T> = directive.parser()
    override fun formatter(): FormatterStructure<T> = directive.formatter()
}

internal class ConstantFormatStructure<in T>(
    val string: String
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "ConstantFormatStructure($string)"

    override fun equals(other: Any?): Boolean = other is ConstantFormatStructure<*> && string == other.string
    override fun hashCode(): Int = string.hashCode()

    override fun parser(): ParserStructure<T> = ParserStructure(
        operations = when {
            string.isEmpty() -> emptyList()
            else -> buildList {
                val suffix = if (string[0].isDigit()) {
                    add(NumberSpanParserOperation(listOf(ConstantNumberConsumer(string.takeWhile { it.isDigit() }))))
                    string.dropWhile { it.isDigit() }
                } else {
                    string
                }
                if (suffix.isNotEmpty()) {
                    if (suffix[suffix.length - 1].isDigit()) {
                        add(PlainStringParserOperation(suffix.dropLastWhile { it.isDigit() }))
                        add(NumberSpanParserOperation(listOf(ConstantNumberConsumer(suffix.takeLastWhile { it.isDigit() }))))
                    } else {
                        add(PlainStringParserOperation(suffix))
                    }
                }
            }
        },
        followedBy = emptyList()
    )

    override fun formatter(): FormatterStructure<T> = ConstantStringFormatterStructure(string)
}

internal class SignedFormatStructure<in T>(
    val format: FormatStructure<T>,
    val withPlusSign: Boolean,
) : NonConcatenatedFormatStructure<T> {

    private val fieldSigns = basicFormats(format).mapNotNull { it.field.sign }.toSet()

    init {
        require(fieldSigns.isNotEmpty()) { "Signed format must contain at least one field with a sign" }
    }

    override fun toString(): String = "SignedFormatStructure($format)"

    override fun equals(other: Any?): Boolean =
        other is SignedFormatStructure<*> && format == other.format && withPlusSign == other.withPlusSign

    override fun hashCode(): Int = 31 * format.hashCode() + withPlusSign.hashCode()

    override fun parser(): ParserStructure<T> = listOf(
        ParserStructure(
            operations = listOf(
                SignParser(
                    isNegativeSetter = { value, isNegative ->
                        for (field in fieldSigns) {
                            val wasNegative = field.isNegative.getter(value) == true
                            // TODO: replacing `!=` with `xor` fails on JS
                            field.isNegative.trySetWithoutReassigning(value, isNegative != wasNegative)
                        }
                    },
                    withPlusSign = withPlusSign,
                    whatThisExpects = "sign for ${this.fieldSigns}"
                )
            ),
            emptyList()
        ), format.parser()
    ).concat()

    override fun formatter(): FormatterStructure<T> {
        val innerFormat = format.formatter()
        fun checkIfAllNegative(value: T): Boolean {
            var seenNonZero = false
            for (check in fieldSigns) {
                when {
                    check.isNegative.getter(value) == true -> seenNonZero = true
                    check.isZero(value) -> continue
                    else -> return false
                }
            }
            return seenNonZero
        }
        return SignedFormatter(
            innerFormat,
            ::checkIfAllNegative,
            withPlusSign
        )
    }
}

internal class AlternativesParsingFormatStructure<in T>(
    val mainFormat: FormatStructure<T>,
    val formats: List<FormatStructure<T>>,
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "AlternativesParsing($formats)"

    override fun equals(other: Any?): Boolean =
        other is AlternativesParsingFormatStructure<*> && mainFormat == other.mainFormat && formats == other.formats

    override fun hashCode(): Int = 31 * mainFormat.hashCode() + formats.hashCode()

    override fun parser(): ParserStructure<T> = ParserStructure(operations = emptyList(), followedBy = buildList {
        add(mainFormat.parser())
        for (format in formats) {
            add(format.parser())
        }
    })

    override fun formatter(): FormatterStructure<T> = mainFormat.formatter()
}

internal class OptionalFormatStructure<in T>(
    val onZero: String,
    val format: FormatStructure<T>,
) : NonConcatenatedFormatStructure<T> {
    override fun toString(): String = "Optional($onZero, $format)"

    private val fields = basicFormats(format).map { it.field }.distinct().map { field ->
        PropertyWithDefault.fromField(field)
    }

    override fun equals(other: Any?): Boolean =
        other is OptionalFormatStructure<*> && onZero == other.onZero && format == other.format

    override fun hashCode(): Int = 31 * onZero.hashCode() + format.hashCode()

    override fun parser(): ParserStructure<T> = ParserStructure(
        operations = emptyList(),
        followedBy = listOf(
            format.parser(),
            listOf(
                ConstantFormatStructure<T>(onZero).parser(),
                ParserStructure(
                    listOf(
                        UnconditionalModification {
                            for (field in fields) {
                                field.assignDefault(it)
                            }
                        }
                    ),
                    emptyList()
                )
            ).concat()
        )
    )

    override fun formatter(): FormatterStructure<T> {
        val formatter = format.formatter()
        val predicate = conjunctionPredicate(fields.map { it.isDefaultComparisonPredicate() })
        return ConditionalFormatter(
            listOf(
                predicate::test to ConstantStringFormatterStructure(onZero),
                Truth::test to formatter
            )
        )
    }

    private class PropertyWithDefault<in T, E> private constructor(
        private val accessor: Accessor<T, E>,
        private val defaultValue: E
    ) {
        companion object {
            fun <T, E> fromField(field: FieldSpec<T, E>): PropertyWithDefault<T, E> {
                val default = field.defaultValue
                require(default != null) {
                    "The field '${field.name}' does not define a default value"
                }
                return PropertyWithDefault(field.accessor, default)
            }
        }

        inline fun assignDefault(target: T) {
            accessor.trySetWithoutReassigning(target, defaultValue)
        }

        inline fun isDefaultComparisonPredicate() = ComparisonPredicate(defaultValue, accessor::getter)
    }
}

internal sealed interface NonConcatenatedFormatStructure<in T> : FormatStructure<T>

internal open class ConcatenatedFormatStructure<in T>(
    val formats: List<NonConcatenatedFormatStructure<T>>
) : FormatStructure<T> {
    override fun toString(): String = "ConcatenatedFormatStructure(${formats.joinToString(", ")})"

    override fun equals(other: Any?): Boolean = other is ConcatenatedFormatStructure<*> && formats == other.formats

    override fun hashCode(): Int = formats.hashCode()

    override fun parser(): ParserStructure<T> = formats.map { it.parser() }.concat()

    override fun formatter(): FormatterStructure<T> {
        val formatters = formats.map { it.formatter() }
        return if (formatters.size == 1) {
            formatters.single()
        } else {
            ConcatenatedFormatter(formatters)
        }
    }
}

internal class CachedFormatStructure<in T>(formats: List<NonConcatenatedFormatStructure<T>>) :
    ConcatenatedFormatStructure<T>(formats) {
    private val cachedFormatter: FormatterStructure<T> = super.formatter()
    private val cachedParser: ParserStructure<T> = super.parser()

    override fun formatter(): FormatterStructure<T> = cachedFormatter

    override fun parser(): ParserStructure<T> = cachedParser
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
