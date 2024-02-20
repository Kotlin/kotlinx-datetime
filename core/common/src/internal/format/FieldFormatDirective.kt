/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

import kotlinx.datetime.internal.DecimalFraction
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
    fun formatter(): FormatterStructure<Target>

    /**
     * The parser structure that parses the field.
     */
    fun parser(): ParserStructure<Target>
}

/**
 * A directive for a decimal format of an integer field that is known to be unsigned.
 * The field is formatted with the field padded to [minDigits] with zeroes,
 * and the parser expects the field to be at least [minDigits] digits long.
 */
internal abstract class UnsignedIntFieldFormatDirective<in Target>(
    final override val field: UnsignedFieldSpec<Target>,
    private val minDigits: Int,
    private val spacePadding: Int?,
) : FieldFormatDirective<Target> {

    private val maxDigits: Int = field.maxDigits

    init {
        require(minDigits >= 0) {
            "The minimum number of digits ($minDigits) is negative"
        }
        require(maxDigits >= minDigits) {
            "The maximum number of digits ($maxDigits) is less than the minimum number of digits ($minDigits)"
        }
        if (spacePadding != null) {
            require(spacePadding > minDigits) {
                "The space padding ($spacePadding) should be more than the minimum number of digits ($minDigits)"
            }
        }
    }

    override fun formatter(): FormatterStructure<Target> {
        val formatter = UnsignedIntFormatterStructure(
            number = field.accessor::getterNotNull,
            zeroPadding = minDigits,
        )
        return if (spacePadding != null) SpacePaddedFormatter(formatter, spacePadding) else formatter
    }

    override fun parser(): ParserStructure<Target> =
        spaceAndZeroPaddedUnsignedInt(minDigits, maxDigits, spacePadding, field.accessor, field.name)
}

/**
 * A directive for a string-based format of an integer field that is known to be unsigned.
 */
internal abstract class NamedUnsignedIntFieldFormatDirective<in Target>(
    final override val field: UnsignedFieldSpec<Target>,
    private val values: List<String>,
    private val name: String,
) : FieldFormatDirective<Target> {

    init {
        require(values.size == field.maxValue - field.minValue + 1) {
            "The number of values (${values.size}) in $values does not match the range of the field (${field.maxValue - field.minValue + 1})"
        }
    }

    private fun getStringValue(target: Target): String = field.accessor.getterNotNull(target).let {
        values.getOrNull(it-field.minValue)
            ?: "The value $it of ${field.name} does not have a corresponding string representation"
    }

    private inner class AssignableString: AssignableField<Target, String> {
        override fun trySetWithoutReassigning(container: Target, newValue: String): String? =
            field.accessor.trySetWithoutReassigning(container, values.indexOf(newValue) + field.minValue)?.let {
                values[it - field.minValue]
            }

        override val name: String get() = this@NamedUnsignedIntFieldFormatDirective.name
    }

    override fun formatter(): FormatterStructure<Target> =
        StringFormatterStructure(::getStringValue)

    override fun parser(): ParserStructure<Target> =
        ParserStructure(
            listOf(
                StringSetParserOperation(values, AssignableString(), "One of $values for $name")
            ), emptyList()
        )
}

/**
 * A directive for a string-based format of an enum field.
 */
internal abstract class NamedEnumIntFieldFormatDirective<in Target, Type>(
    final override val field: FieldSpec<Target, Type>,
    private val mapping: Map<Type, String>,
    private val name: String,
) : FieldFormatDirective<Target> {

    private val reverseMapping = mapping.entries.associate { it.value to it.key }

    private fun getStringValue(target: Target): String = field.accessor.getterNotNull(target).let {
        mapping[field.accessor.getterNotNull(target)]
            ?: "The value $it of ${field.name} does not have a corresponding string representation"
    }

    private inner class AssignableString: AssignableField<Target, String> {
        override fun trySetWithoutReassigning(container: Target, newValue: String): String? =
            field.accessor.trySetWithoutReassigning(container, reverseMapping[newValue]!!)?.let { mapping[it] }

        override val name: String get() = this@NamedEnumIntFieldFormatDirective.name
    }

    override fun formatter(): FormatterStructure<Target> =
        StringFormatterStructure(::getStringValue)

    override fun parser(): ParserStructure<Target> =
        ParserStructure(
            listOf(
                StringSetParserOperation(mapping.values, AssignableString(), "One of ${mapping.values} for $name")
            ), emptyList()
        )
}

internal abstract class StringFieldFormatDirective<in Target>(
    final override val field: FieldSpec<Target, String>,
    private val acceptedStrings: Set<String>,
) : FieldFormatDirective<Target> {

    init {
        require(acceptedStrings.isNotEmpty()) {
            "The set of accepted strings is empty"
        }
    }

    override fun formatter(): FormatterStructure<Target> =
        StringFormatterStructure(field.accessor::getterNotNull)

    override fun parser(): ParserStructure<Target> =
        ParserStructure(
            listOf(StringSetParserOperation(acceptedStrings, field.accessor, field.name)),
            emptyList()
        )
}

internal abstract class SignedIntFieldFormatDirective<in Target>(
    final override val field: FieldSpec<Target, Int>,
    private val minDigits: Int?,
    private val maxDigits: Int?,
    private val spacePadding: Int?,
    private val outputPlusOnExceededWidth: Int?,
) : FieldFormatDirective<Target> {

    init {
        require(minDigits == null || minDigits >= 0) { "The minimum number of digits ($minDigits) is negative" }
        require(maxDigits == null || minDigits == null || maxDigits >= minDigits) {
            "The maximum number of digits ($maxDigits) is less than the minimum number of digits ($minDigits)"
        }
    }

    override fun formatter(): FormatterStructure<Target> {
        val formatter = SignedIntFormatterStructure(
            number = field.accessor::getterNotNull,
            zeroPadding = minDigits ?: 0,
            outputPlusOnExceededWidth = outputPlusOnExceededWidth,
        )
        return if (spacePadding != null) SpacePaddedFormatter(formatter, spacePadding) else formatter
    }

    override fun parser(): ParserStructure<Target> =
        SignedIntParser(
            minDigits = minDigits,
            maxDigits = maxDigits,
            spacePadding = spacePadding,
            field.accessor,
            field.name,
            plusOnExceedsWidth = outputPlusOnExceededWidth,
        )
}

internal abstract class DecimalFractionFieldFormatDirective<in Target>(
    final override val field: FieldSpec<Target, DecimalFraction>,
    private val minDigits: Int,
    private val maxDigits: Int,
    private val zerosToAdd: List<Int>,
) : FieldFormatDirective<Target> {
    override fun formatter(): FormatterStructure<Target> =
        DecimalFractionFormatterStructure(field.accessor::getterNotNull, minDigits, maxDigits, zerosToAdd)

    override fun parser(): ParserStructure<Target> = ParserStructure(
        listOf(
            NumberSpanParserOperation(
                listOf(FractionPartConsumer(minDigits, maxDigits, field.accessor, field.name))
            )
        ),
        emptyList()
    )
}

internal abstract class ReducedIntFieldDirective<in Target>(
    final override val field: FieldSpec<Target, Int>,
    private val digits: Int,
    private val base: Int,
) : FieldFormatDirective<Target> {

    override fun formatter(): FormatterStructure<Target> =
        ReducedIntFormatterStructure(
            number = field.accessor::getterNotNull,
            digits = digits,
            base = base,
        )

    override fun parser(): ParserStructure<Target> =
        ReducedIntParser(digits = digits, base = base, field.accessor, field.name)
}
