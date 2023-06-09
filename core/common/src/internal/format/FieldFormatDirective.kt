/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

import kotlinx.datetime.internal.*
import kotlinx.datetime.internal.format.formatter.*
import kotlinx.datetime.internal.format.parser.*

/**
 * A directive that specifies a way to parse and format the [field].
 */
internal interface FieldFormatDirective<in Target> {
    /**
     * The field parsed and formatted by this directive.
     */
    val field: FieldSpec<Target, *>

    /**
     * The formatter operation that formats the field.
     */
    fun formatter(): FormatterOperation<Target>

    /**
     * The parser structure that parses the field.
     */
    fun parser(): ParserStructure<Target>

    /**
     * The string with the code that, when evaluated in the builder context, appends the directive.
     */
    val builderRepresentation: String
}

/**
 * A directive for a decimal format of an integer field that is known to be unsigned.
 * The field is formatted with the field padded to [minDigits] with zeroes,
 * and the parser expects the field to be at least [minDigits] digits long.
 */
internal abstract class UnsignedIntFieldFormatDirective<in Target>(
    final override val field: UnsignedFieldSpec<Target>,
    private val minDigits: Int,
) : FieldFormatDirective<Target> {

    private val maxDigits: Int = field.maxDigits

    init {
        require(minDigits >= 0)
        require(maxDigits >= minDigits) {
            "The maximum number of digits ($maxDigits) is less than the minimum number of digits ($minDigits)"
        }
    }

    override fun formatter(): FormatterOperation<Target> =
        UnsignedIntFormatterOperation(
            number = field::getNotNull,
            zeroPadding = minDigits,
        )

    override fun parser(): ParserStructure<Target> =
        ParserStructure(
            listOf(
                NumberSpanParserOperation(
                    listOf(
                        UnsignedIntConsumer(
                            minDigits,
                            maxDigits,
                            field::setWithoutReassigning,
                            field.name,
                        )
                    )
                )
            ),
            emptyList()
        )
}

/**
 * A directive for a string-based format of an integer field that is known to be unsigned.
 */
internal abstract class NamedUnsignedIntFieldFormatDirective<in Target>(
    final override val field: UnsignedFieldSpec<Target>,
    private val values: List<String>,
) : FieldFormatDirective<Target> {

    init {
        require(values.size == field.maxValue - field.minValue + 1) {
            "The number of values (${values.size}) in $values does not match the range of the field (${field.maxValue - field.minValue + 1})"
        }
    }

    private fun getStringValue(target: Target): String = values[field.getNotNull(target) - field.minValue]

    private fun setStringValue(target: Target, value: String) {
        field.setWithoutReassigning(target, values.indexOf(value) + field.minValue)
    }

    override fun formatter(): FormatterOperation<Target> =
        StringFormatterOperation(::getStringValue)

    override fun parser(): ParserStructure<Target> =
        ParserStructure(listOf(
            StringSetParserOperation(values, ::setStringValue, "One of $values for ${field.name}")
        ), emptyList())
}

/**
 * A directive for a string-based format of an enum field.
 */
internal abstract class NamedEnumIntFieldFormatDirective<in Target, Type>(
    final override val field: FieldSpec<Target, Type>,
    private val mapping: Map<Type, String>,
) : FieldFormatDirective<Target> {

    private val reverseMapping = mapping.entries.associate { it.value to it.key }

    private fun getStringValue(target: Target): String = mapping[field.getNotNull(target)]
        ?: throw IllegalStateException(
            "The value ${field.getNotNull(target)} is does not have a corresponding string representation"
        )

    private fun setStringValue(target: Target, value: String) {
        field.setWithoutReassigning(target, reverseMapping[value]
            ?: throw IllegalStateException("The string value $value does not have a corresponding enum value"))
    }

    override fun formatter(): FormatterOperation<Target> =
        StringFormatterOperation(::getStringValue)

    override fun parser(): ParserStructure<Target> =
        ParserStructure(listOf(
            StringSetParserOperation(mapping.values, ::setStringValue, "One of ${mapping.values} for ${field.name}")
        ), emptyList())
}

internal abstract class StringFieldFormatDirective<in Target>(
    final override val field: FieldSpec<Target, String>,
    private val acceptedStrings: Set<String>,
) : FieldFormatDirective<Target> {

    init {
        require(acceptedStrings.isNotEmpty())
    }

    override fun formatter(): FormatterOperation<Target> =
        StringFormatterOperation(field::getNotNull)

    override fun parser(): ParserStructure<Target> =
        ParserStructure(
            listOf(StringSetParserOperation(acceptedStrings, field::setWithoutReassigning, field.name)),
            emptyList()
        )
}

internal abstract class SignedIntFieldFormatDirective<in Target>(
    final override val field: SignedFieldSpec<Target>,
    private val minDigits: Int?,
    private val maxDigits: Int? = field.maxDigits,
    private val outputPlusOnExceededPadding: Boolean = false,
) : FieldFormatDirective<Target> {

    init {
        require(minDigits == null || minDigits >= 0)
        require(maxDigits == null || minDigits == null || maxDigits >= minDigits)
    }

    override fun formatter(): FormatterOperation<Target> =
        SignedIntFormatterOperation(
            number = field::getNotNull,
            zeroPadding = minDigits ?: 0,
            outputPlusOnExceedsPad = outputPlusOnExceededPadding,
        )

    override fun parser(): ParserStructure<Target> =
        SignedIntParser(
            minDigits = minDigits,
            maxDigits = maxDigits,
            field::setWithoutReassigning,
            field.name,
            plusOnExceedsPad = outputPlusOnExceededPadding,
        )
}

internal abstract class DecimalFractionFieldFormatDirective<in Target>(
    final override val field: FieldSpec<Target, DecimalFraction>,
    private val minDigits: Int?,
    private val maxDigits: Int?,
) : FieldFormatDirective<Target> {
    override fun formatter(): FormatterOperation<Target> =
        DecimalFractionFormatterOperation(field::getNotNull, minDigits, maxDigits)

    override fun parser(): ParserStructure<Target> = ParserStructure(
        listOf(
            NumberSpanParserOperation(
                listOf(FractionPartConsumer(minDigits, maxDigits, field::setWithoutReassigning, field.name))
            )
        ),
        emptyList()
    )
}
