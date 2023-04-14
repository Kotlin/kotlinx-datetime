/*
 * Copyright 2019-2023 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime.internal.format

import kotlin.reflect.*

private typealias Accessor<Object, Field> = KMutableProperty1<Object, Field?>

internal interface FieldSign<in Target> {
    /**
     * The field that is `true` if the value of the field is known to be negative, and `false` otherwise.
     */
    val isNegative: Accessor<in Target, Boolean>
    fun isZero(obj: Target): Boolean
}

/**
 * A specification of a field.
 *
 * Fields represent parts of objects, regardless of their representation.
 * For example, a field "day of week" can be represented with both strings of various kinds ("Monday", "Mon") and
 * numbers ("1" for Monday, "2" for Tuesday, etc.), but the field itself is the same.
 *
 * Fields can typically contain `null` values, which means that the field is not set.
 */
internal interface FieldSpec<in Target, Type> {
    /**
     * The function with which the field can be accessed.
     */
    val accessor: Accessor<in Target, Type>

    /**
     * The default value of the field, or `null` if the field has none.
     */
    val defaultValue: Type?

    /**
     * The name of the field.
     */
    val name: String

    /**
     * The sign corresponding to the field value, or `null` if the field has none.
     */
    val sign: FieldSign<Target>?
}

internal abstract class AbstractFieldSpec<in Target, Type>: FieldSpec<Target, Type> {
    override fun toString(): String = "The field $name (default value is $defaultValue)"
}

/**
 * Function that returns the value of the field in the given object,
 * or throws [IllegalStateException] if the field is not set.
 *
 * This function is used to access fields during formatting.
 */
internal fun <Object, Field> FieldSpec<Object, Field>.getNotNull(obj: Object): Field =
    accessor.get(obj) ?: throw IllegalStateException("Field $name is not set")

/**
 * If the field is not set, sets it to the given value.
 * If the field is set to the given value, does nothing.
 * If the field is set to a different value, throws [IllegalArgumentException].
 *
 * This function is used to ensure internal consistency during parsing.
 * There exist formats where the same data is repeated several times in the same object, for example,
 * "14:15 (02:15 PM)". In such cases, we want to ensure that the values are consistent.
 */
internal fun <Object, Field> FieldSpec<Object, Field>.setWithoutReassigning(obj: Object, value: Field) {
    val oldValue = accessor.get(obj)
    if (oldValue != null) {
        require(oldValue == value) {
            "Attempting to assign conflicting values '$oldValue' and '$value' to field '$name'"
        }
    } else {
        accessor.set(obj, value)
    }
}

/**
 * A specification of a field that can contain values of any kind.
 * Used for fields additional information about which is not that important for parsing/formatting.
 */
internal class GenericFieldSpec<in Target, Type>(
    override val accessor: Accessor<in Target, Type>,
    override val name: String = accessor.name,
    override val defaultValue: Type? = null,
    override val sign: FieldSign<Target>? = null,
) : AbstractFieldSpec<Target, Type>()

/**
 * A specification of a field that can only contain non-negative values.
 */
internal class UnsignedFieldSpec<in Target>(
    override val accessor: Accessor<in Target, Int>,
    /**
     * The minimum value of the field.
     */
    val minValue: Int,
    /**
     * The maximum value of the field.
     */
    val maxValue: Int,
    override val name: String = accessor.name,
    override val defaultValue: Int? = null,
    override val sign: FieldSign<Target>? = null,
) : AbstractFieldSpec<Target, Int>() {
    /**
     * The maximum length of the field when represented as a decimal number.
     */
    val maxDigits: Int = when {
        maxValue < 10 -> 1
        maxValue < 100 -> 2
        maxValue < 1000 -> 3
        else -> throw IllegalArgumentException("Max value $maxValue is too large")
    }
}

internal class SignedFieldSpec<in Target>(
    override val accessor: Accessor<in Target, Int>,
    val maxAbsoluteValue: Int?,
    override val name: String = accessor.name,
    override val defaultValue: Int? = null,
    override val sign: FieldSign<Target>? = null,
) : AbstractFieldSpec<Target, Int>() {
    val maxDigits: Int? = when {
        maxAbsoluteValue == null -> null
        maxAbsoluteValue < 10 -> 1
        maxAbsoluteValue < 100 -> 2
        maxAbsoluteValue < 1000 -> 3
        maxAbsoluteValue < 10000 -> 4
        maxAbsoluteValue < 100000 -> 5
        maxAbsoluteValue < 1000000 -> 6
        maxAbsoluteValue < 10000000 -> 7
        maxAbsoluteValue < 100000000 -> 8
        maxAbsoluteValue < 1000000000 -> 9
        else -> throw IllegalArgumentException("Max value $maxAbsoluteValue is too large")
    }
}
